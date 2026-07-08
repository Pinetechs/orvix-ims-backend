package com.pinetechs.orvix.ims.inventory.sparepart.service.impl;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.sparepart.dto.SparePartInventoryScanRequest;
import com.pinetechs.orvix.ims.inventory.sparepart.dto.SparePartInventoryScanResponse;
import com.pinetechs.orvix.ims.inventory.sparepart.entity.*;
import com.pinetechs.orvix.ims.inventory.sparepart.enums.*;
import com.pinetechs.orvix.ims.inventory.sparepart.repository.*;
import com.pinetechs.orvix.ims.inventory.sparepart.service.SparePartInventoryScanService;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class SparePartInventoryScanServiceImpl implements SparePartInventoryScanService {

    private final InventoryTaskRepository inventoryTaskRepository;
    private final SparePartInventoryItemRepository itemRepository;
    private final SparePartInventoryBranchRepository branchRepository;
    private final SparePartInventoryLocationRepository locationRepository;
    private final SparePartInventoryBranchAssignmentRepository branchAssignmentRepository;
    private final SparePartInventoryScanRepository scanRepository;

    public SparePartInventoryScanServiceImpl(
            InventoryTaskRepository inventoryTaskRepository,
            SparePartInventoryItemRepository itemRepository,
            SparePartInventoryBranchRepository branchRepository,
            SparePartInventoryLocationRepository locationRepository,
            SparePartInventoryBranchAssignmentRepository branchAssignmentRepository,
            SparePartInventoryScanRepository scanRepository
    ) {
        this.inventoryTaskRepository = inventoryTaskRepository;
        this.itemRepository = itemRepository;
        this.branchRepository = branchRepository;
        this.locationRepository = locationRepository;
        this.branchAssignmentRepository = branchAssignmentRepository;
        this.scanRepository = scanRepository;
    }

    @Override
    @Transactional
    public SparePartInventoryScanResponse scan(Long taskId, SparePartInventoryScanRequest request, User currentUser) {
        InventoryTask task = inventoryTaskRepository.findByIdForUpdate(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Inventory task not found"));

        if (task.getInventoryDomain() != InventoryDomain.SPARE_PART) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Task is not a spare part inventory task");
        }

        if (task.getStatus() != InventoryTaskStatus.READY_TO_START && task.getStatus() != InventoryTaskStatus.IN_PROGRESS) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Spare part scan is allowed only when task is READY_TO_START or IN_PROGRESS");
        }

        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        if (request == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Scan request is required");
        }

        String itemNo = normalize(request.getItemNo());
        if (itemNo == null || itemNo.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Item number is required");
        }

        if (request.getCountedQty() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Counted quantity is required");
        }

        if (request.getCountedQty().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Counted quantity cannot be negative");
        }

        SparePartInventoryBranch actualBranch = getTaskBranch(taskId, request.getBranchId());
        SparePartInventoryLocation actualLocation = getTaskLocation(taskId, actualBranch.getId(), request.getLocationId());

        boolean assigned = branchAssignmentRepository.existsActiveByTaskIdAndUserIdAndBranchId(
                taskId,
                currentUser.getId(),
                actualBranch.getId()
        );

        if (!assigned) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "User is not assigned to this spare part inventory branch");
        }

        SparePartInventoryItem exactItem = itemRepository
                .findByInventoryTaskIdAndItemNoAndPlannedBranchIdAndPlannedLocationId(
                        taskId,
                        itemNo,
                        actualBranch.getId(),
                        actualLocation.getId()
                )
                .orElse(null);

        SparePartInventoryItem item = exactItem != null ? exactItem : resolveMismatchCandidate(taskId, itemNo, actualBranch.getId());
        boolean locationMatched = exactItem != null;

        SparePartInventoryScan scan = new SparePartInventoryScan();
        scan.setInventoryTask(task);
        scan.setItem(item);
        scan.setScannedBy(currentUser);
        scan.setScannedItemNo(itemNo);
        scan.setActualBranch(actualBranch);
        scan.setActualLocation(actualLocation);
        scan.setCountedQty(request.getCountedQty());
        scan.setNotes(request.getNotes());

        if (item == null) {
            scan.setLocationStatus(SparePartInventoryLocationStatus.EXTRA_ITEM);
            scan.setQuantityStatus(SparePartInventoryQuantityStatus.NOT_APPLICABLE);
            scan.setScanResult(SparePartInventoryScanResult.EXTRA);
            scan.setMessage("Item number is not found in the uploaded spare part inventory file");
            SparePartInventoryScan savedScan = scanRepository.save(scan);
            return toResponse(savedScan, null, "Item number is not found in the uploaded spare part inventory file");
        }

        scan.setExpectedBranch(item.getPlannedBranch());
        scan.setExpectedLocation(item.getPlannedLocation());
        scan.setStockQty(item.getStockQty());

        BigDecimal stockQty = safe(item.getStockQty());
        BigDecimal countedQty = request.getCountedQty();
        BigDecimal varianceQty = countedQty.subtract(stockQty);

        SparePartInventoryLocationStatus locationStatus = locationMatched
                ? SparePartInventoryLocationStatus.CORRECT_LOCATION
                : SparePartInventoryLocationStatus.WRONG_LOCATION;
        SparePartInventoryQuantityStatus quantityStatus = resolveQuantityStatus(varianceQty);
        SparePartInventoryScanResult scanResult = resolveScanResult(locationMatched, varianceQty);
        SparePartInventoryItemStatus itemStatus = resolveItemStatus(locationMatched, varianceQty);

        SparePartInventoryItemStatus previousStatus = item.getStatus();
        SparePartInventoryBranch previousActualBranch = item.getActualBranch();
        SparePartInventoryLocation previousActualLocation = item.getActualLocation();
        boolean wasAlreadyCounted = previousStatus != null && previousStatus != SparePartInventoryItemStatus.NOT_COUNTED;
        if (wasAlreadyCounted) {
            boolean sameActual = sameId(previousActualBranch, actualBranch) && sameId(previousActualLocation, actualLocation);
            scanResult = sameActual ? SparePartInventoryScanResult.RECOUNT_SAME_LOCATION : SparePartInventoryScanResult.RECOUNT_DIFFERENT_LOCATION;
        }

        item.setActualBranch(actualBranch);
        item.setActualLocation(actualLocation);
        item.setActualQty(countedQty);
        item.setVarianceQty(varianceQty);
        item.setStatus(itemStatus);
        item.setCountedBy(currentUser);
        item.setCountedAt(LocalDateTime.now());
        item.setNotes(request.getNotes());
        itemRepository.save(item);

        if (wasAlreadyCounted) {
            decrement(previousActualBranch, previousActualLocation, previousStatus);
            if (previousStatus == SparePartInventoryItemStatus.MATCHED) {
                task.setMatchedRecords(Math.max(0, safe(task.getMatchedRecords()) - 1));
            }
        } else {
            task.setProcessedRecords(safe(task.getProcessedRecords()) + 1);
        }

        increment(actualBranch, actualLocation, itemStatus);
        if (itemStatus == SparePartInventoryItemStatus.MATCHED) {
            task.setMatchedRecords(safe(task.getMatchedRecords()) + 1);
        }

        task.setStatus(InventoryTaskStatus.IN_PROGRESS);
        inventoryTaskRepository.save(task);

        String message = buildMessage(locationMatched, stockQty, countedQty, varianceQty, item, actualBranch, actualLocation, wasAlreadyCounted);
        scan.setLocationStatus(locationStatus);
        scan.setQuantityStatus(quantityStatus);
        scan.setScanResult(scanResult);
        scan.setVarianceQty(varianceQty);
        scan.setMessage(message);

        SparePartInventoryScan savedScan = scanRepository.save(scan);
        return toResponse(savedScan, item, message);
    }

    private SparePartInventoryItem resolveMismatchCandidate(Long taskId, String itemNo, Long actualBranchId) {
        List<SparePartInventoryItem> candidates = itemRepository.findByInventoryTaskIdAndItemNo(taskId, itemNo);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        return candidates.stream()
                .sorted(Comparator
                        .comparing((SparePartInventoryItem item) -> !sameId(item.getPlannedBranch(), actualBranchId))
                        .thenComparing(item -> item.getId() == null ? Long.MAX_VALUE : item.getId()))
                .findFirst()
                .orElse(null);
    }

    private SparePartInventoryBranch getTaskBranch(Long taskId, Long branchId) {
        if (branchId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Branch is required");
        }
        return branchRepository.findById(branchId)
                .filter(branch -> branch.getInventoryTask() != null && branch.getInventoryTask().getId().equals(taskId))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Branch does not belong to this spare part inventory task"));
    }

    private SparePartInventoryLocation getTaskLocation(Long taskId, Long branchId, Long locationId) {
        if (locationId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Location is required");
        }
        return locationRepository.findById(locationId)
                .filter(location -> location.getInventoryTask() != null && location.getInventoryTask().getId().equals(taskId))
                .filter(location -> location.getBranch() != null && location.getBranch().getId().equals(branchId))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Location does not belong to selected branch"));
    }

    private SparePartInventoryQuantityStatus resolveQuantityStatus(BigDecimal varianceQty) {
        int compare = varianceQty.compareTo(BigDecimal.ZERO);
        if (compare == 0) {
            return SparePartInventoryQuantityStatus.MATCHED_QTY;
        }
        if (compare < 0) {
            return SparePartInventoryQuantityStatus.SHORTAGE_QTY;
        }
        return SparePartInventoryQuantityStatus.OVERAGE_QTY;
    }

    private SparePartInventoryScanResult resolveScanResult(boolean locationMatched, BigDecimal varianceQty) {
        int compare = varianceQty.compareTo(BigDecimal.ZERO);
        if (locationMatched && compare == 0) {
            return SparePartInventoryScanResult.MATCHED;
        }
        if (locationMatched && compare < 0) {
            return SparePartInventoryScanResult.SHORTAGE;
        }
        if (locationMatched) {
            return SparePartInventoryScanResult.OVERAGE;
        }
        if (compare == 0) {
            return SparePartInventoryScanResult.LOCATION_MISMATCH;
        }
        if (compare < 0) {
            return SparePartInventoryScanResult.LOCATION_MISMATCH_WITH_SHORTAGE;
        }
        return SparePartInventoryScanResult.LOCATION_MISMATCH_WITH_OVERAGE;
    }

    private SparePartInventoryItemStatus resolveItemStatus(boolean locationMatched, BigDecimal varianceQty) {
        int compare = varianceQty.compareTo(BigDecimal.ZERO);
        if (locationMatched && compare == 0) {
            return SparePartInventoryItemStatus.MATCHED;
        }
        if (locationMatched && compare < 0) {
            return SparePartInventoryItemStatus.SHORTAGE;
        }
        if (locationMatched) {
            return SparePartInventoryItemStatus.OVERAGE;
        }
        if (compare == 0) {
            return SparePartInventoryItemStatus.LOCATION_MISMATCH;
        }
        if (compare < 0) {
            return SparePartInventoryItemStatus.LOCATION_MISMATCH_WITH_SHORTAGE;
        }
        return SparePartInventoryItemStatus.LOCATION_MISMATCH_WITH_OVERAGE;
    }

    private String buildMessage(
            boolean locationMatched,
            BigDecimal stockQty,
            BigDecimal countedQty,
            BigDecimal varianceQty,
            SparePartInventoryItem item,
            SparePartInventoryBranch actualBranch,
            SparePartInventoryLocation actualLocation,
            boolean recount
    ) {
        String prefix = recount ? "تم تحديث جرد هذه القطعة. " : "";
        String expected = getBranchName(item.getPlannedBranch()) + " / " + getLocationCode(item.getPlannedLocation());
        String actual = getBranchName(actualBranch) + " / " + getLocationCode(actualLocation);
        int compare = varianceQty.compareTo(BigDecimal.ZERO);

        if (locationMatched && compare == 0) {
            return prefix + "تم جرد القطعة بنجاح. الموقع صحيح والكمية مطابقة.";
        }
        if (locationMatched && compare < 0) {
            return prefix + "الموقع صحيح، لكن يوجد نقص في الكمية. الكمية المعتمدة " + stockQty + ", الكمية الموجودة " + countedQty + ", الفرق " + varianceQty + ".";
        }
        if (locationMatched) {
            return prefix + "الموقع صحيح، لكن يوجد زيادة في الكمية. الكمية المعتمدة " + stockQty + ", الكمية الموجودة " + countedQty + ", الفرق +" + varianceQty + ".";
        }
        if (compare == 0) {
            return prefix + "الكمية مطابقة، لكن الموقع غير صحيح. الموقع المخطط " + expected + ", والموقع الفعلي " + actual + ".";
        }
        if (compare < 0) {
            return prefix + "الموقع غير صحيح ويوجد نقص في الكمية. الموقع المخطط " + expected + ", والموقع الفعلي " + actual + ". الكمية المعتمدة " + stockQty + ", الكمية الموجودة " + countedQty + ", الفرق " + varianceQty + ".";
        }
        return prefix + "الموقع غير صحيح ويوجد زيادة في الكمية. الموقع المخطط " + expected + ", والموقع الفعلي " + actual + ". الكمية المعتمدة " + stockQty + ", الكمية الموجودة " + countedQty + ", الفرق +" + varianceQty + ".";
    }

    private void increment(SparePartInventoryBranch branch, SparePartInventoryLocation location, SparePartInventoryItemStatus status) {
        if (branch == null || location == null || status == null) {
            return;
        }
        branch.setCountedItems(safe(branch.getCountedItems()) + 1);
        location.setCountedItems(safe(location.getCountedItems()) + 1);

        if (status == SparePartInventoryItemStatus.MATCHED) {
            branch.setMatchedItems(safe(branch.getMatchedItems()) + 1);
            location.setMatchedItems(safe(location.getMatchedItems()) + 1);
        } else if (status == SparePartInventoryItemStatus.SHORTAGE || status == SparePartInventoryItemStatus.LOCATION_MISMATCH_WITH_SHORTAGE) {
            branch.setShortageItems(safe(branch.getShortageItems()) + 1);
            location.setShortageItems(safe(location.getShortageItems()) + 1);
        } else if (status == SparePartInventoryItemStatus.OVERAGE || status == SparePartInventoryItemStatus.LOCATION_MISMATCH_WITH_OVERAGE) {
            branch.setOverageItems(safe(branch.getOverageItems()) + 1);
            location.setOverageItems(safe(location.getOverageItems()) + 1);
        }

        if (isLocationMismatchStatus(status)) {
            branch.setLocationMismatchItems(safe(branch.getLocationMismatchItems()) + 1);
            location.setLocationMismatchItems(safe(location.getLocationMismatchItems()) + 1);
        }
    }

    private void decrement(SparePartInventoryBranch branch, SparePartInventoryLocation location, SparePartInventoryItemStatus status) {
        if (branch == null || location == null || status == null) {
            return;
        }
        branch.setCountedItems(Math.max(0, safe(branch.getCountedItems()) - 1));
        location.setCountedItems(Math.max(0, safe(location.getCountedItems()) - 1));

        if (status == SparePartInventoryItemStatus.MATCHED) {
            branch.setMatchedItems(Math.max(0, safe(branch.getMatchedItems()) - 1));
            location.setMatchedItems(Math.max(0, safe(location.getMatchedItems()) - 1));
        } else if (status == SparePartInventoryItemStatus.SHORTAGE || status == SparePartInventoryItemStatus.LOCATION_MISMATCH_WITH_SHORTAGE) {
            branch.setShortageItems(Math.max(0, safe(branch.getShortageItems()) - 1));
            location.setShortageItems(Math.max(0, safe(location.getShortageItems()) - 1));
        } else if (status == SparePartInventoryItemStatus.OVERAGE || status == SparePartInventoryItemStatus.LOCATION_MISMATCH_WITH_OVERAGE) {
            branch.setOverageItems(Math.max(0, safe(branch.getOverageItems()) - 1));
            location.setOverageItems(Math.max(0, safe(location.getOverageItems()) - 1));
        }

        if (isLocationMismatchStatus(status)) {
            branch.setLocationMismatchItems(Math.max(0, safe(branch.getLocationMismatchItems()) - 1));
            location.setLocationMismatchItems(Math.max(0, safe(location.getLocationMismatchItems()) - 1));
        }
    }

    private boolean isLocationMismatchStatus(SparePartInventoryItemStatus status) {
        return status == SparePartInventoryItemStatus.LOCATION_MISMATCH
                || status == SparePartInventoryItemStatus.LOCATION_MISMATCH_WITH_SHORTAGE
                || status == SparePartInventoryItemStatus.LOCATION_MISMATCH_WITH_OVERAGE;
    }

    private SparePartInventoryScanResponse toResponse(SparePartInventoryScan scan, SparePartInventoryItem item, String message) {
        SparePartInventoryScanResponse response = new SparePartInventoryScanResponse();
        response.setScanId(scan.getId());
        response.setItemId(item == null ? null : item.getId());
        response.setItemNo(scan.getScannedItemNo());
        response.setScanResult(scan.getScanResult());
        response.setItemStatus(item == null ? null : item.getStatus());
        response.setLocationStatus(scan.getLocationStatus());
        response.setQuantityStatus(scan.getQuantityStatus());
        response.setMessage(message);
        response.setExpectedBranchId(scan.getExpectedBranch() == null ? null : scan.getExpectedBranch().getId());
        response.setExpectedBranchName(getBranchName(scan.getExpectedBranch()));
        response.setExpectedLocationId(scan.getExpectedLocation() == null ? null : scan.getExpectedLocation().getId());
        response.setExpectedLocationCode(getLocationCode(scan.getExpectedLocation()));
        response.setActualBranchId(scan.getActualBranch() == null ? null : scan.getActualBranch().getId());
        response.setActualBranchName(getBranchName(scan.getActualBranch()));
        response.setActualLocationId(scan.getActualLocation() == null ? null : scan.getActualLocation().getId());
        response.setActualLocationCode(getLocationCode(scan.getActualLocation()));
        response.setStockQty(scan.getStockQty());
        response.setCountedQty(scan.getCountedQty());
        response.setVarianceQty(scan.getVarianceQty());
        return response;
    }

    private boolean sameId(Object left, Object right) {
        if (left == null || right == null) {
            return left == right;
        }
        Long leftId = extractId(left);
        Long rightId = extractId(right);
        return leftId != null && leftId.equals(rightId);
    }

    private Long extractId(Object value) {
        if (value instanceof SparePartInventoryBranch branch) {
            return branch.getId();
        }
        if (value instanceof SparePartInventoryLocation location) {
            return location.getId();
        }
        if (value instanceof Long id) {
            return id;
        }
        return null;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private String getBranchName(SparePartInventoryBranch branch) {
        return branch == null ? null : branch.getBranchName();
    }

    private String getLocationCode(SparePartInventoryLocation location) {
        return location == null ? null : location.getLocationCode();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}

package com.pinetechs.orvix.ims.inventory.vehicle.service.impl;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.dto.VehicleInventoryImportResult;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryItem;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryLocation;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryItemRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryLocationRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.service.VehicleInventoryImportService;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@Transactional
public class VehicleInventoryImportServiceImpl implements VehicleInventoryImportService {

    private final InventoryTaskRepository inventoryTaskRepository;
    private final VehicleInventoryItemRepository itemRepository;
    private final VehicleInventoryLocationRepository locationRepository;

    public VehicleInventoryImportServiceImpl(InventoryTaskRepository inventoryTaskRepository, VehicleInventoryItemRepository itemRepository, VehicleInventoryLocationRepository locationRepository) {
        this.inventoryTaskRepository = inventoryTaskRepository;
        this.itemRepository = itemRepository;
        this.locationRepository = locationRepository;
    }

    @Override
    public VehicleInventoryImportResult importExcel(Long taskId, MultipartFile file, Long uploadedByUserId) {
        InventoryTask task = inventoryTaskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Inventory task not found"));

        if (task.getInventoryDomain() != InventoryDomain.VEHICLE) {
            throw new RuntimeException("Task is not a vehicle inventory task");
        }

        if (task.getStatus() != InventoryTaskStatus.DRAFT) {
            throw new RuntimeException("Excel can be imported only when task is DRAFT");
        }

        VehicleInventoryImportResult result = new VehicleInventoryImportResult();
        result.setTaskId(taskId);

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("Excel header row is missing");
            }

            Map<String, Integer> columns = readColumns(headerRow);
            validateRequiredColumns(columns);

            Set<String> vinSet = new HashSet<>();
            List<VehicleInventoryItem> items = new ArrayList<>();
            Map<String, VehicleInventoryLocation> locationsMap = new HashMap<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                result.setTotalRows(result.getTotalRows() + 1);

                String vinNo = getString(row, columns.get("VIN_NO"));

                if (vinNo == null || vinNo.isBlank()) {
                    result.getErrors().add("Row " + (i + 1) + ": VIN_NO is required");
                    continue;
                }

                vinNo = normalize(vinNo);

                if (!vinSet.add(vinNo)) {
                    result.setDuplicatedVinCount(result.getDuplicatedVinCount() + 1);
                    result.getErrors().add("Row " + (i + 1) + ": Duplicate VIN in file: " + vinNo);
                    continue;
                }

                if (itemRepository.existsByInventoryTaskIdAndVinNo(taskId, vinNo)) {
                    result.getErrors().add("Row " + (i + 1) + ": VIN already exists in this task: " + vinNo);
                    continue;
                }

                String storeNo = normalize(getString(row, columns.get("ST_STORE_NO")));
                String locationName = normalize(getString(row, columns.get("LOCATION")));

                if (storeNo == null || storeNo.isBlank()) {
                    result.getErrors().add("Row " + (i + 1) + ": ST_STORE_NO is required");
                    continue;
                }

                VehicleInventoryItem item = new VehicleInventoryItem();
                item.setInventoryTask(task);
                item.setPartNo(getString(row, columns.get("PART_NO")));
                item.setMake(getString(row, columns.get("MAKE")));
                item.setModelName(getString(row, columns.get("MODEL_NAME")));
                item.setModelYear(getInteger(row, columns.get("Year")));
                item.setVinNo(vinNo);
                item.setSpecification(getString(row, columns.get("SPECIFICATION")));
                item.setQuantity(getInteger(row, columns.get("QTY")));
                item.setReceiptDate(getDate(row, columns.get("RECEIPT_DATE")));
                item.setColorNo(getString(row, columns.get("COLOR_NO")));
                item.setInteriorColor(getString(row, columns.get("INTERIOR_COLOR")));
                item.setMchStatus(getString(row, columns.get("MCH_STATUS")));
                item.setStockStatus(getString(row, columns.get("STOCK_STATUS")));
                item.setLocation(locationName);
                item.setStoreNo(storeNo);
                item.setDarArtId(getString(row, columns.get("DAR_ART_ID")));

                items.add(item);

                VehicleInventoryLocation location = locationsMap.get(storeNo);
                if (location == null) {
                    location = new VehicleInventoryLocation();
                    location.setInventoryTask(task);
                    location.setStoreNo(storeNo);
                    location.setLocationName(locationName);
                    location.setTotalVehicles(0);
                    locationsMap.put(storeNo, location);
                }

                location.setTotalVehicles(location.getTotalVehicles() + 1);
            }

            if (!result.getErrors().isEmpty()) {
                return result;
            }

            itemRepository.saveAll(items);
            locationRepository.saveAll(locationsMap.values());

            task.setTotalRecords(items.size());
            task.setProcessedRecords(0);
            task.setMatchedRecords(0);
            task.setStatus(InventoryTaskStatus.READY_FOR_ASSIGNMENT);

            inventoryTaskRepository.save(task);

            result.setImportedItems(items.size());
            result.setLocationCount(locationsMap.size());

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to import vehicle inventory Excel: " + e.getMessage(), e);
        }
    }



    private Map<String, Integer> readColumns(Row headerRow) {
        Map<String, Integer> columns = new HashMap<>();

        for (Cell cell : headerRow) {
            String columnName = getCellString(cell);
            if (columnName != null && !columnName.isBlank()) {
                columns.put(columnName.trim(), cell.getColumnIndex());
            }
        }

        return columns;
    }

    private void validateRequiredColumns(Map<String, Integer> columns) {
        List<String> required = List.of(
                "PART_NO",
                "MAKE",
                "MODEL_NAME",
                "Year",
                "VIN_NO",
                "SPECIFICATION",
                "QTY",
                "RECEIPT_DATE",
                "COLOR_NO",
                "INTERIOR_COLOR",
                "MCH_STATUS",
                "STOCK_STATUS",
                "LOCATION",
                "ST_STORE_NO",
                "DAR_ART_ID"
        );

        for (String column : required) {
            if (!columns.containsKey(column)) {
                throw new RuntimeException("Missing required column: " + column);
            }
        }
    }

    private boolean isEmptyRow(Row row) {
        for (Cell cell : row) {
            if (getCellString(cell) != null && !getCellString(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String getString(Row row, Integer index) {
        if (index == null) {
            return null;
        }

        Cell cell = row.getCell(index);
        return getCellString(cell);
    }

    private String getCellString(Cell cell) {
        if (cell == null) {
            return null;
        }

        DataFormatter formatter = new DataFormatter();
        String value = formatter.formatCellValue(cell);

        if (value == null) {
            return null;
        }

        return value.trim();
    }

    private Integer getInteger(Row row, Integer index) {
        String value = getString(row, index);

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Double.valueOf(value).intValue();
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate getDate(Row row, Integer index) {
        if (index == null) {
            return null;
        }

        Cell cell = row.getCell(index);

        if (cell == null) {
            return null;
        }

        try {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
            }

            String value = getCellString(cell);
            if (value == null || value.isBlank()) {
                return null;
            }

            double serial = Double.parseDouble(value);
            return DateUtil.getJavaDate(serial)
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

        } catch (Exception e) {
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        return value.trim();
    }
}
package com.pinetechs.orvix.ims.jobs.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinetechs.orvix.ims.file.service.UploadedFileService;
import com.pinetechs.orvix.ims.inventory.sparepart.dto.SparePartInventoryImportResult;
import com.pinetechs.orvix.ims.inventory.sparepart.entity.*;
import com.pinetechs.orvix.ims.inventory.sparepart.service.SparePartInventoryImportPersistenceService;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.jobs.entity.BackgroundJob;
import com.pinetechs.orvix.ims.jobs.enums.JobStatus;
import com.pinetechs.orvix.ims.jobs.enums.JobType;
import com.pinetechs.orvix.ims.jobs.repository.BackgroundJobRepository;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SparePartImportJobProcessor implements BackgroundJobProcessor {

    private static final Logger log = LoggerFactory.getLogger(SparePartImportJobProcessor.class);

    private static final String DEFAULT_BRAND = "غير محدد";
    private static final int PROGRESS_START = 10;
    private static final int PROGRESS_READING_MAX = 80;
    private static final int PROGRESS_SAVING = 90;
    private static final int PROGRESS_COMPLETED = 100;

    private final BackgroundJobRepository backgroundJobRepository;
    private final SparePartInventoryImportPersistenceService persistenceService;
    private final UploadedFileService uploadedFileService;
    private final ObjectMapper objectMapper;

    public SparePartImportJobProcessor(
            BackgroundJobRepository backgroundJobRepository,
            SparePartInventoryImportPersistenceService persistenceService,
            UploadedFileService uploadedFileService,
            ObjectMapper objectMapper
    ) {
        this.backgroundJobRepository = backgroundJobRepository;
        this.persistenceService = persistenceService;
        this.uploadedFileService = uploadedFileService;
        this.objectMapper = objectMapper;
    }

    @Override
    public JobType getJobType() {
        return JobType.SPARE_PART_IMPORT;
    }

    @Override
    public void process(BackgroundJob job) {
        Long jobId = job.getId();
        Long taskId = job.getRelatedId();

        SparePartInventoryImportResult result = new SparePartInventoryImportResult();
        result.setJobId(jobId);
        result.setTaskId(taskId);

        log.info("Spare part import job started. jobId={}, taskId={}, uploadedFileId={}, file={}",
                jobId, taskId, job.getUploadedFileId(), job.getRelatedFile());

        try {
            updateJob(job, PROGRESS_START, "Spare part import job started");
            InventoryTask task = persistenceService.markImportInProgress(taskId);
            validateJobFile(job);

            Path path = Paths.get(job.getRelatedFile());

            List<SparePartInventoryItem> items = new ArrayList<>();
            Map<String, SparePartInventoryBranch> branchesMap = new LinkedHashMap<>();
            Map<String, SparePartInventoryLocation> locationsMap = new LinkedHashMap<>();
            Map<String, SparePartInventoryBrand> brandsMap = new LinkedHashMap<>();
            Map<String, Integer> firstItemBranchLocationRow = new LinkedHashMap<>();
            Set<String> branchNameSet = new LinkedHashSet<>();
            Set<String> locationNameSet = new LinkedHashSet<>();

            readExcelFile(
                    job,
                    task,
                    path,
                    result,
                    items,
                    branchesMap,
                    locationsMap,
                    brandsMap,
                    firstItemBranchLocationRow,
                    branchNameSet,
                    locationNameSet
            );

            if (!result.getErrors().isEmpty()) {
                String firstErrors = result.getErrors().stream().limit(10).collect(Collectors.joining("; "));
                throw new RuntimeException("Spare part import validation failed: " + firstErrors);
            }

            updateJob(job, PROGRESS_SAVING, "Saving spare part inventory data to database");

            persistenceService.replaceSparePartInventoryData(
                    taskId,
                    items,
                    branchesMap.values(),
                    locationsMap.values(),
                    brandsMap.values()
            );

            result.setImportedRows(items.size());
            result.setImportedItems(items.size());
            result.setBranchCount(branchesMap.size());
            result.setLocationCount(locationsMap.size());
            result.setBrandCount(brandsMap.size());
            result.setBranches(new ArrayList<>(branchNameSet));
            result.setLocations(new ArrayList<>(locationNameSet));

            completeJob(job, result);

            log.info("Spare part import job completed successfully. jobId={}, taskId={}, importedRows={}, branches={}, locations={}",
                    jobId, taskId, items.size(), branchesMap.size(), locationsMap.size());

        } catch (Exception ex) {
            log.error("Spare part import job failed. jobId={}, taskId={}, uploadedFileId={}, file={}",
                    jobId, taskId, job.getUploadedFileId(), job.getRelatedFile(), ex);

            persistenceService.markImportFailed(taskId);

            if (result.getErrors().isEmpty()) {
                result.getErrors().add(ex.getMessage());
            }

            failJob(job, result, ex);

        } finally {
            uploadedFileService.markAsDeleted(job.getUploadedFileId());
            log.info("Spare part import uploaded file marked as deleted. jobId={}, uploadedFileId={}",
                    jobId, job.getUploadedFileId());
        }
    }

    private void readExcelFile(
            BackgroundJob job,
            InventoryTask task,
            Path path,
            SparePartInventoryImportResult result,
            List<SparePartInventoryItem> items,
            Map<String, SparePartInventoryBranch> branchesMap,
            Map<String, SparePartInventoryLocation> locationsMap,
            Map<String, SparePartInventoryBrand> brandsMap,
            Map<String, Integer> firstItemBranchLocationRow,
            Set<String> branchNameSet,
            Set<String> locationNameSet
    ) throws Exception {
        Long jobId = job.getId();

        log.info("Opening Spare Part Excel file. jobId={}, path={}", jobId, path);

        try (
                InputStream inputStream = Files.newInputStream(path);
                Workbook workbook = WorkbookFactory.create(inputStream)
        ) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new RuntimeException("The Excel file is empty or does not contain a header row.");
            }

            Map<String, Integer> columns = readColumns(headerRow);
            log.info("Spare part Excel columns loaded. jobId={}, sheet={}, columns={}", jobId, sheet.getSheetName(), columns.keySet());

            List<String> missingColumns = getMissingRequiredColumns(columns);
            if (!missingColumns.isEmpty()) {
                throw new RuntimeException("The Excel file is missing required columns: " + String.join(", ", missingColumns));
            }

            int lastRowNum = sheet.getLastRowNum();
            if (lastRowNum <= 0) {
                throw new RuntimeException("The Excel file does not contain data rows.");
            }

            int lastSavedProgress = PROGRESS_START;

            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                result.setTotalRows(result.getTotalRows() + 1);

                String itemNo = normalize(getString(row, columns.get("ITEM_NO")));
                String brandName = normalizeOrDefault(getString(row, columns.get("BRAND")), DEFAULT_BRAND);
                String branchName = normalize(getString(row, columns.get("BRANCH")));
                String locationCode = normalize(getString(row, columns.get("LOCATION")));
                BigDecimal qty = getBigDecimal(row, columns.get("QTY"));
                BigDecimal stockQty = getBigDecimal(row, columns.get("STKQTY"));
                BigDecimal frozenQty = getBigDecimal(row, columns.get("FZQTY"));

                if (itemNo == null || itemNo.isBlank()) {
                    result.getErrors().add("Row " + (i + 1) + ": ITEM_NO is required");
                    continue;
                }
                if (branchName == null || branchName.isBlank()) {
                    result.getErrors().add("Row " + (i + 1) + ": BRANCH is required");
                    continue;
                }
                if (locationCode == null || locationCode.isBlank()) {
                    result.getErrors().add("Row " + (i + 1) + ": LOCATION is required");
                    continue;
                }
                if (stockQty == null) {
                    result.getErrors().add("Row " + (i + 1) + ": STKQTY is required and must be numeric");
                    continue;
                }

                String duplicateKey = normalizeKey(itemNo) + "||" + normalizeKey(branchName) + "||" + normalizeKey(locationCode);
                Integer firstRow = firstItemBranchLocationRow.putIfAbsent(duplicateKey, i + 1);
                if (firstRow != null) {
                    result.setDuplicateItemLocationCount(result.getDuplicateItemLocationCount() + 1);
                    result.getErrors().add("Row " + (i + 1) + ": Duplicate ITEM_NO + BRANCH + LOCATION in file: " + itemNo + " / " + branchName + " / " + locationCode + " first found at row " + firstRow);
                    continue;
                }

                SparePartInventoryBranch branch = getOrCreateBranch(task, branchesMap, branchName);
                SparePartInventoryLocation location = getOrCreateLocation(task, locationsMap, branch, locationCode);
                SparePartInventoryBrand brand = getOrCreateBrand(task, brandsMap, brandName);

                branch.setTotalItems(branch.getTotalItems() + 1);
                location.setTotalItems(location.getTotalItems() + 1);
                brand.setTotalItems(brand.getTotalItems() + 1);

                SparePartInventoryItem item = new SparePartInventoryItem();
                item.setInventoryTask(task);
                item.setItemNo(itemNo);
                item.setBrandName(brandName);
                item.setBrand(brand);
                item.setPlannedBranch(branch);
                item.setPlannedLocation(location);
                item.setQty(qty);
                item.setStockQty(stockQty);
                item.setFrozenQty(frozenQty);

                items.add(item);
                branchNameSet.add(branchName);
                locationNameSet.add(branchName + " / " + locationCode);

                int currentProgress = calculateReadProgress(i, lastRowNum);
                if (currentProgress > lastSavedProgress) {
                    lastSavedProgress = currentProgress;
                    updateJob(job, currentProgress, "Reading spare part Excel rows: " + i + " / " + lastRowNum);
                }
            }

            log.info("Spare part Excel reading finished. jobId={}, totalRows={}, validItems={}, branches={}, locations={}, brands={}, errors={}, duplicateItemLocationCount={}",
                    jobId,
                    result.getTotalRows(),
                    items.size(),
                    branchesMap.size(),
                    locationsMap.size(),
                    brandsMap.size(),
                    result.getErrors().size(),
                    result.getDuplicateItemLocationCount());
        }
    }

    private SparePartInventoryBranch getOrCreateBranch(InventoryTask task, Map<String, SparePartInventoryBranch> branchesMap, String branchName) {
        String key = normalizeKey(branchName);
        SparePartInventoryBranch branch = branchesMap.get(key);
        if (branch == null) {
            branch = new SparePartInventoryBranch();
            branch.setInventoryTask(task);
            branch.setBranchName(branchName);
            branch.setTotalItems(0);
            branchesMap.put(key, branch);
        }
        return branch;
    }

    private SparePartInventoryLocation getOrCreateLocation(InventoryTask task, Map<String, SparePartInventoryLocation> locationsMap, SparePartInventoryBranch branch, String locationCode) {
        String key = normalizeKey(branch.getBranchName()) + "||" + normalizeKey(locationCode);
        SparePartInventoryLocation location = locationsMap.get(key);
        if (location == null) {
            location = new SparePartInventoryLocation();
            location.setInventoryTask(task);
            location.setBranch(branch);
            location.setLocationCode(locationCode);
            location.setTotalItems(0);
            locationsMap.put(key, location);
        }
        return location;
    }

    private SparePartInventoryBrand getOrCreateBrand(InventoryTask task, Map<String, SparePartInventoryBrand> brandsMap, String brandName) {
        String key = normalizeKey(brandName);
        SparePartInventoryBrand brand = brandsMap.get(key);
        if (brand == null) {
            brand = new SparePartInventoryBrand();
            brand.setInventoryTask(task);
            brand.setBrandName(brandName);
            brand.setTotalItems(0);
            brandsMap.put(key, brand);
        }
        return brand;
    }

    private void validateJobFile(BackgroundJob job) {
        if (job.getRelatedFile() == null || job.getRelatedFile().isBlank()) {
            throw new RuntimeException("Related file path is empty for jobId=" + job.getId());
        }
        Path path = Paths.get(job.getRelatedFile());
        if (!Files.exists(path)) {
            throw new RuntimeException("Excel file not found: " + path);
        }
        if (!Files.isRegularFile(path)) {
            throw new RuntimeException("Related file path is not a file: " + path);
        }
    }

    private void updateJob(BackgroundJob job, int progress, String message) {
        job.setProgress(progress);
        job.setMessage(message);
        backgroundJobRepository.save(job);
        log.info("Spare part import job progress updated. jobId={}, progress={}, message={}", job.getId(), progress, message);
    }

    private void completeJob(BackgroundJob job, SparePartInventoryImportResult result) {
        job.setStatus(JobStatus.COMPLETED);
        job.setProgress(PROGRESS_COMPLETED);
        job.setMessage("Spare part import completed successfully");
        job.setResult(toJson(result));
        job.setErrorMessage(null);
        job.setFinishedAt(LocalDateTime.now());
        backgroundJobRepository.save(job);
    }

    private void failJob(BackgroundJob job, SparePartInventoryImportResult result, Exception ex) {
        job.setStatus(JobStatus.FAILED);
        job.setProgress(PROGRESS_COMPLETED);
        job.setMessage("Spare part import failed");
        job.setResult(toJson(result));
        job.setErrorMessage(ex.getMessage());
        job.setFinishedAt(LocalDateTime.now());
        backgroundJobRepository.save(job);
    }

    private String toJson(SparePartInventoryImportResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            return result.toJsonString();
        }
    }

    private int calculateReadProgress(int currentRow, int totalRows) {
        if (totalRows <= 0) {
            return PROGRESS_START;
        }
        int progress = PROGRESS_START + (int) (((double) currentRow / totalRows) * (PROGRESS_READING_MAX - PROGRESS_START));
        return Math.min(progress, PROGRESS_READING_MAX);
    }

    private Map<String, Integer> readColumns(Row headerRow) {
        Map<String, Integer> columns = new HashMap<>();
        for (Cell cell : headerRow) {
            String columnName = getCellString(cell);
            if (columnName != null && !columnName.isBlank()) {
                columns.put(normalizeColumnName(columnName), cell.getColumnIndex());
            }
        }
        return columns;
    }

    private List<String> getMissingRequiredColumns(Map<String, Integer> columns) {
        List<String> requiredColumns = List.of("ITEM_NO", "BRAND", "BRANCH", "LOCATION", "STKQTY");
        List<String> missing = new ArrayList<>();
        for (String requiredColumn : requiredColumns) {
            if (!columns.containsKey(requiredColumn)) {
                missing.add(requiredColumn);
            }
        }
        return missing;
    }

    private boolean isEmptyRow(Row row) {
        for (Cell cell : row) {
            String value = getCellString(cell);
            if (value != null && !value.isBlank()) {
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
        return value == null ? null : value.trim();
    }

    private BigDecimal getBigDecimal(Row row, Integer index) {
        if (index == null) {
            return null;
        }
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            String value = getCellString(cell);
            if (value == null || value.isBlank()) {
                return null;
            }
            value = value.replace(",", "").trim();
            return new BigDecimal(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeColumnName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toUpperCase(Locale.ROOT)
                .replace(" ", "_")
                .replace("-", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_", "")
                .replaceAll("_$", "");
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeOrDefault(String value, String defaultValue) {
        String normalized = normalize(value);
        return normalized == null || normalized.isBlank() ? defaultValue : normalized;
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}

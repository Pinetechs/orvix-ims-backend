package com.pinetechs.orvix.ims.jobs.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinetechs.orvix.ims.file.service.UploadedFileService;
import com.pinetechs.orvix.ims.inventory.asset.dto.AssetInventoryImportResult;
import com.pinetechs.orvix.ims.inventory.asset.entity.*;
import com.pinetechs.orvix.ims.inventory.asset.service.AssetInventoryImportPersistenceService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AssetImportJobProcessor implements BackgroundJobProcessor {

    private static final Logger log = LoggerFactory.getLogger(AssetImportJobProcessor.class);

    private static final String TARGET_SHEET_NAME = "Overall after Revaluation";
    private static final String DEFAULT_PLACE = "غير محدد";
    private static final String DEFAULT_FLOOR = "غير محدد";
    private static final String DEFAULT_CATEGORY_VALUE = "غير مصنف";

    private static final int PROGRESS_START = 10;
    private static final int PROGRESS_READING_MAX = 80;
    private static final int PROGRESS_SAVING = 90;
    private static final int PROGRESS_COMPLETED = 100;

    private final BackgroundJobRepository backgroundJobRepository;
    private final AssetInventoryImportPersistenceService persistenceService;
    private final UploadedFileService uploadedFileService;
    private final ObjectMapper objectMapper;

    public AssetImportJobProcessor(
            BackgroundJobRepository backgroundJobRepository,
            AssetInventoryImportPersistenceService persistenceService,
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
        return JobType.ASSET_IMPORT;
    }

    @Override
    public void process(BackgroundJob job) {
        Long jobId = job.getId();
        Long taskId = job.getRelatedId();

        AssetInventoryImportResult result = new AssetInventoryImportResult();
        result.setJobId(jobId);
        result.setTaskId(taskId);

        log.info("Asset import job started. jobId={}, taskId={}, uploadedFileId={}, file={}",
                jobId,
                taskId,
                job.getUploadedFileId(),
                job.getRelatedFile()
        );

        try {
            updateJob(job, PROGRESS_START, "Asset import job started");

            InventoryTask task = persistenceService.markImportInProgress(taskId);

            validateJobFile(job);

            Path path = Paths.get(job.getRelatedFile());

            List<AssetInventoryItem> items = new ArrayList<>();
            Map<String, AssetInventoryLocation> locationsMap = new LinkedHashMap<>();
            Map<String, AssetInventoryFloor> floorsMap = new LinkedHashMap<>();
            Map<String, AssetInventoryPlace> placesMap = new LinkedHashMap<>();
            Map<String, AssetInventoryCategory> categoriesMap = new LinkedHashMap<>();
            Map<String, Integer> firstBarcodeRow = new LinkedHashMap<>();
            Set<String> locationNameSet = new LinkedHashSet<>();

            readExcelFile(
                    job,
                    task,
                    path,
                    result,
                    items,
                    locationsMap,
                    floorsMap,
                    placesMap,
                    categoriesMap,
                    firstBarcodeRow,
                    locationNameSet
            );

            if (!result.getErrors().isEmpty()) {
                String firstErrors = result.getErrors()
                        .stream()
                        .limit(10)
                        .collect(Collectors.joining("; "));

                throw new RuntimeException("Asset import validation failed: " + firstErrors);
            }

            updateJob(job, PROGRESS_SAVING, "Saving asset inventory data to database");

            log.info(
                    "Saving asset inventory import result. jobId={}, taskId={}, items={}, locations={}, floors={}, places={}, categories={}",
                    jobId,
                    taskId,
                    items.size(),
                    locationsMap.size(),
                    floorsMap.size(),
                    placesMap.size(),
                    categoriesMap.size()
            );

            persistenceService.replaceAssetInventoryData(
                    taskId,
                    items,
                    locationsMap.values(),
                    floorsMap.values(),
                    placesMap.values(),
                    categoriesMap.values()
            );

            result.setImportedRows(items.size());
            result.setImportedItems(items.size());
            result.setLocationCount(locationsMap.size());
            result.setFloorCount(floorsMap.size());
            result.setPlaceCount(placesMap.size());
            result.setCategoryCount(categoriesMap.size());
            result.setLocations(new ArrayList<>(locationNameSet));

            completeJob(job, result);

            log.info(
                    "Asset import job completed successfully. jobId={}, taskId={}, importedRows={}, locations={}, floors={}, places={}",
                    jobId,
                    taskId,
                    items.size(),
                    locationsMap.size(),
                    floorsMap.size(),
                    placesMap.size()
            );

        } catch (Exception ex) {
            log.error(
                    "Asset import job failed. jobId={}, taskId={}, uploadedFileId={}, file={}",
                    jobId,
                    taskId,
                    job.getUploadedFileId(),
                    job.getRelatedFile(),
                    ex
            );

            persistenceService.markImportFailed(taskId);

            if (result.getErrors().isEmpty()) {
                result.getErrors().add(ex.getMessage());
            }

            failJob(job, result, ex);

        } finally {
            uploadedFileService.markAsDeleted(job.getUploadedFileId());

            log.info(
                    "Asset import uploaded file marked as deleted. jobId={}, uploadedFileId={}",
                    jobId,
                    job.getUploadedFileId()
            );
        }
    }

    private void readExcelFile(BackgroundJob job, InventoryTask task, Path path, AssetInventoryImportResult result, List<AssetInventoryItem> items, Map<String, AssetInventoryLocation> locationsMap,
            Map<String, AssetInventoryFloor> floorsMap,
            Map<String, AssetInventoryPlace> placesMap,
            Map<String, AssetInventoryCategory> categoriesMap,
            Map<String, Integer> firstBarcodeRow,
            Set<String> locationNameSet
    ) throws Exception {

        Long jobId = job.getId();

        log.info("Opening Asset Excel file. jobId={}, path={}", jobId, path);

        try (
                InputStream inputStream = Files.newInputStream(path);
                Workbook workbook = WorkbookFactory.create(inputStream)
        ) {
            Sheet sheet = findSheet(workbook);

            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new RuntimeException("The Excel file is empty or does not contain a header row.");
            }

            Map<String, Integer> columns = readColumns(headerRow);

            log.info("Asset Excel columns loaded. jobId={}, sheet={}, columns={}", jobId, sheet.getSheetName(), columns.keySet());

            List<String> missingColumns = getMissingRequiredColumns(columns);

            if (!missingColumns.isEmpty()) {
                throw new RuntimeException(
                        "The Excel file is missing required columns: " + String.join(", ", missingColumns)
                );
            }

            int lastRowNum = sheet.getLastRowNum();

            if (lastRowNum <= 0) {
                throw new RuntimeException("The Excel file does not contain data rows.");
            }

            log.info("Asset Excel reading started. jobId={}, totalPossibleRows={}", jobId, lastRowNum);

            int lastSavedProgress = PROGRESS_START;

            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);

                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                String barcode = normalize(getString(row, columns.get("BARCODE")));

                if (barcode == null || barcode.isBlank()) {
                    continue;
                }

                result.setTotalRows(result.getTotalRows() + 1);

                Integer firstRow = firstBarcodeRow.putIfAbsent(barcode, i + 1);
                if (firstRow != null) {
                    result.setDuplicatedBarcodeCount(result.getDuplicatedBarcodeCount() + 1);
                    result.getErrors().add("Row " + (i + 1) + ": Duplicate Barcode in file: " + barcode + " first found at row " + firstRow);
                    continue;
                }

                String locationName = normalize(getString(row, columns.get("LOCATION")));
                String floorName = normalize(getString(row, columns.get("FLOOR")));
                String placeName = normalize(getString(row, columns.get("PLACE")));
                String assetCategory = normalizeOrDefault(getString(row, columns.get("ASSETCATEGORY")), DEFAULT_CATEGORY_VALUE);
                String assetType = normalizeOrDefault(getString(row, columns.get("ASSETTYPE")), DEFAULT_CATEGORY_VALUE);
                String categoryCode = normalizeOrDefault(getString(row, columns.get("CATEGORY_CODE")), DEFAULT_CATEGORY_VALUE);

                if (locationName == null || locationName.isBlank()) {
                    result.getErrors().add("Row " + (i + 1) + ": Location is required");
                    continue;
                }

                if (floorName == null || floorName.isBlank()) {
                    floorName = DEFAULT_FLOOR;
                }

                if (placeName == null || placeName.isBlank()) {
                    placeName = DEFAULT_PLACE;
                }

                AssetInventoryLocation location = getOrCreateLocation(task, locationsMap, locationName);
                AssetInventoryFloor floor = getOrCreateFloor(task, floorsMap, location, floorName);
                AssetInventoryPlace place = getOrCreatePlace(task, placesMap, location, floor, placeName);
                AssetInventoryCategory category = getOrCreateCategory(task, categoriesMap, assetCategory, assetType, categoryCode);

                location.setTotalAssets(location.getTotalAssets() + 1);
                floor.setTotalAssets(floor.getTotalAssets() + 1);
                place.setTotalAssets(place.getTotalAssets() + 1);
                category.setTotalAssets(category.getTotalAssets() + 1);

                AssetInventoryItem item = new AssetInventoryItem();
                item.setInventoryTask(task);
                item.setBarcode(barcode);
                item.setDescription(getString(row, columns.get("DESCRIPTION")));
                item.setAssetCategory(assetCategory);
                item.setAssetType(assetType);
                item.setCategoryCode(categoryCode);
                item.setCategory(category);
                item.setQuantity(getBigDecimal(row, columns.get("QTY")));
                item.setPlannedLocation(location);
                item.setPlannedFloor(floor);
                item.setPlannedPlace(place);
                item.setMarketValueWeightPerItem(getBigDecimal(row, columns.get("MARKET_VALUE_WEIGHT_PER_ITEM")));
                item.setMarketValue(getBigDecimal(row, columns.get("MARKET_VALUE")));
                item.setExtraValuePerItem(getBigDecimal(row, columns.get("EXTRA_VALUE_PER_ITEM")));
                item.setNewAccDep(getBigDecimal(row, columns.get("NEW_ACC_DEP")));
                item.setPurchaseValueMarketValue(getBigDecimal(row, columns.get("PURCAHSE_VALUE_MARKET_VALUE")));
                item.setNewOldPurchaseValue(getBigDecimal(row, columns.get("NEW_OLD_PURCHASE_VALUE")));
                item.setNewPurchaseValue(getBigDecimal(row, columns.get("NEW_PURCHASE_VALUE")));
                item.setOldPurchaseValue(getBigDecimal(row, columns.get("OLD_PURCHASE_VALUE")));
                item.setFinalPurchaseValue(getBigDecimal(row, columns.get("FINAL_PURCHASE_VALUE")));
                item.setFinalAccDep(getBigDecimal(row, columns.get("FINAL_ACC_DEP")));
                item.setFinalBookValue(getBigDecimal(row, columns.get("FINAL_BOOK_VALUE")));
                item.setAccDep(getBigDecimal(row, columns.get("ACC_DEP")));
                item.setBookValue(getBigDecimal(row, columns.get("BOOK_VALUE")));
                item.setStatusRatio(getBigDecimal(row, columns.get("STATUS_RATIO")));
                item.setAssetCondition(getString(row, columns.get("STATUS")));
                item.setBranchCode(getString(row, columns.get("BRANCH")));
                item.setMainDepCode(getString(row, columns.get("MAIN_DEP")));
                item.setAssetDate(getDate(row, columns.get("DATE")));

                items.add(item);
                locationNameSet.add(locationName);

                int currentProgress = calculateReadProgress(i, lastRowNum);

                if (currentProgress > lastSavedProgress) {
                    lastSavedProgress = currentProgress;
                    updateJob(job, currentProgress, "Reading asset Excel rows: " + i + " / " + lastRowNum);
                }
            }

            log.info(
                    "Asset Excel reading finished. jobId={}, totalRows={}, validItems={}, locations={}, floors={}, places={}, categories={}, errors={}, duplicatedBarcodeCount={}",
                    jobId,
                    result.getTotalRows(),
                    items.size(),
                    locationsMap.size(),
                    floorsMap.size(),
                    placesMap.size(),
                    categoriesMap.size(),
                    result.getErrors().size(),
                    result.getDuplicatedBarcodeCount()
            );
        }
    }

    private Sheet findSheet(Workbook workbook) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet.getSheetName() != null && TARGET_SHEET_NAME.equalsIgnoreCase(sheet.getSheetName().trim())) {
                return sheet;
            }
        }

        throw new RuntimeException("Required sheet not found: " + TARGET_SHEET_NAME);
    }

    private AssetInventoryLocation getOrCreateLocation(InventoryTask task, Map<String, AssetInventoryLocation> locationsMap, String locationName) {
        String key = normalizeKey(locationName);
        AssetInventoryLocation location = locationsMap.get(key);

        if (location == null) {
            location = new AssetInventoryLocation();
            location.setInventoryTask(task);
            location.setLocationName(locationName);
            location.setTotalAssets(0);
            locationsMap.put(key, location);
        }

        return location;
    }

    private AssetInventoryFloor getOrCreateFloor(InventoryTask task, Map<String, AssetInventoryFloor> floorsMap, AssetInventoryLocation location, String floorName) {
        String key = normalizeKey(location.getLocationName()) + "||" + normalizeKey(floorName);
        AssetInventoryFloor floor = floorsMap.get(key);

        if (floor == null) {
            floor = new AssetInventoryFloor();
            floor.setInventoryTask(task);
            floor.setLocation(location);
            floor.setFloorName(floorName);
            floor.setTotalAssets(0);
            floorsMap.put(key, floor);
        }

        return floor;
    }

    private AssetInventoryPlace getOrCreatePlace(InventoryTask task, Map<String, AssetInventoryPlace> placesMap, AssetInventoryLocation location, AssetInventoryFloor floor, String placeName) {
        String key = normalizeKey(location.getLocationName()) + "||" + normalizeKey(floor.getFloorName()) + "||" + normalizeKey(placeName);
        AssetInventoryPlace place = placesMap.get(key);

        if (place == null) {
            place = new AssetInventoryPlace();
            place.setInventoryTask(task);
            place.setLocation(location);
            place.setFloor(floor);
            place.setPlaceName(placeName);
            place.setTotalAssets(0);
            placesMap.put(key, place);
        }

        return place;
    }

    private AssetInventoryCategory getOrCreateCategory(InventoryTask task, Map<String, AssetInventoryCategory> categoriesMap, String assetCategory, String assetType, String categoryCode) {
        String key = normalizeKey(assetCategory) + "||" + normalizeKey(assetType) + "||" + normalizeKey(categoryCode);
        AssetInventoryCategory category = categoriesMap.get(key);

        if (category == null) {
            category = new AssetInventoryCategory();
            category.setInventoryTask(task);
            category.setAssetCategory(assetCategory);
            category.setAssetType(assetType);
            category.setCategoryCode(categoryCode);
            category.setTotalAssets(0);
            categoriesMap.put(key, category);
        }

        return category;
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

        log.info("Asset import job progress updated. jobId={}, progress={}, message={}", job.getId(), progress, message);
    }

    private void completeJob(BackgroundJob job, AssetInventoryImportResult result) {
        job.setStatus(JobStatus.COMPLETED);
        job.setProgress(PROGRESS_COMPLETED);
        job.setMessage("Asset import completed successfully");
        job.setResult(toJson(result));
        job.setErrorMessage(null);
        job.setFinishedAt(LocalDateTime.now());

        backgroundJobRepository.save(job);
    }

    private void failJob(BackgroundJob job, AssetInventoryImportResult result, Exception ex) {
        job.setStatus(JobStatus.FAILED);
        job.setProgress(PROGRESS_COMPLETED);
        job.setMessage("Asset import failed");
        job.setResult(toJson(result));
        job.setErrorMessage(ex.getMessage());
        job.setFinishedAt(LocalDateTime.now());

        backgroundJobRepository.save(job);
    }

    private String toJson(AssetInventoryImportResult result) {
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

        int range = PROGRESS_READING_MAX - PROGRESS_START;
        int progress = PROGRESS_START + (int) (((double) currentRow / totalRows) * range);
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
        List<String> requiredColumns = List.of(
                "BARCODE",
                "ASSETCATEGORY",
                "LOCATION",
                "FLOOR",
                "PLACE"
        );

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

        if (value == null) {
            return null;
        }

        return value.trim();
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

        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeColumnName(String value) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
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
        return value.trim();
    }

    private String normalizeOrDefault(String value, String defaultValue) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank()) {
            return defaultValue;
        }
        return normalized;
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}

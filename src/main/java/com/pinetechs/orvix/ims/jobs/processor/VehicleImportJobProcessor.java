package com.pinetechs.orvix.ims.jobs.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinetechs.orvix.ims.file.service.UploadedFileService;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.vehicle.dto.VehicleInventoryImportResult;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryItem;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryLocation;
import com.pinetechs.orvix.ims.inventory.vehicle.service.VehicleInventoryImportPersistenceService;
import com.pinetechs.orvix.ims.jobs.entity.BackgroundJob;
import com.pinetechs.orvix.ims.jobs.enums.JobStatus;
import com.pinetechs.orvix.ims.jobs.enums.JobType;
import com.pinetechs.orvix.ims.jobs.repository.BackgroundJobRepository;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class VehicleImportJobProcessor implements BackgroundJobProcessor {

    private static final Logger log = LoggerFactory.getLogger(VehicleImportJobProcessor.class);

    private static final int PROGRESS_START = 10;
    private static final int PROGRESS_READING_MAX = 80;
    private static final int PROGRESS_SAVING = 90;
    private static final int PROGRESS_COMPLETED = 100;

    private final BackgroundJobRepository backgroundJobRepository;
    private final VehicleInventoryImportPersistenceService persistenceService;
    private final UploadedFileService uploadedFileService;
    private final ObjectMapper objectMapper;

    public VehicleImportJobProcessor(
            BackgroundJobRepository backgroundJobRepository,
            VehicleInventoryImportPersistenceService persistenceService,
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
        return JobType.VEHICLE_IMPORT;
    }

    @Override
    public void process(BackgroundJob job) {

        Long jobId = job.getId();
        Long taskId = job.getRelatedId();

        VehicleInventoryImportResult result = new VehicleInventoryImportResult();
        result.setJobId(jobId);
        result.setTaskId(taskId);

        log.info(
                "Vehicle import job started. jobId={}, taskId={}, uploadedFileId={}, file={}",
                jobId,
                taskId,
                job.getUploadedFileId(),
                job.getRelatedFile()
        );

        try {
            updateJob(job, PROGRESS_START, "Vehicle import job started");

            InventoryTask task = persistenceService.markImportInProgress(taskId);

            validateJobFile(job);

            Path path = Paths.get(job.getRelatedFile());

            List<VehicleInventoryItem> items = new ArrayList<>();
            Map<String, VehicleInventoryLocation> locationsMap = new LinkedHashMap<>();
            Set<String> vinSet = new HashSet<>();
            Set<String> storeNoSet = new LinkedHashSet<>();
            Set<String> locationNameSet = new LinkedHashSet<>();

            readExcelFile(
                    job,
                    task,
                    path,
                    result,
                    items,
                    locationsMap,
                    vinSet,
                    storeNoSet,
                    locationNameSet
            );

            if (!result.getErrors().isEmpty()) {
                String firstErrors = result.getErrors()
                        .stream()
                        .limit(10)
                        .collect(Collectors.joining("; "));

                throw new RuntimeException("Vehicle import validation failed: " + firstErrors);
            }

            updateJob(job, PROGRESS_SAVING, "Saving vehicle inventory data to database");

            log.info(
                    "Saving vehicle inventory import result. jobId={}, taskId={}, items={}, locations={}",
                    jobId,
                    taskId,
                    items.size(),
                    locationsMap.size()
            );

            persistenceService.replaceVehicleInventoryData(
                    taskId,
                    items,
                    locationsMap.values()
            );

            result.setImportedRows(items.size());
            result.setImportedItems(items.size());
            result.setLocationCount(locationsMap.size());
            result.setStoreNoCount(storeNoSet.size());
            result.setLocations(new ArrayList<>(locationNameSet));
            result.setStoreNos(new ArrayList<>(storeNoSet));

            completeJob(job, result);

            log.info(
                    "Vehicle import job completed successfully. jobId={}, taskId={}, importedRows={}, locations={}",
                    jobId,
                    taskId,
                    items.size(),
                    locationsMap.size()
            );

        } catch (Exception ex) {
            log.error(
                    "Vehicle import job failed. jobId={}, taskId={}, uploadedFileId={}, file={}",
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
                    "Vehicle import uploaded file marked as deleted. jobId={}, uploadedFileId={}",
                    jobId,
                    job.getUploadedFileId()
            );
        }
    }

    private void readExcelFile(
            BackgroundJob job,
            InventoryTask task,
            Path path,
            VehicleInventoryImportResult result,
            List<VehicleInventoryItem> items,
            Map<String, VehicleInventoryLocation> locationsMap,
            Set<String> vinSet,
            Set<String> storeNoSet,
            Set<String> locationNameSet
    ) throws Exception {

        Long jobId = job.getId();

        log.info("Opening Excel file. jobId={}, path={}", jobId, path);

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

            log.info("Excel columns loaded. jobId={}, columns={}", jobId, columns.keySet());

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

            log.info("Excel reading started. jobId={}, totalPossibleRows={}", jobId, lastRowNum);

            int lastSavedProgress = PROGRESS_START;

            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);

                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                result.setTotalRows(result.getTotalRows() + 1);

                String vinNo = normalize(getString(row, columns.get("VIN_NO")));
                String storeNo = normalize(getString(row, columns.get("ST_STORE_NO")));
                String locationName = normalize(getString(row, columns.get("LOCATION")));

                if (vinNo == null || vinNo.isBlank()) {
                    result.getErrors().add("Row " + (i + 1) + ": VIN_NO is required");
                    continue;
                }

                if (!vinSet.add(vinNo)) {
                    result.setDuplicatedVinCount(result.getDuplicatedVinCount() + 1);
                    result.getErrors().add("Row " + (i + 1) + ": Duplicate VIN in file: " + vinNo);
                    continue;
                }

                if (storeNo == null || storeNo.isBlank()) {
                    result.getErrors().add("Row " + (i + 1) + ": ST_STORE_NO is required");
                    continue;
                }

                if (locationName == null || locationName.isBlank()) {
                    result.getErrors().add("Row " + (i + 1) + ": LOCATION is required");
                    continue;
                }

                VehicleInventoryLocation existingLocation = locationsMap.get(storeNo);

                if (existingLocation != null &&
                        !existingLocation.getLocationName().equalsIgnoreCase(locationName)) {
                    result.getErrors().add(
                            "Row " + (i + 1)
                                    + ": ST_STORE_NO " + storeNo
                                    + " has multiple LOCATION values"
                    );
                    continue;
                }

                VehicleInventoryItem item = new VehicleInventoryItem();
                item.setInventoryTask(task);
                item.setPartNo(getString(row, columns.get("PART_NO")));
                item.setMake(getString(row, columns.get("MAKE")));
                item.setModelName(getString(row, columns.get("MODEL_NAME")));
                item.setModelYear(getInteger(row, columns.get("YEAR")));
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

                storeNoSet.add(storeNo);
                locationNameSet.add(locationName);

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

                int currentProgress = calculateReadProgress(i, lastRowNum);

                if (currentProgress > lastSavedProgress) {
                    lastSavedProgress = currentProgress;
                    updateJob(job, currentProgress, "Reading Excel rows: " + i + " / " + lastRowNum);
                }
            }

            log.info(
                    "Excel reading finished. jobId={}, totalRows={}, validItems={}, locations={}, errors={}, duplicatedVinCount={}",
                    jobId,
                    result.getTotalRows(),
                    items.size(),
                    locationsMap.size(),
                    result.getErrors().size(),
                    result.getDuplicatedVinCount()
            );
        }
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

        log.info(
                "Vehicle import job progress updated. jobId={}, progress={}, message={}",
                job.getId(),
                progress,
                message
        );
    }

    private void completeJob(BackgroundJob job, VehicleInventoryImportResult result) {
        job.setStatus(JobStatus.COMPLETED);
        job.setProgress(PROGRESS_COMPLETED);
        job.setMessage("Vehicle import completed successfully");
        job.setResult(toJson(result));
        job.setErrorMessage(null);
        job.setFinishedAt(LocalDateTime.now());

        backgroundJobRepository.save(job);
    }

    private void failJob(BackgroundJob job, VehicleInventoryImportResult result, Exception ex) {
        job.setStatus(JobStatus.FAILED);
        job.setProgress(PROGRESS_COMPLETED);
        job.setMessage("Vehicle import failed");
        job.setResult(toJson(result));
        job.setErrorMessage(ex.getMessage());
        job.setFinishedAt(LocalDateTime.now());

        backgroundJobRepository.save(job);
    }

    private String toJson(VehicleInventoryImportResult result) {
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
                columns.put(columnName.trim(), cell.getColumnIndex());
            }
        }

        return columns;
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

    private Integer getInteger(Row row, Integer index) {
        String value = getString(row, index);

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Double.valueOf(value).intValue();
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

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        return value.trim();
    }

    private List<String> getMissingRequiredColumns(Map<String, Integer> columns) {
        List<String> requiredColumns = List.of(
                "VIN_NO",
                "ST_STORE_NO",
                "LOCATION"
        );

        List<String> missing = new ArrayList<>();

        for (String requiredColumn : requiredColumns) {
            if (!columns.containsKey(requiredColumn)) {
                missing.add(requiredColumn);
            }
        }

        return missing;
    }
}

package com.pinetechs.orvix.ims.inventory.vehicle.service.impl;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.company.entity.Company;
import com.pinetechs.orvix.ims.file.entity.UploadedFile;
import com.pinetechs.orvix.ims.file.service.UploadedFileService;
import com.pinetechs.orvix.ims.inventory.common.dto.UploadExcelResponse;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.dto.VehicleInventoryItemResponse;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryItem;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryItemRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryLocationRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.service.VehicleInventoryImportService;
import com.pinetechs.orvix.ims.jobs.entity.BackgroundJob;
import com.pinetechs.orvix.ims.jobs.enums.JobStatus;
import com.pinetechs.orvix.ims.jobs.enums.JobType;
import com.pinetechs.orvix.ims.jobs.repository.BackgroundJobRepository;
import com.pinetechs.orvix.ims.security.AccessPolicyService;
import com.pinetechs.orvix.ims.user.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@Transactional
public class VehicleInventoryImportServiceImpl implements VehicleInventoryImportService {

    private final InventoryTaskRepository inventoryTaskRepository;
    private final VehicleInventoryItemRepository itemRepository;
    private final VehicleInventoryLocationRepository locationRepository;
    private final AccessPolicyService accessPolicyService;
    private final UploadedFileService uploadedFileService;
    private final BackgroundJobRepository backgroundJobRepository;

    public VehicleInventoryImportServiceImpl(
            InventoryTaskRepository inventoryTaskRepository,
            VehicleInventoryItemRepository itemRepository,
            VehicleInventoryLocationRepository locationRepository,
            AccessPolicyService accessPolicyService,
            UploadedFileService uploadedFileService,
            BackgroundJobRepository backgroundJobRepository
    ) {
        this.inventoryTaskRepository = inventoryTaskRepository;
        this.itemRepository = itemRepository;
        this.locationRepository = locationRepository;
        this.accessPolicyService = accessPolicyService;
        this.uploadedFileService = uploadedFileService;
        this.backgroundJobRepository = backgroundJobRepository;
    }

    @Override
    @Transactional
    public UploadExcelResponse uploadExcel(Long taskId, MultipartFile file, User currentUser) {
        InventoryTask task = inventoryTaskRepository.findById(taskId).orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Inventory task not found"));

        Company company = task.getCompany();

        if (task.getInventoryDomain() != InventoryDomain.VEHICLE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Task is not a vehicle inventory task");
        }

        if (task.getStatus() != InventoryTaskStatus.CREATED && task.getStatus() != InventoryTaskStatus.IMPORT_FAILED && task.getStatus() != InventoryTaskStatus.IMPORT_COMPLETED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Excel can be imported only when task is in CREATED or IMPORT_FAILED status");
        }

        accessPolicyService.assertCanImportExcel(
                currentUser,
                company.getId(),
                InventoryDomain.VEHICLE
        );

        try {
            UploadedFile uploadedFile = uploadedFileService.uploadExcel(
                    file,
                    "vehicle-inventory",
                    true,
                    false
            );

            BackgroundJob job = new BackgroundJob();
            job.setJobType(JobType.VEHICLE_IMPORT);
            job.setStatus(JobStatus.PENDING);
            job.setJobName("Vehicle Inventory Import for Task ID: " + taskId);
            job.setRelatedId(taskId);
            job.setRelatedFile(uploadedFile.getFilePath());
            job.setUploadedFileId(uploadedFile.getId());
            job.setMaxRetry(3);
            job.setProgress(0);
            job.setMessage("Vehicle import job created");
            job.setScheduledTime(LocalDateTime.now());

            job.setPayload(
                    "{"
                            + "\"taskId\":" + taskId + ","
                            + "\"uploadedFileId\":" + uploadedFile.getId() + ","
                            + "\"fileName\":\"" + uploadedFile.getFileName() + "\"," 
                            + "\"originalFileName\":\"" + uploadedFile.getOriginalFileName() + "\""
                            + "}"
            );

            job = backgroundJobRepository.save(job);

            task.setStatus(InventoryTaskStatus.IMPORT_PENDING);
            task.setImportJobId(job.getId());
            inventoryTaskRepository.save(task);

            UploadExcelResponse response = new UploadExcelResponse();
            response.setJobId(job.getId());
            response.setTaskId(taskId);
            response.setUploadedFileId(uploadedFile.getId());
            response.setFileName(uploadedFile.getFileName());
            response.setOriginalFileName(uploadedFile.getOriginalFileName());
            response.setFilePath(uploadedFile.getPublicUrl());
            response.setStatus(task.getStatus().name());

            return response;

        } catch (IllegalArgumentException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ex.getMessage());

        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload Excel file");
        }
    }

    @Override
    @Transactional
    public Page<VehicleInventoryItemResponse> getImportedVehicleItems(Long taskId, Pageable pageable, User currentUser) {
        InventoryTask task = inventoryTaskRepository.findById(taskId).orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Inventory task not found"));

        if (task.getInventoryDomain() != InventoryDomain.VEHICLE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Task is not a vehicle inventory task");
        }

        Company company = task.getCompany();

        accessPolicyService.assertCanViewInventoryTask(currentUser, company.getId(), InventoryDomain.VEHICLE);

        return itemRepository.findByInventoryTaskIdOrderByIdAsc(taskId, pageable).map(this::toResponse);
    }

    private VehicleInventoryItemResponse toResponse(VehicleInventoryItem item) {
        VehicleInventoryItemResponse response = new VehicleInventoryItemResponse();

        response.setId(item.getId());
        response.setPartNo(item.getPartNo());
        response.setMake(item.getMake());
        response.setModelName(item.getModelName());
        response.setModelYear(item.getModelYear());
        response.setVinNo(item.getVinNo());
        response.setSpecification(item.getSpecification());
        response.setQuantity(item.getQuantity());
        response.setReceiptDate(item.getReceiptDate());
        response.setColorNo(item.getColorNo());
        response.setInteriorColor(item.getInteriorColor());
        response.setMchStatus(item.getMchStatus());
        response.setStockStatus(item.getStockStatus());
        response.setLocation(item.getLocation());
        response.setStoreNo(item.getStoreNo());
        response.setDarArtId(item.getDarArtId());

        return response;
    }
}

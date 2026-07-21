package com.pinetechs.orvix.ims.inventory.sparepart.service.impl;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.company.entity.Company;
import com.pinetechs.orvix.ims.file.entity.UploadedFile;
import com.pinetechs.orvix.ims.file.service.UploadedFileService;
import com.pinetechs.orvix.ims.inventory.common.dto.UploadExcelResponse;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.sparepart.dto.SparePartInventoryItemResponse;
import com.pinetechs.orvix.ims.inventory.sparepart.repository.SparePartInventoryItemRepository;
import com.pinetechs.orvix.ims.inventory.sparepart.service.SparePartInventoryImportService;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.inventory.task.enums.InventoryTaskActivityType;
import com.pinetechs.orvix.ims.inventory.task.service.InventoryTaskActivityService;
import com.pinetechs.orvix.ims.jobs.entity.BackgroundJob;
import com.pinetechs.orvix.ims.jobs.enums.JobStatus;
import com.pinetechs.orvix.ims.jobs.enums.JobType;
import com.pinetechs.orvix.ims.jobs.repository.BackgroundJobRepository;
import com.pinetechs.orvix.ims.security.AccessPolicyService;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@Transactional
public class SparePartInventoryImportServiceImpl implements SparePartInventoryImportService {

    private final InventoryTaskRepository inventoryTaskRepository;
    private final SparePartInventoryItemRepository itemRepository;
    private final AccessPolicyService accessPolicyService;
    private final UploadedFileService uploadedFileService;
    private final BackgroundJobRepository backgroundJobRepository;
    private final InventoryTaskActivityService taskActivityService;

    public SparePartInventoryImportServiceImpl(
            InventoryTaskRepository inventoryTaskRepository,
            SparePartInventoryItemRepository itemRepository,
            AccessPolicyService accessPolicyService,
            UploadedFileService uploadedFileService,
            BackgroundJobRepository backgroundJobRepository,
            InventoryTaskActivityService taskActivityService
    ) {
        this.inventoryTaskRepository = inventoryTaskRepository;
        this.itemRepository = itemRepository;
        this.accessPolicyService = accessPolicyService;
        this.uploadedFileService = uploadedFileService;
        this.backgroundJobRepository = backgroundJobRepository;
        this.taskActivityService = taskActivityService;
    }

    @Override
    @Transactional
    public UploadExcelResponse uploadExcel(Long taskId, MultipartFile file, User currentUser) {
        InventoryTask task = inventoryTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Inventory task not found"));

        Company company = task.getCompany();

        if (task.getInventoryDomain() != InventoryDomain.SPARE_PART) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Task is not a spare part inventory task");
        }

        if (task.getStatus() != InventoryTaskStatus.CREATED
                && task.getStatus() != InventoryTaskStatus.IMPORT_FAILED
                && task.getStatus() != InventoryTaskStatus.DRAFT
                && task.getStatus() != InventoryTaskStatus.IMPORT_COMPLETED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Excel can be imported only when task is in CREATED, DRAFT, IMPORT_FAILED or IMPORT_COMPLETED status");
        }

        accessPolicyService.assertCanImportExcel(currentUser, company.getId(), InventoryDomain.SPARE_PART);

        try {
            UploadedFile uploadedFile = uploadedFileService.uploadExcel(
                    file,
                    "spare-part-inventory",
                    true,
                    false
            );

            BackgroundJob job = new BackgroundJob();
            job.setJobType(JobType.SPARE_PART_IMPORT);
            job.setStatus(JobStatus.PENDING);
            job.setJobName("Spare Part Inventory Import for Task ID: " + taskId);
            job.setRelatedId(taskId);
            job.setRelatedFile(uploadedFile.getFilePath());
            job.setUploadedFileId(uploadedFile.getId());
            job.setMaxRetry(3);
            job.setProgress(0);
            job.setMessage("Spare part import job created");
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

            InventoryTaskStatus fromStatus = task.getStatus();
            task.setStatus(InventoryTaskStatus.IMPORT_PENDING);
            task.setImportJobId(job.getId());
            inventoryTaskRepository.save(task);
            taskActivityService.record(
                    task,
                    InventoryTaskActivityType.IMPORT_QUEUED,
                    fromStatus,
                    InventoryTaskStatus.IMPORT_PENDING,
                    currentUser,
                    null,
                    "jobId=" + job.getId() + ", file=" + uploadedFile.getOriginalFileName()
            );

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
    @Transactional(readOnly = true)
    public Page<SparePartInventoryItemResponse> getImportedSparePartItems(Long taskId, Pageable pageable, User currentUser) {
        InventoryTask task = inventoryTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Inventory task not found"));

        if (task.getInventoryDomain() != InventoryDomain.SPARE_PART) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Task is not a spare part inventory task");
        }

        accessPolicyService.assertCanViewInventoryTask(currentUser, task.getCompany().getId(), InventoryDomain.SPARE_PART);

        return itemRepository.findByInventoryTaskIdOrderByIdAsc(taskId, pageable)
                .map(SparePartInventoryItemResponse::from);
    }
}

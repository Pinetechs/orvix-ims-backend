package com.pinetechs.orvix.ims.inventory.task.service.impl;

import com.pinetechs.orvix.ims.company.entity.Company;
import com.pinetechs.orvix.ims.company.repository.CompanyRepository;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.task.repository.InventoryTaskRepository;
import com.pinetechs.orvix.ims.inventory.task.service.InventoryTaskService;
import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Transactional
public class InventoryTaskServiceImpl implements InventoryTaskService {

    private final InventoryTaskRepository inventoryTaskRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    public InventoryTaskServiceImpl(
            InventoryTaskRepository inventoryTaskRepository,
            CompanyRepository companyRepository,
            UserRepository userRepository
    ) {
        this.inventoryTaskRepository = inventoryTaskRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
    }

    @Override
    public InventoryTask createVehicleTask(Long companyId, Long createdByUserId, String notes) {
        Company company = companyRepository.findById(companyId).orElseThrow(() -> new RuntimeException("Company not found"));
        User createdBy = userRepository.findById(createdByUserId).orElseThrow(() -> new RuntimeException("User not found"));

        InventoryTask task = new InventoryTask();
        task.setTaskNumber(generateTaskNumber());
        task.setCompany(company);
        task.setCreatedBy(createdBy);
        task.setInventoryDomain(InventoryDomain.VEHICLE);
        task.setStatus(InventoryTaskStatus.DRAFT);
        task.setNotes(notes);

        return inventoryTaskRepository.save(task);
    }

    @Override
    public InventoryTask startTask(Long taskId) {
        InventoryTask task = getTask(taskId);

        if (task.getStatus() != InventoryTaskStatus.READY_FOR_ASSIGNMENT
                && task.getStatus() != InventoryTaskStatus.PAUSED) {
            throw new RuntimeException("Task cannot be started from status: " + task.getStatus());
        }

        task.setStatus(InventoryTaskStatus.IN_PROGRESS);

        if (task.getStartDate() == null) {
            task.setStartDate(LocalDate.now());
        }

        task.setPausedAt(null);
        task.setPauseReason(null);

        return inventoryTaskRepository.save(task);
    }

    @Override
    public InventoryTask pauseTask(Long taskId, String pauseReason) {
        InventoryTask task = getTask(taskId);

        if (task.getStatus() != InventoryTaskStatus.IN_PROGRESS) {
            throw new RuntimeException("Only IN_PROGRESS task can be paused");
        }

        task.setStatus(InventoryTaskStatus.PAUSED);
        task.setPausedAt(LocalDateTime.now());
        task.setPauseReason(pauseReason);

        return inventoryTaskRepository.save(task);
    }

    @Override
    public InventoryTask resumeTask(Long taskId) {
        InventoryTask task = getTask(taskId);

        if (task.getStatus() != InventoryTaskStatus.PAUSED) {
            throw new RuntimeException("Only PAUSED task can be resumed");
        }

        task.setStatus(InventoryTaskStatus.IN_PROGRESS);
        task.setPausedAt(null);
        task.setPauseReason(null);

        return inventoryTaskRepository.save(task);
    }

    @Override
    public InventoryTask moveToReview(Long taskId) {
        InventoryTask task = getTask(taskId);

        if (task.getStatus() != InventoryTaskStatus.IN_PROGRESS) {
            throw new RuntimeException("Only IN_PROGRESS task can move to review");
        }

        task.setStatus(InventoryTaskStatus.UNDER_REVIEW);

        return inventoryTaskRepository.save(task);
    }

    @Override
    public InventoryTask completeTask(Long taskId) {
        InventoryTask task = getTask(taskId);

        if (task.getStatus() != InventoryTaskStatus.UNDER_REVIEW) {
            throw new RuntimeException("Only UNDER_REVIEW task can be completed");
        }

        task.setStatus(InventoryTaskStatus.COMPLETED);
        task.setClosedAt(LocalDateTime.now());

        return inventoryTaskRepository.save(task);
    }

    @Override
    public InventoryTask cancelTask(Long taskId, String cancelReason) {
        InventoryTask task = getTask(taskId);

        if (task.getStatus() == InventoryTaskStatus.COMPLETED) {
            throw new RuntimeException("Completed task cannot be cancelled");
        }

        task.setStatus(InventoryTaskStatus.CANCELLED);
        task.setClosedAt(LocalDateTime.now());
        task.setCancelReason(cancelReason);

        return inventoryTaskRepository.save(task);
    }

    private InventoryTask getTask(Long taskId) {
        return inventoryTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Inventory task not found"));
    }

    private String generateTaskNumber() {
        return "INV-" + System.currentTimeMillis();
    }
}
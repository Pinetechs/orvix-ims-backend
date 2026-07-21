package com.pinetechs.orvix.ims.inventory.task.repository;

import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTaskActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryTaskActivityRepository extends JpaRepository<InventoryTaskActivity, Long> {

    Page<InventoryTaskActivity> findByInventoryTaskIdOrderByPerformedAtDescIdDesc(
            Long taskId,
            Pageable pageable
    );

    List<InventoryTaskActivity> findByInventoryTaskIdOrderByPerformedAtAscIdAsc(Long taskId);

    @Modifying(flushAutomatically = true)
    @Query("delete from InventoryTaskActivity activity where activity.inventoryTask.id = :taskId")
    void deleteByInventoryTaskId(@Param("taskId") Long taskId);
}

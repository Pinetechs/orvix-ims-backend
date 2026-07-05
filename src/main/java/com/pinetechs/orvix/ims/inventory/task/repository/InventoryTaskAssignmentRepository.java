package com.pinetechs.orvix.ims.inventory.task.repository;

import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryTaskAssignmentRepository extends JpaRepository<InventoryTaskAssignment, Long> {

    List<InventoryTaskAssignment> findByInventoryTaskId(Long taskId);

    Optional<InventoryTaskAssignment> findByInventoryTaskIdAndUserId(Long taskId, Long userId);

    boolean existsByInventoryTaskIdAndUserId(Long taskId, Long userId);

    long countByInventoryTaskId(Long taskId);

    @Query("""
           select assignment
           from InventoryTaskAssignment assignment
           join fetch assignment.user
           where assignment.inventoryTask.id = :taskId
             and assignment.active = true
           """)
    List<InventoryTaskAssignment> findActiveByInventoryTaskIdWithUser(@Param("taskId") Long taskId);

    @Modifying(flushAutomatically = true)
    @Query("""
           delete from InventoryTaskAssignment assignment
           where assignment.inventoryTask.id = :taskId
           """)
    int deleteByInventoryTaskId(@Param("taskId") Long taskId);
}

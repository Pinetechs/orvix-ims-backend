package com.pinetechs.orvix.ims.inventory.task.repository;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTaskAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryTaskAssignmentRepository extends JpaRepository<InventoryTaskAssignment, Long> {

    List<InventoryTaskAssignment> findByInventoryTaskId(Long taskId);

    Optional<InventoryTaskAssignment> findByInventoryTaskIdAndUserId(Long taskId, Long userId);

    @Query("""
       select assignment
       from InventoryTaskAssignment assignment
       join fetch assignment.inventoryTask task
       join fetch task.company company
       where assignment.user.id = :userId
         and assignment.active = true
         and task.status in :statuses
       order by task.startDate asc, task.createdAt desc
       """)
    Slice<InventoryTaskAssignment> findActiveByUserIdWithTaskAndCompany(
            @Param("userId") Long userId,
            @Param("statuses") Collection<InventoryTaskStatus> statuses,
            Pageable pageable
    );




    @Query("select count(i) from InventoryTaskAssignment i where i.active = true and  i.user.id = ?1 and i.inventoryTask.status = ?2")
    long countActiveByUserIdAndStatus(Long id, InventoryTaskStatus status);

    @Query("select count(i) from InventoryTaskAssignment i where i.active = true and  i.user.id = ?1")
    long countActiveByUserId(Long id);







    @Query("""
           select assignment
           from InventoryTaskAssignment assignment
           join fetch assignment.inventoryTask task
           join fetch task.company
           where task.id = :taskId
             and assignment.user.id = :userId
             and assignment.active = true
           """)
    Optional<InventoryTaskAssignment> findActiveByTaskIdAndUserIdWithTaskAndCompany(
            @Param("taskId") Long taskId,
            @Param("userId") Long userId
    );

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

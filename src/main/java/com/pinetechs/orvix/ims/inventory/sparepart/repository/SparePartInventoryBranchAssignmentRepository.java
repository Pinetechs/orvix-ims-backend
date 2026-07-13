package com.pinetechs.orvix.ims.inventory.sparepart.repository;

import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryBranchAssignment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SparePartInventoryBranchAssignmentRepository extends JpaRepository<SparePartInventoryBranchAssignment, Long> {

    List<SparePartInventoryBranchAssignment> findByAssignmentId(Long assignmentId);

    List<SparePartInventoryBranchAssignment> findByBranchId(Long branchId);

    boolean existsByAssignmentIdAndBranchId(Long assignmentId, Long branchId);

    long countByAssignmentId(Long assignmentId);

    long countByBranchId(Long branchId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           delete from SparePartInventoryBranchAssignment branchAssignment
           where branchAssignment.assignment.id in (
               select assignment.id
               from InventoryTaskAssignment assignment
               where assignment.inventoryTask.id = :taskId
           )
           """)
    int deleteByTaskId(@Param("taskId") Long taskId);

    @Query("""
           select branchAssignment
           from SparePartInventoryBranchAssignment branchAssignment
           join fetch branchAssignment.branch
           where branchAssignment.assignment.id = :assignmentId
             and branchAssignment.active = true
           """)
    List<SparePartInventoryBranchAssignment> findActiveByAssignmentIdWithBranch(@Param("assignmentId") Long assignmentId);

    @Query("""
           select branchAssignment
           from SparePartInventoryBranchAssignment branchAssignment
           join fetch branchAssignment.branch
           where branchAssignment.assignment.inventoryTask.id = :taskId
             and branchAssignment.assignment.user.id = :userId
             and branchAssignment.active = true
             and branchAssignment.assignment.active = true
           """)
    List<SparePartInventoryBranchAssignment> findActiveByTaskIdAndUserIdWithBranch(
            @Param("taskId") Long taskId,
            @Param("userId") Long userId
    );





    @Query("""
           select branchAssignment
           from SparePartInventoryBranchAssignment branchAssignment
           join fetch branchAssignment.branch
           where branchAssignment.assignment.inventoryTask.id = :taskId
             and branchAssignment.assignment.user.id = :userId
             and branchAssignment.active = true
             and branchAssignment.assignment.active = true
           """)
    Slice<SparePartInventoryBranchAssignment> findActiveByTaskIdAndUserIdWithBranchSlice(
            @Param("taskId") Long taskId,
            @Param("userId") Long userId,
            Pageable pageable
    );


    @Query("""
           select case when count(branchAssignment.id) > 0 then true else false end
           from SparePartInventoryBranchAssignment branchAssignment
           where branchAssignment.assignment.inventoryTask.id = :taskId
             and branchAssignment.assignment.user.id = :userId
             and branchAssignment.branch.id = :branchId
             and branchAssignment.active = true
             and branchAssignment.assignment.active = true
           """)
    boolean existsActiveByTaskIdAndUserIdAndBranchId(
            @Param("taskId") Long taskId,
            @Param("userId") Long userId,
            @Param("branchId") Long branchId
    );
}

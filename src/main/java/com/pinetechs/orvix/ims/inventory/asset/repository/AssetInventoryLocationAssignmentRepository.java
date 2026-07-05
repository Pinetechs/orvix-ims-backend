package com.pinetechs.orvix.ims.inventory.asset.repository;

import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryLocationAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssetInventoryLocationAssignmentRepository extends JpaRepository<AssetInventoryLocationAssignment, Long> {

    List<AssetInventoryLocationAssignment> findByAssignmentId(Long assignmentId);

    List<AssetInventoryLocationAssignment> findByLocationId(Long locationId);

    boolean existsByAssignmentIdAndLocationId(Long assignmentId, Long locationId);

    long countByAssignmentId(Long assignmentId);

    long countByLocationId(Long locationId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           delete from AssetInventoryLocationAssignment locationAssignment
           where locationAssignment.assignment.id in (
               select assignment.id
               from InventoryTaskAssignment assignment
               where assignment.inventoryTask.id = :taskId
           )
           """)
    int deleteByTaskId(@Param("taskId") Long taskId);

    @Query("""
           select count(distinct locationAssignment.location.id)
           from AssetInventoryLocationAssignment locationAssignment
           where locationAssignment.assignment.inventoryTask.id = :taskId
             and locationAssignment.active = true
             and locationAssignment.assignment.active = true
           """)
    long countDistinctActiveLocationsByTaskId(@Param("taskId") Long taskId);

    @Query("""
           select locationAssignment
           from AssetInventoryLocationAssignment locationAssignment
           join fetch locationAssignment.location
           where locationAssignment.assignment.id = :assignmentId
             and locationAssignment.active = true
           """)
    List<AssetInventoryLocationAssignment> findActiveByAssignmentIdWithLocation(@Param("assignmentId") Long assignmentId);

    @Query("""
           select locationAssignment
           from AssetInventoryLocationAssignment locationAssignment
           join fetch locationAssignment.location
           where locationAssignment.assignment.inventoryTask.id = :taskId
             and locationAssignment.assignment.user.id = :userId
             and locationAssignment.active = true
             and locationAssignment.assignment.active = true
           """)
    List<AssetInventoryLocationAssignment> findActiveByTaskIdAndUserIdWithLocation(
            @Param("taskId") Long taskId,
            @Param("userId") Long userId
    );

    @Query("""
           select case when count(locationAssignment.id) > 0 then true else false end
           from AssetInventoryLocationAssignment locationAssignment
           where locationAssignment.assignment.inventoryTask.id = :taskId
             and locationAssignment.assignment.user.id = :userId
             and locationAssignment.location.id = :locationId
             and locationAssignment.active = true
             and locationAssignment.assignment.active = true
           """)
    boolean existsActiveByTaskIdAndUserIdAndLocationId(
            @Param("taskId") Long taskId,
            @Param("userId") Long userId,
            @Param("locationId") Long locationId
    );
}

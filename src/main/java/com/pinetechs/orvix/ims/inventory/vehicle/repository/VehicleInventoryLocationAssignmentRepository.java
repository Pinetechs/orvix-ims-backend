package com.pinetechs.orvix.ims.inventory.vehicle.repository;

import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryLocationAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VehicleInventoryLocationAssignmentRepository extends JpaRepository<VehicleInventoryLocationAssignment, Long> {

    List<VehicleInventoryLocationAssignment> findByAssignmentId(
            Long assignmentId
    );

    List<VehicleInventoryLocationAssignment> findByLocationId(
            Long locationId
    );

    boolean existsByAssignmentIdAndLocationId(
            Long assignmentId,
            Long locationId
    );

    long countByAssignmentId(Long assignmentId);

    long countByLocationId(Long locationId);

    @Modifying(flushAutomatically = true)
    @Query("""
           delete from VehicleInventoryLocationAssignment locationAssignment
           where locationAssignment.assignment.inventoryTask.id = :taskId
           """)
    int deleteByTaskId2(@Param("taskId") Long taskId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
    delete from VehicleInventoryLocationAssignment v
    where v.assignment.id in (
        select a.id
        from InventoryTaskAssignment a
        where a.inventoryTask.id = :taskId
    )
""")
    int deleteByTaskId(@Param("taskId") Long taskId);




    @Query("""
           select count(distinct locationAssignment.location.id)
           from VehicleInventoryLocationAssignment locationAssignment
           where locationAssignment.assignment.inventoryTask.id = :taskId
             and locationAssignment.active = true
             and locationAssignment.assignment.active = true
           """)
    long countDistinctActiveLocationsByTaskId(@Param("taskId") Long taskId);

    @Query("""
           select locationAssignment
           from VehicleInventoryLocationAssignment locationAssignment
           join fetch locationAssignment.location
           where locationAssignment.assignment.id = :assignmentId
             and locationAssignment.active = true
           """)
    List<VehicleInventoryLocationAssignment> findActiveByAssignmentIdWithLocation(@Param("assignmentId") Long assignmentId);

    @Query("""
           select locationAssignment
           from VehicleInventoryLocationAssignment locationAssignment
           join fetch locationAssignment.location
           where locationAssignment.assignment.inventoryTask.id = :taskId
             and locationAssignment.assignment.user.id = :userId
             and locationAssignment.active = true
             and locationAssignment.assignment.active = true
           """)
    List<VehicleInventoryLocationAssignment> findActiveByTaskIdAndUserIdWithLocation(
            @Param("taskId") Long taskId,
            @Param("userId") Long userId
    );
}

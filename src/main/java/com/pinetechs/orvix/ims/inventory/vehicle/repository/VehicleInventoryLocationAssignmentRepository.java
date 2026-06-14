package com.pinetechs.orvix.ims.inventory.vehicle.repository;

import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryLocationAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleInventoryLocationAssignmentRepository
        extends JpaRepository<VehicleInventoryLocationAssignment, Long> {

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
}
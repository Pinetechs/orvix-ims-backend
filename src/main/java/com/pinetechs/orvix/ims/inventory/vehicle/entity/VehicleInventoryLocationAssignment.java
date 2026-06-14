package com.pinetechs.orvix.ims.inventory.vehicle.entity;


import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTaskAssignment;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "vehicle_inventory_location_assignments",
        indexes = {
                @Index(
                        name = "idx_vehicle_inv_loc_assign_assignment",
                        columnList = "assignment_id"
                ),
                @Index(
                        name = "idx_vehicle_inv_loc_assign_location",
                        columnList = "location_id"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vehicle_inv_loc_assignment",
                        columnNames = {"assignment_id", "location_id"}
                )
        }
)
public class VehicleInventoryLocationAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private InventoryTaskAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private VehicleInventoryLocation location;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "notes", length = 500)
    private String notes;

    public Long getId() {
        return id;
    }

    public InventoryTaskAssignment getAssignment() {
        return assignment;
    }

    public VehicleInventoryLocation getLocation() {
        return location;
    }

    public Boolean getActive() {
        return active;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setAssignment(InventoryTaskAssignment assignment) {
        this.assignment = assignment;
    }

    public void setLocation(VehicleInventoryLocation location) {
        this.location = location;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
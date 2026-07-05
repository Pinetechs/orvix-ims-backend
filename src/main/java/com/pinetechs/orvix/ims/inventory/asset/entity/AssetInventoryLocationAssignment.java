package com.pinetechs.orvix.ims.inventory.asset.entity;

import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTaskAssignment;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "asset_inventory_location_assignments",
        indexes = {
                @Index(name = "idx_asset_inv_loc_assign_assignment", columnList = "assignment_id"),
                @Index(name = "idx_asset_inv_loc_assign_location", columnList = "location_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_asset_inv_loc_assignment",
                        columnNames = {"assignment_id", "location_id"}
                )
        }
)
public class AssetInventoryLocationAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private InventoryTaskAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private AssetInventoryLocation location;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "notes", length = 500)
    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public InventoryTaskAssignment getAssignment() { return assignment; }
    public void setAssignment(InventoryTaskAssignment assignment) { this.assignment = assignment; }
    public AssetInventoryLocation getLocation() { return location; }
    public void setLocation(AssetInventoryLocation location) { this.location = location; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

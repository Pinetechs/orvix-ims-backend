package com.pinetechs.orvix.ims.inventory.vehicle.entity;

import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "vehicle_inventory_locations",
        indexes = {
                @Index(name = "idx_vehicle_inventory_locations_task",
                        columnList = "task_id"),

                @Index(name = "idx_vehicle_inventory_locations_store_no",
                        columnList = "store_no")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vehicle_inventory_location_task_store",
                        columnNames = {"task_id", "store_no"}
                )
        }
)
public class VehicleInventoryLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private InventoryTask inventoryTask;

    @Column(name = "store_no", nullable = false, length = 100)
    private String storeNo;

    @Column(name = "location_name", length = 255)
    private String locationName;

    @Column(name = "total_vehicles", nullable = false)
    private Integer totalVehicles = 0;

    @Column(name = "processed_vehicles", nullable = false)
    private Integer processedVehicles = 0;

    @Column(name = "matched_vehicles", nullable = false)
    private Integer matchedVehicles = 0;

    @Column(name = "missing_vehicles", nullable = false)
    private Integer missingVehicles = 0;

    @CreationTimestamp
    @Column(name = "created_at",
            nullable = false,
            updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @Transient
    public double getProgressPercentage() {

        if (totalVehicles == null || totalVehicles == 0) {
            return 0;
        }

        return (processedVehicles * 100.0) / totalVehicles;
    }



    public Long getId() {
        return id;
    }

    public InventoryTask getInventoryTask() {
        return inventoryTask;
    }

    public String getStoreNo() {
        return storeNo;
    }

    public String getLocationName() {
        return locationName;
    }

    public Integer getTotalVehicles() {
        return totalVehicles;
    }

    public Integer getProcessedVehicles() {
        return processedVehicles;
    }

    public Integer getMatchedVehicles() {
        return matchedVehicles;
    }

    public Integer getMissingVehicles() {
        return missingVehicles;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setInventoryTask(InventoryTask inventoryTask) {
        this.inventoryTask = inventoryTask;
    }

    public void setStoreNo(String storeNo) {
        this.storeNo = storeNo;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public void setTotalVehicles(Integer totalVehicles) {
        this.totalVehicles = totalVehicles;
    }

    public void setProcessedVehicles(Integer processedVehicles) {
        this.processedVehicles = processedVehicles;
    }

    public void setMatchedVehicles(Integer matchedVehicles) {
        this.matchedVehicles = matchedVehicles;
    }

    public void setMissingVehicles(Integer missingVehicles) {
        this.missingVehicles = missingVehicles;
    }
}
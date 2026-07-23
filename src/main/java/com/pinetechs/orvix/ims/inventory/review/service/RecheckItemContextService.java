package com.pinetechs.orvix.ims.inventory.review.service;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryItem;
import com.pinetechs.orvix.ims.inventory.asset.repository.AssetInventoryItemRepository;
import com.pinetechs.orvix.ims.inventory.review.entity.InventoryRecheckItem;
import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryItem;
import com.pinetechs.orvix.ims.inventory.sparepart.repository.SparePartInventoryItemRepository;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryItem;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryItemRepository;
import com.pinetechs.orvix.ims.inventory.vehicle.repository.VehicleInventoryLocationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Copies the expected physical context to a new recheck item.
 *
 * <p>The regular app hierarchy is tied to the original inventory assignment.
 * A recheck can be assigned to another employee, so the recheck itself must
 * carry the ids needed to submit a valid observed result.</p>
 */
@Service
public class RecheckItemContextService {

    private final VehicleInventoryItemRepository vehicleItemRepository;
    private final VehicleInventoryLocationRepository vehicleLocationRepository;
    private final AssetInventoryItemRepository assetItemRepository;
    private final SparePartInventoryItemRepository sparePartItemRepository;

    public RecheckItemContextService(
            VehicleInventoryItemRepository vehicleItemRepository,
            VehicleInventoryLocationRepository vehicleLocationRepository,
            AssetInventoryItemRepository assetItemRepository,
            SparePartInventoryItemRepository sparePartItemRepository
    ) {
        this.vehicleItemRepository = vehicleItemRepository;
        this.vehicleLocationRepository = vehicleLocationRepository;
        this.assetItemRepository = assetItemRepository;
        this.sparePartItemRepository = sparePartItemRepository;
    }

    public void applyExpectedContext(
            InventoryTask task,
            InventoryRecheckItem recheckItem
    ) {
        if (recheckItem.getReferenceItemId() == null) {
            return;
        }

        switch (task.getInventoryDomain()) {
            case VEHICLE -> applyVehicle(task, recheckItem);
            case ASSET -> applyAsset(task, recheckItem);
            case SPARE_PART -> applySparePart(task, recheckItem);
        }
    }

    private void applyVehicle(
            InventoryTask task,
            InventoryRecheckItem recheckItem
    ) {
        VehicleInventoryItem item = vehicleItemRepository
                .findById(recheckItem.getReferenceItemId())
                .filter(value -> task.getId().equals(value.getInventoryTask().getId()))
                .orElseThrow(() -> itemNotFound("Vehicle"));

        Long locationId = vehicleLocationRepository
                .findByInventoryTaskIdAndStoreNo(task.getId(), item.getStoreNo())
                .map(location -> location.getId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.CONFLICT,
                        "Vehicle expected location is not configured"
                ));
        recheckItem.setLocationId(locationId);
    }

    private void applyAsset(
            InventoryTask task,
            InventoryRecheckItem recheckItem
    ) {
        AssetInventoryItem item = assetItemRepository
                .findById(recheckItem.getReferenceItemId())
                .filter(value -> task.getId().equals(value.getInventoryTask().getId()))
                .orElseThrow(() -> itemNotFound("Asset"));

        recheckItem.setLocationId(item.getPlannedLocation().getId());
        recheckItem.setFloorId(item.getPlannedFloor().getId());
        recheckItem.setPlaceId(item.getPlannedPlace().getId());
    }

    private void applySparePart(
            InventoryTask task,
            InventoryRecheckItem recheckItem
    ) {
        SparePartInventoryItem item = sparePartItemRepository
                .findById(recheckItem.getReferenceItemId())
                .filter(value -> task.getId().equals(value.getInventoryTask().getId()))
                .orElseThrow(() -> itemNotFound("Spare-part"));

        recheckItem.setBranchId(item.getPlannedBranch().getId());
        recheckItem.setLocationId(item.getPlannedLocation().getId());
    }

    private BusinessException itemNotFound(String domain) {
        return new BusinessException(
                HttpStatus.NOT_FOUND,
                domain + " inventory item not found while creating recheck"
        );
    }
}

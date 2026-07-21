package com.pinetechs.orvix.ims.inventory.tracking.provider;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.tracking.repository.TrackingJdbcRepository;
import org.springframework.stereotype.Component;

@Component
public class VehicleTrackingProvider extends AbstractJdbcTrackingProvider {

    public VehicleTrackingProvider(TrackingJdbcRepository repository) {
        super(InventoryDomain.VEHICLE, repository);
    }
}

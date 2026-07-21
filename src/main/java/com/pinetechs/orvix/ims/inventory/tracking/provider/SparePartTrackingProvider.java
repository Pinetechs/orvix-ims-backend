package com.pinetechs.orvix.ims.inventory.tracking.provider;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.tracking.repository.TrackingJdbcRepository;
import org.springframework.stereotype.Component;

@Component
public class SparePartTrackingProvider extends AbstractJdbcTrackingProvider {

    public SparePartTrackingProvider(TrackingJdbcRepository repository) {
        super(InventoryDomain.SPARE_PART, repository);
    }
}

package com.pinetechs.orvix.ims.inventory.task.dto;

import com.pinetechs.orvix.ims.inventory.sparepart.enums.SparePartLocationProgressMode;

public class UpdateInventoryTaskScanSettingsRequest {

    private Boolean scanImageRequired;
    private SparePartLocationProgressMode sparePartLocationProgressMode;

    public Boolean getScanImageRequired() {
        return scanImageRequired;
    }

    public void setScanImageRequired(Boolean scanImageRequired) {
        this.scanImageRequired = scanImageRequired;
    }

    public SparePartLocationProgressMode getSparePartLocationProgressMode() {
        return sparePartLocationProgressMode;
    }

    public void setSparePartLocationProgressMode(SparePartLocationProgressMode sparePartLocationProgressMode) {
        this.sparePartLocationProgressMode = sparePartLocationProgressMode;
    }
}

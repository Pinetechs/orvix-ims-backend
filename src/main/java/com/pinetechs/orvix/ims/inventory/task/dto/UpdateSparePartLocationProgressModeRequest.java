package com.pinetechs.orvix.ims.inventory.task.dto;

import com.pinetechs.orvix.ims.inventory.sparepart.enums.SparePartLocationProgressMode;

public class UpdateSparePartLocationProgressModeRequest {

    private SparePartLocationProgressMode mode;

    public SparePartLocationProgressMode getMode() { return mode; }
    public void setMode(SparePartLocationProgressMode mode) { this.mode = mode; }
}

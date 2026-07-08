package com.pinetechs.orvix.ims.inventory.sparepart.service;

import com.pinetechs.orvix.ims.inventory.sparepart.dto.SparePartInventoryScanRequest;
import com.pinetechs.orvix.ims.inventory.sparepart.dto.SparePartInventoryScanResponse;
import com.pinetechs.orvix.ims.user.entity.User;

public interface SparePartInventoryScanService {
    SparePartInventoryScanResponse scan(Long taskId, SparePartInventoryScanRequest request, User currentUser);
}

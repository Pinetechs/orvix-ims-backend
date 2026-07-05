package com.pinetechs.orvix.ims.inventory.asset.service;

import com.pinetechs.orvix.ims.inventory.asset.dto.AssetInventoryScanRequest;
import com.pinetechs.orvix.ims.inventory.asset.dto.AssetInventoryScanResponse;
import com.pinetechs.orvix.ims.user.entity.User;

public interface AssetInventoryScanService {

    AssetInventoryScanResponse scan(Long taskId, AssetInventoryScanRequest request, User currentUser);
}

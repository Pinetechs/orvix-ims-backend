package com.pinetechs.orvix.ims.inventory.vehicle.service;

import com.pinetechs.orvix.ims.inventory.vehicle.dto.VehicleInventoryImportResult;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface VehicleInventoryImportService {

    VehicleInventoryImportResult importExcel(
            Long taskId,
            MultipartFile file,
            Long uploadedByUserId
    );
}
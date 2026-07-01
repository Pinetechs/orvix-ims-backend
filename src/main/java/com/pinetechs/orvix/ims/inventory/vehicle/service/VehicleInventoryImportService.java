package com.pinetechs.orvix.ims.inventory.vehicle.service;

import com.pinetechs.orvix.ims.inventory.common.dto.UploadExcelResponse;
import com.pinetechs.orvix.ims.inventory.vehicle.dto.VehicleInventoryItemResponse;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface VehicleInventoryImportService {

    UploadExcelResponse uploadExcel(
            Long taskId,
            MultipartFile file,
            User user    );


    Page<VehicleInventoryItemResponse> getImportedVehicleItems(Long taskId, Pageable pageable, User currentUser);
}
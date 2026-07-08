package com.pinetechs.orvix.ims.inventory.sparepart.service;

import com.pinetechs.orvix.ims.inventory.common.dto.UploadExcelResponse;
import com.pinetechs.orvix.ims.inventory.sparepart.dto.SparePartInventoryItemResponse;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface SparePartInventoryImportService {
    UploadExcelResponse uploadExcel(Long taskId, MultipartFile file, User currentUser);
    Page<SparePartInventoryItemResponse> getImportedSparePartItems(Long taskId, Pageable pageable, User currentUser);
}

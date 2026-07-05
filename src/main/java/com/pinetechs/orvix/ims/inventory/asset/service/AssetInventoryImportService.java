package com.pinetechs.orvix.ims.inventory.asset.service;

import com.pinetechs.orvix.ims.inventory.asset.dto.AssetInventoryItemResponse;
import com.pinetechs.orvix.ims.inventory.common.dto.UploadExcelResponse;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface AssetInventoryImportService {

    UploadExcelResponse uploadExcel(Long taskId, MultipartFile file, User currentUser);

    Page<AssetInventoryItemResponse> getImportedAssetItems(Long taskId, Pageable pageable, User currentUser);
}

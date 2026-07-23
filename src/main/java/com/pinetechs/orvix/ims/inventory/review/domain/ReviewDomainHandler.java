package com.pinetechs.orvix.ims.inventory.review.domain;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.review.entity.InventoryRecheckItem;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.user.entity.User;

public interface ReviewDomainHandler {

    InventoryDomain domain();

    void validateSubmission(InventoryTask task, InventoryRecheckItem item);

    AppliedReviewResult accept(InventoryTask task, InventoryRecheckItem item, User inventoryStaff);
}

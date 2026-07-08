package com.pinetechs.orvix.ims.inventory.sparepart.dto;

import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryBrand;

public class SparePartInventoryBrandResponse {
    private Long id;
    private Long taskId;
    private String brandName;
    private Integer totalItems;

    public static SparePartInventoryBrandResponse from(SparePartInventoryBrand brand) {
        SparePartInventoryBrandResponse response = new SparePartInventoryBrandResponse();
        response.setId(brand.getId());
        response.setTaskId(brand.getInventoryTask() == null ? null : brand.getInventoryTask().getId());
        response.setBrandName(brand.getBrandName());
        response.setTotalItems(brand.getTotalItems());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    public Integer getTotalItems() { return totalItems; }
    public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }
}

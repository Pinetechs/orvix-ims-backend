package com.pinetechs.orvix.ims.inventory.vehicle.dto;

import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryItem;

import java.time.LocalDate;

public class VehicleInventoryItemResponse {
    private Long id;

    private String partNo;
    private String make;
    private String modelName;
    private String modelYear;
    private String vinNo;
    private String specification;
    private Integer quantity;
    private LocalDate receiptDate;
    private String colorNo;
    private String interiorColor;
    private String mchStatus;
    private String stockStatus;
    private String location;
    private String storeNo;
    private String darArtId;


    public static VehicleInventoryItemResponse from(VehicleInventoryItem item) {
        VehicleInventoryItemResponse response = new VehicleInventoryItemResponse();
        response.setId(item.getId());
        response.setPartNo(item.getPartNo());
        response.setMake(item.getMake());
        response.setModelName(item.getModelName());
        response.setModelYear(item.getModelYear());
        response.setVinNo(item.getVinNo());
        response.setSpecification(item.getSpecification());
        response.setQuantity(item.getQuantity());
        response.setReceiptDate(item.getReceiptDate());
        response.setColorNo(item.getColorNo());
        response.setInteriorColor(item.getInteriorColor());
        response.setMchStatus(item.getMchStatus());
        response.setStockStatus(item.getStockStatus());
        response.setLocation(item.getLocation());
        response.setStoreNo(item.getStoreNo());
        response.setDarArtId(item.getDarArtId());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPartNo() {
        return partNo;
    }

    public void setPartNo(String partNo) {
        this.partNo = partNo;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelYear() {
        return modelYear;
    }

    public void setModelYear(String modelYear) {
        this.modelYear = modelYear;
    }

    public String getVinNo() {
        return vinNo;
    }

    public void setVinNo(String vinNo) {
        this.vinNo = vinNo;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(LocalDate receiptDate) {
        this.receiptDate = receiptDate;
    }

    public String getColorNo() {
        return colorNo;
    }

    public void setColorNo(String colorNo) {
        this.colorNo = colorNo;
    }

    public String getInteriorColor() {
        return interiorColor;
    }

    public void setInteriorColor(String interiorColor) {
        this.interiorColor = interiorColor;
    }

    public String getMchStatus() {
        return mchStatus;
    }

    public void setMchStatus(String mchStatus) {
        this.mchStatus = mchStatus;
    }

    public String getStockStatus() {
        return stockStatus;
    }

    public void setStockStatus(String stockStatus) {
        this.stockStatus = stockStatus;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStoreNo() {
        return storeNo;
    }

    public void setStoreNo(String storeNo) {
        this.storeNo = storeNo;
    }

    public String getDarArtId() {
        return darArtId;
    }

    public void setDarArtId(String darArtId) {
        this.darArtId = darArtId;
    }
}

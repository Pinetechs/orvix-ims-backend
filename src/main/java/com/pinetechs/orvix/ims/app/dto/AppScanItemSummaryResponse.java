package com.pinetechs.orvix.ims.app.dto;

import java.math.BigDecimal;

/** Minimal post-scan item details safe for inventory staff. */
public class AppScanItemSummaryResponse {

    private String code;
    private String barcode;
    private String displayName;
    private String category;
    private String type;
    private String brand;
    private String make;
    private String model;
    private String modelYear;
    private String color;
    private String condition;
    private BigDecimal countedQuantity;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getModelYear() { return modelYear; }
    public void setModelYear(String modelYear) { this.modelYear = modelYear; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public BigDecimal getCountedQuantity() { return countedQuantity; }
    public void setCountedQuantity(BigDecimal countedQuantity) { this.countedQuantity = countedQuantity; }
}

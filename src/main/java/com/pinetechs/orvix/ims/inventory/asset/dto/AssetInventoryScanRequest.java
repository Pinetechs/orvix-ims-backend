package com.pinetechs.orvix.ims.inventory.asset.dto;

public class AssetInventoryScanRequest {
    private String barcode;
    private Long locationId;
    private Long floorId;
    private Long placeId;
    private String notes;

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public Long getFloorId() { return floorId; }
    public void setFloorId(Long floorId) { this.floorId = floorId; }
    public Long getPlaceId() { return placeId; }
    public void setPlaceId(Long placeId) { this.placeId = placeId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

package com.pinetechs.orvix.ims.app.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AppScanCorrectionRequest {
    private String clientScanId;
    private String reason;
    private Long branchId;
    private Long locationId;
    private Long floorId;
    private Long placeId;
    private BigDecimal countedQty;
    private LocalDateTime deviceScannedAt;
    private String deviceId;
    private String symbology;
    private String imageSource;

    public String getClientScanId() { return clientScanId; }
    public void setClientScanId(String clientScanId) { this.clientScanId = clientScanId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public Long getFloorId() { return floorId; }
    public void setFloorId(Long floorId) { this.floorId = floorId; }
    public Long getPlaceId() { return placeId; }
    public void setPlaceId(Long placeId) { this.placeId = placeId; }
    public BigDecimal getCountedQty() { return countedQty; }
    public void setCountedQty(BigDecimal countedQty) { this.countedQty = countedQty; }
    public LocalDateTime getDeviceScannedAt() { return deviceScannedAt; }
    public void setDeviceScannedAt(LocalDateTime deviceScannedAt) { this.deviceScannedAt = deviceScannedAt; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getSymbology() { return symbology; }
    public void setSymbology(String symbology) { this.symbology = symbology; }
    public String getImageSource() { return imageSource; }
    public void setImageSource(String imageSource) { this.imageSource = imageSource; }
}

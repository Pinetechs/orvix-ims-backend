package com.pinetechs.orvix.ims.inventory.review.dto;

import com.pinetechs.orvix.ims.inventory.review.enums.RecheckResult;
import com.pinetechs.orvix.ims.inventory.review.enums.ReviewReasonCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SubmitRecheckItemRequest {

    private String clientSubmissionId;
    private RecheckResult result;
    private Long resolvedItemId;
    private String scannedCode;
    private Long branchId;
    private Long locationId;
    private Long floorId;
    private Long placeId;
    private BigDecimal countedQuantity;
    private ReviewReasonCode reasonCode;
    private String note;
    private LocalDateTime deviceScannedAt;
    private String deviceId;
    private String symbology;
    private String imageSource;

    public String getClientSubmissionId() { return clientSubmissionId; }
    public void setClientSubmissionId(String clientSubmissionId) { this.clientSubmissionId = clientSubmissionId; }
    public RecheckResult getResult() { return result; }
    public void setResult(RecheckResult result) { this.result = result; }
    public Long getResolvedItemId() { return resolvedItemId; }
    public void setResolvedItemId(Long resolvedItemId) { this.resolvedItemId = resolvedItemId; }
    public String getScannedCode() { return scannedCode; }
    public void setScannedCode(String scannedCode) { this.scannedCode = scannedCode; }
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public Long getFloorId() { return floorId; }
    public void setFloorId(Long floorId) { this.floorId = floorId; }
    public Long getPlaceId() { return placeId; }
    public void setPlaceId(Long placeId) { this.placeId = placeId; }
    public BigDecimal getCountedQuantity() { return countedQuantity; }
    public void setCountedQuantity(BigDecimal countedQuantity) { this.countedQuantity = countedQuantity; }
    public ReviewReasonCode getReasonCode() { return reasonCode; }
    public void setReasonCode(ReviewReasonCode reasonCode) { this.reasonCode = reasonCode; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getDeviceScannedAt() { return deviceScannedAt; }
    public void setDeviceScannedAt(LocalDateTime deviceScannedAt) { this.deviceScannedAt = deviceScannedAt; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getSymbology() { return symbology; }
    public void setSymbology(String symbology) { this.symbology = symbology; }
    public String getImageSource() { return imageSource; }
    public void setImageSource(String imageSource) { this.imageSource = imageSource; }
}

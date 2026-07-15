package com.pinetechs.orvix.ims.app.dto;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AppScanResponse {
    private Long scanId;
    private Long currentAcceptedScanId;
    private Long itemId;
    private Long imageFileId;
    private InventoryDomain inventoryDomain;
    private String clientScanId;
    private String eventType;
    private String resultCode;
    private String messageKey;
    private boolean accepted;
    private boolean correctionAllowed;
    private boolean idempotentReplay;
    private LocalDateTime serverScannedAt;
    private List<String> mismatchFields = new ArrayList<>();

    public Long getScanId() { return scanId; }
    public void setScanId(Long scanId) { this.scanId = scanId; }
    public Long getCurrentAcceptedScanId() { return currentAcceptedScanId; }
    public void setCurrentAcceptedScanId(Long currentAcceptedScanId) { this.currentAcceptedScanId = currentAcceptedScanId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getImageFileId() { return imageFileId; }
    public void setImageFileId(Long imageFileId) { this.imageFileId = imageFileId; }
    public InventoryDomain getInventoryDomain() { return inventoryDomain; }
    public void setInventoryDomain(InventoryDomain inventoryDomain) { this.inventoryDomain = inventoryDomain; }
    public String getClientScanId() { return clientScanId; }
    public void setClientScanId(String clientScanId) { this.clientScanId = clientScanId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getResultCode() { return resultCode; }
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }
    public String getMessageKey() { return messageKey; }
    public void setMessageKey(String messageKey) { this.messageKey = messageKey; }
    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }
    public boolean isCorrectionAllowed() { return correctionAllowed; }
    public void setCorrectionAllowed(boolean correctionAllowed) { this.correctionAllowed = correctionAllowed; }
    public boolean isIdempotentReplay() { return idempotentReplay; }
    public void setIdempotentReplay(boolean idempotentReplay) { this.idempotentReplay = idempotentReplay; }
    public LocalDateTime getServerScannedAt() { return serverScannedAt; }
    public void setServerScannedAt(LocalDateTime serverScannedAt) { this.serverScannedAt = serverScannedAt; }
    public List<String> getMismatchFields() { return mismatchFields; }
    public void setMismatchFields(List<String> mismatchFields) {
        this.mismatchFields = mismatchFields == null ? new ArrayList<>() : new ArrayList<>(mismatchFields);
    }
}

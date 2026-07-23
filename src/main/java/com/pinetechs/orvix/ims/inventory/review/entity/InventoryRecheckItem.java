package com.pinetechs.orvix.ims.inventory.review.entity;

import com.pinetechs.orvix.ims.file.entity.UploadedFile;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryScanImageSource;
import com.pinetechs.orvix.ims.inventory.review.enums.RecheckItemStatus;
import com.pinetechs.orvix.ims.inventory.review.enums.RecheckResult;
import com.pinetechs.orvix.ims.inventory.review.enums.ReviewReasonCode;
import com.pinetechs.orvix.ims.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "inventory_recheck_items",
        indexes = {
                @Index(name = "idx_recheck_item_request_status", columnList = "recheck_request_id, status"),
                @Index(name = "idx_recheck_item_reference", columnList = "reference_item_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_recheck_item_client_submission",
                columnNames = "client_submission_id"
        )
)
public class InventoryRecheckItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recheck_request_id", nullable = false)
    private InventoryRecheckRequest recheckRequest;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "inventory_recheck_item_issues",
            joinColumns = @JoinColumn(name = "recheck_item_id"),
            inverseJoinColumns = @JoinColumn(name = "review_issue_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_recheck_item_issue",
                    columnNames = {"recheck_item_id", "review_issue_id"}
            )
    )
    @OrderBy("id asc")
    private Set<InventoryReviewIssue> issues = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private RecheckItemStatus status = RecheckItemStatus.PENDING;

    @Column(name = "reference_item_id")
    private Long referenceItemId;

    @Column(name = "resolved_item_id")
    private Long resolvedItemId;

    @Column(name = "previous_scan_id")
    private Long previousScanId;

    @Column(name = "accepted_scan_id")
    private Long acceptedScanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 60)
    private RecheckResult result;

    @Column(name = "client_submission_id", length = 36)
    private String clientSubmissionId;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @Column(name = "scanned_code", length = 180)
    private String scannedCode;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "floor_id")
    private Long floorId;

    @Column(name = "place_id")
    private Long placeId;

    @Column(name = "counted_quantity", precision = 18, scale = 3)
    private BigDecimal countedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 60)
    private ReviewReasonCode reasonCode;

    @Column(name = "note", length = 1000)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evidence_file_id")
    private UploadedFile evidenceImage;

    @Column(name = "device_scanned_at")
    private LocalDateTime deviceScannedAt;

    @Column(name = "device_id", length = 150)
    private String deviceId;

    @Column(name = "symbology", length = 80)
    private String symbology;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_source", length = 40)
    private InventoryScanImageSource imageSource;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void submit(LocalDateTime time) {
        status = RecheckItemStatus.SUBMITTED;
        submittedAt = time;
    }

    public void accept(Long scanId, User reviewer, LocalDateTime time) {
        status = RecheckItemStatus.ACCEPTED;
        acceptedScanId = scanId;
        reviewedBy = reviewer;
        reviewedAt = time;
    }

    public void reject(User reviewer, LocalDateTime time) {
        status = RecheckItemStatus.REJECTED;
        reviewedBy = reviewer;
        reviewedAt = time;
    }

    public void cancel() {
        status = RecheckItemStatus.CANCELLED;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public InventoryRecheckRequest getRecheckRequest() { return recheckRequest; }
    public void setRecheckRequest(InventoryRecheckRequest recheckRequest) { this.recheckRequest = recheckRequest; }
    public Set<InventoryReviewIssue> getIssues() { return issues; }
    public void setIssues(Set<InventoryReviewIssue> issues) { this.issues = issues; }
    public RecheckItemStatus getStatus() { return status; }
    public void setStatus(RecheckItemStatus status) { this.status = status; }
    public Long getReferenceItemId() { return referenceItemId; }
    public void setReferenceItemId(Long referenceItemId) { this.referenceItemId = referenceItemId; }
    public Long getResolvedItemId() { return resolvedItemId; }
    public void setResolvedItemId(Long resolvedItemId) { this.resolvedItemId = resolvedItemId; }
    public Long getPreviousScanId() { return previousScanId; }
    public void setPreviousScanId(Long previousScanId) { this.previousScanId = previousScanId; }
    public Long getAcceptedScanId() { return acceptedScanId; }
    public RecheckResult getResult() { return result; }
    public void setResult(RecheckResult result) { this.result = result; }
    public String getClientSubmissionId() { return clientSubmissionId; }
    public void setClientSubmissionId(String clientSubmissionId) { this.clientSubmissionId = clientSubmissionId; }
    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }
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
    public UploadedFile getEvidenceImage() { return evidenceImage; }
    public void setEvidenceImage(UploadedFile evidenceImage) { this.evidenceImage = evidenceImage; }
    public LocalDateTime getDeviceScannedAt() { return deviceScannedAt; }
    public void setDeviceScannedAt(LocalDateTime deviceScannedAt) { this.deviceScannedAt = deviceScannedAt; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getSymbology() { return symbology; }
    public void setSymbology(String symbology) { this.symbology = symbology; }
    public InventoryScanImageSource getImageSource() { return imageSource; }
    public void setImageSource(InventoryScanImageSource imageSource) { this.imageSource = imageSource; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public User getReviewedBy() { return reviewedBy; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

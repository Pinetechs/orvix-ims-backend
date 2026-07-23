package com.pinetechs.orvix.ims.inventory.review.entity;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.review.enums.ReviewIssueStatus;
import com.pinetechs.orvix.ims.inventory.review.enums.ReviewIssueType;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventory_review_issues",
        indexes = {
                @Index(name = "idx_review_issue_task_status", columnList = "task_id, status"),
                @Index(name = "idx_review_issue_task_type", columnList = "task_id, issue_type"),
                @Index(name = "idx_review_issue_source_scan", columnList = "task_id, source_scan_id"),
                @Index(name = "idx_review_issue_item", columnList = "task_id, item_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_review_issue_task_key",
                columnNames = {"task_id", "issue_key"}
        )
)
public class InventoryReviewIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private InventoryTask inventoryTask;

    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_domain", nullable = false, length = 30)
    private InventoryDomain inventoryDomain;

    @Column(name = "issue_key", nullable = false, length = 220)
    private String issueKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false, length = 50)
    private ReviewIssueType issueType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ReviewIssueStatus status = ReviewIssueStatus.OPEN;

    @Column(name = "is_blocking", nullable = false)
    private boolean blocking = true;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "source_scan_id")
    private Long sourceScanId;

    @Column(name = "current_scan_id")
    private Long currentScanId;

    @Column(name = "item_code", length = 180)
    private String itemCode;

    @Column(name = "secondary_code", length = 180)
    private String secondaryCode;

    @Column(name = "item_description", length = 1000)
    private String itemDescription;

    @Column(name = "work_area_key", length = 300)
    private String workAreaKey;

    @Column(name = "work_area_label", length = 500)
    private String workAreaLabel;

    @Column(name = "expected_area", length = 255)
    private String expectedArea;

    @Column(name = "expected_sub_area", length = 255)
    private String expectedSubArea;

    @Column(name = "expected_leaf_area", length = 255)
    private String expectedLeafArea;

    @Column(name = "actual_area", length = 255)
    private String actualArea;

    @Column(name = "actual_sub_area", length = 255)
    private String actualSubArea;

    @Column(name = "actual_leaf_area", length = 255)
    private String actualLeafArea;

    @Column(name = "expected_quantity", precision = 18, scale = 3)
    private BigDecimal expectedQuantity;

    @Column(name = "actual_quantity", precision = 18, scale = 3)
    private BigDecimal actualQuantity;

    @Column(name = "variance_quantity", precision = 18, scale = 3)
    private BigDecimal varianceQuantity;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_id")
    private User resolvedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void markOpen() {
        status = ReviewIssueStatus.OPEN;
        resolvedAt = null;
        resolvedBy = null;
    }

    public void markRecheckRequested() {
        status = ReviewIssueStatus.RECHECK_REQUESTED;
    }

    public void markRecheckInProgress() {
        status = ReviewIssueStatus.RECHECK_IN_PROGRESS;
    }

    public void markRecheckSubmitted() {
        status = ReviewIssueStatus.RECHECK_SUBMITTED;
    }

    public void resolve(User user, LocalDateTime time) {
        status = ReviewIssueStatus.RESOLVED;
        resolvedBy = user;
        resolvedAt = time;
    }

    public void supersede(LocalDateTime time) {
        status = ReviewIssueStatus.SUPERSEDED;
        resolvedAt = time;
        resolvedBy = null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public InventoryTask getInventoryTask() { return inventoryTask; }
    public void setInventoryTask(InventoryTask inventoryTask) { this.inventoryTask = inventoryTask; }
    public InventoryDomain getInventoryDomain() { return inventoryDomain; }
    public void setInventoryDomain(InventoryDomain inventoryDomain) { this.inventoryDomain = inventoryDomain; }
    public String getIssueKey() { return issueKey; }
    public void setIssueKey(String issueKey) { this.issueKey = issueKey; }
    public ReviewIssueType getIssueType() { return issueType; }
    public void setIssueType(ReviewIssueType issueType) { this.issueType = issueType; }
    public ReviewIssueStatus getStatus() { return status; }
    public void setStatus(ReviewIssueStatus status) { this.status = status; }
    public boolean isBlocking() { return blocking; }
    public void setBlocking(boolean blocking) { this.blocking = blocking; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getSourceScanId() { return sourceScanId; }
    public void setSourceScanId(Long sourceScanId) { this.sourceScanId = sourceScanId; }
    public Long getCurrentScanId() { return currentScanId; }
    public void setCurrentScanId(Long currentScanId) { this.currentScanId = currentScanId; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getSecondaryCode() { return secondaryCode; }
    public void setSecondaryCode(String secondaryCode) { this.secondaryCode = secondaryCode; }
    public String getItemDescription() { return itemDescription; }
    public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }
    public String getWorkAreaKey() { return workAreaKey; }
    public void setWorkAreaKey(String workAreaKey) { this.workAreaKey = workAreaKey; }
    public String getWorkAreaLabel() { return workAreaLabel; }
    public void setWorkAreaLabel(String workAreaLabel) { this.workAreaLabel = workAreaLabel; }
    public String getExpectedArea() { return expectedArea; }
    public void setExpectedArea(String expectedArea) { this.expectedArea = expectedArea; }
    public String getExpectedSubArea() { return expectedSubArea; }
    public void setExpectedSubArea(String expectedSubArea) { this.expectedSubArea = expectedSubArea; }
    public String getExpectedLeafArea() { return expectedLeafArea; }
    public void setExpectedLeafArea(String expectedLeafArea) { this.expectedLeafArea = expectedLeafArea; }
    public String getActualArea() { return actualArea; }
    public void setActualArea(String actualArea) { this.actualArea = actualArea; }
    public String getActualSubArea() { return actualSubArea; }
    public void setActualSubArea(String actualSubArea) { this.actualSubArea = actualSubArea; }
    public String getActualLeafArea() { return actualLeafArea; }
    public void setActualLeafArea(String actualLeafArea) { this.actualLeafArea = actualLeafArea; }
    public BigDecimal getExpectedQuantity() { return expectedQuantity; }
    public void setExpectedQuantity(BigDecimal expectedQuantity) { this.expectedQuantity = expectedQuantity; }
    public BigDecimal getActualQuantity() { return actualQuantity; }
    public void setActualQuantity(BigDecimal actualQuantity) { this.actualQuantity = actualQuantity; }
    public BigDecimal getVarianceQuantity() { return varianceQuantity; }
    public void setVarianceQuantity(BigDecimal varianceQuantity) { this.varianceQuantity = varianceQuantity; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public User getResolvedBy() { return resolvedBy; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

package com.pinetechs.orvix.ims.inventory.review.entity;

import com.pinetechs.orvix.ims.inventory.review.enums.ReviewDecisionType;
import com.pinetechs.orvix.ims.inventory.review.enums.ReviewReasonCode;
import com.pinetechs.orvix.ims.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "inventory_review_decisions",
        indexes = {
                @Index(name = "idx_review_decision_recheck_item", columnList = "recheck_item_id"),
                @Index(name = "idx_review_decision_time", columnList = "decided_at")
        }
)
public class InventoryReviewDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "inventory_review_decision_issues",
            joinColumns = @JoinColumn(name = "review_decision_id"),
            inverseJoinColumns = @JoinColumn(name = "review_issue_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_review_decision_issue",
                    columnNames = {"review_decision_id", "review_issue_id"}
            )
    )
    @OrderBy("id asc")
    private Set<InventoryReviewIssue> issues = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recheck_item_id")
    private InventoryRecheckItem recheckItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type", nullable = false, length = 60)
    private ReviewDecisionType decisionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 60)
    private ReviewReasonCode reasonCode;

    @Column(name = "note", length = 1000)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decided_by_user_id", nullable = false)
    private User decidedBy;

    @Column(name = "decided_at", nullable = false)
    private LocalDateTime decidedAt;

    @Column(name = "previous_scan_id")
    private Long previousScanId;

    @Column(name = "accepted_scan_id")
    private Long acceptedScanId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Set<InventoryReviewIssue> getIssues() { return issues; }
    public void setIssues(Set<InventoryReviewIssue> issues) { this.issues = issues; }
    public InventoryRecheckItem getRecheckItem() { return recheckItem; }
    public void setRecheckItem(InventoryRecheckItem recheckItem) { this.recheckItem = recheckItem; }
    public ReviewDecisionType getDecisionType() { return decisionType; }
    public void setDecisionType(ReviewDecisionType decisionType) { this.decisionType = decisionType; }
    public ReviewReasonCode getReasonCode() { return reasonCode; }
    public void setReasonCode(ReviewReasonCode reasonCode) { this.reasonCode = reasonCode; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public User getDecidedBy() { return decidedBy; }
    public void setDecidedBy(User decidedBy) { this.decidedBy = decidedBy; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
    public Long getPreviousScanId() { return previousScanId; }
    public void setPreviousScanId(Long previousScanId) { this.previousScanId = previousScanId; }
    public Long getAcceptedScanId() { return acceptedScanId; }
    public void setAcceptedScanId(Long acceptedScanId) { this.acceptedScanId = acceptedScanId; }
}

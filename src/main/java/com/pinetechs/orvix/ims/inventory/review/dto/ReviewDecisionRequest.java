package com.pinetechs.orvix.ims.inventory.review.dto;

import com.pinetechs.orvix.ims.inventory.review.enums.ReviewDecisionType;
import com.pinetechs.orvix.ims.inventory.review.enums.ReviewReasonCode;

public class ReviewDecisionRequest {

    private ReviewDecisionType decision;
    private ReviewReasonCode reasonCode;
    private String note;

    public ReviewDecisionType getDecision() { return decision; }
    public void setDecision(ReviewDecisionType decision) { this.decision = decision; }
    public ReviewReasonCode getReasonCode() { return reasonCode; }
    public void setReasonCode(ReviewReasonCode reasonCode) { this.reasonCode = reasonCode; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}

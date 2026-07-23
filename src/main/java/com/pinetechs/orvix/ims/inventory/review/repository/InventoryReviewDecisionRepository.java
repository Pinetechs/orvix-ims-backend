package com.pinetechs.orvix.ims.inventory.review.repository;

import com.pinetechs.orvix.ims.inventory.review.entity.InventoryReviewDecision;
import com.pinetechs.orvix.ims.inventory.review.enums.ReviewDecisionType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryReviewDecisionRepository
        extends JpaRepository<InventoryReviewDecision, Long> {

    @EntityGraph(attributePaths = {"issues", "decidedBy", "recheckItem"})
    @Query("""
           select distinct decision
           from InventoryReviewDecision decision
           join decision.issues issue
           where issue.id = :issueId
           order by decision.decidedAt desc, decision.id desc
           """)
    List<InventoryReviewDecision> findByIssueId(@Param("issueId") Long issueId);

    @Query("""
           select distinct decision.acceptedScanId
           from InventoryReviewDecision decision
           join decision.issues issue
           where issue.inventoryTask.id = :taskId
             and decision.decisionType = :decisionType
             and decision.acceptedScanId is not null
           """)
    List<Long> findAcceptedScanIdsByTaskId(
            @Param("taskId") Long taskId,
            @Param("decisionType") ReviewDecisionType decisionType
    );
}

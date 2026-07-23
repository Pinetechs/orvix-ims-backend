package com.pinetechs.orvix.ims.inventory.review.repository;

import com.pinetechs.orvix.ims.inventory.review.entity.InventoryReviewIssue;
import com.pinetechs.orvix.ims.inventory.review.enums.ReviewIssueStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryReviewIssueRepository
        extends JpaRepository<InventoryReviewIssue, Long>,
        JpaSpecificationExecutor<InventoryReviewIssue> {

    Optional<InventoryReviewIssue> findByInventoryTaskIdAndIssueKey(Long taskId, String issueKey);

    List<InventoryReviewIssue> findByInventoryTaskIdAndIssueKeyIn(
            Long taskId,
            Collection<String> issueKeys
    );

    List<InventoryReviewIssue> findByInventoryTaskIdAndStatusIn(
            Long taskId,
            Collection<ReviewIssueStatus> statuses
    );

    List<InventoryReviewIssue> findByInventoryTaskIdAndItemIdAndStatusIn(
            Long taskId,
            Long itemId,
            Collection<ReviewIssueStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select issue
           from InventoryReviewIssue issue
           join fetch issue.inventoryTask task
           join fetch task.company
           where issue.id = :issueId
             and task.id = :taskId
           """)
    Optional<InventoryReviewIssue> findForUpdate(
            @Param("taskId") Long taskId,
            @Param("issueId") Long issueId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select issue
           from InventoryReviewIssue issue
           where issue.inventoryTask.id = :taskId
             and issue.id in :issueIds
           order by issue.id
           """)
    List<InventoryReviewIssue> findAllForUpdate(
            @Param("taskId") Long taskId,
            @Param("issueIds") Collection<Long> issueIds
    );

    @Query("""
           select issue.status, count(issue.id)
           from InventoryReviewIssue issue
           where issue.inventoryTask.id = :taskId
           group by issue.status
           """)
    List<Object[]> countByStatus(@Param("taskId") Long taskId);

    @Query("""
           select issue.issueType, count(issue.id)
           from InventoryReviewIssue issue
           where issue.inventoryTask.id = :taskId
             and issue.status not in :closedStatuses
           group by issue.issueType
           """)
    List<Object[]> countOpenByType(
            @Param("taskId") Long taskId,
            @Param("closedStatuses") Collection<ReviewIssueStatus> closedStatuses
    );

    long countByInventoryTaskIdAndBlockingTrueAndStatusIn(
            Long taskId,
            Collection<ReviewIssueStatus> statuses
    );

    long countByInventoryTaskIdAndStatusIn(
            Long taskId,
            Collection<ReviewIssueStatus> statuses
    );

    @Query("""
           select count(issue.id)
           from InventoryReviewIssue issue
           where issue.inventoryTask.id = :taskId
             and issue.status in :statuses
             and (issue.sourceScanId = :scanId or issue.currentScanId = :scanId)
           """)
    long countOpenByScanReference(
            @Param("taskId") Long taskId,
            @Param("scanId") Long scanId,
            @Param("statuses") Collection<ReviewIssueStatus> statuses
    );
}

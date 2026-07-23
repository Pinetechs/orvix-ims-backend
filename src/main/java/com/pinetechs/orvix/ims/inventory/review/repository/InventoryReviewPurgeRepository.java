package com.pinetechs.orvix.ims.inventory.review.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InventoryReviewPurgeRepository {

    private static final int DELETE_BATCH_SIZE = 500;

    private final NamedParameterJdbcTemplate jdbc;

    public InventoryReviewPurgeRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Deletes the complete Review Center graph for one task.
     *
     * Native deletes are intentional here. JPQL bulk deletes do not apply
     * orphanRemoval and do not reliably clean the two many-to-many join tables.
     */
    public void deleteByTaskId(Long taskId) {
        List<Long> decisionIds = findDecisionIds(taskId);
        deleteDecisions(decisionIds);

        MapSqlParameterSource taskParameter =
                new MapSqlParameterSource("taskId", taskId);

        jdbc.update("""
                delete item_issue
                from inventory_recheck_item_issues item_issue
                join inventory_recheck_items item
                  on item.id = item_issue.recheck_item_id
                join inventory_recheck_requests recheck_request
                  on recheck_request.id = item.recheck_request_id
                where recheck_request.task_id = :taskId
                """, taskParameter);

        jdbc.update("""
                delete item
                from inventory_recheck_items item
                join inventory_recheck_requests recheck_request
                  on recheck_request.id = item.recheck_request_id
                where recheck_request.task_id = :taskId
                """, taskParameter);

        jdbc.update("""
                delete from inventory_recheck_requests
                where task_id = :taskId
                """, taskParameter);

        jdbc.update("""
                delete from inventory_review_issues
                where task_id = :taskId
                """, taskParameter);
    }

    private List<Long> findDecisionIds(Long taskId) {
        return jdbc.queryForList("""
                select distinct decision.id
                from inventory_review_decisions decision
                left join inventory_recheck_items item
                  on item.id = decision.recheck_item_id
                left join inventory_recheck_requests recheck_request
                  on recheck_request.id = item.recheck_request_id
                left join inventory_review_decision_issues decision_issue
                  on decision_issue.review_decision_id = decision.id
                left join inventory_review_issues issue
                  on issue.id = decision_issue.review_issue_id
                where recheck_request.task_id = :taskId
                   or issue.task_id = :taskId
                """, new MapSqlParameterSource("taskId", taskId), Long.class);
    }

    private void deleteDecisions(List<Long> decisionIds) {
        for (int start = 0; start < decisionIds.size(); start += DELETE_BATCH_SIZE) {
            int end = Math.min(start + DELETE_BATCH_SIZE, decisionIds.size());
            MapSqlParameterSource ids = new MapSqlParameterSource(
                    "decisionIds",
                    decisionIds.subList(start, end)
            );

            jdbc.update("""
                    delete from inventory_review_decision_issues
                    where review_decision_id in (:decisionIds)
                    """, ids);

            jdbc.update("""
                    delete from inventory_review_decisions
                    where id in (:decisionIds)
                    """, ids);
        }
    }
}

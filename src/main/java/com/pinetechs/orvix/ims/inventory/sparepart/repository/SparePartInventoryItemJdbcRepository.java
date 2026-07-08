package com.pinetechs.orvix.ims.inventory.sparepart.repository;

import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryItem;
import com.pinetechs.orvix.ims.inventory.sparepart.enums.SparePartInventoryItemStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class SparePartInventoryItemJdbcRepository {

    private static final int BATCH_SIZE = 500;

    private final JdbcTemplate jdbcTemplate;

    public SparePartInventoryItemJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void batchInsert(Long taskId, List<SparePartInventoryItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO spare_part_inventory_items
                (
                    task_id,
                    item_no,
                    brand_name,
                    brand_id,
                    planned_branch_id,
                    planned_location_id,
                    actual_branch_id,
                    actual_location_id,
                    qty,
                    stock_qty,
                    frozen_qty,
                    actual_qty,
                    variance_qty,
                    status,
                    counted_by_user_id,
                    counted_at,
                    notes,
                    created_at,
                    updated_at
                )
                VALUES
                (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?, ?, ?
                )
                """;

        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.batchUpdate(sql, items, BATCH_SIZE, (PreparedStatement ps, SparePartInventoryItem item) -> {
            ps.setLong(1, taskId);
            setString(ps, 2, item.getItemNo());
            setString(ps, 3, item.getBrandName());
            setLong(ps, 4, item.getBrand() != null ? item.getBrand().getId() : null);
            setRequiredLong(ps, 5, item.getPlannedBranch() != null ? item.getPlannedBranch().getId() : null, "planned_branch_id");
            setRequiredLong(ps, 6, item.getPlannedLocation() != null ? item.getPlannedLocation().getId() : null, "planned_location_id");
            setLong(ps, 7, item.getActualBranch() != null ? item.getActualBranch().getId() : null);
            setLong(ps, 8, item.getActualLocation() != null ? item.getActualLocation().getId() : null);
            setBigDecimal(ps, 9, item.getQty());
            setBigDecimal(ps, 10, item.getStockQty());
            setBigDecimal(ps, 11, item.getFrozenQty());
            setBigDecimal(ps, 12, item.getActualQty());
            setBigDecimal(ps, 13, item.getVarianceQty());

            SparePartInventoryItemStatus status = item.getStatus() != null
                    ? item.getStatus()
                    : SparePartInventoryItemStatus.NOT_COUNTED;
            ps.setString(14, status.name());
            setLong(ps, 15, item.getCountedBy() != null ? item.getCountedBy().getId() : null);
            if (item.getCountedAt() != null) {
                ps.setTimestamp(16, Timestamp.valueOf(item.getCountedAt()));
            } else {
                ps.setNull(16, Types.TIMESTAMP);
            }
            setString(ps, 17, item.getNotes());
            ps.setTimestamp(18, Timestamp.valueOf(now));
            ps.setTimestamp(19, Timestamp.valueOf(now));
        });
    }

    private static void setString(PreparedStatement ps, int index, String value) throws java.sql.SQLException {
        if (value != null) {
            ps.setString(index, value);
        } else {
            ps.setNull(index, Types.VARCHAR);
        }
    }

    private static void setBigDecimal(PreparedStatement ps, int index, BigDecimal value) throws java.sql.SQLException {
        if (value != null) {
            ps.setBigDecimal(index, value);
        } else {
            ps.setNull(index, Types.DECIMAL);
        }
    }

    private static void setLong(PreparedStatement ps, int index, Long value) throws java.sql.SQLException {
        if (value != null) {
            ps.setLong(index, value);
        } else {
            ps.setNull(index, Types.BIGINT);
        }
    }

    private static void setRequiredLong(PreparedStatement ps, int index, Long value, String columnName) throws java.sql.SQLException {
        if (value == null) {
            throw new IllegalStateException("Spare Part JDBC batch insert failed. Required FK is null: " + columnName);
        }
        ps.setLong(index, value);
    }
}

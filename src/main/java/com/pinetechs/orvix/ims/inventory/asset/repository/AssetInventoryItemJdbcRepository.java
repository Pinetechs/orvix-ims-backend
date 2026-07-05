package com.pinetechs.orvix.ims.inventory.asset.repository;

import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryItem;
import com.pinetechs.orvix.ims.inventory.asset.enums.AssetInventoryItemStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class AssetInventoryItemJdbcRepository {

    private static final int BATCH_SIZE = 500;

    private final JdbcTemplate jdbcTemplate;

    public AssetInventoryItemJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void batchInsert(Long taskId, List<AssetInventoryItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO asset_inventory_items
                (
                    task_id,
                    barcode,
                    description,
                    asset_category,
                    asset_type,
                    category_code,
                    category_id,
                    quantity,
                    planned_location_id,
                    planned_floor_id,
                    planned_place_id,
                    actual_location_id,
                    actual_floor_id,
                    actual_place_id,
                    market_value_weight_per_item,
                    market_value,
                    extra_value_per_item,
                    new_acc_dep,
                    purchase_value_market_value,
                    new_old_purchase_value,
                    new_purchase_value,
                    old_purchase_value,
                    final_purchase_value,
                    final_acc_dep,
                    final_book_value,
                    acc_dep,
                    book_value,
                    status_ratio,
                    asset_condition,
                    branch_code,
                    main_dep_code,
                    asset_date,
                    status,
                    checked_by_user_id,
                    checked_at,
                    notes,
                    created_at,
                    updated_at
                )
                VALUES
                (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?, ?
                )
                """;

        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.batchUpdate(sql, items, BATCH_SIZE, (PreparedStatement ps, AssetInventoryItem item) -> {
            ps.setLong(1, taskId);
            setString(ps, 2, item.getBarcode());
            setString(ps, 3, item.getDescription());
            setString(ps, 4, item.getAssetCategory());
            setString(ps, 5, item.getAssetType());
            setString(ps, 6, item.getCategoryCode());
            setLong(ps, 7, item.getCategory() != null ? item.getCategory().getId() : null);
            setBigDecimal(ps, 8, item.getQuantity());

            setRequiredLong(ps, 9, item.getPlannedLocation() != null ? item.getPlannedLocation().getId() : null, "planned_location_id");
            setRequiredLong(ps, 10, item.getPlannedFloor() != null ? item.getPlannedFloor().getId() : null, "planned_floor_id");
            setRequiredLong(ps, 11, item.getPlannedPlace() != null ? item.getPlannedPlace().getId() : null, "planned_place_id");

            setLong(ps, 12, item.getActualLocation() != null ? item.getActualLocation().getId() : null);
            setLong(ps, 13, item.getActualFloor() != null ? item.getActualFloor().getId() : null);
            setLong(ps, 14, item.getActualPlace() != null ? item.getActualPlace().getId() : null);

            setBigDecimal(ps, 15, item.getMarketValueWeightPerItem());
            setBigDecimal(ps, 16, item.getMarketValue());
            setBigDecimal(ps, 17, item.getExtraValuePerItem());
            setBigDecimal(ps, 18, item.getNewAccDep());
            setBigDecimal(ps, 19, item.getPurchaseValueMarketValue());
            setBigDecimal(ps, 20, item.getNewOldPurchaseValue());
            setBigDecimal(ps, 21, item.getNewPurchaseValue());
            setBigDecimal(ps, 22, item.getOldPurchaseValue());
            setBigDecimal(ps, 23, item.getFinalPurchaseValue());
            setBigDecimal(ps, 24, item.getFinalAccDep());
            setBigDecimal(ps, 25, item.getFinalBookValue());
            setBigDecimal(ps, 26, item.getAccDep());
            setBigDecimal(ps, 27, item.getBookValue());
            setBigDecimal(ps, 28, item.getStatusRatio());

            setString(ps, 29, item.getAssetCondition());
            setString(ps, 30, item.getBranchCode());
            setString(ps, 31, item.getMainDepCode());

            if (item.getAssetDate() != null) {
                ps.setDate(32, Date.valueOf(item.getAssetDate()));
            } else {
                ps.setNull(32, Types.DATE);
            }

            AssetInventoryItemStatus status = item.getStatus() != null
                    ? item.getStatus()
                    : AssetInventoryItemStatus.NOT_SCANNED;

            ps.setString(33, status.name());
            setLong(ps, 34, item.getCheckedBy() != null ? item.getCheckedBy().getId() : null);

            if (item.getCheckedAt() != null) {
                ps.setTimestamp(35, Timestamp.valueOf(item.getCheckedAt()));
            } else {
                ps.setNull(35, Types.TIMESTAMP);
            }

            setString(ps, 36, item.getNotes());
            ps.setTimestamp(37, Timestamp.valueOf(now));
            ps.setTimestamp(38, Timestamp.valueOf(now));
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
            throw new IllegalStateException("Asset JDBC batch insert failed. Required FK is null: " + columnName);
        }

        ps.setLong(index, value);
    }
}

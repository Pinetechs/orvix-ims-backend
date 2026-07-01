package com.pinetechs.orvix.ims.inventory.vehicle.repository;


import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryItem;
import com.pinetechs.orvix.ims.inventory.vehicle.enums.VehicleInventoryItemStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class VehicleInventoryItemJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public VehicleInventoryItemJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void batchInsert(Long taskId, List<VehicleInventoryItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO vehicle_inventory_items
                (
                    actual_location,
                    actual_store_no,
                    checked_at,
                    checked_by_user_id,
                    color_no,
                    created_at,
                    dar_art_id,
                    interior_color,
                    task_id,
                    location,
                    make,
                    mch_status,
                    model_name,
                    model_year,
                    notes,
                    part_no,
                    quantity,
                    receipt_date,
                    specification,
                    status,
                    stock_status,
                    store_no,
                    updated_at,
                    vin_no
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.batchUpdate(sql, items, 500, (PreparedStatement ps, VehicleInventoryItem item) -> {
            setString(ps, 1, item.getActualLocation());
            setString(ps, 2, item.getActualStoreNo());

            if (item.getCheckedAt() != null) {
                ps.setTimestamp(3, Timestamp.valueOf(item.getCheckedAt()));
            } else {
                ps.setNull(3, Types.TIMESTAMP);
            }

            if (item.getCheckedBy() != null && item.getCheckedBy().getId() != null) {
                ps.setLong(4, item.getCheckedBy().getId());
            } else {
                ps.setNull(4, Types.BIGINT);
            }

            setString(ps, 5, item.getColorNo());
            ps.setTimestamp(6, Timestamp.valueOf(now));

            setString(ps, 7, item.getDarArtId());
            setString(ps, 8, item.getInteriorColor());

            ps.setLong(9, taskId);

            setString(ps, 10, item.getLocation());
            setString(ps, 11, item.getMake());
            setString(ps, 12, item.getMchStatus());
            setString(ps, 13, item.getModelName());
            setString(ps, 14,  item.getModelYear());



            setString(ps, 15, item.getNotes());
            setString(ps, 16, item.getPartNo());

            if (item.getQuantity() != null) {
                ps.setInt(17, item.getQuantity());
            } else {
                ps.setNull(17, Types.INTEGER);
            }

            if (item.getReceiptDate() != null) {
                ps.setDate(18, Date.valueOf(item.getReceiptDate()));
            } else {
                ps.setNull(18, Types.DATE);
            }

            setString(ps, 19, item.getSpecification());

            VehicleInventoryItemStatus status =
                    item.getStatus() != null ? item.getStatus() : VehicleInventoryItemStatus.PENDING;

            ps.setString(20, status.name());

            setString(ps, 21, item.getStockStatus());
            setString(ps, 22, item.getStoreNo());

            ps.setTimestamp(23, Timestamp.valueOf(now));

            setString(ps, 24, item.getVinNo());
        });
    }

    private static void setString(PreparedStatement ps, int index, String value) throws java.sql.SQLException {
        if (value != null) {
            ps.setString(index, value);
        } else {
            ps.setNull(index, Types.VARCHAR);
        }
    }
}
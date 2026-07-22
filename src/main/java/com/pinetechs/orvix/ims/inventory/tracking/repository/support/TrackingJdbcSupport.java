package com.pinetechs.orvix.ims.inventory.tracking.repository.support;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.tracking.dto.TrackingResponses;
import com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingAreaLevel;
import com.pinetechs.orvix.ims.inventory.tracking.policy.TrackingStatusPolicy;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class TrackingJdbcSupport {

    private final NamedParameterJdbcTemplate jdbc;
    private final TrackingStatusPolicy statusPolicy;

    public TrackingJdbcSupport(
            NamedParameterJdbcTemplate jdbc,
            TrackingStatusPolicy statusPolicy
    ) {
        this.jdbc = jdbc;
        this.statusPolicy = statusPolicy;
    }

    public TrackingResponses.ImageFile findImage(
            String scanTable,
            Long taskId,
            Long scanId
    ) {
        String query = """
                select f.id, f.file_path, f.original_file_name, f.file_name, f.content_type, f.file_size
                  from %s s
                  join uploaded_files f on f.id = s.scan_image_file_id
                 where s.task_id = :taskId
                   and s.id = :scanId
                   and f.is_deleted = false
                """.formatted(scanTable);

        List<TrackingResponses.ImageFile> files = jdbc.query(
                query,
                new MapSqlParameterSource("taskId", taskId).addValue("scanId", scanId),
                (rs, rowNum) -> new TrackingResponses.ImageFile(
                        rs.getLong("id"),
                        rs.getString("file_path"),
                        rs.getString("original_file_name"),
                        rs.getString("file_name"),
                        rs.getString("content_type"),
                        nullableLong(rs.getObject("file_size"))
                )
        );
        return files.isEmpty() ? null : files.get(0);
    }

    public TrackingResponses.Area area(Long areaId, String parentKey, Long rootAreaId, TrackingAreaLevel level, String code, String name, long planned, long processed, long matched, long mismatched, long extras, long scanEvents,
            LocalDateTime lastActivity,
            InventoryTaskStatus taskStatus,
            List<TrackingResponses.UserRef> staff
    ) {

        long remaining = Math.max(planned - processed, 0);
        return new TrackingResponses.Area(
                key(level, areaId),
                parentKey,
                areaId,
                rootAreaId,
                level,
                code,
                name,
                planned,
                processed,
                matched,
                mismatched,
                remaining,
                extras,
                scanEvents,
                percentage(processed, planned),
                statusPolicy.areaStatus(planned, processed, scanEvents, lastActivity, taskStatus),
                lastActivity,
                staff
        );
    }

    public static String key(TrackingAreaLevel level, Long id) {
        return level.name() + ":" + id;
    }

    public static String imageUrl(Long taskId, Long scanId, boolean hasImage) {
        if (!hasImage || scanId == null) {
            return null;
        }
        return "/api/inventory/tracking/tasks/" + taskId + "/scan-events/" + scanId + "/image";
    }

    public static TrackingResponses.UserRef nullableUserRef(
            Object id,
            String firstName,
            String lastName,
            String username,
            String mobile
    ) {
        Long userId = nullableLong(id);
        return userId == null ? null : userRef(userId, firstName, lastName, username, mobile);
    }

    public static TrackingResponses.UserRef userRef(
            Long id,
            String firstName,
            String lastName,
            String username,
            String mobile
    ) {
        String name = ((firstName == null ? "" : firstName.trim()) + " "
                + (lastName == null ? "" : lastName.trim())).trim();
        if (name.isBlank()) {
            name = username;
        }
        return new TrackingResponses.UserRef(id, name, username, mobile);
    }

    public static int percentage(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.min(Math.round(value * 100.0 / total), 100);
    }

    public static String normalizeSearch(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static BigDecimal decimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public static Long nullableLong(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).longValue();
    }

    public static LocalDateTime localDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        throw new IllegalArgumentException(
                "Unsupported date-time value: " + value.getClass().getName()
        );
    }
}

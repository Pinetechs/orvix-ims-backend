package com.pinetechs.orvix.ims.inventory.tracking.provider;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryScanEventType;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.tracking.dto.TrackingResponses;
import com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingAreaLevel;
import com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingResultFilter;
import com.pinetechs.orvix.ims.inventory.tracking.repository.support.TrackingJdbcSupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.pinetechs.orvix.ims.inventory.tracking.repository.support.TrackingJdbcSupport.decimal;
import static com.pinetechs.orvix.ims.inventory.tracking.repository.support.TrackingJdbcSupport.imageUrl;
import static com.pinetechs.orvix.ims.inventory.tracking.repository.support.TrackingJdbcSupport.key;
import static com.pinetechs.orvix.ims.inventory.tracking.repository.support.TrackingJdbcSupport.localDateTime;
import static com.pinetechs.orvix.ims.inventory.tracking.repository.support.TrackingJdbcSupport.normalizeSearch;
import static com.pinetechs.orvix.ims.inventory.tracking.repository.support.TrackingJdbcSupport.nullableLong;
import static com.pinetechs.orvix.ims.inventory.tracking.repository.support.TrackingJdbcSupport.nullableUserRef;
import static com.pinetechs.orvix.ims.inventory.tracking.repository.support.TrackingJdbcSupport.percentage;
import static com.pinetechs.orvix.ims.inventory.tracking.repository.support.TrackingJdbcSupport.userRef;

/**
 * Read-only tracking queries for spare-part inventory tasks.
 */
@Repository
public class SparePartTrackingProvider implements InventoryTrackingProvider {

    private static final String SCAN_TABLE = "spare_part_inventory_scans";

    private final TrackingJdbcSupport support;
    private final NamedParameterJdbcTemplate jdbc;

    public SparePartTrackingProvider(
            NamedParameterJdbcTemplate jdbc,
            TrackingJdbcSupport support
    ) {
        this.jdbc = jdbc;
        this.support = support;
    }

    @Override
    public InventoryDomain domain() {
        return InventoryDomain.SPARE_PART;
    }

    // Summary metrics used by task lists and overview screens.
    @Override
    public Map<Long, TrackingResponses.CurrentMetrics> currentMetrics(Collection<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String query = """
                select i.task_id,
                       count(i.id) as total_expected,
                       sum(case when i.current_scan_id is not null then 1 else 0 end) as processed_expected,
                       sum(case when i.current_scan_id is not null and i.status = 'MATCHED' then 1 else 0 end) as matched,
                       sum(case when i.current_scan_id is not null and i.status <> 'MATCHED' then 1 else 0 end) as mismatched,
                       sum(case when i.current_scan_id is not null and img.id is null then 1 else 0 end) as accepted_without_image,
                       sum(case when i.current_scan_id is not null and
                                     (nullif(trim(i.notes), '') is not null or nullif(trim(s.notes), '') is not null)
                                then 1 else 0 end) as accepted_with_notes,
                       coalesce(sum(i.stock_qty), 0) as total_expected_quantity,
                       coalesce(sum(case when i.current_scan_id is not null then i.stock_qty else 0 end), 0) as processed_expected_quantity,
                       coalesce(sum(case when i.current_scan_id is not null then i.actual_qty else 0 end), 0) as actual_quantity,
                       coalesce(sum(case when i.current_scan_id is not null and i.variance_qty < 0
                                         then -i.variance_qty else 0 end), 0) as shortage_quantity,
                       coalesce(sum(case when i.current_scan_id is not null and i.variance_qty > 0
                                         then i.variance_qty else 0 end), 0) as overage_quantity,
                       coalesce(sum(case when i.current_scan_id is not null then i.variance_qty else 0 end), 0) as net_variance_quantity
                  from spare_part_inventory_items i
                  left join spare_part_inventory_scans s on s.id = i.current_scan_id
                  left join uploaded_files img on img.id = s.scan_image_file_id and img.is_deleted = false
                 where i.task_id in (:taskIds)
                 group by i.task_id
                """;

        Map<Long, TrackingResponses.CurrentMetrics> result = new HashMap<>();
        jdbc.query(query, new MapSqlParameterSource("taskIds", taskIds), rs -> {
            long taskId = rs.getLong("task_id");
            long total = rs.getLong("total_expected");
            long processed = rs.getLong("processed_expected");
            result.put(taskId, new TrackingResponses.CurrentMetrics(
                    total,
                    processed,
                    rs.getLong("matched"),
                    rs.getLong("mismatched"),
                    Math.max(total - processed, 0),
                    rs.getLong("accepted_without_image"),
                    rs.getLong("accepted_with_notes"),
                    percentage(processed, total),
                    decimal(rs.getBigDecimal("total_expected_quantity")),
                    decimal(rs.getBigDecimal("processed_expected_quantity")),
                    decimal(rs.getBigDecimal("actual_quantity")),
                    decimal(rs.getBigDecimal("shortage_quantity")),
                    decimal(rs.getBigDecimal("overage_quantity")),
                    decimal(rs.getBigDecimal("net_variance_quantity"))
            ));
        });
        return result;
    }

    @Override
    public Map<Long, TrackingResponses.EventMetrics> eventMetrics(Collection<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String query = """
                select s.task_id,
                       count(s.id) as total_events,
                       sum(case when s.event_type = 'FIRST_SCAN' then 1 else 0 end) as first_scans,
                       sum(case when s.event_type = 'DUPLICATE' then 1 else 0 end) as duplicates,
                       sum(case when s.event_type = 'CONFLICT' then 1 else 0 end) as conflicts,
                       sum(case when s.event_type = 'CORRECTION' then 1 else 0 end) as corrections,
                       sum(case when s.event_type = 'EXTRA' then 1 else 0 end) as extra_events,
                       sum(case when s.event_type = 'AMBIGUOUS' then 1 else 0 end) as ambiguous_events,
                       count(distinct case when s.event_type = 'EXTRA' then s.scanned_item_no end) as unique_unexpected_codes,
                       min(s.scanned_at) as first_activity_at,
                       max(s.scanned_at) as last_activity_at
                  from spare_part_inventory_scans s
                 where s.task_id in (:taskIds)
                 group by s.task_id
                """;

        Map<Long, TrackingResponses.EventMetrics> result = new HashMap<>();
        jdbc.query(query, new MapSqlParameterSource("taskIds", taskIds), rs -> {
            result.put(rs.getLong("task_id"), new TrackingResponses.EventMetrics(
                    rs.getLong("total_events"),
                    rs.getLong("first_scans"),
                    rs.getLong("duplicates"),
                    rs.getLong("conflicts"),
                    rs.getLong("corrections"),
                    rs.getLong("extra_events"),
                    rs.getLong("ambiguous_events"),
                    rs.getLong("unique_unexpected_codes"),
                    localDateTime(rs.getObject("first_activity_at")),
                    localDateTime(rs.getObject("last_activity_at"))
            ));
        });
        return result;
    }

    // Area progress follows the hierarchy used by this inventory domain.
    @Override
    public List<TrackingResponses.Area> areas(Long taskId, InventoryTaskStatus taskStatus) {
        List<TrackingResponses.Area> areas = new ArrayList<>();
        Map<Long, List<TrackingResponses.UserRef>> staff = assignedStaffByBranch(taskId);

        appendBranchAreas(areas, taskId, taskStatus, staff);
       // appendLocationAreas(areas, taskId, taskStatus, staff);

        return areas;
    }

    private void appendBranchAreas(List<TrackingResponses.Area> target, Long taskId, InventoryTaskStatus taskStatus, Map<Long, List<TrackingResponses.UserRef>> staff) {
        String query = """
                select b.id as area_id,
                       null as parent_id,
                       b.id as root_area_id,
                       b.branch_name as area_code,
                       b.branch_name as area_name,
                       count(i.id) as planned,
                       sum(case when i.current_scan_id is not null then 1 else 0 end) as processed,
                       sum(case when i.current_scan_id is not null and i.status = 'MATCHED' then 1 else 0 end) as matched,
                       sum(case when i.current_scan_id is not null and i.status <> 'MATCHED' then 1 else 0 end) as mismatched,
                       coalesce(e.extras, 0) as extras,
                       coalesce(e.scan_events, 0) as scan_events,
                       e.last_activity_at
                  from spare_part_inventory_branches b
                  left join spare_part_inventory_items i
                    on i.planned_branch_id = b.id and i.task_id = b.task_id
                  left join (
                        select actual_branch_id as area_id,
                               count(id) as scan_events,
                               sum(case when event_type = 'EXTRA' then 1 else 0 end) as extras,
                               max(scanned_at) as last_activity_at
                          from spare_part_inventory_scans
                         where task_id = :taskId
                         group by actual_branch_id
                  ) e on e.area_id = b.id
                 where b.task_id = :taskId
                 group by b.id, b.branch_name, e.extras, e.scan_events, e.last_activity_at
                 order by b.branch_name
                """;
        appendAreas(target, query, taskId, taskStatus, TrackingAreaLevel.BRANCH, null, staff);
    }

    private void appendLocationAreas(List<TrackingResponses.Area> target, Long taskId, InventoryTaskStatus taskStatus, Map<Long, List<TrackingResponses.UserRef>> staff) {
        String query = """
                select l.id as area_id,
                       b.id as parent_id,
                       b.id as root_area_id,
                       l.location_code as area_code,
                       l.location_code as area_name,
                       count(i.id) as planned,
                       sum(case when i.current_scan_id is not null then 1 else 0 end) as processed,
                       sum(case when i.current_scan_id is not null and i.status = 'MATCHED' then 1 else 0 end) as matched,
                       sum(case when i.current_scan_id is not null and i.status <> 'MATCHED' then 1 else 0 end) as mismatched,
                       coalesce(e.extras, 0) as extras,
                       coalesce(e.scan_events, 0) as scan_events,
                       e.last_activity_at
                  from spare_part_inventory_locations l
                  join spare_part_inventory_branches b on b.id = l.branch_id
                  left join spare_part_inventory_items i
                    on i.planned_location_id = l.id and i.task_id = l.task_id
                  left join (
                        select actual_location_id as area_id,
                               count(id) as scan_events,
                               sum(case when event_type = 'EXTRA' then 1 else 0 end) as extras,
                               max(scanned_at) as last_activity_at
                          from spare_part_inventory_scans
                         where task_id = :taskId
                         group by actual_location_id
                  ) e on e.area_id = l.id
                 where l.task_id = :taskId
                 group by l.id, b.id, b.branch_name, l.location_code,
                          e.extras, e.scan_events, e.last_activity_at
                 order by b.branch_name, l.location_code
                """;
        appendAreas(target, query, taskId, taskStatus, TrackingAreaLevel.LOCATION,
                TrackingAreaLevel.BRANCH, staff);
    }

    private void appendAreas(List<TrackingResponses.Area> target, String query, Long taskId, InventoryTaskStatus taskStatus,
            TrackingAreaLevel level,
            TrackingAreaLevel parentLevel,
            Map<Long, List<TrackingResponses.UserRef>> staff
    ) {
        target.addAll(jdbc.query(query, new MapSqlParameterSource("taskId", taskId), (rs, rowNum) -> {
            Long areaId = rs.getLong("area_id");
            Long parentId = nullableLong(rs.getObject("parent_id"));
            Long rootAreaId = rs.getLong("root_area_id");
            String parentKey = parentId == null || parentLevel == null
                    ? null
                    : key(parentLevel, parentId);
            return support.area(
                    areaId,
                    parentKey,
                    rootAreaId,
                    level,
                    rs.getString("area_code"),
                    rs.getString("area_name"),
                    rs.getLong("planned"),
                    rs.getLong("processed"),
                    rs.getLong("matched"),
                    rs.getLong("mismatched"),
                    rs.getLong("extras"),
                    rs.getLong("scan_events"),
                    localDateTime(rs.getObject("last_activity_at")),
                    taskStatus,
                    staff.getOrDefault(rootAreaId, List.of())
            );
        }));
    }

    // Team statistics include current assignments and historical scan activity.
    @Override
    public List<TrackingResponses.TeamMember> team(Long taskId) {
        String query = """
                select u.id as user_id, u.first_name, u.last_name, u.username, u.mobile,
                       coalesce(a.currently_assigned, 0) as currently_assigned,
                       coalesce(c.accepted_count, 0) as accepted_count,
                       coalesce(c.matched_count, 0) as matched_count,
                       coalesce(c.mismatched_count, 0) as mismatched_count,
                       coalesce(e.scan_events, 0) as scan_events,
                       coalesce(e.first_scans, 0) as first_scans,
                       coalesce(e.duplicates, 0) as duplicates,
                       coalesce(e.conflicts, 0) as conflicts,
                       coalesce(e.corrections, 0) as corrections,
                       coalesce(e.extras, 0) as extras,
                       e.first_activity_at, e.last_activity_at
                  from users u
                  join (
                        select user_id
                          from inventory_task_assignments
                         where task_id = :taskId
                        union
                        select scanned_by_user_id as user_id
                          from spare_part_inventory_scans
                         where task_id = :taskId
                  ) people on people.user_id = u.id
                  left join (
                        select user_id,
                               max(case when active = true then 1 else 0 end) as currently_assigned
                          from inventory_task_assignments
                         where task_id = :taskId
                         group by user_id
                  ) a on a.user_id = u.id
                  left join (
                        select i.counted_by_user_id as user_id,
                               count(i.id) as accepted_count,
                               sum(case when i.status = 'MATCHED' then 1 else 0 end) as matched_count,
                               sum(case when i.current_scan_id is not null and i.status <> 'MATCHED' then 1 else 0 end) as mismatched_count
                          from spare_part_inventory_items i
                         where i.task_id = :taskId
                           and i.counted_by_user_id is not null
                           and i.current_scan_id is not null
                         group by i.counted_by_user_id
                  ) c on c.user_id = u.id
                  left join (
                        select scanned_by_user_id as user_id,
                               count(id) as scan_events,
                               sum(case when event_type = 'FIRST_SCAN' then 1 else 0 end) as first_scans,
                               sum(case when event_type = 'DUPLICATE' then 1 else 0 end) as duplicates,
                               sum(case when event_type = 'CONFLICT' then 1 else 0 end) as conflicts,
                               sum(case when event_type = 'CORRECTION' then 1 else 0 end) as corrections,
                               sum(case when event_type = 'EXTRA' then 1 else 0 end) as extras,
                               min(scanned_at) as first_activity_at,
                               max(scanned_at) as last_activity_at
                          from spare_part_inventory_scans
                         where task_id = :taskId
                         group by scanned_by_user_id
                  ) e on e.user_id = u.id
                 order by e.last_activity_at desc, u.first_name asc, u.last_name asc
                """;

        Map<Long, List<String>> assignedAreas = assignedAreasByUser(taskId);
        LocalDateTime now = LocalDateTime.now();
        return jdbc.query(query, new MapSqlParameterSource("taskId", taskId), (rs, rowNum) -> {
            LocalDateTime lastActivity = localDateTime(rs.getObject("last_activity_at"));
            Long inactiveSeconds = lastActivity == null
                    ? null
                    : Math.max(Duration.between(lastActivity, now).getSeconds(), 0);
            long userId = rs.getLong("user_id");
            return new TrackingResponses.TeamMember(
                    userRef(userId, rs.getString("first_name"), rs.getString("last_name"),
                            rs.getString("username"), rs.getString("mobile")),
                    rs.getInt("currently_assigned") > 0,
                    assignedAreas.getOrDefault(userId, List.of()),
                    rs.getLong("accepted_count"),
                    rs.getLong("matched_count"),
                    rs.getLong("mismatched_count"),
                    rs.getLong("scan_events"),
                    rs.getLong("first_scans"),
                    rs.getLong("duplicates"),
                    rs.getLong("conflicts"),
                    rs.getLong("corrections"),
                    rs.getLong("extras"),
                    localDateTime(rs.getObject("first_activity_at")),
                    lastActivity,
                    inactiveSeconds
            );
        });
    }

    // Detailed result and event queries back the tracking drill-down pages.
    @Override
    public Page<TrackingResponses.ResultItem> results(Long taskId, TrackingResultFilter filter, String search, Pageable pageable) {
        TrackingResultFilter resolvedFilter = filter == null ? TrackingResultFilter.ALL : filter;
        String normalizedSearch = normalizeSearch(search);
        String where = " where i.task_id = :taskId "
                + resultFilterCondition(resolvedFilter)
                + (normalizedSearch == null ? "" : " and (upper(i.item_no) like :search "
                + "or upper(concat_ws(' ', i.item_no, i.brand_name)) like :search)");

        MapSqlParameterSource params = new MapSqlParameterSource("taskId", taskId)
                .addValue("search", normalizedSearch == null
                        ? null
                        : "%" + normalizedSearch.toUpperCase() + "%");

        String from = """
                  from spare_part_inventory_items i
                  join spare_part_inventory_branches pb on pb.id = i.planned_branch_id
                  join spare_part_inventory_locations pl on pl.id = i.planned_location_id
                  left join spare_part_inventory_branches ab on ab.id = i.actual_branch_id
                  left join spare_part_inventory_locations al on al.id = i.actual_location_id
                  left join spare_part_inventory_scans s on s.id = i.current_scan_id
                  left join users u on u.id = i.counted_by_user_id
                  left join uploaded_files img on img.id = s.scan_image_file_id and img.is_deleted = false
                """;

        long total = jdbc.queryForObject("select count(i.id) " + from + where, params, Long.class);
        String query = """
                select i.id as item_id, i.item_no as code, i.brand_name as secondary_code,
                       i.brand_name as description, i.status as item_status,
                       (i.current_scan_id is not null) as processed,
                       (i.current_scan_id is not null and i.status = 'MATCHED') as matched,
                       pb.branch_name as expected_area, pl.location_code as expected_sub_area,
                       null as expected_leaf_area,
                       ab.branch_name as actual_area, al.location_code as actual_sub_area,
                       null as actual_leaf_area,
                       i.stock_qty as expected_quantity, i.actual_qty as actual_quantity,
                       i.variance_qty as variance_quantity,
                       u.id as user_id, u.first_name, u.last_name, u.username, u.mobile,
                       i.counted_at as accepted_at, s.id as scan_id, (img.id is not null) as has_image,
                       coalesce(nullif(s.notes, ''), i.notes) as notes
                """ + from + where + " order by i.id asc limit :limit offset :offset";

        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        List<TrackingResponses.ResultItem> content = jdbc.query(query, params, (rs, rowNum) ->
                new TrackingResponses.ResultItem(
                        rs.getLong("item_id"),
                        InventoryDomain.SPARE_PART,
                        rs.getString("code"),
                        rs.getString("secondary_code"),
                        rs.getString("description"),
                        rs.getString("item_status"),
                        rs.getBoolean("processed"),
                        rs.getBoolean("matched"),
                        rs.getString("expected_area"),
                        rs.getString("expected_sub_area"),
                        rs.getString("expected_leaf_area"),
                        rs.getString("actual_area"),
                        rs.getString("actual_sub_area"),
                        rs.getString("actual_leaf_area"),
                        rs.getBigDecimal("expected_quantity"),
                        rs.getBigDecimal("actual_quantity"),
                        rs.getBigDecimal("variance_quantity"),
                        nullableUserRef(rs.getObject("user_id"), rs.getString("first_name"),
                                rs.getString("last_name"), rs.getString("username"), rs.getString("mobile")),
                        localDateTime(rs.getObject("accepted_at")),
                        nullableLong(rs.getObject("scan_id")),
                        rs.getBoolean("has_image"),
                        imageUrl(taskId, nullableLong(rs.getObject("scan_id")), rs.getBoolean("has_image")),
                        rs.getString("notes")
                ));
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<TrackingResponses.ScanEvent> scanEvents(
            Long taskId,
            InventoryScanEventType eventType,
            String search,
            Pageable pageable
    ) {
        String normalizedSearch = normalizeSearch(search);
        String where = " where s.task_id = :taskId "
                + (eventType == null ? "" : " and s.event_type = :eventType ")
                + (normalizedSearch == null ? "" : " and upper(s.scanned_item_no) like :search");

        MapSqlParameterSource params = new MapSqlParameterSource("taskId", taskId)
                .addValue("eventType", eventType == null ? null : eventType.name())
                .addValue("search", normalizedSearch == null
                        ? null
                        : "%" + normalizedSearch.toUpperCase() + "%");

        String from = """
                  from spare_part_inventory_scans s
                  join users u on u.id = s.scanned_by_user_id
                  left join spare_part_inventory_branches eb on eb.id = s.expected_branch_id
                  left join spare_part_inventory_locations el on el.id = s.expected_location_id
                  left join spare_part_inventory_branches ab on ab.id = s.actual_branch_id
                  left join spare_part_inventory_locations al on al.id = s.actual_location_id
                  left join uploaded_files img on img.id = s.scan_image_file_id and img.is_deleted = false
                """;

        long total = jdbc.queryForObject("select count(s.id) " + from + where, params, Long.class);
        String query = """
                select s.id as scan_id, s.scanned_item_no as scanned_code, s.event_type, s.scan_result,
                       s.item_id, eb.branch_name as expected_area, el.location_code as expected_sub_area,
                       null as expected_leaf_area, ab.branch_name as actual_area,
                       al.location_code as actual_sub_area, null as actual_leaf_area,
                       s.stock_qty as expected_quantity, s.counted_qty as actual_quantity,
                       s.variance_qty as variance_quantity,
                       u.id as user_id, u.first_name, u.last_name, u.username, u.mobile,
                       s.scanned_at, s.device_scanned_at, s.device_id, s.symbology,
                       (img.id is not null) as has_image, s.notes,
                       concat_ws(' | ', s.location_status, s.quantity_status, s.message) as details
                """ + from + where
                + " order by s.scanned_at desc, s.id desc limit :limit offset :offset";

        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        List<TrackingResponses.ScanEvent> content = jdbc.query(query, params, (rs, rowNum) -> {
            long scanId = rs.getLong("scan_id");
            boolean hasImage = rs.getBoolean("has_image");
            return new TrackingResponses.ScanEvent(
                    scanId,
                    InventoryDomain.SPARE_PART,
                    rs.getString("scanned_code"),
                    InventoryScanEventType.valueOf(rs.getString("event_type")),
                    rs.getString("scan_result"),
                    nullableLong(rs.getObject("item_id")),
                    rs.getString("expected_area"),
                    rs.getString("expected_sub_area"),
                    rs.getString("expected_leaf_area"),
                    rs.getString("actual_area"),
                    rs.getString("actual_sub_area"),
                    rs.getString("actual_leaf_area"),
                    rs.getBigDecimal("expected_quantity"),
                    rs.getBigDecimal("actual_quantity"),
                    rs.getBigDecimal("variance_quantity"),
                    userRef(rs.getLong("user_id"), rs.getString("first_name"),
                            rs.getString("last_name"), rs.getString("username"), rs.getString("mobile")),
                    localDateTime(rs.getObject("scanned_at")),
                    localDateTime(rs.getObject("device_scanned_at")),
                    rs.getString("device_id"),
                    rs.getString("symbology"),
                    hasImage,
                    imageUrl(taskId, scanId, hasImage),
                    rs.getString("notes"),
                    rs.getString("details")
            );
        });
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public TrackingResponses.ImageFile image(Long taskId, Long scanId) {
        return support.findImage(SCAN_TABLE, taskId, scanId);
    }

    private String resultFilterCondition(TrackingResultFilter filter) {
        return switch (filter) {
            case ALL -> "";
            case MATCHED -> " and i.current_scan_id is not null and i.status = 'MATCHED'";
            case MISMATCHED -> " and i.current_scan_id is not null and i.status <> 'MATCHED'";
            case REMAINING -> " and i.current_scan_id is null";
            case MISSING_IMAGE -> " and i.current_scan_id is not null and img.id is null";
            case NOTES -> " and nullif(trim(coalesce(nullif(s.notes, ''), i.notes)), '') is not null";
        };
    }

    private Map<Long, List<TrackingResponses.UserRef>> assignedStaffByBranch(Long taskId) {
        String query = """
                select x.branch_id as root_area_id,
                       u.id as user_id,
                       u.first_name, u.last_name, u.username, u.mobile
                  from spare_part_inventory_branch_assignments x
                  join inventory_task_assignments a on a.id = x.assignment_id
                  join users u on u.id = a.user_id
                 where a.task_id = :taskId
                   and a.active = true
                   and x.active = true
                 order by u.first_name, u.last_name
                """;

        Map<Long, List<TrackingResponses.UserRef>> result = new HashMap<>();
        jdbc.query(query, new MapSqlParameterSource("taskId", taskId), rs -> {
            long rootId = rs.getLong("root_area_id");
            TrackingResponses.UserRef user = userRef(
                    rs.getLong("user_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("username"),
                    rs.getString("mobile")
            );
            List<TrackingResponses.UserRef> users = result.computeIfAbsent(
                    rootId,
                    ignored -> new ArrayList<>()
            );
            if (users.stream().noneMatch(existing -> existing.id().equals(user.id()))) {
                users.add(user);
            }
        });
        return result;
    }

    private Map<Long, List<String>> assignedAreasByUser(Long taskId) {
        String query = """
                select a.user_id, area.branch_name as area_name
                  from spare_part_inventory_branch_assignments x
                  join inventory_task_assignments a on a.id = x.assignment_id
                  join spare_part_inventory_branches area on area.id = x.branch_id
                 where a.task_id = :taskId
                   and a.active = true
                   and x.active = true
                 order by area_name
                """;

        Map<Long, Set<String>> collected = new LinkedHashMap<>();
        jdbc.query(query, new MapSqlParameterSource("taskId", taskId), rs -> {
            collected.computeIfAbsent(rs.getLong("user_id"), ignored -> new LinkedHashSet<>())
                    .add(rs.getString("area_name"));
        });

        Map<Long, List<String>> result = new HashMap<>();
        collected.forEach((userId, areas) -> result.put(userId, List.copyOf(areas)));
        return result;
    }
}

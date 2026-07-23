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

import java.math.BigDecimal;
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

import static com.pinetechs.orvix.ims.inventory.tracking.repository.support.TrackingJdbcSupport.imageUrl;
import static com.pinetechs.orvix.ims.inventory.tracking.repository.support.TrackingJdbcSupport.key;
import static com.pinetechs.orvix.ims.inventory.tracking.repository.support.TrackingJdbcSupport.localDateTime;
import static com.pinetechs.orvix.ims.inventory.tracking.repository.support.TrackingJdbcSupport.normalizeSearch;
import static com.pinetechs.orvix.ims.inventory.tracking.repository.support.TrackingJdbcSupport.nullableLong;
import static com.pinetechs.orvix.ims.inventory.tracking.repository.support.TrackingJdbcSupport.nullableUserRef;
import static com.pinetechs.orvix.ims.inventory.tracking.repository.support.TrackingJdbcSupport.orderBy;
import static com.pinetechs.orvix.ims.inventory.tracking.repository.support.TrackingJdbcSupport.percentage;
import static com.pinetechs.orvix.ims.inventory.tracking.repository.support.TrackingJdbcSupport.userRef;

/**
 * Read-only tracking queries for asset inventory tasks.
 */
@Repository
public class AssetTrackingProvider implements InventoryTrackingProvider {

    private static final String SCAN_TABLE = "asset_inventory_scans";
    private static final Map<String, String> RESULT_SORT_COLUMNS = Map.of(
            "code", "code", "description", "description",
            "expectedQuantity", "expected_quantity", "actualQuantity", "actual_quantity",
            "varianceQuantity", "variance_quantity", "acceptedAt", "accepted_at"
    );
    private static final Map<String, String> EVENT_SORT_COLUMNS = Map.of(
            "scannedCode", "scanned_code", "eventType", "event_type",
            "result", "scan_result", "expectedQuantity", "expected_quantity",
            "actualQuantity", "actual_quantity", "varianceQuantity", "variance_quantity",
            "scannedAt", "scanned_at", "deviceId", "device_id"
    );

    private final TrackingJdbcSupport support;
    private final NamedParameterJdbcTemplate jdbc;

    public AssetTrackingProvider(
            NamedParameterJdbcTemplate jdbc,
            TrackingJdbcSupport support
    ) {
        this.jdbc = jdbc;
        this.support = support;
    }

    @Override
    public InventoryDomain domain() {
        return InventoryDomain.ASSET;
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
                                then 1 else 0 end) as accepted_with_notes
                  from asset_inventory_items i
                  left join asset_inventory_scans s on s.id = i.current_scan_id
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
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
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
                       count(distinct case when s.event_type = 'EXTRA' then s.scanned_barcode end) as unique_unexpected_codes,
                       min(s.scanned_at) as first_activity_at,
                       max(s.scanned_at) as last_activity_at
                  from asset_inventory_scans s
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
        Map<Long, List<TrackingResponses.UserRef>> staff = assignedStaffByLocation(taskId);

        appendLocationAreas(areas, taskId, taskStatus, staff);
        appendFloorAreas(areas, taskId, taskStatus, staff);
        appendPlaceAreas(areas, taskId, taskStatus, staff);

        return areas;
    }

    private void appendLocationAreas(
            List<TrackingResponses.Area> target,
            Long taskId,
            InventoryTaskStatus taskStatus,
            Map<Long, List<TrackingResponses.UserRef>> staff
    ) {
        String query = """
                select l.id as area_id,
                       null as parent_id,
                       i.planned_location_id as root_area_id,
                       l.location_name as area_code,
                       l.location_name as area_name,
                       count(i.id) as planned,
                       sum(case when i.current_scan_id is not null then 1 else 0 end) as processed,
                       sum(case when i.current_scan_id is not null and i.status = 'MATCHED' then 1 else 0 end) as matched,
                       sum(case when i.current_scan_id is not null and i.status <> 'MATCHED' then 1 else 0 end) as mismatched,
                       coalesce(e.extras, 0) as extras,
                       coalesce(e.scan_events, 0) as scan_events,
                       e.last_activity_at
                  from asset_inventory_items i
                  join asset_inventory_locations l on l.id = i.planned_location_id
                  join asset_inventory_floors f on f.id = i.planned_floor_id
                  join asset_inventory_places p on p.id = i.planned_place_id
                  left join (
                        select actual_location_id as area_id,
                               count(id) as scan_events,
                               sum(case when event_type = 'EXTRA' then 1 else 0 end) as extras,
                               max(scanned_at) as last_activity_at
                          from asset_inventory_scans
                         where task_id = :taskId
                         group by actual_location_id
                  ) e on e.area_id = l.id
                 where i.task_id = :taskId
                 group by l.id, i.planned_location_id, l.location_name,
                          e.extras, e.scan_events, e.last_activity_at
                 order by l.location_name
                """;
        appendAreas(target, query, taskId, taskStatus, TrackingAreaLevel.LOCATION, null, staff);
    }

    private void appendFloorAreas(
            List<TrackingResponses.Area> target,
            Long taskId,
            InventoryTaskStatus taskStatus,
            Map<Long, List<TrackingResponses.UserRef>> staff
    ) {
        String query = """
                select f.id as area_id,
                       l.id as parent_id,
                       i.planned_location_id as root_area_id,
                       f.floor_name as area_code,
                       f.floor_name as area_name,
                       count(i.id) as planned,
                       sum(case when i.current_scan_id is not null then 1 else 0 end) as processed,
                       sum(case when i.current_scan_id is not null and i.status = 'MATCHED' then 1 else 0 end) as matched,
                       sum(case when i.current_scan_id is not null and i.status <> 'MATCHED' then 1 else 0 end) as mismatched,
                       coalesce(e.extras, 0) as extras,
                       coalesce(e.scan_events, 0) as scan_events,
                       e.last_activity_at
                  from asset_inventory_items i
                  join asset_inventory_locations l on l.id = i.planned_location_id
                  join asset_inventory_floors f on f.id = i.planned_floor_id
                  join asset_inventory_places p on p.id = i.planned_place_id
                  left join (
                        select actual_floor_id as area_id,
                               count(id) as scan_events,
                               sum(case when event_type = 'EXTRA' then 1 else 0 end) as extras,
                               max(scanned_at) as last_activity_at
                          from asset_inventory_scans
                         where task_id = :taskId
                         group by actual_floor_id
                  ) e on e.area_id = f.id
                 where i.task_id = :taskId
                 group by f.id, l.id, i.planned_location_id, f.floor_name,
                          e.extras, e.scan_events, e.last_activity_at
                 order by l.location_name, f.floor_name
                """;
        appendAreas(target, query, taskId, taskStatus, TrackingAreaLevel.FLOOR,
                TrackingAreaLevel.LOCATION, staff);
    }

    private void appendPlaceAreas(
            List<TrackingResponses.Area> target,
            Long taskId,
            InventoryTaskStatus taskStatus,
            Map<Long, List<TrackingResponses.UserRef>> staff
    ) {
        String query = """
                select p.id as area_id,
                       f.id as parent_id,
                       i.planned_location_id as root_area_id,
                       p.place_name as area_code,
                       p.place_name as area_name,
                       count(i.id) as planned,
                       sum(case when i.current_scan_id is not null then 1 else 0 end) as processed,
                       sum(case when i.current_scan_id is not null and i.status = 'MATCHED' then 1 else 0 end) as matched,
                       sum(case when i.current_scan_id is not null and i.status <> 'MATCHED' then 1 else 0 end) as mismatched,
                       coalesce(e.extras, 0) as extras,
                       coalesce(e.scan_events, 0) as scan_events,
                       e.last_activity_at
                  from asset_inventory_items i
                  join asset_inventory_locations l on l.id = i.planned_location_id
                  join asset_inventory_floors f on f.id = i.planned_floor_id
                  join asset_inventory_places p on p.id = i.planned_place_id
                  left join (
                        select actual_place_id as area_id,
                               count(id) as scan_events,
                               sum(case when event_type = 'EXTRA' then 1 else 0 end) as extras,
                               max(scanned_at) as last_activity_at
                          from asset_inventory_scans
                         where task_id = :taskId
                         group by actual_place_id
                  ) e on e.area_id = p.id
                 where i.task_id = :taskId
                 group by p.id, f.id, i.planned_location_id, p.place_name,
                          e.extras, e.scan_events, e.last_activity_at
                 order by l.location_name, f.floor_name, p.place_name
                """;
        appendAreas(target, query, taskId, taskStatus, TrackingAreaLevel.PLACE,
                TrackingAreaLevel.FLOOR, staff);
    }

    private void appendAreas(
            List<TrackingResponses.Area> target,
            String query,
            Long taskId,
            InventoryTaskStatus taskStatus,
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
                        select user_id
                          from asset_inventory_scans
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
                        select i.checked_by_user_id as user_id,
                               count(i.id) as accepted_count,
                               sum(case when i.status = 'MATCHED' then 1 else 0 end) as matched_count,
                               sum(case when i.current_scan_id is not null and i.status <> 'MATCHED' then 1 else 0 end) as mismatched_count
                          from asset_inventory_items i
                         where i.task_id = :taskId
                           and i.checked_by_user_id is not null
                           and i.current_scan_id is not null
                         group by i.checked_by_user_id
                  ) c on c.user_id = u.id
                  left join (
                        select user_id,
                               count(id) as scan_events,
                               sum(case when event_type = 'FIRST_SCAN' then 1 else 0 end) as first_scans,
                               sum(case when event_type = 'DUPLICATE' then 1 else 0 end) as duplicates,
                               sum(case when event_type = 'CONFLICT' then 1 else 0 end) as conflicts,
                               sum(case when event_type = 'CORRECTION' then 1 else 0 end) as corrections,
                               sum(case when event_type = 'EXTRA' then 1 else 0 end) as extras,
                               min(scanned_at) as first_activity_at,
                               max(scanned_at) as last_activity_at
                          from asset_inventory_scans
                         where task_id = :taskId
                         group by user_id
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
    public Page<TrackingResponses.ResultItem> results(
            Long taskId,
            TrackingResultFilter filter,
            String search,
            Pageable pageable
    ) {
        TrackingResultFilter resolvedFilter = filter == null ? TrackingResultFilter.ALL : filter;
        String normalizedSearch = normalizeSearch(search);
        String where = " where i.task_id = :taskId "
                + resultFilterCondition(resolvedFilter)
                + (normalizedSearch == null ? "" : " and (upper(i.barcode) like :search "
                + "or upper(concat_ws(' ', i.description, i.asset_category, i.asset_type)) like :search)");

        MapSqlParameterSource params = new MapSqlParameterSource("taskId", taskId)
                .addValue("search", normalizedSearch == null
                        ? null
                        : "%" + normalizedSearch.toUpperCase() + "%");

        String from = """
                  from asset_inventory_items i
                  join asset_inventory_locations pl on pl.id = i.planned_location_id
                  join asset_inventory_floors pf on pf.id = i.planned_floor_id
                  join asset_inventory_places pp on pp.id = i.planned_place_id
                  left join asset_inventory_locations al on al.id = i.actual_location_id
                  left join asset_inventory_floors af on af.id = i.actual_floor_id
                  left join asset_inventory_places ap on ap.id = i.actual_place_id
                  left join asset_inventory_scans s on s.id = i.current_scan_id
                  left join users u on u.id = i.checked_by_user_id
                  left join uploaded_files img on img.id = s.scan_image_file_id and img.is_deleted = false
                """;

        long total = jdbc.queryForObject("select count(i.id) " + from + where, params, Long.class);
        String query = """
                select i.id as item_id, i.barcode as code, i.category_code as secondary_code,
                       i.description as description, i.status as item_status,
                       (i.current_scan_id is not null) as processed,
                       (i.current_scan_id is not null and i.status = 'MATCHED') as matched,
                       pl.location_name as expected_area, pf.floor_name as expected_sub_area,
                       pp.place_name as expected_leaf_area,
                       al.location_name as actual_area, af.floor_name as actual_sub_area,
                       ap.place_name as actual_leaf_area,
                       i.quantity as expected_quantity, i.quantity as actual_quantity,
                       null as variance_quantity,
                       u.id as user_id, u.first_name, u.last_name, u.username, u.mobile,
                       i.checked_at as accepted_at, s.id as scan_id, (img.id is not null) as has_image,
                       coalesce(nullif(s.notes, ''), i.notes) as notes
                """ + from + where
                + orderBy(pageable, RESULT_SORT_COLUMNS, "i.id asc")
                + " limit :limit offset :offset";

        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        List<TrackingResponses.ResultItem> content = jdbc.query(query, params, (rs, rowNum) ->
                new TrackingResponses.ResultItem(
                        rs.getLong("item_id"),
                        InventoryDomain.ASSET,
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
        return findScanEvents(taskId, eventType, search, pageable, false);
    }

    @Override
    public Page<TrackingResponses.ScanEvent> unresolvedScanEvents(
            Long taskId,
            InventoryScanEventType eventType,
            Pageable pageable
    ) {
        requireAttentionEventType(eventType);
        return findScanEvents(taskId, eventType, null, pageable, true);
    }

    private Page<TrackingResponses.ScanEvent> findScanEvents(
            Long taskId,
            InventoryScanEventType eventType,
            String search,
            Pageable pageable,
            boolean unresolvedOnly
    ) {
        String normalizedSearch = normalizeSearch(search);
        String where = " where s.task_id = :taskId "
                + (eventType == null ? "" : " and s.event_type = :eventType ")
                + unresolvedEventCondition(eventType, unresolvedOnly)
                + (normalizedSearch == null ? "" : " and upper(s.scanned_barcode) like :search");

        MapSqlParameterSource params = new MapSqlParameterSource("taskId", taskId)
                .addValue("eventType", eventType == null ? null : eventType.name())
                .addValue("search", normalizedSearch == null
                        ? null
                        : "%" + normalizedSearch.toUpperCase() + "%");

        String from = """
                  from asset_inventory_scans s
                  join users u on u.id = s.user_id
                  left join asset_inventory_locations el on el.id = s.expected_location_id
                  left join asset_inventory_floors ef on ef.id = s.expected_floor_id
                  left join asset_inventory_places ep on ep.id = s.expected_place_id
                  left join asset_inventory_locations al on al.id = s.actual_location_id
                  left join asset_inventory_floors af on af.id = s.actual_floor_id
                  left join asset_inventory_places ap on ap.id = s.actual_place_id
                  left join uploaded_files img on img.id = s.scan_image_file_id and img.is_deleted = false
                """;

        long total = jdbc.queryForObject("select count(s.id) " + from + where, params, Long.class);
        String query = """
                select s.id as scan_id, s.scanned_barcode as scanned_code, s.event_type, s.scan_result,
                       s.item_id, el.location_name as expected_area, ef.floor_name as expected_sub_area,
                       ep.place_name as expected_leaf_area, al.location_name as actual_area,
                       af.floor_name as actual_sub_area, ap.place_name as actual_leaf_area,
                       null as expected_quantity, null as actual_quantity, null as variance_quantity,
                       u.id as user_id, u.first_name, u.last_name, u.username, u.mobile,
                       s.scanned_at, s.device_scanned_at, s.device_id, s.symbology,
                       (img.id is not null) as has_image, s.notes, s.mismatch_fields as details
                """ + from + where
                + orderBy(pageable, EVENT_SORT_COLUMNS, "s.scanned_at desc, s.id desc")
                + " limit :limit offset :offset";

        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        List<TrackingResponses.ScanEvent> content = jdbc.query(query, params, (rs, rowNum) -> {
            long scanId = rs.getLong("scan_id");
            boolean hasImage = rs.getBoolean("has_image");
            return new TrackingResponses.ScanEvent(
                    scanId,
                    InventoryDomain.ASSET,
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

    private String unresolvedEventCondition(InventoryScanEventType eventType, boolean unresolvedOnly) {
        if (!unresolvedOnly) {
            return "";
        }
        String resolvedByReviewCenter = """
                 and not exists (
                       select 1 from inventory_review_issues ri
                        where ri.task_id = s.task_id
                          and ri.source_scan_id = s.id
                          and ri.status in ('RESOLVED', 'SUPERSEDED')
                 )
                """;
        if (eventType != InventoryScanEventType.CONFLICT) {
            return resolvedByReviewCenter;
        }
        return resolvedByReviewCenter + """
                 and not exists (
                       select 1 from asset_inventory_scans correction
                        where correction.task_id = s.task_id
                          and correction.item_id = s.item_id
                          and correction.event_type = 'CORRECTION'
                          and (correction.scanned_at > s.scanned_at
                               or (correction.scanned_at = s.scanned_at and correction.id > s.id))
                 )
                """;
    }

    private void requireAttentionEventType(InventoryScanEventType eventType) {
        if (eventType != InventoryScanEventType.EXTRA
                && eventType != InventoryScanEventType.AMBIGUOUS
                && eventType != InventoryScanEventType.CONFLICT) {
            throw new IllegalArgumentException("Unsupported attention event type: " + eventType);
        }
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

    private Map<Long, List<TrackingResponses.UserRef>> assignedStaffByLocation(Long taskId) {
        String query = """
                select x.location_id as root_area_id,
                       u.id as user_id,
                       u.first_name, u.last_name, u.username, u.mobile
                  from asset_inventory_location_assignments x
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
                select a.user_id, area.location_name as area_name
                  from asset_inventory_location_assignments x
                  join inventory_task_assignments a on a.id = x.assignment_id
                  join asset_inventory_locations area on area.id = x.location_id
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

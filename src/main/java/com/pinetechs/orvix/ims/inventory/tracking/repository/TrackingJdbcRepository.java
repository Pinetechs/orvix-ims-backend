package com.pinetechs.orvix.ims.inventory.tracking.repository;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryScanEventType;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.tracking.dto.TrackingResponses;
import com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingAreaLevel;
import com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingAreaStatus;
import com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingResultFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
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

@Repository
public class TrackingJdbcRepository {

    private static final long STALLED_AFTER_MINUTES = 30;

    private final NamedParameterJdbcTemplate jdbc;

    public TrackingJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<Long, TrackingResponses.CurrentMetrics> findCurrentMetrics(
            InventoryDomain domain,
            Collection<Long> taskIds
    ) {
        if (taskIds == null || taskIds.isEmpty()) return Collections.emptyMap();

        DomainSql sql = DomainSql.forDomain(domain);
        String query = """
                select i.task_id,
                       count(i.id) as total_expected,
                       sum(case when i.current_scan_id is not null then 1 else 0 end) as processed_expected,
                       sum(case when i.current_scan_id is not null and %s then 1 else 0 end) as matched,
                       sum(case when i.current_scan_id is not null and not (%s) then 1 else 0 end) as mismatched,
                       sum(case when i.current_scan_id is not null and img.id is null then 1 else 0 end) as accepted_without_image,
                       sum(case when i.current_scan_id is not null and
                                     (nullif(trim(i.notes), '') is not null or nullif(trim(s.notes), '') is not null)
                                then 1 else 0 end) as accepted_with_notes,
                       %s as total_expected_quantity,
                       %s as processed_expected_quantity,
                       %s as actual_quantity,
                       %s as shortage_quantity,
                       %s as overage_quantity,
                       %s as net_variance_quantity
                  from %s i
                  left join %s s on s.id = i.current_scan_id
                  left join uploaded_files img on img.id = s.scan_image_file_id and img.is_deleted = false
                 where i.task_id in (:taskIds)
                 group by i.task_id
                """.formatted(
                sql.matchedCondition(), sql.matchedCondition(),
                sql.totalQuantityExpression(), sql.processedQuantityExpression(),
                sql.actualQuantityExpression(), sql.shortageQuantityExpression(),
                sql.overageQuantityExpression(), sql.netVarianceExpression(),
                sql.itemTable(), sql.scanTable()
        );

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

    public Map<Long, TrackingResponses.EventMetrics> findEventMetrics(
            InventoryDomain domain,
            Collection<Long> taskIds
    ) {
        if (taskIds == null || taskIds.isEmpty()) return Collections.emptyMap();

        DomainSql sql = DomainSql.forDomain(domain);
        String query = """
                select s.task_id,
                       count(s.id) as total_events,
                       sum(case when s.event_type = 'FIRST_SCAN' then 1 else 0 end) as first_scans,
                       sum(case when s.event_type = 'DUPLICATE' then 1 else 0 end) as duplicates,
                       sum(case when s.event_type = 'CONFLICT' then 1 else 0 end) as conflicts,
                       sum(case when s.event_type = 'CORRECTION' then 1 else 0 end) as corrections,
                       sum(case when s.event_type = 'EXTRA' then 1 else 0 end) as extra_events,
                       sum(case when s.event_type = 'AMBIGUOUS' then 1 else 0 end) as ambiguous_events,
                       count(distinct case when s.event_type = 'EXTRA' then %s end) as unique_unexpected_codes,
                       min(s.scanned_at) as first_activity_at,
                       max(s.scanned_at) as last_activity_at
                  from %s s
                 where s.task_id in (:taskIds)
                 group by s.task_id
                """.formatted(sql.scannedCodeColumn(), sql.scanTable());

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

    public List<TrackingResponses.Area> findAreas(
            Long taskId,
            InventoryDomain domain,
            InventoryTaskStatus taskStatus
    ) {
        if (domain == InventoryDomain.VEHICLE) return findVehicleAreas(taskId, taskStatus);
        if (domain == InventoryDomain.ASSET) return findAssetAreas(taskId, taskStatus);
        return findSparePartAreas(taskId, taskStatus);
    }

    public List<TrackingResponses.TeamMember> findTeam(Long taskId, InventoryDomain domain) {
        DomainSql sql = DomainSql.forDomain(domain);
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
                        select %s as user_id
                          from %s
                         where task_id = :taskId
                  ) people on people.user_id = u.id
                  left join (
                        select user_id, max(case when active = true then 1 else 0 end) as currently_assigned
                          from inventory_task_assignments
                         where task_id = :taskId
                         group by user_id
                  ) a on a.user_id = u.id
                  left join (
                        select i.%s as user_id,
                               count(id) as accepted_count,
                               sum(case when %s then 1 else 0 end) as matched_count,
                               sum(case when current_scan_id is not null and not (%s) then 1 else 0 end) as mismatched_count
                          from %s i
                         where i.task_id = :taskId and i.%s is not null and i.current_scan_id is not null
                         group by i.%s
                  ) c on c.user_id = u.id
                  left join (
                        select %s as user_id,
                               count(id) as scan_events,
                               sum(case when event_type = 'FIRST_SCAN' then 1 else 0 end) as first_scans,
                               sum(case when event_type = 'DUPLICATE' then 1 else 0 end) as duplicates,
                               sum(case when event_type = 'CONFLICT' then 1 else 0 end) as conflicts,
                               sum(case when event_type = 'CORRECTION' then 1 else 0 end) as corrections,
                               sum(case when event_type = 'EXTRA' then 1 else 0 end) as extras,
                               min(scanned_at) as first_activity_at,
                               max(scanned_at) as last_activity_at
                          from %s
                         where task_id = :taskId
                         group by %s
                  ) e on e.user_id = u.id
                 order by e.last_activity_at desc, u.first_name asc, u.last_name asc
                """.formatted(
                sql.scanUserColumn(), sql.scanTable(),
                sql.acceptedByColumn(), sql.matchedCondition(), sql.matchedCondition(),
                sql.itemTable(), sql.acceptedByColumn(), sql.acceptedByColumn(),
                sql.scanUserColumn(), sql.scanTable(), sql.scanUserColumn()
        );

        Map<Long, List<String>> assignedAreas = findAssignedAreasByUser(taskId, domain);
        LocalDateTime now = LocalDateTime.now();
        return jdbc.query(query, new MapSqlParameterSource("taskId", taskId), (rs, rowNum) -> {
            LocalDateTime lastActivity = localDateTime(rs.getObject("last_activity_at"));
            Long inactiveSeconds = lastActivity == null ? null
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

    public Page<TrackingResponses.ResultItem> findResults(
            Long taskId,
            InventoryDomain domain,
            TrackingResultFilter filter,
            String search,
            Pageable pageable
    ) {
        ResultSql resultSql = ResultSql.forDomain(domain);
        String normalizedSearch = normalizeSearch(search);
        String where = " where i.task_id = :taskId "
                + resultSql.filterCondition(filter == null ? TrackingResultFilter.ALL : filter)
                + (normalizedSearch == null ? "" : resultSql.searchCondition());
        MapSqlParameterSource params = new MapSqlParameterSource("taskId", taskId)
                .addValue("search", normalizedSearch == null ? null : "%" + normalizedSearch.toUpperCase() + "%");

        long total = jdbc.queryForObject(
                "select count(i.id) " + resultSql.fromClause() + where,
                params,
                Long.class
        );

        String query = resultSql.selectClause() + resultSql.fromClause() + where
                + " order by i.id asc limit :limit offset :offset";
        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        List<TrackingResponses.ResultItem> content = jdbc.query(query, params,
                (rs, rowNum) -> new TrackingResponses.ResultItem(
                        rs.getLong("item_id"),
                        domain,
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

    public Page<TrackingResponses.ScanEvent> findScanEvents(
            Long taskId,
            InventoryDomain domain,
            InventoryScanEventType eventType,
            String search,
            Pageable pageable
    ) {
        ScanSql scanSql = ScanSql.forDomain(domain);
        String normalizedSearch = normalizeSearch(search);
        String where = " where s.task_id = :taskId "
                + (eventType == null ? "" : " and s.event_type = :eventType ")
                + (normalizedSearch == null ? "" : scanSql.searchCondition());
        MapSqlParameterSource params = new MapSqlParameterSource("taskId", taskId)
                .addValue("eventType", eventType == null ? null : eventType.name())
                .addValue("search", normalizedSearch == null ? null : "%" + normalizedSearch.toUpperCase() + "%");

        long total = jdbc.queryForObject(
                "select count(s.id) " + scanSql.fromClause() + where,
                params,
                Long.class
        );

        String query = scanSql.selectClause() + scanSql.fromClause() + where
                + " order by s.scanned_at desc, s.id desc limit :limit offset :offset";
        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        List<TrackingResponses.ScanEvent> content = jdbc.query(query, params,
                (rs, rowNum) -> {
                    Long scanId = rs.getLong("scan_id");
                    boolean hasImage = rs.getBoolean("has_image");
                    return new TrackingResponses.ScanEvent(
                            scanId,
                            domain,
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

    public TrackingResponses.ImageFile findImage(Long taskId, InventoryDomain domain, Long scanId) {
        DomainSql sql = DomainSql.forDomain(domain);
        String query = """
                select f.id, f.file_path, f.original_file_name, f.file_name, f.content_type, f.file_size
                  from %s s
                  join uploaded_files f on f.id = s.scan_image_file_id
                 where s.task_id = :taskId
                   and s.id = :scanId
                   and f.is_deleted = false
                """.formatted(sql.scanTable());
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

    private List<TrackingResponses.Area> findVehicleAreas(Long taskId, InventoryTaskStatus taskStatus) {
        String query = """
                select l.id as area_id, l.id as root_area_id, l.store_no as area_code,
                       coalesce(l.location_name, l.store_no) as area_name,
                       count(i.id) as planned,
                       sum(case when i.current_scan_id is not null then 1 else 0 end) as processed,
                       sum(case when i.current_scan_id is not null and i.status = 'FOUND' then 1 else 0 end) as matched,
                       sum(case when i.current_scan_id is not null and i.status <> 'FOUND' then 1 else 0 end) as mismatched,
                       coalesce(e.extras, 0) as extras,
                       coalesce(e.scan_events, 0) as scan_events,
                       e.last_activity_at
                  from vehicle_inventory_locations l
                  left join vehicle_inventory_items i
                    on i.task_id = l.task_id and i.store_no = l.store_no
                  left join (
                        select actual_location_id,
                               count(id) as scan_events,
                               sum(case when event_type = 'EXTRA' then 1 else 0 end) as extras,
                               max(scanned_at) as last_activity_at
                          from vehicle_inventory_scans
                         where task_id = :taskId
                         group by actual_location_id
                  ) e on e.actual_location_id = l.id
                 where l.task_id = :taskId
                 group by l.id, l.store_no, l.location_name, e.extras, e.scan_events, e.last_activity_at
                 order by l.store_no asc
                """;
        Map<Long, List<TrackingResponses.UserRef>> staff = findAssignedStaffByRootArea(taskId, InventoryDomain.VEHICLE);
        return jdbc.query(query, new MapSqlParameterSource("taskId", taskId), (rs, rowNum) ->
                areaFromRow(rs.getLong("area_id"), null, rs.getLong("root_area_id"),
                        TrackingAreaLevel.STORE, rs.getString("area_code"), rs.getString("area_name"),
                        rs.getLong("planned"), rs.getLong("processed"), rs.getLong("matched"),
                        rs.getLong("mismatched"), rs.getLong("extras"), rs.getLong("scan_events"),
                        localDateTime(rs.getObject("last_activity_at")), taskStatus,
                        staff.getOrDefault(rs.getLong("root_area_id"), List.of())));
    }

    private List<TrackingResponses.Area> findAssetAreas(Long taskId, InventoryTaskStatus taskStatus) {
        List<TrackingResponses.Area> result = new ArrayList<>();
        Map<Long, List<TrackingResponses.UserRef>> staff = findAssignedStaffByRootArea(taskId, InventoryDomain.ASSET);
        String template = """
                select %s as area_id, %s as parent_id, i.planned_location_id as root_area_id,
                       %s as area_code, %s as area_name,
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
                  left join (%s) e on e.area_id = %s
                 where i.task_id = :taskId
                 group by %s, %s, i.planned_location_id, %s, %s, e.extras, e.scan_events, e.last_activity_at
                 order by %s
                """;

        String eventLocation = assetEventAggregate("actual_location_id");
        String locationQuery = template.formatted(
                "l.id", "null", "l.location_name", "l.location_name", eventLocation, "l.id",
                "l.id", "null", "l.location_name", "l.location_name", "l.location_name");
        appendAreas(result, locationQuery, taskId, taskStatus, TrackingAreaLevel.LOCATION, null, staff);

        String eventFloor = assetEventAggregate("actual_floor_id");
        String floorQuery = template.formatted(
                "f.id", "l.id", "f.floor_name", "f.floor_name", eventFloor, "f.id",
                "f.id", "l.id", "f.floor_name", "f.floor_name", "l.location_name, f.floor_name");
        appendAreas(result, floorQuery, taskId, taskStatus, TrackingAreaLevel.FLOOR,
                TrackingAreaLevel.LOCATION, staff);

        String eventPlace = assetEventAggregate("actual_place_id");
        String placeQuery = template.formatted(
                "p.id", "f.id", "p.place_name", "p.place_name", eventPlace, "p.id",
                "p.id", "f.id", "p.place_name", "p.place_name", "l.location_name, f.floor_name, p.place_name");
        appendAreas(result, placeQuery, taskId, taskStatus, TrackingAreaLevel.PLACE,
                TrackingAreaLevel.FLOOR, staff);
        return result;
    }

    private List<TrackingResponses.Area> findSparePartAreas(Long taskId, InventoryTaskStatus taskStatus) {
        List<TrackingResponses.Area> result = new ArrayList<>();
        Map<Long, List<TrackingResponses.UserRef>> staff = findAssignedStaffByRootArea(taskId, InventoryDomain.SPARE_PART);
        String branchQuery = """
                select b.id as area_id, null as parent_id, b.id as root_area_id,
                       b.branch_name as area_code, b.branch_name as area_name,
                       count(i.id) as planned,
                       sum(case when i.current_scan_id is not null then 1 else 0 end) as processed,
                       sum(case when i.current_scan_id is not null and i.status = 'MATCHED' then 1 else 0 end) as matched,
                       sum(case when i.current_scan_id is not null and i.status <> 'MATCHED' then 1 else 0 end) as mismatched,
                       coalesce(e.extras, 0) as extras, coalesce(e.scan_events, 0) as scan_events, e.last_activity_at
                  from spare_part_inventory_branches b
                  left join spare_part_inventory_items i on i.planned_branch_id = b.id and i.task_id = b.task_id
                  left join (
                        select actual_branch_id as area_id, count(id) as scan_events,
                               sum(case when event_type = 'EXTRA' then 1 else 0 end) as extras,
                               max(scanned_at) as last_activity_at
                          from spare_part_inventory_scans where task_id = :taskId group by actual_branch_id
                  ) e on e.area_id = b.id
                 where b.task_id = :taskId
                 group by b.id, b.branch_name, e.extras, e.scan_events, e.last_activity_at
                 order by b.branch_name
                """;
        appendAreas(result, branchQuery, taskId, taskStatus, TrackingAreaLevel.BRANCH, null, staff);

        String locationQuery = """
                select l.id as area_id, b.id as parent_id, b.id as root_area_id,
                       l.location_code as area_code, l.location_code as area_name,
                       count(i.id) as planned,
                       sum(case when i.current_scan_id is not null then 1 else 0 end) as processed,
                       sum(case when i.current_scan_id is not null and i.status = 'MATCHED' then 1 else 0 end) as matched,
                       sum(case when i.current_scan_id is not null and i.status <> 'MATCHED' then 1 else 0 end) as mismatched,
                       coalesce(e.extras, 0) as extras, coalesce(e.scan_events, 0) as scan_events, e.last_activity_at
                  from spare_part_inventory_locations l
                  join spare_part_inventory_branches b on b.id = l.branch_id
                  left join spare_part_inventory_items i on i.planned_location_id = l.id and i.task_id = l.task_id
                  left join (
                        select actual_location_id as area_id, count(id) as scan_events,
                               sum(case when event_type = 'EXTRA' then 1 else 0 end) as extras,
                               max(scanned_at) as last_activity_at
                          from spare_part_inventory_scans where task_id = :taskId group by actual_location_id
                  ) e on e.area_id = l.id
                 where l.task_id = :taskId
                 group by l.id, b.id, b.branch_name, l.location_code, e.extras, e.scan_events, e.last_activity_at
                 order by b.branch_name, l.location_code
                """;
        appendAreas(result, locationQuery, taskId, taskStatus, TrackingAreaLevel.LOCATION,
                TrackingAreaLevel.BRANCH, staff);
        return result;
    }

    private String assetEventAggregate(String column) {
        return "select " + column + " as area_id, count(id) as scan_events, "
                + "sum(case when event_type = 'EXTRA' then 1 else 0 end) as extras, "
                + "max(scanned_at) as last_activity_at from asset_inventory_scans "
                + "where task_id = :taskId group by " + column;
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
            Long rootAreaId = rs.getLong("root_area_id");
            Long parentId = nullableLong(rs.getObject("parent_id"));
            String parentKey = parentId == null || parentLevel == null ? null : key(parentLevel, parentId);
            return areaFromRow(areaId, parentKey, rootAreaId, level,
                    rs.getString("area_code"), rs.getString("area_name"),
                    rs.getLong("planned"), rs.getLong("processed"), rs.getLong("matched"),
                    rs.getLong("mismatched"), rs.getLong("extras"), rs.getLong("scan_events"),
                    localDateTime(rs.getObject("last_activity_at")), taskStatus,
                    staff.getOrDefault(rootAreaId, List.of()));
        }));
    }

    private TrackingResponses.Area areaFromRow(
            Long areaId,
            String parentKey,
            Long rootAreaId,
            TrackingAreaLevel level,
            String code,
            String name,
            long planned,
            long processed,
            long matched,
            long mismatched,
            long extras,
            long scanEvents,
            LocalDateTime lastActivity,
            InventoryTaskStatus taskStatus,
            List<TrackingResponses.UserRef> staff
    ) {
        long remaining = Math.max(planned - processed, 0);
        return new TrackingResponses.Area(
                key(level, areaId), parentKey, areaId, rootAreaId, level, code, name,
                planned, processed, matched, mismatched, remaining, extras, scanEvents,
                percentage(processed, planned), areaStatus(planned, processed, scanEvents, lastActivity, taskStatus),
                lastActivity, staff
        );
    }

    private Map<Long, List<TrackingResponses.UserRef>> findAssignedStaffByRootArea(
            Long taskId,
            InventoryDomain domain
    ) {
        AssignmentSql sql = AssignmentSql.forDomain(domain);
        String query = """
                select x.%s as root_area_id, u.id as user_id,
                       u.first_name, u.last_name, u.username, u.mobile
                  from %s x
                  join inventory_task_assignments a on a.id = x.assignment_id
                  join users u on u.id = a.user_id
                 where a.task_id = :taskId and a.active = true and x.active = true
                 order by u.first_name, u.last_name
                """.formatted(sql.areaIdColumn(), sql.assignmentTable());
        Map<Long, List<TrackingResponses.UserRef>> result = new HashMap<>();
        jdbc.query(query, new MapSqlParameterSource("taskId", taskId), rs -> {
            long rootId = rs.getLong("root_area_id");
            TrackingResponses.UserRef user = userRef(rs.getLong("user_id"),
                    rs.getString("first_name"), rs.getString("last_name"),
                    rs.getString("username"), rs.getString("mobile"));
            List<TrackingResponses.UserRef> users = result.computeIfAbsent(rootId, ignored -> new ArrayList<>());
            if (users.stream().noneMatch(existing -> existing.id().equals(user.id()))) users.add(user);
        });
        return result;
    }

    private Map<Long, List<String>> findAssignedAreasByUser(Long taskId, InventoryDomain domain) {
        AssignmentSql sql = AssignmentSql.forDomain(domain);
        String query = """
                select a.user_id, %s as area_name
                  from %s x
                  join inventory_task_assignments a on a.id = x.assignment_id
                  join %s area on area.id = x.%s
                 where a.task_id = :taskId and a.active = true and x.active = true
                 order by area_name
                """.formatted(sql.areaNameExpression(), sql.assignmentTable(),
                sql.areaTable(), sql.areaIdColumn());
        Map<Long, Set<String>> collected = new LinkedHashMap<>();
        jdbc.query(query, new MapSqlParameterSource("taskId", taskId), rs -> {
            collected.computeIfAbsent(rs.getLong("user_id"), ignored -> new LinkedHashSet<>())
                    .add(rs.getString("area_name"));
        });
        Map<Long, List<String>> result = new HashMap<>();
        collected.forEach((userId, areas) -> result.put(userId, List.copyOf(areas)));
        return result;
    }

    private TrackingAreaStatus areaStatus(
            long planned,
            long processed,
            long scanEvents,
            LocalDateTime lastActivity,
            InventoryTaskStatus taskStatus
    ) {
        if (planned > 0 && processed >= planned) return TrackingAreaStatus.COMPLETED;
        if (processed == 0 && scanEvents == 0) return TrackingAreaStatus.NOT_STARTED;
        if (taskStatus == InventoryTaskStatus.IN_PROGRESS
                && lastActivity != null
                && lastActivity.isBefore(LocalDateTime.now().minusMinutes(STALLED_AFTER_MINUTES))) {
            return TrackingAreaStatus.STALLED;
        }
        return TrackingAreaStatus.ACTIVE;
    }

    private static String key(TrackingAreaLevel level, Long id) {
        return level.name() + ":" + id;
    }

    private static String imageUrl(Long taskId, Long scanId, boolean hasImage) {
        if (!hasImage || scanId == null) return null;
        return "/api/inventory/tracking/tasks/" + taskId + "/scan-events/" + scanId + "/image";
    }

    private static TrackingResponses.UserRef nullableUserRef(
            Object id,
            String firstName,
            String lastName,
            String username,
            String mobile
    ) {
        Long userId = nullableLong(id);
        return userId == null ? null : userRef(userId, firstName, lastName, username, mobile);
    }

    private static TrackingResponses.UserRef userRef(
            Long id,
            String firstName,
            String lastName,
            String username,
            String mobile
    ) {
        String name = ((firstName == null ? "" : firstName.trim()) + " "
                + (lastName == null ? "" : lastName.trim())).trim();
        if (name.isBlank()) name = username;
        return new TrackingResponses.UserRef(id, name, username, mobile);
    }

    private static int percentage(long value, long total) {
        if (total <= 0) return 0;
        return (int) Math.min(Math.round(value * 100.0 / total), 100);
    }

    private static String normalizeSearch(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static BigDecimal decimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static Long nullableLong(Object value) {
        if (value == null) return null;
        return ((Number) value).longValue();
    }

    private static LocalDateTime localDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime dateTime) return dateTime;
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime();
        throw new IllegalArgumentException("Unsupported date-time value: " + value.getClass().getName());
    }

    private record DomainSql(
            String itemTable,
            String scanTable,
            String matchedCondition,
            String scannedCodeColumn,
            String scanUserColumn,
            String acceptedByColumn,
            String totalQuantityExpression,
            String processedQuantityExpression,
            String actualQuantityExpression,
            String shortageQuantityExpression,
            String overageQuantityExpression,
            String netVarianceExpression
    ) {
        private static DomainSql forDomain(InventoryDomain domain) {
            if (domain == InventoryDomain.VEHICLE) {
                return new DomainSql(
                        "vehicle_inventory_items", "vehicle_inventory_scans", "i.status = 'FOUND'",
                        "s.scanned_vin", "user_id", "checked_by_user_id",
                        "0", "0", "0", "0", "0", "0"
                );
            }
            if (domain == InventoryDomain.ASSET) {
                return new DomainSql(
                        "asset_inventory_items", "asset_inventory_scans", "i.status = 'MATCHED'",
                        "s.scanned_barcode", "user_id", "checked_by_user_id",
                        "0", "0", "0", "0", "0", "0"
                );
            }
            return new DomainSql(
                    "spare_part_inventory_items", "spare_part_inventory_scans", "i.status = 'MATCHED'",
                    "s.scanned_item_no", "scanned_by_user_id", "counted_by_user_id",
                    "coalesce(sum(i.stock_qty), 0)",
                    "coalesce(sum(case when i.current_scan_id is not null then i.stock_qty else 0 end), 0)",
                    "coalesce(sum(case when i.current_scan_id is not null then i.actual_qty else 0 end), 0)",
                    "coalesce(sum(case when i.current_scan_id is not null and i.variance_qty < 0 then -i.variance_qty else 0 end), 0)",
                    "coalesce(sum(case when i.current_scan_id is not null and i.variance_qty > 0 then i.variance_qty else 0 end), 0)",
                    "coalesce(sum(case when i.current_scan_id is not null then i.variance_qty else 0 end), 0)"
            );
        }
    }

    private record AssignmentSql(
            String assignmentTable,
            String areaIdColumn,
            String areaTable,
            String areaNameExpression
    ) {
        private static AssignmentSql forDomain(InventoryDomain domain) {
            if (domain == InventoryDomain.VEHICLE) {
                return new AssignmentSql("vehicle_inventory_location_assignments", "location_id",
                        "vehicle_inventory_locations", "concat(area.store_no, ' - ', coalesce(area.location_name, area.store_no))");
            }
            if (domain == InventoryDomain.ASSET) {
                return new AssignmentSql("asset_inventory_location_assignments", "location_id",
                        "asset_inventory_locations", "area.location_name");
            }
            return new AssignmentSql("spare_part_inventory_branch_assignments", "branch_id",
                    "spare_part_inventory_branches", "area.branch_name");
        }
    }

    private record ResultSql(String selectClause, String fromClause, String codeExpression, String descriptionExpression,
                             String matchedExpression, String notesExpression) {
        private static ResultSql forDomain(InventoryDomain domain) {
            if (domain == InventoryDomain.VEHICLE) {
                return new ResultSql(
                        """
                        select i.id as item_id, i.vin_no as code, i.part_no as secondary_code,
                               trim(concat_ws(' ', i.make, i.model_name, i.model_year)) as description,
                               i.status as item_status, (i.current_scan_id is not null) as processed,
                               (i.current_scan_id is not null and i.status = 'FOUND') as matched,
                               i.store_no as expected_area, i.location as expected_sub_area, null as expected_leaf_area,
                               i.actual_store_no as actual_area, i.actual_location as actual_sub_area, null as actual_leaf_area,
                               null as expected_quantity, null as actual_quantity, null as variance_quantity,
                               u.id as user_id, u.first_name, u.last_name, u.username, u.mobile,
                               i.checked_at as accepted_at, s.id as scan_id, (img.id is not null) as has_image,
                               coalesce(nullif(s.notes, ''), i.notes) as notes
                        """,
                        """
                          from vehicle_inventory_items i
                          left join vehicle_inventory_scans s on s.id = i.current_scan_id
                          left join users u on u.id = i.checked_by_user_id
                          left join uploaded_files img on img.id = s.scan_image_file_id and img.is_deleted = false
                        """,
                        "i.vin_no", "concat_ws(' ', i.make, i.model_name, i.part_no)",
                        "i.status = 'FOUND'", "coalesce(nullif(s.notes, ''), i.notes)"
                );
            }
            if (domain == InventoryDomain.ASSET) {
                return new ResultSql(
                        """
                        select i.id as item_id, i.barcode as code, i.category_code as secondary_code,
                               i.description as description, i.status as item_status,
                               (i.current_scan_id is not null) as processed,
                               (i.current_scan_id is not null and i.status = 'MATCHED') as matched,
                               pl.location_name as expected_area, pf.floor_name as expected_sub_area, pp.place_name as expected_leaf_area,
                               al.location_name as actual_area, af.floor_name as actual_sub_area, ap.place_name as actual_leaf_area,
                               i.quantity as expected_quantity, i.quantity as actual_quantity, null as variance_quantity,
                               u.id as user_id, u.first_name, u.last_name, u.username, u.mobile,
                               i.checked_at as accepted_at, s.id as scan_id, (img.id is not null) as has_image,
                               coalesce(nullif(s.notes, ''), i.notes) as notes
                        """,
                        """
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
                        """,
                        "i.barcode", "concat_ws(' ', i.description, i.asset_category, i.asset_type)",
                        "i.status = 'MATCHED'", "coalesce(nullif(s.notes, ''), i.notes)"
                );
            }
            return new ResultSql(
                    """
                    select i.id as item_id, i.item_no as code, i.brand_name as secondary_code,
                           i.brand_name as description, i.status as item_status,
                           (i.current_scan_id is not null) as processed,
                           (i.current_scan_id is not null and i.status = 'MATCHED') as matched,
                           pb.branch_name as expected_area, pl.location_code as expected_sub_area, null as expected_leaf_area,
                           ab.branch_name as actual_area, al.location_code as actual_sub_area, null as actual_leaf_area,
                           i.stock_qty as expected_quantity, i.actual_qty as actual_quantity, i.variance_qty as variance_quantity,
                           u.id as user_id, u.first_name, u.last_name, u.username, u.mobile,
                           i.counted_at as accepted_at, s.id as scan_id, (img.id is not null) as has_image,
                           coalesce(nullif(s.notes, ''), i.notes) as notes
                    """,
                    """
                      from spare_part_inventory_items i
                      join spare_part_inventory_branches pb on pb.id = i.planned_branch_id
                      join spare_part_inventory_locations pl on pl.id = i.planned_location_id
                      left join spare_part_inventory_branches ab on ab.id = i.actual_branch_id
                      left join spare_part_inventory_locations al on al.id = i.actual_location_id
                      left join spare_part_inventory_scans s on s.id = i.current_scan_id
                      left join users u on u.id = i.counted_by_user_id
                      left join uploaded_files img on img.id = s.scan_image_file_id and img.is_deleted = false
                    """,
                    "i.item_no", "concat_ws(' ', i.item_no, i.brand_name)",
                    "i.status = 'MATCHED'", "coalesce(nullif(s.notes, ''), i.notes)"
            );
        }

        private String filterCondition(TrackingResultFilter filter) {
            return switch (filter) {
                case ALL -> "";
                case MATCHED -> " and i.current_scan_id is not null and " + matchedExpression;
                case MISMATCHED -> " and i.current_scan_id is not null and not (" + matchedExpression + ")";
                case REMAINING -> " and i.current_scan_id is null";
                case MISSING_IMAGE -> " and i.current_scan_id is not null and img.id is null";
                case NOTES -> " and nullif(trim(" + notesExpression + "), '') is not null";
            };
        }

        private String searchCondition() {
            return " and (upper(" + codeExpression + ") like :search or upper(" + descriptionExpression + ") like :search)";
        }
    }

    private record ScanSql(String selectClause, String fromClause, String codeExpression) {
        private static ScanSql forDomain(InventoryDomain domain) {
            if (domain == InventoryDomain.VEHICLE) {
                return new ScanSql(
                        """
                        select s.id as scan_id, s.scanned_vin as scanned_code, s.event_type, s.scan_result,
                               s.item_id, s.expected_store_no as expected_area, s.expected_location as expected_sub_area,
                               null as expected_leaf_area, s.actual_store_no as actual_area,
                               s.actual_location as actual_sub_area, null as actual_leaf_area,
                               null as expected_quantity, null as actual_quantity, null as variance_quantity,
                               u.id as user_id, u.first_name, u.last_name, u.username, u.mobile,
                               s.scanned_at, s.device_scanned_at, s.device_id, s.symbology,
                               (img.id is not null) as has_image, s.notes, null as details
                        """,
                        """
                          from vehicle_inventory_scans s
                          join users u on u.id = s.user_id
                          left join uploaded_files img on img.id = s.scan_image_file_id and img.is_deleted = false
                        """,
                        "s.scanned_vin"
                );
            }
            if (domain == InventoryDomain.ASSET) {
                return new ScanSql(
                        """
                        select s.id as scan_id, s.scanned_barcode as scanned_code, s.event_type, s.scan_result,
                               s.item_id, el.location_name as expected_area, ef.floor_name as expected_sub_area,
                               ep.place_name as expected_leaf_area, al.location_name as actual_area,
                               af.floor_name as actual_sub_area, ap.place_name as actual_leaf_area,
                               null as expected_quantity, null as actual_quantity, null as variance_quantity,
                               u.id as user_id, u.first_name, u.last_name, u.username, u.mobile,
                               s.scanned_at, s.device_scanned_at, s.device_id, s.symbology,
                               (img.id is not null) as has_image, s.notes, s.mismatch_fields as details
                        """,
                        """
                          from asset_inventory_scans s
                          join users u on u.id = s.user_id
                          left join asset_inventory_locations el on el.id = s.expected_location_id
                          left join asset_inventory_floors ef on ef.id = s.expected_floor_id
                          left join asset_inventory_places ep on ep.id = s.expected_place_id
                          left join asset_inventory_locations al on al.id = s.actual_location_id
                          left join asset_inventory_floors af on af.id = s.actual_floor_id
                          left join asset_inventory_places ap on ap.id = s.actual_place_id
                          left join uploaded_files img on img.id = s.scan_image_file_id and img.is_deleted = false
                        """,
                        "s.scanned_barcode"
                );
            }
            return new ScanSql(
                    """
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
                    """,
                    """
                      from spare_part_inventory_scans s
                      join users u on u.id = s.scanned_by_user_id
                      left join spare_part_inventory_branches eb on eb.id = s.expected_branch_id
                      left join spare_part_inventory_locations el on el.id = s.expected_location_id
                      left join spare_part_inventory_branches ab on ab.id = s.actual_branch_id
                      left join spare_part_inventory_locations al on al.id = s.actual_location_id
                      left join uploaded_files img on img.id = s.scan_image_file_id and img.is_deleted = false
                    """,
                    "s.scanned_item_no"
            );
        }

        private String searchCondition() {
            return " and upper(" + codeExpression + ") like :search";
        }
    }
}

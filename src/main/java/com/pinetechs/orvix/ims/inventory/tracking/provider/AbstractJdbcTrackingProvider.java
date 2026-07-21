package com.pinetechs.orvix.ims.inventory.tracking.provider;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryScanEventType;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.tracking.dto.TrackingResponses;
import com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingResultFilter;
import com.pinetechs.orvix.ims.inventory.tracking.repository.TrackingJdbcRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

abstract class AbstractJdbcTrackingProvider implements InventoryTrackingProvider {

    private final InventoryDomain domain;
    private final TrackingJdbcRepository repository;

    protected AbstractJdbcTrackingProvider(InventoryDomain domain, TrackingJdbcRepository repository) {
        this.domain = domain;
        this.repository = repository;
    }

    @Override
    public InventoryDomain domain() {
        return domain;
    }

    @Override
    public Map<Long, TrackingResponses.CurrentMetrics> currentMetrics(Collection<Long> taskIds) {
        return repository.findCurrentMetrics(domain, taskIds);
    }

    @Override
    public Map<Long, TrackingResponses.EventMetrics> eventMetrics(Collection<Long> taskIds) {
        return repository.findEventMetrics(domain, taskIds);
    }

    @Override
    public List<TrackingResponses.Area> areas(Long taskId, InventoryTaskStatus taskStatus) {
        return repository.findAreas(taskId, domain, taskStatus);
    }

    @Override
    public List<TrackingResponses.TeamMember> team(Long taskId) {
        return repository.findTeam(taskId, domain);
    }

    @Override
    public Page<TrackingResponses.ResultItem> results(
            Long taskId,
            TrackingResultFilter filter,
            String search,
            Pageable pageable
    ) {
        return repository.findResults(taskId, domain, filter, search, pageable);
    }

    @Override
    public Page<TrackingResponses.ScanEvent> scanEvents(
            Long taskId,
            InventoryScanEventType eventType,
            String search,
            Pageable pageable
    ) {
        return repository.findScanEvents(taskId, domain, eventType, search, pageable);
    }

    @Override
    public TrackingResponses.ImageFile image(Long taskId, Long scanId) {
        return repository.findImage(taskId, domain, scanId);
    }
}

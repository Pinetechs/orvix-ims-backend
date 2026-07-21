package com.pinetechs.orvix.ims.inventory.tracking.provider;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryScanEventType;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.tracking.dto.TrackingResponses;
import com.pinetechs.orvix.ims.inventory.tracking.enums.TrackingResultFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface InventoryTrackingProvider {

    InventoryDomain domain();

    Map<Long, TrackingResponses.CurrentMetrics> currentMetrics(Collection<Long> taskIds);

    Map<Long, TrackingResponses.EventMetrics> eventMetrics(Collection<Long> taskIds);

    List<TrackingResponses.Area> areas(Long taskId, InventoryTaskStatus taskStatus);

    List<TrackingResponses.TeamMember> team(Long taskId);

    Page<TrackingResponses.ResultItem> results(
            Long taskId,
            TrackingResultFilter filter,
            String search,
            Pageable pageable
    );

    Page<TrackingResponses.ScanEvent> scanEvents(
            Long taskId,
            InventoryScanEventType eventType,
            String search,
            Pageable pageable
    );

    TrackingResponses.ImageFile image(Long taskId, Long scanId);
}

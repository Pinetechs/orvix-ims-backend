package com.pinetechs.orvix.ims.inventory.tracking.service;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.tracking.dto.TrackingResponses;
import com.pinetechs.orvix.ims.inventory.tracking.dto.TrackingSnapshot;
import com.pinetechs.orvix.ims.inventory.tracking.provider.InventoryTrackingProvider;
import com.pinetechs.orvix.ims.inventory.tracking.provider.InventoryTrackingProviderRegistry;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TrackingSnapshotService {

    private final InventoryTrackingProviderRegistry providerRegistry;

    public TrackingSnapshotService(InventoryTrackingProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    public TrackingSnapshot load(InventoryTask task) {
        return load(List.of(task)).get(task.getId());
    }

    public Map<Long, TrackingSnapshot> load(List<InventoryTask> tasks) {
        Map<Long, TrackingSnapshot> snapshots = new LinkedHashMap<>();
        if (tasks == null || tasks.isEmpty()) {
            return snapshots;
        }

        Map<InventoryDomain, List<InventoryTask>> tasksByDomain = tasks.stream()
                .collect(Collectors.groupingBy(
                        InventoryTask::getInventoryDomain,
                        () -> new EnumMap<>(InventoryDomain.class),
                        Collectors.toList()
                ));






        tasksByDomain.forEach((domain, domainTasks) -> loadDomainSnapshots(domain, domainTasks, snapshots));


        return snapshots;
    }

    private void loadDomainSnapshots(InventoryDomain domain, List<InventoryTask> tasks, Map<Long, TrackingSnapshot> target) {
        List<Long> taskIds = tasks.stream().map(InventoryTask::getId).toList();

        InventoryTrackingProvider provider = providerRegistry.get(domain);

        Map<Long, TrackingResponses.CurrentMetrics> currentMetrics = provider.currentMetrics(taskIds);
        Map<Long, TrackingResponses.EventMetrics> eventMetrics = provider.eventMetrics(taskIds);


        for (InventoryTask task : tasks) {
            target.put(task.getId(), new TrackingSnapshot(
                    currentMetrics.getOrDefault(
                            task.getId(),
                            TrackingResponses.CurrentMetrics.empty()
                    ),
                    eventMetrics.getOrDefault(
                            task.getId(),
                            TrackingResponses.EventMetrics.empty()
                    )
            ));
        }
    }
}

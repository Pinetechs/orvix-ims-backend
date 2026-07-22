package com.pinetechs.orvix.ims.inventory.tracking.dto;

public record TrackingSnapshot(
        TrackingResponses.CurrentMetrics current,
        TrackingResponses.EventMetrics events
) {
}

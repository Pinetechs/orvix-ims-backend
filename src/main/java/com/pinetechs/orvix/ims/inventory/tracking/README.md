# Inventory Tracking

The tracking module is split by responsibility so changes stay local and predictable.

## Request flow

1. `InventoryTaskTrackingController` exposes the tracking endpoints.
2. `InventoryTrackingService` checks task access and coordinates the response.
3. `InventoryTrackingProviderRegistry` selects the provider for the task domain.
4. The selected provider reads domain-specific data with JDBC:
   - `VehicleTrackingProvider`
   - `AssetTrackingProvider`
   - `SparePartTrackingProvider`
5. Policies calculate business decisions that are shared by all domains:
   - `TrackingStatusPolicy`: stalled tasks, area status, duplicate-rate warnings, leaf areas.
   - `TrackingActionPolicy`: actions allowed for the current user and task status.
6. Supporting services handle focused use cases:
   - `TrackingSnapshotService`: loads current and event metrics in batches.
   - `TrackingAttentionService`: builds supervisor attention summaries and items.
   - `TrackingDurationService`: calculates active working time.

## Where to make changes

- Vehicle SQL or vehicle tracking fields: `VehicleTrackingProvider`
- Asset SQL or area hierarchy: `AssetTrackingProvider`
- Spare-part quantities or locations: `SparePartTrackingProvider`
- Stalled/duplicate/ready-for-review rules: `TrackingStatusPolicy`
- Permissions and available buttons: `TrackingActionPolicy`
- Attention list content or limits: `TrackingAttentionService`
- API response coordination: `InventoryTrackingService`

`TrackingResponses` remains unchanged because it is the public API contract used by the frontend.

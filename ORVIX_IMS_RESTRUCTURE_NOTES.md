# Orvix IMS Restructure Notes

## Removed from active code path

- Legacy Namaa controllers
- Legacy `Roles` entity
- Legacy `ERole` enum
- Legacy `Entity.User`
- Legacy helper-based current user access
- Legacy stock movement entities that represented classic stock management rather than inventory audit

## Adopted design

The current project scope is inventory audit/verification, not traditional stock movement management.
Therefore the backend is centered around:

- `Company`
- `User`
- `InventoryDomain`
- `PermissionCode`
- `InventoryTask`

## User rules

- `SYSTEM_ADMIN`: Web only, no company, manages all companies and users.
- `COMPANY_ADMIN`: Web only, linked to one company, sees all inventory domains within company.
- `SUPERVISOR`: Web only, linked to one company, limited by inventory domains/screens.
- `INVENTORY_STAFF`: Mobile only, linked to one company, limited by assigned domains and tasks.

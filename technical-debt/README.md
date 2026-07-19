# Technical Debt

This folder tracks known technical debt items so they are not forgotten.

Each item lives in its own Markdown file and follows a consistent structure:

- A `#` title describing the problem.
- `**Priority:**` and `**Affected areas:**` metadata.
- A `## Problem` section describing the issue and its impact.
- A `## Proposed resolution` section outlining the fix.
- Optional `## Notes` and `## Verification` sections.

## Current items

- [`integration-repository-error-swallowing.md`](integration-repository-error-swallowing.md) — `IntegrationRepository` swallows connection errors and returns empty lists.
- [`server-data-loading.md`](server-data-loading.md) — Duplicated server-data loading logic across three feature areas.
- [`entity-id-prefix-cleanup.md`](entity-id-prefix-cleanup.md) — Move shortcut raw-path encoding out of the common shortcut model.
- [`shortcuts-v2-pinned-no-removal.md`](shortcuts-v2-pinned-no-removal.md) — Home shortcuts can be disabled but not deleted.
- [`entity-picker-loading-flicker.md`](entity-picker-loading-flicker.md) — `EntityPicker` briefly shows "Add entity" before its entities finish mapping.
- [`entity-picker-load-failure-retry.md`](entity-picker-load-failure-retry.md) — Entity catalog load failure has no one-tap retry.
- [`shortcut-editor-destination-caching.md`](shortcut-editor-destination-caching.md) — Editor loses in-progress dashboard/entity destination when toggling type or switching server; caching scope undecided.
- [`shortcuts-list-partial-retry-on-error.md`](shortcuts-list-partial-retry-on-error.md) — Shortcuts list cannot show partial section errors or retry only one shortcut source.

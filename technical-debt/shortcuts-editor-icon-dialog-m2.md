# Shortcuts V2: editor reuses the legacy Material 2 icon picker

**Priority:** Low
**Affected areas:** Shortcuts v2 editor screen, shared `IconDialog` component

## Problem

`ShortcutEditorScreen` launches the shared `IconDialog` (`util/icondialog/`) to let users
pick a Material Dynamic Community icon for a shortcut. That component is implemented with
Material 2 (`androidx.compose.material.MaterialTheme` / `Surface`) wrapped by
`HomeAssistantAppTheme`, while the shortcuts editor screen is otherwise Material 3 / HA-themed
under `HATheme`. The result is a visual mismatch: the icon picker renders with Material 2
shapes/colors instead of the HA/Material 3 design system used by the rest of the screen.

`IconDialog` is a shared component with several call sites (QSTiles, ButtonWidget, the legacy
shortcuts view, and the new shortcuts editor), so migrating it is a cross-feature change rather
than a shortcuts-only fix.

## Accepted trade-off

The picker still renders under `HomeAssistantAppTheme`, so its colors are Home Assistant themed —
only the Material level (2 vs 3) differs. The mismatch is accepted as temporary tech debt so the
Shortcuts V2 editor can ship without touching unrelated icon-picker call sites.

## Proposed resolution

Migrate `IconDialog` (and `IconDialogSearch` / `IconDialogGrid`) from Material 2 to HA/Material 3
in a standalone preceding PR. This also fixes the same Material 2 debt for every other
`IconDialog` caller (QSTiles, ButtonWidget, legacy shortcuts) in one change, and should be paired
with re-validation of those call sites' screenshots.

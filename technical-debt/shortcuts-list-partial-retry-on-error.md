# Shortcuts list has no per-section retry

**Priority:** Low
**Affected areas:** `LoadShortcutsUseCase`, `ManageShortcutsViewModel`, shortcuts list UI

## Problem

The shortcuts list loads app shortcuts and home shortcuts from separate data
sources. If one section fails, the UI has no way to represent "app shortcuts
loaded, but home shortcuts failed" or to retry only the failed section.

For now, `LoadShortcutsUseCase` treats a home shortcut load failure as a full
screen error, matching the app shortcut failure path. This avoids showing a
partial list where a launcher/API failure looks identical to "there are no home
shortcuts."

## Proposed resolution

Add section-level load state to the shortcuts list so each shortcut section can
render independently:

- app shortcuts loading/error/ready
- home shortcuts loading/error/ready
- retry action scoped to the failed section

Once that exists, `LoadShortcutsUseCase` can return partial data plus a
section-level error instead of failing the whole screen when only one shortcut
source fails.

## Notes

- This is intentionally deferred to keep the initial shortcuts list PR simple.
- The current full-screen error is conservative: it prevents hidden home
  shortcuts when `ShortcutManagerCompat` fails to return pinned shortcuts.

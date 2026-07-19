# Clarify shortcut editor in-progress destination caching scope

**Affected areas:** `:app` shortcut editor (`EditorState`, `ShortcutEditorForm`, `ShortcutEditorViewModel`), `settings/shortcuts/ShortcutsUiState.kt`, `settings/shortcuts/views/components/ShortcutEditorForm.kt`
**Status:** Open (needs product/UX decision)

## Problem

The shortcut editor keeps only the **single** `ShortcutDraft.destination` as the source of truth.
When the user toggles the destination type (Dashboard ↔ Entity) or switches the destination server, the
previous in-progress value is lost:

- Tapping the Entity radio while a Dashboard path is filled emits an empty `Entity("")`, discarding
  the typed dashboard path (the form calls `onDraftChange(draft.copy(destination = ...))`, which
  overwrites `draft.destination`). Tapping back to Dashboard then shows an empty field.
- Switching the destination server keeps the active destination only when the new server supports it; an
  entity destination is dropped to an empty dashboard on a dashboard-only server. There is no retention
  of what the user had typed for each server.

The caching scope requirements still need to be clarified:

- **Per server only** — keep each server's last destination, but still lose the other *type* within a
  server when toggling.
- **Per destination type only** — keep the dashboard path and entity id independently (so toggling
  the type preserves both), but still lose content when switching servers.
- **Both (per server AND per type)** — keep a `(serverId, type) → value` matrix. Most robust, but
  the largest change.


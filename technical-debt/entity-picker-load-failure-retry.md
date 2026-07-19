# Entity catalog load failure has no one-tap retry

**Priority:** Low
**Affected areas:** `EntityPicker` (`app/src/main/kotlin/io/homeassistant/companion/android/util/compose/entity/EntityPicker.kt`), shortcut editor (`ShortcutEditorViewModel`, `ShortcutEditorForm`)

## Problem

When loading a server's entities fails, the shortcut editor shows a snackbar
("Unable to load entities") and the picker falls back to its empty state. But
there is no direct way to try again.

Today the only ways to retry are:

- Switch to another server and back (not possible on a single-server setup), or
- Close and reopen the editor.

There is no "Retry" button, so recovery is not obvious to the user.

## Proposed resolution

Give the entity picker a proper error state with a "Retry" button, shown in place
of the picker when its entities failed to load. Tapping it re-triggers the load.

This keeps the retry next to where the problem is visible, and works even on
single-server setups.

## Notes

- The editor already surfaces the failure via a snackbar and resets its loading
  spinner; this item is only about adding an easy retry action.
- We intentionally did not add retry for now. It first needs clarifying whether it
  makes sense to update the shared `EntityPicker` component to support a retry
  state, since it is used in other places too.

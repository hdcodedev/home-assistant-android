# EntityPicker briefly shows "Add entity" before its entities render

**Priority:** Low
**Affected areas:** `EntityPicker` (`app/src/main/kotlin/io/homeassistant/companion/android/util/compose/entity/EntityPicker.kt`)

## Problem

When `EntityPicker` receives its entities, it does a quick background step to
prepare them for display. During that split second the list is still empty, so
the picker briefly shows the "Add entity" button (or an empty state) before the
real content appears.

It's most noticeable when editing an existing entity shortcut: the selected
entity flashes as "Add entity" for a moment before showing the correct chip.

Note: this is separate from the shortcut editor's own loader. The editor already
shows a spinner while the entity catalog is being fetched. This flicker happens
*after* that, inside the picker itself.

## Proposed resolution

Don't show the "empty / add" state while the picker is still preparing its
entities. Simplest option: show the existing `HALoading` spinner during that
brief prep step, then reveal the entities.

## Notes

- The data is passed to the picker correctly — this is purely about how the
  first frame looks, not missing data.

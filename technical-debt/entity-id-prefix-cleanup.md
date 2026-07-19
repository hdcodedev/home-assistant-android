# Move shortcut raw-path encoding out of the common model

**Priority:** Low
**Affected areas:** `:common` shortcut entities, `:app` frontend navigation, legacy shortcut screen
**Status:** Done (core). Legacy migration deferred.

## Resolution

`ENTITY_ID_PREFIX`, `fromPath()`, and `toPath()` were removed from `:common`'s
`ShortcutDestination`, so the domain model no longer knows the transport format. The `entityId:`
literal now lives only in `FrontendTarget` (`:app`). Two `:app` converters in
`settings/shortcuts/di/ShortcutFrontendMapping.kt` bridge the boundary:
`ShortcutDestination.toFrontendTarget()` and `FrontendTarget.toShortcutDestination()`. The v2 factory
encodes via `destination.toFrontendTarget().toRawPath()`; the intent serializer decodes via
`FrontendTarget.fromRawPath(path).toShortcutDestination()`.

## Remaining (deferred — touches legacy shortcut code)

- `HaShortcutManager.buildShortcutInfo(...)` still takes a raw `path: String`. Could take a
  `FrontendTarget` and persist `SHORTCUT_EXTRA_PATH` via `target.toRawPath().orEmpty()`. Blocked by
  the legacy caller `legacy/ManageShortcutsViewModel.kt:174` which passes raw path strings.
- Legacy screen uses stringly-typed state and an inline `path.startsWith("entityId:")`
  (`legacy/ManageShortcutsViewModel.kt:245`) plus a `... is FrontendTarget.EntityMoreInfo` subtype
  check (`legacy/views/ManageShortcutsView.kt:280`). Prefer deriving a `ShortcutDestination`/
  `ShortcutType` once instead.

## Notes

- `ShortcutDestination` (`:common`) and `FrontendTarget` (`:app`) stay separate on purpose: domain/UI
  model vs. transport type, and `:common` cannot depend on `:app`. Don't collapse them.
- Keep invalid-entity behavior unchanged: an `entityId:` raw path still decodes as an entity, with
  validity enforced separately by `ShortcutDestination.isValid`.

## Verification

- `./gradlew :app:ktlintCheck :common:ktlintCheck`
- `./gradlew :common:testDebugUnitTest :app:testFullDebugUnitTest --tests "*shortcuts*" --tests "*FrontendTarget*"`

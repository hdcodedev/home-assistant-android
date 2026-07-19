package io.homeassistant.companion.android.settings.shortcuts

import io.homeassistant.companion.android.frontend.navigation.FrontendTarget
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDestination

/**
 * Converts the shortcut domain destination into the [FrontendTarget] used by the Android shortcut
 * intent and deep-link boundary. The raw-path wire format lives in [FrontendTarget], so the
 * shortcut domain model in `:common` stays free of transport details.
 */
internal fun ShortcutDestination.toFrontendTarget(): FrontendTarget = when (this) {
    is ShortcutDestination.Dashboard -> FrontendTarget.Path(path)
    is ShortcutDestination.Entity -> FrontendTarget.EntityMoreInfo(entityId)
}

/**
 * Converts a [FrontendTarget] back into the shortcut domain destination.
 *
 * [FrontendTarget.Default] has no meaningful shortcut destination, so it maps to an empty dashboard
 * path. Prefix-based entity discrimination is preserved so that a malformed entity id stays typed
 * as a [ShortcutDestination.Entity]; validity is enforced separately by [ShortcutDestination.isValid].
 */
internal fun FrontendTarget.toShortcutDestination(): ShortcutDestination = when (this) {
    FrontendTarget.Default -> ShortcutDestination.Dashboard("")
    is FrontendTarget.Path -> ShortcutDestination.Dashboard(path)
    is FrontendTarget.EntityMoreInfo -> ShortcutDestination.Entity(entityId)
}

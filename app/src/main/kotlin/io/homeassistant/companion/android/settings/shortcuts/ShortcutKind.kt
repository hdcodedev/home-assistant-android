package io.homeassistant.companion.android.settings.shortcuts

import kotlinx.serialization.Serializable

/**
 * Distinguishes the two kinds of launcher shortcut the feature manages, selecting which repository
 * a modification is routed to.
 *
 * - [APP]: dynamic shortcuts the app controls directly (created, updated, and deleted in place).
 * - [HOME]: shortcuts the user pins to the home screen; they require launcher pin support and are
 *   disabled rather than deleted.
 *
 * [Serializable] is required because [ShortcutKind] is used as a navigation route argument.
 */
@Serializable
internal enum class ShortcutKind { APP, HOME }

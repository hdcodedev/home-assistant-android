package io.homeassistant.companion.android

/**
 * Central configuration for work-in-progress features.
 *
 * This file provides a single location to manage feature flags for features that are under active
 * development but not yet ready for production release. By centralizing these flags, we can:
 * - Easily enable/disable features for testing
 * - Maintain visibility of all in-progress work
 * - Easily find all the places that needs to be updated when we want to finalize the feature.
 *
 * Feature flags should be removed from this file once the feature is fully released and stable.
 */
object WIPFeature {
    /**
     * Default value for [USE_SHORTCUTS_V2], used when no runtime override is set.
     *
     * The new shortcuts v2 implementation is only enabled in DEBUG builds during development.
     */
    private val defaultUseShortcutsV2 = BuildConfig.DEBUG

    /**
     * Runtime override for [USE_SHORTCUTS_V2], set to `true`/`false` to force a specific
     * implementation, or `null` to fall back to [defaultUseShortcutsV2].
     *
     * This is intended for local development so legacy and v2 shortcuts can be tested
     * interchangeably within the same running app. It must stay out of any release build path
     * and is never persisted.
     */
    @Volatile
    var shortcutsV2Override: Boolean? = null

    /**
     * Enables the new shortcuts v2 implementation.
     *
     * When true, the settings entry navigates to [io.homeassistant.companion.android.settings.shortcuts.ManageShortcutsSettingsFragment].
     * When false, the settings entry navigates to [io.homeassistant.companion.android.settings.shortcuts.legacy.ManageShortcutsSettingsFragment].
     *
     * A non-null [shortcutsV2Override] takes precedence over the DEBUG default, allowing the
     * active implementation to be switched at runtime during development.
     */
    val USE_SHORTCUTS_V2: Boolean
        get() = shortcutsV2Override ?: defaultUseShortcutsV2
}

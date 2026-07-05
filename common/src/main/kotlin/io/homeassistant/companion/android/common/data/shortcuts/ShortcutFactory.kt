package io.homeassistant.companion.android.common.data.shortcuts

import androidx.core.content.pm.ShortcutInfoCompat
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDraft

/**
 * Creates Android [ShortcutInfoCompat] instances from [ShortcutDraft] data.
 * Implementations define the target activity and intent construction (e.g. WebView).
 */
interface ShortcutFactory {
    fun createShortcutInfo(draft: ShortcutDraft): ShortcutInfoCompat
}

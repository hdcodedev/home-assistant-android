package io.homeassistant.companion.android.common.data.shortcuts

import android.content.Intent
import android.os.Bundle
import androidx.core.content.pm.ShortcutInfoCompat
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDestination
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDraft

/**
 * Encodes and decodes shortcut intent extras (destination, icon) to/from Android [Bundle]/[Intent].
 * Also converts platform [ShortcutInfoCompat] back to [ShortcutDraft] for editing.
 */
interface ShortcutIntentCodec {
    fun parseIcon(extras: Bundle?, iconIdToName: Map<Int, String>): String?
    fun parseDestination(path: String): ShortcutDestination
    fun encodeDestination(shortcutDestination: ShortcutDestination): String
    fun applyShortcutExtras(intent: Intent, iconName: String?)
    fun toDraft(shortcut: ShortcutInfoCompat, defaultServerId: Int, iconIdToName: Map<Int, String>): ShortcutDraft
}

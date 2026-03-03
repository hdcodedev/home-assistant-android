package io.homeassistant.companion.android.common.data.shortcuts

import android.content.Intent
import android.os.Bundle
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDestination

interface ShortcutIntentCodec {
    fun parseIcon(extras: Bundle?, iconIdToName: Map<Int, String>): String?
    fun parseDestination(extras: Bundle?, path: String): ShortcutDestination
    fun encodeDestination(shortcutDestination: ShortcutDestination): String
    fun applyShortcutExtras(intent: Intent, shortcutDestination: ShortcutDestination, path: String, iconName: String?)
}

package io.homeassistant.companion.android.common.data.shortcuts.impl

import android.content.Intent
import android.os.Bundle
import io.homeassistant.companion.android.common.data.shortcuts.ShortcutIntentCodec
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDestination
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ShortcutIntentCodecImpl @Inject constructor() : ShortcutIntentCodec {
    override fun parseIcon(extras: Bundle?, iconIdToName: Map<Int, String>): String? {
        val bundle = extras ?: return null

        return when {
            bundle.containsKey(EXTRA_ICON_NAME) -> {
                val iconName = bundle.getString(EXTRA_ICON_NAME) ?: return null
                if (iconName.startsWith(MDI_PREFIX)) iconName else "$MDI_PREFIX$iconName"
            }

            bundle.containsKey(EXTRA_ICON_ID) -> {
                val iconId = bundle.getInt(EXTRA_ICON_ID)
                if (iconId == 0) return null
                val iconName = iconIdToName[iconId] ?: return null
                if (iconName.startsWith(MDI_PREFIX)) iconName else "$MDI_PREFIX$iconName"
            }

            else -> null
        }
    }

    override fun parseDestination(extras: Bundle?, path: String): ShortcutDestination {
        return when (parseShortcutType(extras, path)) {
            ShortcutType.LOVELACE -> {
                ShortcutDestination.Lovelace(path.takeUnless { it.startsWith(ENTITY_PREFIX) }.orEmpty())
            }

            ShortcutType.ENTITY_ID -> ShortcutDestination.Entity(path.removePrefix(ENTITY_PREFIX))
        }
    }

    override fun encodeDestination(shortcutDestination: ShortcutDestination): String {
        return when (shortcutDestination) {
            is ShortcutDestination.Lovelace -> shortcutDestination.path
            is ShortcutDestination.Entity -> "$ENTITY_PREFIX${shortcutDestination.entityId}"
        }
    }

    override fun applyShortcutExtras(intent: Intent, shortcutDestination: ShortcutDestination, path: String, iconName: String?) {
        intent.putExtra(EXTRA_SHORTCUT_PATH, path)
        iconName?.let { intent.putExtra(EXTRA_ICON_NAME, it) }
        val type = when (shortcutDestination) {
            is ShortcutDestination.Lovelace -> ShortcutType.LOVELACE
            is ShortcutDestination.Entity -> ShortcutType.ENTITY_ID
        }
        intent.putExtra(EXTRA_TYPE, type.name)
    }

    private fun parseShortcutType(extras: Bundle?, path: String): ShortcutType {
        val raw = extras?.getString(EXTRA_TYPE)
        val fromExtra = raw?.let { ShortcutType.entries.firstOrNull { e -> e.name == it } }
        return fromExtra ?: if (path.startsWith(ENTITY_PREFIX)) ShortcutType.ENTITY_ID else ShortcutType.LOVELACE
    }

    internal companion object {
        private const val ENTITY_PREFIX = "entityId:"
        private const val MDI_PREFIX = "mdi:"
        private const val EXTRA_ICON_ID = "iconId"
        private const val EXTRA_ICON_NAME = "iconName"
        internal const val EXTRA_SHORTCUT_PATH = "shortcutPath"
        private const val EXTRA_TYPE = "type"
    }
}

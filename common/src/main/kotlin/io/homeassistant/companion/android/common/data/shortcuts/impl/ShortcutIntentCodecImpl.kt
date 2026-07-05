package io.homeassistant.companion.android.common.data.shortcuts.impl

import android.content.Intent
import android.os.Bundle
import androidx.core.content.pm.ShortcutInfoCompat
import io.homeassistant.companion.android.common.data.shortcuts.ShortcutIntentCodec
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDestination
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDraft
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

    override fun parseDestination(path: String): ShortcutDestination {
        return if (path.startsWith(ENTITY_PREFIX)) {
            ShortcutDestination.Entity(path.removePrefix(ENTITY_PREFIX))
        } else {
            ShortcutDestination.Dashboard(path)
        }
    }

    override fun encodeDestination(shortcutDestination: ShortcutDestination): String {
        return when (shortcutDestination) {
            is ShortcutDestination.Dashboard -> shortcutDestination.path
            is ShortcutDestination.Entity -> "$ENTITY_PREFIX${shortcutDestination.entityId}"
        }
    }

    override fun applyShortcutExtras(intent: Intent, iconName: String?) {
        iconName?.let { intent.putExtra(EXTRA_ICON_NAME, it) }
    }

    override fun toDraft(
        shortcut: ShortcutInfoCompat,
        defaultServerId: Int,
        iconIdToName: Map<Int, String>,
    ): ShortcutDraft {
        val extras = shortcut.intent.extras
        val serverId = extras?.getInt(EXTRA_SERVER, defaultServerId) ?: defaultServerId
        val path = extras?.getString(EXTRA_WEBVIEW_PATH).orEmpty()
            .ifBlank { shortcut.intent.action.orEmpty() }

        return ShortcutDraft(
            id = shortcut.id,
            serverId = serverId,
            selectedIconName = parseIcon(extras, iconIdToName),
            label = shortcut.shortLabel.toString(),
            description = shortcut.longLabel?.toString().orEmpty(),
            destination = parseDestination(path),
        )
    }

    internal companion object {
        private const val ENTITY_PREFIX = "entityId:"
        private const val MDI_PREFIX = "mdi:"
        private const val EXTRA_ICON_ID = "iconId"
        private const val EXTRA_ICON_NAME = "iconName"
        private const val EXTRA_SERVER = "server"
        private const val EXTRA_WEBVIEW_PATH = "path"
    }
}

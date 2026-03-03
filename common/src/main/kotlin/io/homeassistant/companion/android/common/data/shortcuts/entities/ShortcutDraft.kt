package io.homeassistant.companion.android.common.data.shortcuts.entities

import android.os.Bundle
import androidx.compose.runtime.Immutable
import androidx.core.content.pm.ShortcutInfoCompat
import io.homeassistant.companion.android.common.data.shortcuts.ShortcutIntentCodec

@Immutable
data class ShortcutDraft(
    val id: String,
    val serverId: Int?,
    val selectedIconName: String?,
    val label: String,
    val description: String,
    val destination: ShortcutDestination,
) {
    companion object
}

fun ShortcutDraft.Companion.empty(id: String = ""): ShortcutDraft {
    return ShortcutDraft(
        id = id,
        serverId = null,
        selectedIconName = null,
        label = "",
        description = "",
        destination = ShortcutDestination.Lovelace(""),
    )
}

fun ShortcutDraft.toSummary(): ShortcutSummary {
    return ShortcutSummary(
        id = id,
        selectedIconName = selectedIconName,
        label = label,
    )
}

private const val EXTRA_SERVER = "server"
private const val EXTRA_WEBVIEW_PATH = "path"

internal fun resolveShortcutPath(extras: Bundle?, action: String?): String {
    return extras?.getString(EXTRA_WEBVIEW_PATH).orEmpty()
        .ifBlank { action.orEmpty() }
}

internal fun ShortcutInfoCompat.toDraft(
    defaultServerId: Int,
    iconIdToName: Map<Int, String>,
    shortcutIntentCodec: ShortcutIntentCodec,
): ShortcutDraft {
    val extras = intent.extras
    val serverId = extras?.getInt(EXTRA_SERVER, defaultServerId) ?: defaultServerId
    val path = resolveShortcutPath(extras = extras, action = intent.action)

    val selectedIconName = shortcutIntentCodec.parseIcon(extras, iconIdToName)
    val destination = shortcutIntentCodec.parseDestination(extras, path)

    return ShortcutDraft(
        id = id,
        serverId = serverId,
        selectedIconName = selectedIconName,
        label = shortLabel.toString(),
        description = longLabel?.toString().orEmpty(),
        destination = destination,
    )
}

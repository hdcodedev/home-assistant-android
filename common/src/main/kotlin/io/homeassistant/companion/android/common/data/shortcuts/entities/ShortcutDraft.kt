package io.homeassistant.companion.android.common.data.shortcuts.entities

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

fun ShortcutDraft.Companion.empty(id: String = ""): ShortcutDraft =
    ShortcutDraft(
        id = id,
        serverId = null,
        selectedIconName = null,
        label = "",
        description = "",
        destination = ShortcutDestination.Lovelace(""),
    )

fun ShortcutDraft.toSummary(): ShortcutSummary =
    ShortcutSummary(
        id = id,
        selectedIconName = selectedIconName,
        label = label,
    )

private const val EXTRA_SERVER = "server"
private const val EXTRA_WEBVIEW_PATH = "path"

internal fun ShortcutInfoCompat.toDraft(
    defaultServerId: Int,
    iconIdToName: Map<Int, String>,
    shortcutIntentCodec: ShortcutIntentCodec,
): ShortcutDraft {
    val extras = intent.extras
    val serverId = extras?.getInt(EXTRA_SERVER, defaultServerId) ?: defaultServerId
    val path = extras?.getString(EXTRA_WEBVIEW_PATH).orEmpty()
        .ifBlank { intent.action.orEmpty() }

    return ShortcutDraft(
        id = id,
        serverId = serverId,
        selectedIconName = shortcutIntentCodec.parseIcon(extras, iconIdToName),
        label = shortLabel.toString(),
        description = longLabel?.toString().orEmpty(),
        destination = shortcutIntentCodec.parseDestination(path),
    )
}

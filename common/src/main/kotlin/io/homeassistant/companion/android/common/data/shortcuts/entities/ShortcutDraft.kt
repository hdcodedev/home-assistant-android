package io.homeassistant.companion.android.common.data.shortcuts.entities

import androidx.compose.runtime.Immutable

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

fun ShortcutDraft.Companion.empty(id: String = ""): ShortcutDraft = ShortcutDraft(
    id = id,
    serverId = null,
    selectedIconName = null,
    label = "",
    description = "",
    destination = ShortcutDestination.Dashboard(""),
)

fun ShortcutDraft.toSummary(): ShortcutSummary = ShortcutSummary(
    id = id,
    selectedIconName = selectedIconName,
    label = label,
)

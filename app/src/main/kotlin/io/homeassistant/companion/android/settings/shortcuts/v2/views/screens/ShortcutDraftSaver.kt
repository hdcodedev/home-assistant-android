package io.homeassistant.companion.android.settings.shortcuts.v2.views.screens

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDestination
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDraft

private const val TARGET_TYPE_LOVELACE = "lovelace"
private const val TARGET_TYPE_ENTITY = "entity"

internal val ShortcutDraftSaver: Saver<ShortcutDraft, Any> = listSaver(
    save = { draft ->
        val targetType = when (draft.destination) {
            is ShortcutDestination.Lovelace -> TARGET_TYPE_LOVELACE
            is ShortcutDestination.Entity -> TARGET_TYPE_ENTITY
        }
        val targetValue = when (val destination = draft.destination) {
            is ShortcutDestination.Lovelace -> destination.path
            is ShortcutDestination.Entity -> destination.entityId
        }
        listOf(
            draft.id,
            draft.serverId,
            draft.selectedIconName,
            draft.label,
            draft.description,
            targetType,
            targetValue,
        )
    },
    restore = { values ->
        val id = values[0] as String
        val serverId = values[1] as Int?
        val iconName = values[2] as String?
        val label = values[3] as String
        val description = values[4] as String
        val targetType = values[5] as String
        val targetValue = values[6] as String
        val destination = if (targetType == TARGET_TYPE_ENTITY) {
            ShortcutDestination.Entity(targetValue)
        } else {
            ShortcutDestination.Lovelace(targetValue)
        }
        ShortcutDraft(
            id = id,
            serverId = serverId,
            selectedIconName = iconName,
            label = label,
            description = description,
            destination = destination,
        )
    },
)

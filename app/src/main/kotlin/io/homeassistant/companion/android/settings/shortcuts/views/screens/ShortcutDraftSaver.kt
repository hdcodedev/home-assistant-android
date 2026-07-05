package io.homeassistant.companion.android.settings.shortcuts.views.screens

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDestination
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDraft

/**
 * Compose [Saver] for [ShortcutDraft]. Needed because [ShortcutDestination] is a sealed interface
 * that Compose's default saveable mechanism can't auto-serialize. Persists only to an in-memory
 * [android.os.Bundle] for config changes — not to disk or database.
 *
 * V1 kept draft state in the ViewModel, which avoids this Saver but requires passing every form
 * field change back up to the ViewModel. V2 keeps the draft in the screen's rememberSaveable for
 * simpler form binding — the tradeoff is needing this custom Saver.
 */
private const val TARGET_TYPE_DASHBOARD = "dashboard"
private const val TARGET_TYPE_ENTITY = "entity"

internal val ShortcutDraftSaver: Saver<ShortcutDraft, Any> = listSaver(
    save = { draft ->
        val targetType = when (draft.destination) {
            is ShortcutDestination.Dashboard -> TARGET_TYPE_DASHBOARD
            is ShortcutDestination.Entity -> TARGET_TYPE_ENTITY
        }
        val targetValue = when (val destination = draft.destination) {
            is ShortcutDestination.Dashboard -> destination.path
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
            ShortcutDestination.Dashboard(targetValue)
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

package io.homeassistant.companion.android.settings.shortcuts

import androidx.compose.runtime.Immutable
import io.homeassistant.companion.android.common.data.integration.display.EntityDisplayState
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDestination
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDraft
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutError
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutServer
import io.homeassistant.companion.android.settings.shortcuts.data.entities.isValid

/** Loading / error / editor states for the shortcut editor screens. */
@Immutable
internal sealed interface ShortcutsUiState {
    data object Loading : ShortcutsUiState

    data class LoadError(val error: ShortcutError) : ShortcutsUiState

    data class Editor(val state: EditorState) : ShortcutsUiState
}

/**
 * Editor state. [draft] is the single source of truth for the editor content, including the
 * selected destination. [entityDisplayStatesByServerId] feed the entity picker and are populated by the
 * ViewModel on load and whenever the selected server changes.
 */
@Immutable
internal data class EditorState(
    val servers: List<ShortcutServer>,
    val entityDisplayStatesByServerId: Map<Int, EntityDisplayState> = emptyMap(),
    val draft: ShortcutDraft,
    val isSaving: Boolean = false,
) {
    val selectedServer: ShortcutServer
        get() = servers.first { it.id == draft.serverId }

    val isServerSelectionVisible: Boolean
        get() = servers.size > 1

    val selectedEntityDisplayState: EntityDisplayState
        get() = entityDisplayStatesByServerId[draft.serverId] ?: EntityDisplayState.Loading

    val isDisplayStateLoading: Boolean
        get() = selectedServer.supportsEntity && selectedEntityDisplayState is EntityDisplayState.Loading

    val canSubmit: Boolean
        get() = draft.label.isNotBlank() &&
            draft.destination.isValid &&
            draft.destination.isDestinationReady(isDisplayStateLoading) &&
            draft.destination.isSupportedBy(selectedServer)

    fun withDraft(draft: ShortcutDraft): EditorState = copy(draft = draft)

    fun withServer(serverId: Int): EditorState {
        val server = servers.first { it.id == serverId }
        val destination = if (draft.destination.isSupportedBy(server)) {
            draft.destination
        } else {
            ShortcutDestination.Dashboard(path = "")
        }
        return copy(draft = draft.copy(serverId = serverId, destination = destination))
    }
}

/**
 * Whether the destination is ready to submit. Dashboards are always ready; an entity target is
 * ready once its display state has finished loading.
 */
private fun ShortcutDestination.isDestinationReady(isDisplayStateLoading: Boolean): Boolean = when (this) {
    is ShortcutDestination.Dashboard -> true
    is ShortcutDestination.Entity -> !isDisplayStateLoading
}

/**
 * Whether the destination is supported by the given server. An entity target requires the server
 * to advertise entity support; dashboards are always supported.
 */
private fun ShortcutDestination.isSupportedBy(server: ShortcutServer): Boolean = when (this) {
    is ShortcutDestination.Dashboard -> true
    is ShortcutDestination.Entity -> server.supportsEntity
}

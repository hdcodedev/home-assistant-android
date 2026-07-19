package io.homeassistant.companion.android.settings.shortcuts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.data.integration.display.EntityDisplayState
import io.homeassistant.companion.android.common.data.integration.display.GetEntitiesForDisplayUseCase
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDraft
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutError
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class ShortcutCloseEvent(val messageRes: Int? = null)

internal sealed interface EditorRoute {
    val kind: ShortcutKind

    data class Create(override val kind: ShortcutKind) : EditorRoute
    data class Edit(override val kind: ShortcutKind, val id: String) : EditorRoute
}

/** Editor ViewModel shared by the app and home shortcut create/edit flows. */
@HiltViewModel(assistedFactory = ShortcutEditorViewModelFactory::class)
internal class ShortcutEditorViewModel @AssistedInject constructor(
    @Assisted private val editorRoute: EditorRoute,
    private val loadShortcutEditorUseCase: LoadShortcutEditorUseCase,
    private val modifyShortcutUseCase: ModifyShortcutUseCase,
    private val getEntitiesForDisplay: GetEntitiesForDisplayUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ShortcutsUiState>(ShortcutsUiState.Loading)
    val uiState: StateFlow<ShortcutsUiState> = _uiState.asStateFlow()

    private val _closeEvents = MutableSharedFlow<ShortcutCloseEvent>(extraBufferCapacity = 1)
    val closeEvents = _closeEvents.asSharedFlow()

    private val _errorSnackbar = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val errorSnackbar = _errorSnackbar.asSharedFlow()

    private var loadJob: Job? = null

    init {
        loadEditorForRoute(editorRoute)
    }

    // region Load

    fun retry() = loadEditorForRoute(editorRoute)

    private fun loadEditorForRoute(route: EditorRoute) {
        when (route) {
            is EditorRoute.Create -> loadNewShortcut()
            is EditorRoute.Edit -> when (route.kind) {
                ShortcutKind.APP -> loadAppShortcut(route.id)
                ShortcutKind.HOME -> loadHomeShortcut(route.id)
            }
        }
    }

    private fun loadNewShortcut() = load { loadShortcutEditorUseCase.loadNewShortcut() }

    private fun loadAppShortcut(id: String) = load {
        loadShortcutEditorUseCase.loadAppShortcut(id)
    }

    private fun loadHomeShortcut(id: String) = load {
        loadShortcutEditorUseCase.loadHomeShortcut(id)
    }

    private fun load(load: suspend () -> ShortcutResult<ShortcutEditor>) {
        loadJob?.cancel()
        _uiState.value = ShortcutsUiState.Loading
        loadJob = viewModelScope.launch {
            when (val result = load()) {
                is ShortcutResult.Success -> {
                    setEditorState(result.data)
                    loadSelectedServerCatalog()
                }
                is ShortcutResult.Error -> {
                    _uiState.value = ShortcutsUiState.LoadError(result.error)
                }
            }
        }
    }

    // endregion

    fun updateDraft(draft: ShortcutDraft) {
        val state = currentState() ?: return
        if (draft.serverId != state.draft.serverId) return
        updateState { it.withDraft(draft) }
    }

    fun selectServer(serverId: Int) {
        val state = currentState() ?: return
        if (state.servers.none { it.id == serverId }) return
        if (state.draft.serverId != serverId) {
            updateState { it.withServer(serverId) }
            loadSelectedServerCatalog()
        }
    }

    // region Save

    fun createAppShortcut() = submit(errorRes = commonR.string.shortcut_create_error) {
        modifyShortcutUseCase(ShortcutModification.Create(kind = ShortcutKind.APP, draft = it))
    }

    fun createHomeShortcut() = submit(
        errorRes = commonR.string.shortcut_create_error,
        successMessageRes = commonR.string.shortcut_home_request_sent,
    ) {
        modifyShortcutUseCase(ShortcutModification.Create(kind = ShortcutKind.HOME, draft = it))
    }

    fun updateAppShortcut(id: String) = submit(errorRes = commonR.string.shortcut_update_error) {
        modifyShortcutUseCase(ShortcutModification.Update(kind = ShortcutKind.APP, id = id, draft = it))
    }

    fun updateHomeShortcut(id: String) = submit(errorRes = commonR.string.shortcut_update_error) {
        modifyShortcutUseCase(ShortcutModification.Update(kind = ShortcutKind.HOME, id = id, draft = it))
    }

    private fun submit(
        errorRes: Int,
        successMessageRes: Int? = null,
        saveShortcut: suspend (ShortcutDraft) -> ShortcutResult<*>,
    ) {
        val state = currentState() ?: return
        if (state.isSaving || !state.canSubmit) return
        val draft = state.draft

        updateState { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = saveShortcut(draft)) {
                is ShortcutResult.Success -> _closeEvents.emit(ShortcutCloseEvent(messageRes = successMessageRes))
                is ShortcutResult.Error -> {
                    _errorSnackbar.emit(errorResFor(result.error, fallbackRes = errorRes))
                }
            }
            updateState { it.copy(isSaving = false) }
        }
    }

    // endregion

    // region Delete

    fun deleteAppShortcut(id: String) = delete(commonR.string.shortcut_delete_error) {
        modifyShortcutUseCase(ShortcutModification.Remove(kind = ShortcutKind.APP, id = id))
    }

    fun disableHomeShortcut(id: String) = delete(commonR.string.shortcut_disable_error) {
        modifyShortcutUseCase(ShortcutModification.Remove(kind = ShortcutKind.HOME, id = id))
    }

    private fun delete(errorRes: Int, deleteShortcut: suspend () -> ShortcutResult<Unit>) {
        val state = currentState() ?: return
        if (state.isSaving) return

        updateState { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = deleteShortcut()) {
                is ShortcutResult.Success -> _closeEvents.emit(ShortcutCloseEvent())
                is ShortcutResult.Error -> {
                    _errorSnackbar.emit(errorResFor(result.error, fallbackRes = errorRes))
                }
            }
            updateState { it.copy(isSaving = false) }
        }
    }

    // endregion

    /**
     * Maps an operation [ShortcutError] to a specific, actionable snackbar message, falling back to
     * [fallbackRes] (the generic create/update/delete message) when no dedicated string exists.
     */
    private fun errorResFor(error: ShortcutError, fallbackRes: Int): Int = when (error) {
        ShortcutError.AppShortcutSlotsFull -> commonR.string.shortcut_dynamic_slots_full
        ShortcutError.ShortcutNotFound -> commonR.string.shortcut_not_found
        ShortcutError.HomeShortcutPinningNotSupported -> commonR.string.shortcut_pin_not_supported
        else -> fallbackRes
    }

    private fun loadSelectedServerCatalog() {
        val state = currentState() ?: return
        if (!state.selectedServer.supportsEntity) return
        val serverId = state.draft.serverId
        // Reuse the already loaded display state, or an in-flight load, instead of fetching again.
        if (state.entityDisplayStatesByServerId.containsKey(serverId)) return
        updateState {
            it.copy(
                entityDisplayStatesByServerId = it.entityDisplayStatesByServerId +
                    (serverId to EntityDisplayState.Loading),
            )
        }
        viewModelScope.launch {
            getEntitiesForDisplay(serverId).collect { entityDisplayState ->
                updateState {
                    it.copy(
                        entityDisplayStatesByServerId = it.entityDisplayStatesByServerId +
                            (serverId to entityDisplayState),
                    )
                }
                if (entityDisplayState is EntityDisplayState.Error) {
                    _errorSnackbar.emit(commonR.string.shortcut_entity_error_title)
                }
            }
        }
    }

    private fun setEditorState(editor: ShortcutEditor) {
        _uiState.value = ShortcutsUiState.Editor(
            EditorState(
                servers = editor.servers,
                draft = editor.draft,
            ),
        )
    }

    private fun currentState(): EditorState? = (_uiState.value as? ShortcutsUiState.Editor)?.state

    private fun updateState(block: (EditorState) -> EditorState) {
        val current = _uiState.value as? ShortcutsUiState.Editor ?: return
        _uiState.value = current.copy(state = block(current.state))
    }
}

@AssistedFactory
internal interface ShortcutEditorViewModelFactory {
    fun create(route: EditorRoute): ShortcutEditorViewModel
}

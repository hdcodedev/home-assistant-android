package io.homeassistant.companion.android.settings.shortcuts.v2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.homeassistant.companion.android.common.data.shortcuts.ShortcutsRepository
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutData
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDraft
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutError
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutResult
import io.homeassistant.companion.android.common.data.shortcuts.entities.empty
import io.homeassistant.companion.android.settings.shortcuts.v2.views.screens.ShortcutEditAction
import io.homeassistant.companion.android.settings.shortcuts.v2.views.screens.ShortcutEditorScreenState
import io.homeassistant.companion.android.settings.shortcuts.v2.views.screens.toUi
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ShortcutEditorViewModel @Inject constructor(
    private val repository: ShortcutsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShortcutEditorUiState())

    val uiState: StateFlow<ShortcutEditorUiState> = _uiState.asStateFlow()

    private val _closeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val closeEvents = _closeEvents.asSharedFlow()

    // Action entry point.
    fun dispatch(action: ShortcutEditAction) {
        when (action) {
            is ShortcutEditAction.Submit -> submitShortcut(action.draft)
            is ShortcutEditAction.DeleteAppShortcut ->
                launchCloseMutation { repository.deleteAppShortcut(action.appIndex) }
            is ShortcutEditAction.DeleteHomeShortcut ->
                launchCloseMutation { repository.deleteHomeShortcut(action.shortcutId) }
        }
    }

    // Editor opening flows.
    fun draftAppShortcutEditor() = openEditor(
        initial = ShortcutEditorUiState.AppCreateState(ShortcutDraft.empty()),
        request = repository::draftAppShortcutEditor,
        mapEditor = { data ->
            ShortcutEditorUiState.AppCreateState(data.draftSeed)
        },
    )

    fun draftHomeShortcutEditor() = openEditor(
        initial = ShortcutEditorUiState.HomeCreateState(ShortcutDraft.empty()),
        request = repository::draftHomeShortcutEditor,
        mapEditor = { data ->
            ShortcutEditorUiState.HomeCreateState(data.draftSeed)
        },
    )

    fun openEditAppShortcut(index: Int) = openEditor(
        initial = ShortcutEditorUiState.AppEditState(
            initialDraft = ShortcutDraft.empty(),
            appIndex = index,
        ),
        request = { repository.loadAppShortcut(index) },
        mapEditor = { data ->
            ShortcutEditorUiState.AppEditState(
                initialDraft = data.draftSeed,
                appIndex = index,
            )
        },
    )

    fun openEditHomeShortcut(shortcutId: String) = openEditor(
        initial = ShortcutEditorUiState.HomeEditState(
            initialDraft = ShortcutDraft.empty(shortcutId),
            shortcutId = shortcutId,
        ),
        request = { repository.loadHomeShortcut(shortcutId) },
        mapEditor = { data ->
            ShortcutEditorUiState.HomeEditState(
                initialDraft = data.draftSeed,
                shortcutId = shortcutId,
            )
        },
    )

    // Create and update flows
    private fun submitShortcut(draft: ShortcutDraft) {
        val editor = uiState.value.editor ?: return
        when (editor) {
            is ShortcutEditorUiState.AppCreateState ->
                launchCloseMutation { repository.createAppShortcut(draft) }
            is ShortcutEditorUiState.AppEditState ->
                launchCloseMutation { repository.updateAppShortcut(editor.appIndex, draft) }
            is ShortcutEditorUiState.HomeEditorState ->
                launchCloseMutation { repository.upsertHomeShortcut(draft) }
        }
    }

    // Shared mutation helpers.
    private fun openEditor(
        initial: ShortcutEditorUiState.EditorState,
        request: suspend () -> ShortcutResult<ShortcutData>,
        mapEditor: (ShortcutData) -> ShortcutEditorUiState.EditorState,
    ) {
        _uiState.update { state ->
            state.copy(
                content = state.content.copy(isLoading = true, error = null),
                editor = initial,
            )
        }
        viewModelScope.launch {
            when (val result = request()) {
                is ShortcutResult.Success -> {
                    val data = result.data
                    _uiState.update { state ->
                        state.copy(
                            content = data.servers.toUi().copy(
                                isSaving = state.content.isSaving,
                                error = null,
                            ),
                            editor = mapEditor(data),
                        )
                    }
                }

                is ShortcutResult.Error ->
                    updateScreen { it.copy(isLoading = false, error = result.error) }
            }
        }
    }


    // UI state update helpers.
    private fun setScreenError(error: ShortcutError?) {
        updateScreen { state ->
            if (state.error == error) state else state.copy(error = error)
        }
    }

    private fun setSaving(isSaving: Boolean) {
        updateScreen { it.copy(isSaving = isSaving) }
    }

    private fun updateScreen(updater: (ShortcutEditorScreenState) -> ShortcutEditorScreenState) {
        _uiState.update { state ->
            state.copy(content = updater(state.content))
        }
    }

    private fun <T> launchCloseMutation(request: suspend () -> ShortcutResult<T>) {
        launchMutation(
            request = request,
            onSuccess = { _closeEvents.emit(Unit) },
        )
    }

    private fun <T> launchMutation(
        request: suspend () -> ShortcutResult<T>,
        onSuccess: suspend (T) -> Unit,
    ) {
        viewModelScope.launch {
            setSaving(true)
            try {
                when (val result = request()) {
                    is ShortcutResult.Success -> onSuccess(result.data)
                    is ShortcutResult.Error -> setScreenError(result.error)
                }
            } finally {
                setSaving(false)
            }
        }
    }
}

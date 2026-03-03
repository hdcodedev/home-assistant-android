package io.homeassistant.companion.android.settings.shortcuts.v2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.homeassistant.companion.android.common.data.shortcuts.ShortcutsRepository
import io.homeassistant.companion.android.common.data.shortcuts.entities.AppEditorData
import io.homeassistant.companion.android.common.data.shortcuts.entities.EditorMode
import io.homeassistant.companion.android.common.data.shortcuts.entities.HomeEditorData
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
    private val shortcutsRepository: ShortcutsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ShortcutEditorUiState(
            editor = ShortcutEditorUiState.LoadingEditorState,
        ),
    )

    val uiState: StateFlow<ShortcutEditorUiState> = _uiState.asStateFlow()

    private val _closeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val closeEvents = _closeEvents.asSharedFlow()

    init {
        loadScreenData()
    }

    fun dispatch(action: ShortcutEditAction) {
        when (action) {
            is ShortcutEditAction.Submit -> submitShortcut(action.draft)
            is ShortcutEditAction.Delete -> deleteShortcut()
        }
    }

    fun openCreateAppShortcut() {
        updateEditor { INITIAL_APP_EDITOR_STATE }
    }

    fun openCreateHomeShortcut() {
        updateEditor { INITIAL_HOME_EDITOR_STATE }
    }

    fun openEditAppShortcut(index: Int) {
        loadMappedEditorState(
            request = { shortcutsRepository.loadAppEditor(index) },
            mapper = { editorData -> editorData.toEditorState() },
        )
    }

    fun openEditHomeShortcut(shortcutId: String) {
        loadMappedEditorState(
            request = { shortcutsRepository.loadHomeEditor(shortcutId) },
            mapper = { editorData -> editorData.toEditorState() },
        )
    }

    private fun submitShortcut(draft: ShortcutDraft) {
        when (val editor = uiState.value.editor) {
            is ShortcutEditorUiState.LoadingEditorState -> return
            is ShortcutEditorUiState.AppEditorState -> {
                launchCloseMutation {
                    shortcutsRepository.saveAppShortcut(
                        editor.appIndex.takeIf { editor.isEditing },
                        draft,
                    )
                }
            }

            is ShortcutEditorUiState.HomeEditorState -> {
                launchCloseMutation { shortcutsRepository.upsertHomeShortcut(draft) }
            }
        }
    }

    private fun deleteShortcut() {
        when (val editor = uiState.value.editor) {
            is ShortcutEditorUiState.LoadingEditorState -> return
            is ShortcutEditorUiState.AppEditorState -> {
                val index = editor.appIndex?.takeIf { editor.isEditing } ?: return
                launchCloseMutation { shortcutsRepository.deleteAppShortcut(index) }
            }

            is ShortcutEditorUiState.HomeEditorState -> {
                if (!editor.isEditing) return
                launchCloseMutation { shortcutsRepository.deleteHomeShortcut(editor.draftSeed.id) }
            }
        }
    }

    private fun <T> launchCloseMutation(request: suspend () -> ShortcutResult<T>) {
        launchMutation(
            request = request,
            onSuccess = { _closeEvents.emit(Unit) },
        )
    }

    private fun loadScreenData() {
        viewModelScope.launch {
            val screenState = when (val result = shortcutsRepository.loadEditorData()) {
                is ShortcutResult.Success -> result.data.toUi()
                is ShortcutResult.Error -> ShortcutEditorScreenState(isLoading = false, error = result.error)
            }
            updateScreen { screenState }
        }
    }

    private fun loadEditorState(request: suspend () -> ShortcutResult<ShortcutEditorUiState.EditorState>) {
        viewModelScope.launch {
            updateScreen { it.copy(isLoading = true, error = null) }
            when (val result = request()) {
                is ShortcutResult.Success -> {
                    updateEditor { result.data }
                    updateScreen { it.copy(isLoading = false) }
                }

                is ShortcutResult.Error -> {
                    updateScreen { it.copy(isLoading = false, error = result.error) }
                }
            }
        }
    }

    private fun <T> loadMappedEditorState(
        request: suspend () -> ShortcutResult<T>,
        mapper: (T) -> ShortcutEditorUiState.EditorState,
    ) {
        loadEditorState {
            when (val result = request()) {
                is ShortcutResult.Success -> ShortcutResult.Success(mapper(result.data))
                is ShortcutResult.Error -> result
            }
        }
    }

    private fun <T> launchMutation(
        request: suspend () -> ShortcutResult<T>,
        onSuccess: suspend (T) -> Unit,
    ) {
        viewModelScope.launch {
            setSaving(true)
            when (val result = request()) {
                is ShortcutResult.Success -> {
                    setSaving(false)
                    onSuccess(result.data)
                }

                is ShortcutResult.Error -> {
                    setSaving(false)
                    setScreenError(result.error)
                }
            }
        }
    }

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
            state.copy(screen = updater(state.screen))
        }
    }

    private fun updateEditor(updater: (ShortcutEditorUiState.EditorState) -> ShortcutEditorUiState.EditorState) {
        _uiState.update { state ->
            state.copy(editor = updater(state.editor), screen = state.screen.copy(error = null))
        }
    }

    private fun AppEditorData.toEditorState(): ShortcutEditorUiState.AppEditorState {
        return ShortcutEditorUiState.AppEditorState(
            mode = mode,
            draftSeed = draftSeed,
            appIndex = index,
        )
    }

    private fun HomeEditorData.toEditorState(): ShortcutEditorUiState.HomeEditorState {
        return ShortcutEditorUiState.HomeEditorState(
            mode = mode,
            draftSeed = draftSeed,
        )
    }

    private companion object {
        val INITIAL_APP_EDITOR_STATE = ShortcutEditorUiState.AppEditorState(
            mode = EditorMode.CREATE,
            draftSeed = ShortcutDraft.empty(),
        )

        val INITIAL_HOME_EDITOR_STATE = ShortcutEditorUiState.HomeEditorState(
            mode = EditorMode.CREATE,
            draftSeed = ShortcutDraft.empty(),
        )
    }
}

package io.homeassistant.companion.android.settings.shortcuts.v2

import androidx.compose.runtime.Immutable
import io.homeassistant.companion.android.common.data.shortcuts.entities.EditorMode
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDraft
import io.homeassistant.companion.android.settings.shortcuts.v2.views.screens.ShortcutEditorScreenState

@Immutable
data class ShortcutEditorUiState(
    val screen: ShortcutEditorScreenState = ShortcutEditorScreenState(isLoading = true),
    val editor: EditorState,
) {
    sealed interface EditorState {
        val isEditing: Boolean get() = false
    }

    @Immutable
    data object LoadingEditorState : EditorState

    @Immutable
    data class AppEditorState(
        val mode: EditorMode,
        val draftSeed: ShortcutDraft,
        val appIndex: Int? = null,
    ) : EditorState {
        override val isEditing: Boolean get() = mode == EditorMode.EDIT

        init {
            require(mode != EditorMode.EDIT || appIndex != null) {
                "App editor in EDIT mode requires a non-null appIndex."
            }
        }
    }

    @Immutable
    data class HomeEditorState(
        val mode: EditorMode,
        val draftSeed: ShortcutDraft,
    ) : EditorState {
        override val isEditing: Boolean get() = mode == EditorMode.EDIT

        init {
            require(mode != EditorMode.EDIT || draftSeed.id.isNotBlank()) {
                "Home editor in EDIT mode requires a persisted shortcut id."
            }
        }
    }
}

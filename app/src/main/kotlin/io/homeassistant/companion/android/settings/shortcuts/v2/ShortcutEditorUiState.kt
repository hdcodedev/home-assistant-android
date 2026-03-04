package io.homeassistant.companion.android.settings.shortcuts.v2

import androidx.compose.runtime.Immutable
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDraft
import io.homeassistant.companion.android.settings.shortcuts.v2.views.screens.ShortcutEditorScreenState

@Immutable
data class ShortcutEditorUiState(
    val content: ShortcutEditorScreenState = ShortcutEditorScreenState(isLoading = true),
    val editor: EditorState? = null,
) {
    val isLoading: Boolean
        get() = content.isLoading || editor == null

    sealed interface EditorState {
        val isEditing: Boolean get() = false
    }

    sealed interface AppEditorState : EditorState {
        val initialDraft: ShortcutDraft
    }

    @Immutable
    data class AppCreateState(
        override val initialDraft: ShortcutDraft,
    ) : AppEditorState

    @Immutable
    data class AppEditState(
        override val initialDraft: ShortcutDraft,
        val appIndex: Int,
    ) : AppEditorState {
        override val isEditing: Boolean = true
    }

    sealed interface HomeEditorState : EditorState {
        val initialDraft: ShortcutDraft
    }

    @Immutable
    data class HomeCreateState(
        override val initialDraft: ShortcutDraft,
    ) : HomeEditorState

    @Immutable
    data class HomeEditState(
        override val initialDraft: ShortcutDraft,
        val shortcutId: String,
    ) : HomeEditorState {
        override val isEditing: Boolean = true
    }
}

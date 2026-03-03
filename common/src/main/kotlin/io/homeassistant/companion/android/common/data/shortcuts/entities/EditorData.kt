package io.homeassistant.companion.android.common.data.shortcuts.entities

import androidx.compose.runtime.Immutable

enum class EditorMode {
    CREATE,
    EDIT,
}

@Immutable
data class AppEditorData(
    val index: Int,
    val draftSeed: ShortcutDraft,
    val mode: EditorMode,
)

@Immutable
data class HomeEditorData(
    val draftSeed: ShortcutDraft,
    val mode: EditorMode,
)

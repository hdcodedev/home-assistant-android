package io.homeassistant.companion.android.common.data.shortcuts

import io.homeassistant.companion.android.common.data.shortcuts.entities.AppEditorData
import io.homeassistant.companion.android.common.data.shortcuts.entities.HomeEditorData
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDraft
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutEditorData
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutResult
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutsListData

interface ShortcutsRepository {
    suspend fun loadShortcutsList(): ShortcutResult<ShortcutsListData>
    suspend fun loadEditorData(): ShortcutResult<ShortcutEditorData>
    suspend fun loadAppEditor(index: Int): ShortcutResult<AppEditorData>
    suspend fun loadHomeEditor(shortcutId: String): ShortcutResult<HomeEditorData>
    suspend fun saveAppShortcut(
        index: Int?,
        shortcut: ShortcutDraft,
    ): ShortcutResult<AppEditorData>
    fun deleteAppShortcut(index: Int): ShortcutResult<Unit>
    suspend fun upsertHomeShortcut(shortcut: ShortcutDraft): ShortcutResult<Unit>
    fun deleteHomeShortcut(shortcutId: String): ShortcutResult<Unit>
}

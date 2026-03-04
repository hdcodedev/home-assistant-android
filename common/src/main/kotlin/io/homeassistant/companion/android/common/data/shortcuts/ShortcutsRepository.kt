package io.homeassistant.companion.android.common.data.shortcuts

import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutData
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDraft
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutResult
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutsListData

interface ShortcutsRepository {
    suspend fun loadShortcuts(): ShortcutResult<ShortcutsListData>

    suspend fun draftAppShortcutEditor(): ShortcutResult<ShortcutData>
    suspend fun draftHomeShortcutEditor(): ShortcutResult<ShortcutData>

    suspend fun loadAppShortcut(index: Int): ShortcutResult<ShortcutData>
    suspend fun loadHomeShortcut(shortcutId: String): ShortcutResult<ShortcutData>

    suspend fun createAppShortcut(shortcut: ShortcutDraft): ShortcutResult<Unit>
    suspend fun updateAppShortcut(index: Int, shortcut: ShortcutDraft): ShortcutResult<Unit>
    suspend fun upsertHomeShortcut(shortcut: ShortcutDraft): ShortcutResult<Unit>

    fun deleteAppShortcut(index: Int): ShortcutResult<Unit>
    fun deleteHomeShortcut(shortcutId: String): ShortcutResult<Unit>
}

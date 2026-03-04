package io.homeassistant.companion.android.settings.shortcuts.v2.views.screens

import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDraft

sealed interface ShortcutEditAction {
    data class Submit(val draft: ShortcutDraft) : ShortcutEditAction
    data class DeleteAppShortcut(val appIndex: Int) : ShortcutEditAction
    data class DeleteHomeShortcut(val shortcutId: String) : ShortcutEditAction
}

package io.homeassistant.companion.android.common.data.shortcuts.entities

import androidx.compose.runtime.Immutable

@Immutable
sealed interface ShortcutDestination {
    data class Dashboard(val path: String) : ShortcutDestination
    data class Entity(val entityId: String) : ShortcutDestination
}

enum class ShortcutType {
    DASHBOARD,
    ENTITY,
}

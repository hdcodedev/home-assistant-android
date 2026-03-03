package io.homeassistant.companion.android.common.data.shortcuts.entities

import androidx.compose.runtime.Immutable

@Immutable
data class ShortcutsListData(
    val maxAppShortcuts: Int,
    val appShortcuts: List<AppShortcutSummary>,
    val homeShortcuts: List<ShortcutSummary>,
    val homeShortcutsError: ShortcutError? = null,
)

@Immutable
data class AppShortcutSummary(val index: Int, val summary: ShortcutSummary)


@Immutable
data class ShortcutSummary(val id: String, val selectedIconName: String?, val label: String)

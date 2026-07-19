package io.homeassistant.companion.android.settings.shortcuts.views.preview

import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import io.homeassistant.companion.android.common.data.integration.display.EntityDisplayItem
import io.homeassistant.companion.android.common.data.integration.display.EntityDisplayState
import io.homeassistant.companion.android.settings.shortcuts.EditorState
import io.homeassistant.companion.android.settings.shortcuts.ManageShortcutsLoadState
import io.homeassistant.companion.android.settings.shortcuts.ManageShortcutsUiState
import io.homeassistant.companion.android.settings.shortcuts.data.entities.AppShortcuts
import io.homeassistant.companion.android.settings.shortcuts.data.entities.HomeShortcutListItem
import io.homeassistant.companion.android.settings.shortcuts.data.entities.HomeShortcuts
import io.homeassistant.companion.android.settings.shortcuts.data.entities.Shortcut
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDestination
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDraft
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutIcon
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutListItem
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutServer
import io.homeassistant.companion.android.settings.shortcuts.data.entities.toDraft

private const val PREVIEW_APP_SHORTCUT_PREFIX = "shortcut"
private const val PREVIEW_HOME_SHORTCUT_PREFIX = "home"

internal object ShortcutPreviewData {
    private fun appShortcutId(index: Int): String {
        return "${PREVIEW_APP_SHORTCUT_PREFIX}_${index + 1}"
    }

    fun buildEditorState(
        servers: List<ShortcutServer> = previewServers,
        isSaving: Boolean = false,
        entityDisplayStatesByServerId: Map<Int, EntityDisplayState> = previewEntityDisplayStates,
        draft: ShortcutDraft = ShortcutDraft.initial(servers.firstOrNull()?.id ?: 0),
    ): EditorState {
        return EditorState(
            servers = servers,
            entityDisplayStatesByServerId = entityDisplayStatesByServerId,
            draft = draft,
            isSaving = isSaving,
        )
    }

    fun buildDraft(
        destination: ShortcutDestination = ShortcutDestination.Dashboard("/lovelace/shortcut"),
        serverId: Int = 1,
    ): ShortcutDraft {
        val isEntity = destination is ShortcutDestination.Entity
        return ShortcutDraft(
            serverId = serverId,
            icon = ShortcutIcon.Default,
            label = if (isEntity) "Lights" else "Shortcut",
            description = if (isEntity) "Toggle living room lights" else "Description",
            destination = destination,
        )
    }

    fun buildAppDrafts(count: Int, destination: ShortcutDestination): List<Shortcut> {
        return List(count) { index ->
            val number = index + 1
            val perIndexDestination = when (destination) {
                is ShortcutDestination.Dashboard -> destination.copy(path = "${destination.path}$number")
                is ShortcutDestination.Entity -> destination
            }
            Shortcut(
                id = appShortcutId(index),
                serverId = 1,
                label = if (destination is ShortcutDestination.Entity) "Lights" else "Shortcut $number",
                description = if (destination is ShortcutDestination.Entity) {
                    "Toggle living room lights"
                } else {
                    "Description $number"
                },
                destination = perIndexDestination,
            )
        }
    }

    fun buildAppSummaries(count: Int, destination: ShortcutDestination): List<ShortcutListItem> {
        return buildAppDrafts(count = count, destination = destination).map { draft ->
            ShortcutListItem(
                id = draft.id,
                label = draft.label,
                icon = draft.icon,
            )
        }
    }

    fun buildHomeDraft(): Shortcut {
        return Shortcut(
            id = "home_1",
            serverId = 1,
            label = "Home",
            description = "Home shortcut",
            destination = ShortcutDestination.Dashboard("/lovelace/home"),
        )
    }

    fun buildHomeSummaries(count: Int = 1): List<HomeShortcutListItem> {
        return List(count) { index ->
            val number = index + 1
            HomeShortcutListItem(
                shortcut = ShortcutListItem(
                    id = "${PREVIEW_HOME_SHORTCUT_PREFIX}_$number",
                    label = if (count == 1) "Home" else "Home $number",
                    icon = if (index == 0) ShortcutIcon.Mdi("mdi:pin") else ShortcutIcon.Default,
                ),
                isEnabled = true,
            )
        }
    }

    fun buildDisabledHomeSummaries(): List<HomeShortcutListItem> {
        return listOf(
            HomeShortcutListItem(
                shortcut = ShortcutListItem(
                    id = "home_enabled",
                    label = "Enabled",
                    icon = ShortcutIcon.Mdi("mdi:pin"),
                ),
                isEnabled = true,
            ),
            HomeShortcutListItem(
                shortcut = ShortcutListItem(
                    id = "home_disabled",
                    label = "Disabled",
                    icon = ShortcutIcon.Mdi("mdi:pin-off"),
                ),
                isEnabled = false,
            ),
        )
    }

    fun buildListState(
        loadState: ManageShortcutsLoadState = ManageShortcutsLoadState.Ready,
        maxAppShortcuts: Int = 5,
        appSummaries: List<ShortcutListItem> = buildAppSummaries(
            count = 2,
            destination = ShortcutDestination.Dashboard("/lovelace/shortcut"),
        ),
        homeSummaries: List<HomeShortcutListItem> = buildHomeSummaries(),
        isHomeSupported: Boolean = true,
    ): ManageShortcutsUiState {
        val homeItems = if (isHomeSupported) homeSummaries else emptyList()
        return ManageShortcutsUiState(
            loadState = loadState,
            appShortcuts = AppShortcuts(
                items = appSummaries,
                maxAppShortcuts = maxAppShortcuts,
            ),
            homeShortcuts = HomeShortcuts(
                items = homeItems,
                canPinShortcuts = isHomeSupported,
            ),
        )
    }

    // region Shared preview data

    val previewServers = listOf(
        ShortcutServer(
            id = 1,
            name = "Home",
            supportsEntity = true,
        ),
        ShortcutServer(
            id = 2,
            name = "Office",
            supportsEntity = true,
        ),
    )

    val previewEntityDisplayStates: Map<Int, EntityDisplayState> = mapOf(
        1 to EntityDisplayState.Loaded(
            listOf(
                EntityDisplayItem(
                    entityId = "light.living_room",
                    name = "Living room light",
                    icon = CommunityMaterial.Icon2.cmd_lightbulb,
                ),
                EntityDisplayItem(
                    entityId = "switch.kitchen",
                    name = "Kitchen switch",
                    icon = CommunityMaterial.Icon3.cmd_toggle_switch,
                ),
            ),
        ),
    )

    val previewAppDraft = buildDraft()

    val previewEditAppDraft = buildDraft().copy(
        label = "Shortcut 1",
    )

    val previewEntityDraft = ShortcutDraft(
        serverId = 1,
        icon = ShortcutIcon.Default,
        label = "Lights",
        description = "Toggle living room lights",
        destination = ShortcutDestination.Entity("light.living_room"),
    )

    val previewHomeDraft = buildHomeDraft().toDraft()

    // endregion
}

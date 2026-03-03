package io.homeassistant.companion.android.common.data.shortcuts.impl

import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.shortcuts.ShortcutsRepository
import io.homeassistant.companion.android.common.data.shortcuts.entities.AppEditorData
import io.homeassistant.companion.android.common.data.shortcuts.entities.AppShortcutSummary
import io.homeassistant.companion.android.common.data.shortcuts.entities.EditorMode
import io.homeassistant.companion.android.common.data.shortcuts.entities.HomeEditorData
import io.homeassistant.companion.android.common.data.shortcuts.entities.ServerData
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDraft
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutEditorData
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutError
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutResult
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDestination
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutsListData
import io.homeassistant.companion.android.common.data.shortcuts.entities.empty
import io.homeassistant.companion.android.common.data.shortcuts.entities.toSummary
import io.homeassistant.companion.android.common.data.websocket.impl.entities.AreaRegistryResponse
import io.homeassistant.companion.android.common.data.websocket.impl.entities.DeviceRegistryResponse
import io.homeassistant.companion.android.common.data.websocket.impl.entities.EntityRegistryResponse
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.database.server.ServerConnectionInfo
import io.homeassistant.companion.android.database.server.ServerSessionInfo
import io.homeassistant.companion.android.database.server.ServerUserInfo
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

private const val MOCK_MAX_APP_SHORTCUTS = 5
private const val MOCK_APP_SHORTCUT_PREFIX = "shortcut"
private const val MOCK_HOME_SHORTCUT_PREFIX = "pinned"

@Singleton
internal class MockShortcutsRepositoryImpl @Inject constructor() : ShortcutsRepository {

    private val defaultServerId = 1

    private val servers = listOf(
        Server(
            id = 1,
            _name = "Home",
            connection = ServerConnectionInfo(externalUrl = "https://home.example.com"),
            session = ServerSessionInfo(),
            user = ServerUserInfo(),
        ),
        Server(
            id = 2,
            _name = "Office",
            connection = ServerConnectionInfo(externalUrl = "https://office.example.com"),
            session = ServerSessionInfo(),
            user = ServerUserInfo(),
        ),
    )

    private val serverDataById = mapOf(
        1 to ServerData(
            entities = listOf(
                Entity(
                    entityId = "light.living_room",
                    state = "on",
                    attributes = mapOf("friendly_name" to "Living Room Light"),
                    lastChanged = LocalDateTime.now(),
                    lastUpdated = LocalDateTime.now(),
                ),
                Entity(
                    entityId = "switch.kitchen",
                    state = "off",
                    attributes = mapOf("friendly_name" to "Kitchen Switch"),
                    lastChanged = LocalDateTime.now(),
                    lastUpdated = LocalDateTime.now(),
                ),
            ),
            entityRegistry = listOf(
                EntityRegistryResponse(
                    entityId = "light.living_room",
                    areaId = "living_room",
                    deviceId = "device_1",
                ),
                EntityRegistryResponse(
                    entityId = "switch.kitchen",
                    areaId = "kitchen",
                    deviceId = "device_2",
                ),
            ),
            deviceRegistry = listOf(
                DeviceRegistryResponse(
                    id = "device_1",
                    name = "Ceiling Lights",
                ),
                DeviceRegistryResponse(
                    id = "device_2",
                    name = "Kitchen Switches",
                ),
            ),
            areaRegistry = listOf(
                AreaRegistryResponse(
                    areaId = "living_room",
                    name = "Living Room",
                ),
                AreaRegistryResponse(
                    areaId = "kitchen",
                    name = "Kitchen",
                ),
            ),
        ),
        2 to ServerData(),
    )

    private val appShortcuts = linkedMapOf(
        0 to ShortcutDraft(
            id = buildAppId(0),
            serverId = defaultServerId,
            selectedIconName = "mdi:home",
            label = "Home Dashboard",
            description = "Open the main dashboard",
            destination = ShortcutDestination.Lovelace("/lovelace/home"),
        ),
        1 to ShortcutDraft(
            id = buildAppId(1),
            serverId = defaultServerId,
            selectedIconName = "mdi:flash",
            label = "Energy",
            description = "Open energy dashboard",
            destination = ShortcutDestination.Lovelace("/lovelace/energy"),
        ),
        2 to ShortcutDraft(
            id = buildAppId(2),
            serverId = defaultServerId,
            selectedIconName = "mdi:shield",
            label = "Security",
            description = "Open security dashboard",
            destination = ShortcutDestination.Lovelace("/lovelace/security"),
        ),
        3 to ShortcutDraft(
            id = buildAppId(3),
            serverId = defaultServerId,
            selectedIconName = "mdi:stove",
            label = "Kitchen",
            description = "Open kitchen controls",
            destination = ShortcutDestination.Entity("switch.kitchen"),
        ),
    )

    private val homeShortcuts = linkedMapOf(
        "pinned_living_room" to ShortcutDraft(
            id = "pinned_living_room",
            serverId = defaultServerId,
            selectedIconName = "mdi:lightbulb",
            label = "Living Room",
            description = "Open Living Room",
            destination = ShortcutDestination.Entity("light.living_room"),
        ),
        "pinned_kitchen" to ShortcutDraft(
            id = "pinned_kitchen",
            serverId = defaultServerId,
            selectedIconName = "mdi:silverware-fork-knife",
            label = "Kitchen",
            description = "Open Kitchen",
            destination = ShortcutDestination.Entity("switch.kitchen"),
        ),
        "pinned_bedroom" to ShortcutDraft(
            id = "pinned_bedroom",
            serverId = defaultServerId,
            selectedIconName = "mdi:bed",
            label = "Bedroom",
            description = "Open Bedroom",
            destination = ShortcutDestination.Lovelace("/lovelace/bedroom"),
        ),
        "pinned_garage" to ShortcutDraft(
            id = "pinned_garage",
            serverId = defaultServerId,
            selectedIconName = "mdi:garage",
            label = "Garage",
            description = "Open Garage",
            destination = ShortcutDestination.Lovelace("/lovelace/garage"),
        ),
        "pinned_energy" to ShortcutDraft(
            id = "pinned_energy",
            serverId = defaultServerId,
            selectedIconName = "mdi:flash",
            label = "Energy",
            description = "Open Energy",
            destination = ShortcutDestination.Lovelace("/lovelace/energy"),
        ),
        "pinned_security" to ShortcutDraft(
            id = "pinned_security",
            serverId = defaultServerId,
            selectedIconName = "mdi:shield",
            label = "Security",
            description = "Open Security",
            destination = ShortcutDestination.Lovelace("/lovelace/security"),
        ),
        "pinned_cameras" to ShortcutDraft(
            id = "pinned_cameras",
            serverId = defaultServerId,
            selectedIconName = "mdi:camera",
            label = "Cameras",
            description = "Open Cameras",
            destination = ShortcutDestination.Lovelace("/lovelace/cameras"),
        ),
        "pinned_climate" to ShortcutDraft(
            id = "pinned_climate",
            serverId = defaultServerId,
            selectedIconName = "mdi:thermostat",
            label = "Climate",
            description = "Open Climate",
            destination = ShortcutDestination.Lovelace("/lovelace/climate"),
        ),
        "pinned_office" to ShortcutDraft(
            id = "pinned_office",
            serverId = defaultServerId,
            selectedIconName = "mdi:briefcase",
            label = "Office",
            description = "Open Office",
            destination = ShortcutDestination.Lovelace("/lovelace/office"),
        ),
        "pinned_lights" to ShortcutDraft(
            id = "pinned_lights",
            serverId = defaultServerId,
            selectedIconName = "mdi:lightbulb",
            label = "Lights",
            description = "Open Lights",
            destination = ShortcutDestination.Entity("light.living_room"),
        ),
        "pinned_media" to ShortcutDraft(
            id = "pinned_media",
            serverId = defaultServerId,
            selectedIconName = "mdi:play-circle",
            label = "Media",
            description = "Open Media",
            destination = ShortcutDestination.Lovelace("/lovelace/media"),
        ),
        "pinned_garden" to ShortcutDraft(
            id = "pinned_garden",
            serverId = defaultServerId,
            selectedIconName = "mdi:home",
            label = "Garden",
            description = "Open Garden",
            destination = ShortcutDestination.Lovelace("/lovelace/garden"),
        ),
        "pinned_patio" to ShortcutDraft(
            id = "pinned_patio",
            serverId = defaultServerId,
            selectedIconName = "mdi:flash",
            label = "Patio",
            description = "Open Patio",
            destination = ShortcutDestination.Lovelace("/lovelace/patio"),
        ),
        "pinned_guests" to ShortcutDraft(
            id = "pinned_guests",
            serverId = defaultServerId,
            selectedIconName = "mdi:briefcase",
            label = "Guests",
            description = "Open Guests",
            destination = ShortcutDestination.Lovelace("/lovelace/guests"),
        ),
        "pinned_laundry" to ShortcutDraft(
            id = "pinned_laundry",
            serverId = defaultServerId,
            selectedIconName = "mdi:stove",
            label = "Laundry",
            description = "Open Laundry",
            destination = ShortcutDestination.Lovelace("/lovelace/laundry"),
        ),
        "pinned_dining" to ShortcutDraft(
            id = "pinned_dining",
            serverId = defaultServerId,
            selectedIconName = "mdi:silverware-fork-knife",
            label = "Dining",
            description = "Open Dining",
            destination = ShortcutDestination.Lovelace("/lovelace/dining"),
        ),
        "pinned_hallway" to ShortcutDraft(
            id = "pinned_hallway",
            serverId = defaultServerId,
            selectedIconName = "mdi:camera",
            label = "Hallway",
            description = "Open Hallway",
            destination = ShortcutDestination.Lovelace("/lovelace/hallway"),
        ),
        "pinned_kids_room" to ShortcutDraft(
            id = "pinned_kids_room",
            serverId = defaultServerId,
            selectedIconName = "mdi:lightbulb",
            label = "Kids Room",
            description = "Open Kids Room",
            destination = ShortcutDestination.Lovelace("/lovelace/kids-room"),
        ),
        "pinned_bathroom" to ShortcutDraft(
            id = "pinned_bathroom",
            serverId = defaultServerId,
            selectedIconName = "mdi:thermostat",
            label = "Bathroom",
            description = "Open Bathroom",
            destination = ShortcutDestination.Lovelace("/lovelace/bathroom"),
        ),
        "pinned_guest_room" to ShortcutDraft(
            id = "pinned_guest_room",
            serverId = defaultServerId,
            selectedIconName = "mdi:bed",
            label = "Guest Room",
            description = "Open Guest Room",
            destination = ShortcutDestination.Lovelace("/lovelace/guest-room"),
        ),
        "pinned_server_room" to ShortcutDraft(
            id = "pinned_server_room",
            serverId = defaultServerId,
            selectedIconName = "mdi:shield",
            label = "Server Room",
            description = "Open Server Room",
            destination = ShortcutDestination.Lovelace("/lovelace/server-room"),
        ),
    )

    override suspend fun loadShortcutsList(): ShortcutResult<ShortcutsListData> {
        return ShortcutResult.Success(
            ShortcutsListData(
                maxAppShortcuts = MOCK_MAX_APP_SHORTCUTS,
                appShortcuts = appShortcuts.entries.sortedBy { it.key }.map { (index, shortcut) ->
                    AppShortcutSummary(index = index, summary = shortcut.toSummary())
                },
                homeShortcuts = homeShortcuts.values.map { it.toSummary() }.reversed(),
            ),
        )
    }

    override suspend fun loadEditorData(): ShortcutResult<ShortcutEditorData> {
        return ShortcutResult.Success(
            ShortcutEditorData(
                servers = servers,
                serverDataById = serverDataById,
            ),
        )
    }

    override suspend fun loadAppEditor(index: Int): ShortcutResult<AppEditorData> {
        if (index !in 0 until MOCK_MAX_APP_SHORTCUTS) {
            return ShortcutResult.Error(ShortcutError.InvalidIndex)
        }

        val existingDraft = appShortcuts[index]
        val draft = existingDraft ?: ShortcutDraft.empty().copy(serverId = defaultServerId)
        return ShortcutResult.Success(
            AppEditorData(
                index = index,
                draftSeed = draft,
                mode = if (existingDraft == null) EditorMode.CREATE else EditorMode.EDIT,
            ),
        )
    }

    override suspend fun loadHomeEditor(shortcutId: String): ShortcutResult<HomeEditorData> {
        val requestedId = shortcutId.trim()
        if (requestedId.isEmpty()) {
            return ShortcutResult.Error(ShortcutError.InvalidInput)
        }

        val existingDraft = homeShortcuts[requestedId]
        val draft = existingDraft ?: ShortcutDraft.empty(requestedId).copy(serverId = defaultServerId)
        return ShortcutResult.Success(
            HomeEditorData(
                draftSeed = draft,
                mode = if (existingDraft == null) EditorMode.CREATE else EditorMode.EDIT,
            ),
        )
    }

    override suspend fun saveAppShortcut(
        index: Int?,
        shortcut: ShortcutDraft,
    ): ShortcutResult<AppEditorData> {
        val resolvedIndex = if (index != null) {
            if (index !in 0 until MOCK_MAX_APP_SHORTCUTS) {
                return ShortcutResult.Error(ShortcutError.InvalidIndex)
            }
            index
        } else {
            (0 until MOCK_MAX_APP_SHORTCUTS).firstOrNull { !appShortcuts.containsKey(it) }
                ?: return ShortcutResult.Error(ShortcutError.SlotsFull)
        }
        val expectedId = buildAppId(resolvedIndex)
        if (appShortcuts.containsKey(resolvedIndex) && shortcut.id != expectedId) {
            return ShortcutResult.Error(ShortcutError.SlotsFull)
        }

        val normalized = shortcut.copy(
            id = expectedId,
            serverId = normalizeServerId(shortcut.serverId),
        )
        appShortcuts[resolvedIndex] = normalized
        return ShortcutResult.Success(
            AppEditorData(
                index = resolvedIndex,
                draftSeed = normalized,
                mode = EditorMode.EDIT,
            ),
        )
    }

    override fun deleteAppShortcut(index: Int): ShortcutResult<Unit> {
        if (index !in 0 until MOCK_MAX_APP_SHORTCUTS) {
            return ShortcutResult.Error(ShortcutError.InvalidIndex)
        }
        appShortcuts.remove(index)
        return ShortcutResult.Success(Unit)
    }

    override suspend fun upsertHomeShortcut(shortcut: ShortcutDraft): ShortcutResult<Unit> {
        val inputId = shortcut.id.trim()
        val id = if (inputId.isNotBlank()) inputId else buildHomeId(shortcut.label)

        homeShortcuts[id] = shortcut.copy(
            id = id,
            serverId = normalizeServerId(shortcut.serverId),
        )

        return ShortcutResult.Success(Unit)
    }

    override fun deleteHomeShortcut(shortcutId: String): ShortcutResult<Unit> {
        if (shortcutId.isBlank()) return ShortcutResult.Error(ShortcutError.InvalidInput)
        homeShortcuts.remove(shortcutId)
        return ShortcutResult.Success(Unit)
    }

    private fun normalizeServerId(serverId: Int?): Int {
        return serverId
            ?.let { requestedId -> servers.firstOrNull { it.id == requestedId }?.id }
            ?: defaultServerId
    }

    private fun buildHomeId(label: String): String {
        val baseSlug = label.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "shortcut" }
        val base = "${MOCK_HOME_SHORTCUT_PREFIX}_$baseSlug"
        if (!homeShortcuts.containsKey(base)) return base

        var index = 2
        var candidate = "${base}_$index"
        while (homeShortcuts.containsKey(candidate)) {
            index += 1
            candidate = "${base}_$index"
        }
        return candidate
    }

    private fun buildAppId(index: Int): String {
        return "${MOCK_APP_SHORTCUT_PREFIX}_${index + 1}"
    }
}

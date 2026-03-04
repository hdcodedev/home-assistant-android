package io.homeassistant.companion.android.common.data.shortcuts.entities

import androidx.compose.runtime.Immutable
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.websocket.impl.entities.AreaRegistryResponse
import io.homeassistant.companion.android.common.data.websocket.impl.entities.DeviceRegistryResponse
import io.homeassistant.companion.android.common.data.websocket.impl.entities.EntityRegistryResponse
import io.homeassistant.companion.android.database.server.Server

@Immutable
data class ShortcutData(
    val servers: List<ShortcutServerItem>,
    val draftSeed: ShortcutDraft,
    val mode: ShortcutMode,
)

@Immutable
data class ShortcutServerItem(
    val server: Server,
    val data: ServerData,
)

@Immutable
data class ServerData(
    val entities: List<Entity> = emptyList(),
    val entityRegistry: List<EntityRegistryResponse> = emptyList(),
    val deviceRegistry: List<DeviceRegistryResponse> = emptyList(),
    val areaRegistry: List<AreaRegistryResponse> = emptyList(),
)

enum class ShortcutMode {
    CREATE,
    EDIT,
}

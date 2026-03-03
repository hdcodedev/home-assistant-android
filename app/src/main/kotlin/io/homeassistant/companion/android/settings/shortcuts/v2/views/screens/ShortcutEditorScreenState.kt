package io.homeassistant.companion.android.settings.shortcuts.v2.views.screens

import androidx.compose.runtime.Immutable
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutEditorData
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutError
import io.homeassistant.companion.android.common.data.websocket.impl.entities.AreaRegistryResponse
import io.homeassistant.companion.android.common.data.websocket.impl.entities.DeviceRegistryResponse
import io.homeassistant.companion.android.common.data.websocket.impl.entities.EntityRegistryResponse
import io.homeassistant.companion.android.database.server.Server

@Immutable
data class ShortcutEditorScreenState(
    val isLoading: Boolean,
    val isSaving: Boolean = false,
    val error: ShortcutError? = null,
    val servers: List<Server> = emptyList(),
    val entities: Map<Int, List<Entity>> = emptyMap(),
    val entityRegistry: Map<Int, List<EntityRegistryResponse>> = emptyMap(),
    val deviceRegistry: Map<Int, List<DeviceRegistryResponse>> = emptyMap(),
    val areaRegistry: Map<Int, List<AreaRegistryResponse>> = emptyMap(),
)

internal fun ShortcutEditorData.toUi(): ShortcutEditorScreenState {
    return ShortcutEditorScreenState(
        isLoading = false,
        error = null,
        servers = servers.toList(),
        entities = serverDataById.mapValues { it.value.entities.toList() },
        entityRegistry = serverDataById.mapValues { it.value.entityRegistry.toList() },
        deviceRegistry = serverDataById.mapValues { it.value.deviceRegistry.toList() },
        areaRegistry = serverDataById.mapValues { it.value.areaRegistry.toList() },
    )
}

package io.homeassistant.companion.android.common.data.shortcuts.impl

import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.common.data.shortcuts.entities.ServerData
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutError
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutResult
import io.homeassistant.companion.android.database.server.Server
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

internal data class ServersData(val servers: List<Server>, val defaultServerId: Int)
internal data class SaveServerData(val defaultServerId: Int, val resolvedServerId: Int)

internal class ServersDataSource(
    private val serverManager: ServerManager,
) {
    suspend fun getServers(): ShortcutResult<ServersData> {
        val availableServers = serverManager.servers()
        if (availableServers.isEmpty()) {
            return ShortcutResult.Error(ShortcutError.NoServers)
        }
        val selectedServerId = serverManager.getServer()?.id
        val defaultServerId =
            availableServers.firstOrNull { it.id == selectedServerId }?.id ?: availableServers.first().id
        return ShortcutResult.Success(ServersData(availableServers, defaultServerId))
    }

    private fun resolveServerIdForSave(requestedServerId: Int?, serversData: ServersData): Int =
        serversData.servers.firstOrNull { it.id == requestedServerId }?.id ?: serversData.defaultServerId

    suspend fun resolveForSave(requestedServerId: Int?): ShortcutResult<SaveServerData> =
        when (val result = getServers()) {
            is ShortcutResult.Success -> ShortcutResult.Success(
                SaveServerData(
                    defaultServerId = result.data.defaultServerId,
                    resolvedServerId = resolveServerIdForSave(requestedServerId, result.data),
                ),
            )
            is ShortcutResult.Error -> result
        }

    suspend fun loadServerData(serverId: Int): ServerData = coroutineScope {
        val integrationRepository = serverManager.integrationRepository(serverId)
        val webSocketRepository = serverManager.webSocketRepository(serverId)

        val entitiesJob = async {
            runCatching {
                integrationRepository.getEntities().orEmpty().sortedBy { it.entityId }
            }.getOrElse { e ->
                if (e is CancellationException) throw e
                Timber.e(e, "Couldn't load entities for server %d", serverId)
                emptyList()
            }
        }

        val entityRegistryJob = async {
            runCatching { webSocketRepository.getEntityRegistry().orEmpty() }
                .getOrElse { e ->
                    if (e is CancellationException) throw e
                    Timber.e(e, "Couldn't load entity registry for server %d", serverId)
                    emptyList()
                }
        }

        val deviceRegistryJob = async {
            runCatching { webSocketRepository.getDeviceRegistry().orEmpty() }
                .getOrElse { e ->
                    if (e is CancellationException) throw e
                    Timber.e(e, "Couldn't load device registry for server %d", serverId)
                    emptyList()
                }
        }

        val areaRegistryJob = async {
            runCatching { webSocketRepository.getAreaRegistry().orEmpty() }
                .getOrElse { e ->
                    if (e is CancellationException) throw e
                    Timber.e(e, "Couldn't load area registry for server %d", serverId)
                    emptyList()
                }
        }

        ServerData(
            entities = entitiesJob.await(),
            entityRegistry = entityRegistryJob.await(),
            deviceRegistry = deviceRegistryJob.await(),
            areaRegistry = areaRegistryJob.await(),
        )
    }

    suspend fun loadEditorDataByServerId(servers: List<Server>): Map<Int, ServerData> = coroutineScope {
        servers.map { server ->
            async {
                try {
                    server.id to loadServerData(server.id)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load data for serverId=%d", server.id)
                    null
                }
            }
        }.awaitAll().filterNotNull().toMap()
    }
}

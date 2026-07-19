package io.homeassistant.companion.android.settings.shortcuts.data.impl

import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.settings.shortcuts.data.ShortcutServersRepository
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutError
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutResult
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutServer
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutServersSnapshot
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
internal class ServersDataSource @Inject constructor(private val serverManager: ServerManager) :
    ShortcutServersRepository {

    override suspend fun loadServers(): ShortcutResult<ShortcutServersSnapshot> = withContext(Dispatchers.IO) {
        runCatchingShortcut("Failed to load shortcut servers") {
            val availableServers = serverManager.servers()
            if (availableServers.isEmpty()) {
                ShortcutResult.Error(ShortcutError.NoServersConfigured)
            } else {
                val servers = availableServers.map { it.toShortcutServerOption() }
                val activeServerId = serverManager.getServer()?.id
                val defaultServer = servers.firstOrNull { it.id == activeServerId } ?: servers.first()
                ShortcutResult.Success(
                    ShortcutServersSnapshot(
                        servers = servers,
                        defaultServer = defaultServer,
                    ),
                )
            }
        }
    }
}

private fun Server.toShortcutServerOption(): ShortcutServer = ShortcutServer(
    id = id,
    name = friendlyName.ifBlank { id.toString() },
    // Entity-targeted shortcuts require server-side entity support added in Home Assistant 2025.6.
    supportsEntity = version?.isAtLeast(2025, 6) == true,
)

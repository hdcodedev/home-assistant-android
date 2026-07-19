package io.homeassistant.companion.android.settings.shortcuts

import androidx.compose.runtime.Immutable
import io.homeassistant.companion.android.settings.shortcuts.data.AppShortcutsRepository
import io.homeassistant.companion.android.settings.shortcuts.data.HomeShortcutsRepository
import io.homeassistant.companion.android.settings.shortcuts.data.ShortcutServersRepository
import io.homeassistant.companion.android.settings.shortcuts.data.entities.Shortcut
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDraft
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutError
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutResult
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutServer
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutServersSnapshot
import io.homeassistant.companion.android.settings.shortcuts.data.entities.toDraft
import javax.inject.Inject

/** Loaded starting point for the shortcut editor: the editable [draft] and the [servers] the user can target. */
@Immutable
internal data class ShortcutEditor(val draft: ShortcutDraft, val servers: List<ShortcutServer>)

internal class LoadShortcutEditorUseCase @Inject constructor(
    private val appShortcutsRepository: AppShortcutsRepository,
    private val homeShortcutsRepository: HomeShortcutsRepository,
    private val shortcutServersRepository: ShortcutServersRepository,
) {
    suspend fun loadNewShortcut(): ShortcutResult<ShortcutEditor> = loadEditor(::newEditor)

    suspend fun loadAppShortcut(id: String): ShortcutResult<ShortcutEditor> = loadEditor { servers ->
        editShortcut(
            id = id,
            servers = servers,
            shortcutLoader = appShortcutsRepository::loadEditor,
        )
    }

    suspend fun loadHomeShortcut(id: String): ShortcutResult<ShortcutEditor> = loadEditor { servers ->
        editShortcut(
            id = id,
            servers = servers,
            shortcutLoader = homeShortcutsRepository::loadEditor,
        )
    }

    private suspend fun loadEditor(
        editor: suspend (ShortcutServersSnapshot) -> ShortcutResult<ShortcutEditor>,
    ): ShortcutResult<ShortcutEditor> {
        if (!areShortcutsSupported()) {
            return ShortcutResult.Error(ShortcutError.AndroidVersionNotSupported)
        }

        val servers = when (val result = shortcutServersRepository.loadServers()) {
            is ShortcutResult.Success -> result.data
            is ShortcutResult.Error -> return ShortcutResult.Error(result.error)
        }

        return editor(servers)
    }

    private fun newEditor(servers: ShortcutServersSnapshot): ShortcutResult<ShortcutEditor> = ShortcutResult.Success(
        ShortcutEditor(
            draft = ShortcutDraft.initial(servers.defaultServer.id),
            servers = servers.servers,
        ),
    )

    private suspend fun editShortcut(
        id: String,
        servers: ShortcutServersSnapshot,
        shortcutLoader: suspend (String, Int) -> ShortcutResult<Shortcut>,
    ): ShortcutResult<ShortcutEditor> {
        val shortcut = when (val result = shortcutLoader(id, servers.defaultServer.id)) {
            is ShortcutResult.Success -> result.data
            is ShortcutResult.Error -> return ShortcutResult.Error(result.error)
        }
        val selectedServer = servers.resolvePersisted(shortcut.serverId)
        val draft = shortcut.toDraft()
            .copy(serverId = selectedServer.id)

        return ShortcutResult.Success(
            ShortcutEditor(
                draft = draft,
                servers = servers.servers,
            ),
        )
    }
}

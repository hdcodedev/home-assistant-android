package io.homeassistant.companion.android.common.data.shortcuts.impl

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.common.data.shortcuts.ShortcutFactory
import io.homeassistant.companion.android.common.data.shortcuts.ShortcutIntentCodec
import io.homeassistant.companion.android.common.data.shortcuts.ShortcutsRepository
import io.homeassistant.companion.android.common.data.shortcuts.entities.AppShortcutSummary
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDraft
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutData
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutError
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutResult
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutsListData
import io.homeassistant.companion.android.common.data.shortcuts.entities.ServerData
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutServerItem
import io.homeassistant.companion.android.common.data.shortcuts.entities.toSummary
import io.homeassistant.companion.android.database.IconDialogCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ShortcutsRepositoryImpl @Inject constructor(
    @ApplicationContext private val app: Context,
    private val serverManager: ServerManager,
    private val shortcutFactory: ShortcutFactory,
    private val shortcutIntentCodec: ShortcutIntentCodec,
) : ShortcutsRepository {

    private val isShortcutsSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1

    private val iconIdToName: Map<Int, String> by lazy { IconDialogCompat(app.assets).loadAllIcons() }

    private val appShortcutsDataSource: AppShortcutsDataSource by lazy {
        AppShortcutsDataSource(
            app = app,
            shortcutFactory = shortcutFactory,
            shortcutIntentCodec = shortcutIntentCodec,
            iconIdToName = iconIdToName,
        )
    }

    private val homeShortcutsDataSource: HomeShortcutsDataSource by lazy {
        HomeShortcutsDataSource(
            app = app,
            shortcutFactory = shortcutFactory,
            shortcutIntentCodec = shortcutIntentCodec,
            iconIdToName = iconIdToName,
        )
    }

    private val serversDataSource: ServersDataSource by lazy {
        ServersDataSource(serverManager)
    }

    private data class EditorContext(
        val defaultServerId: Int,
        val servers: List<ShortcutServerItem>,
    )

    override suspend fun loadShortcuts(): ShortcutResult<ShortcutsListData> {
        if (!isShortcutsSupported) {
            return ShortcutResult.Error(ShortcutError.ApiNotSupported)
        }

        val servers = when (val result = serversDataSource.getServers()) {
            is ShortcutResult.Success -> result.data
            is ShortcutResult.Error -> return ShortcutResult.Error(result.error)
        }

        val defaultServerId = servers.defaultServerId
        val appShortcutsData = appShortcutsDataSource.load(defaultServerId)
        val appShortcuts = appShortcutsData.orderedShortcuts.map { (index, draft) ->
            AppShortcutSummary(index = index, summary = draft.toSummary())
        }

        return if (!homeShortcutsDataSource.canPinShortcuts) {
            ShortcutResult.Success(
                ShortcutsListData(
                    maxAppShortcuts = appShortcutsData.maxAppShortcuts,
                    appShortcuts = appShortcuts,
                    homeShortcuts = emptyList(),
                    homeShortcutsError = ShortcutError.HomeShortcutNotSupported,
                ),
            )
        } else {
            ShortcutResult.Success(
                ShortcutsListData(
                    maxAppShortcuts = appShortcutsData.maxAppShortcuts,
                    appShortcuts = appShortcuts,
                    homeShortcuts = homeShortcutsDataSource.load(defaultServerId).map { it.toSummary() },
                ),
            )
        }
    }

    override suspend fun draftAppShortcutEditor(): ShortcutResult<ShortcutData> {
        if (!isShortcutsSupported) {
            return ShortcutResult.Error(ShortcutError.ApiNotSupported)
        }

        val context = when (val result = loadEditorContext()) {
            is ShortcutResult.Success -> result.data
            is ShortcutResult.Error -> return result
        }

        val editor = when (val result = appShortcutsDataSource.loadCreateEditor(context.defaultServerId)) {
            is ShortcutResult.Success -> result.data
            is ShortcutResult.Error -> return ShortcutResult.Error(result.error)
        }
        val (draftSeed, mode) = editor

        return ShortcutResult.Success(
            ShortcutData(
                servers = context.servers,
                draftSeed = draftSeed,
                mode = mode,
            ),
        )
    }

    override suspend fun loadAppShortcut(index: Int): ShortcutResult<ShortcutData> {
        if (!isShortcutsSupported) {
            return ShortcutResult.Error(ShortcutError.ApiNotSupported)
        }

        val context = when (val result = loadEditorContext()) {
            is ShortcutResult.Success -> result.data
            is ShortcutResult.Error -> return result
        }

        val editor = when (val result = appShortcutsDataSource.loadEditor(index, context.defaultServerId)) {
            is ShortcutResult.Success -> result.data
            is ShortcutResult.Error -> return ShortcutResult.Error(result.error)
        }
        val (draftSeed, mode) = editor

        return ShortcutResult.Success(
            ShortcutData(
                servers = context.servers,
                draftSeed = draftSeed,
                mode = mode,
            ),
        )
    }

    override suspend fun draftHomeShortcutEditor(): ShortcutResult<ShortcutData> {
        if (!isShortcutsSupported) {
            return ShortcutResult.Error(ShortcutError.ApiNotSupported)
        }
        if (!homeShortcutsDataSource.canPinShortcuts) {
            return ShortcutResult.Error(ShortcutError.HomeShortcutNotSupported)
        }

        val context = when (val result = loadEditorContext()) {
            is ShortcutResult.Success -> result.data
            is ShortcutResult.Error -> return result
        }

        val editor = when (val result = homeShortcutsDataSource.loadCreateEditor(context.defaultServerId)) {
            is ShortcutResult.Success -> result.data
            is ShortcutResult.Error -> return ShortcutResult.Error(result.error)
        }
        val (draftSeed, mode) = editor

        return ShortcutResult.Success(
            ShortcutData(
                servers = context.servers,
                draftSeed = draftSeed,
                mode = mode,
            ),
        )
    }

    override suspend fun loadHomeShortcut(shortcutId: String): ShortcutResult<ShortcutData> {
        if (!isShortcutsSupported) {
            return ShortcutResult.Error(ShortcutError.ApiNotSupported)
        }
        if (!homeShortcutsDataSource.canPinShortcuts) {
            return ShortcutResult.Error(ShortcutError.HomeShortcutNotSupported)
        }

        val context = when (val result = loadEditorContext()) {
            is ShortcutResult.Success -> result.data
            is ShortcutResult.Error -> return result
        }

        val editor = when (val result = homeShortcutsDataSource.loadEditor(shortcutId, context.defaultServerId)) {
            is ShortcutResult.Success -> result.data
            is ShortcutResult.Error -> return ShortcutResult.Error(result.error)
        }
        val (draftSeed, mode) = editor

        return ShortcutResult.Success(
            ShortcutData(
                servers = context.servers,
                draftSeed = draftSeed,
                mode = mode,
            ),
        )
    }

    override suspend fun createAppShortcut(shortcut: ShortcutDraft): ShortcutResult<Unit> {
        return saveAppShortcut(index = null, shortcut = shortcut)
    }

    override suspend fun updateAppShortcut(index: Int, shortcut: ShortcutDraft): ShortcutResult<Unit> {
        return saveAppShortcut(index = index, shortcut = shortcut)
    }

    private suspend fun saveAppShortcut(
        index: Int?,
        shortcut: ShortcutDraft,
    ): ShortcutResult<Unit> {
        if (!isShortcutsSupported) {
            return ShortcutResult.Error(ShortcutError.ApiNotSupported)
        }

        val saveServerData = when (val result = serversDataSource.resolveForSave(shortcut.serverId)) {
            is ShortcutResult.Success -> result.data
            is ShortcutResult.Error -> return ShortcutResult.Error(result.error)
        }

        return appShortcutsDataSource.save(
            index = index,
            shortcut = shortcut.copy(serverId = saveServerData.resolvedServerId),
            defaultServerId = saveServerData.defaultServerId,
        )
    }

    override fun deleteAppShortcut(index: Int): ShortcutResult<Unit> {
        if (!isShortcutsSupported) {
            return ShortcutResult.Error(ShortcutError.ApiNotSupported)
        }
        if (index !in 0 until appShortcutsDataSource.maxShortcuts) {
            return ShortcutResult.Error(ShortcutError.InvalidIndex)
        }

        return appShortcutsDataSource.delete(index)
    }

    override suspend fun upsertHomeShortcut(shortcut: ShortcutDraft): ShortcutResult<Unit> {
        if (!isShortcutsSupported) {
            return ShortcutResult.Error(ShortcutError.ApiNotSupported)
        }
        if (!homeShortcutsDataSource.canPinShortcuts) {
            return ShortcutResult.Error(ShortcutError.HomeShortcutNotSupported)
        }

        val saveServerData = when (val result = serversDataSource.resolveForSave(shortcut.serverId)) {
            is ShortcutResult.Success -> result.data
            is ShortcutResult.Error -> return ShortcutResult.Error(result.error)
        }

        return homeShortcutsDataSource.upsert(
            shortcut = shortcut.copy(serverId = saveServerData.resolvedServerId),
            defaultServerId = saveServerData.defaultServerId,
        )
    }

    override fun deleteHomeShortcut(shortcutId: String): ShortcutResult<Unit> {
        if (!isShortcutsSupported) {
            return ShortcutResult.Error(ShortcutError.ApiNotSupported)
        }
        if (!homeShortcutsDataSource.canPinShortcuts) {
            return ShortcutResult.Error(ShortcutError.HomeShortcutNotSupported)
        }
        if (shortcutId.isBlank()) {
            return ShortcutResult.Error(ShortcutError.InvalidInput)
        }

        return homeShortcutsDataSource.delete(shortcutId)
    }

    private suspend fun loadEditorContext(): ShortcutResult<EditorContext> {
        val servers = when (val result = serversDataSource.getServers()) {
            is ShortcutResult.Success -> result.data
            is ShortcutResult.Error -> return ShortcutResult.Error(result.error)
        }

        val dataById = serversDataSource.loadEditorDataByServerId(servers.servers)
        val editorServers = servers.servers.map { server ->
            ShortcutServerItem(
                server = server,
                data = dataById[server.id] ?: ServerData(),
            )
        }
        return ShortcutResult.Success(
            EditorContext(
                defaultServerId = servers.defaultServerId,
                servers = editorServers,
            ),
        )
    }
}

package io.homeassistant.companion.android.common.data.shortcuts.impl

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.common.data.shortcuts.ShortcutFactory
import io.homeassistant.companion.android.common.data.shortcuts.ShortcutIntentCodec
import io.homeassistant.companion.android.common.data.shortcuts.ShortcutsRepository
import io.homeassistant.companion.android.common.data.shortcuts.entities.AppEditorData
import io.homeassistant.companion.android.common.data.shortcuts.entities.AppShortcutSummary
import io.homeassistant.companion.android.common.data.shortcuts.entities.HomeEditorData
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDraft
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutEditorData
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutError
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutResult
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutsListData
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

    override suspend fun loadShortcutsList(): ShortcutResult<ShortcutsListData> {
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

    override suspend fun loadEditorData(): ShortcutResult<ShortcutEditorData> {
        if (!isShortcutsSupported) {
            return ShortcutResult.Error(ShortcutError.ApiNotSupported)
        }

        val servers = when (val result = serversDataSource.getServers()) {
            is ShortcutResult.Success -> result.data
            is ShortcutResult.Error -> return ShortcutResult.Error(result.error)
        }

        val dataById = serversDataSource.loadEditorDataByServerId(servers.servers)

        return ShortcutResult.Success(
            ShortcutEditorData(
                servers = servers.servers,
                serverDataById = dataById,
            ),
        )
    }

    override suspend fun loadAppEditor(index: Int): ShortcutResult<AppEditorData> {
        if (!isShortcutsSupported) {
            return ShortcutResult.Error(ShortcutError.ApiNotSupported)
        }

        val defaultServerId = when (val result = serversDataSource.getDefaultServerId()) {
            is ShortcutResult.Success -> result.data
            is ShortcutResult.Error -> return ShortcutResult.Error(result.error)
        }
        return appShortcutsDataSource.loadEditor(index, defaultServerId)
    }

    override suspend fun loadHomeEditor(shortcutId: String): ShortcutResult<HomeEditorData> {
        if (!isShortcutsSupported) {
            return ShortcutResult.Error(ShortcutError.ApiNotSupported)
        }
        if (!homeShortcutsDataSource.canPinShortcuts) {
            return ShortcutResult.Error(ShortcutError.HomeShortcutNotSupported)
        }

        val defaultServerId = when (val result = serversDataSource.getDefaultServerId()) {
            is ShortcutResult.Success -> result.data
            is ShortcutResult.Error -> return ShortcutResult.Error(result.error)
        }
        return homeShortcutsDataSource.loadEditor(shortcutId, defaultServerId)
    }

    override suspend fun saveAppShortcut(
        index: Int?,
        shortcut: ShortcutDraft,
    ): ShortcutResult<AppEditorData> {
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
}

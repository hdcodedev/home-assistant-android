package io.homeassistant.companion.android.settings.shortcuts.data.impl

import android.content.Context
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.homeassistant.companion.android.settings.shortcuts.data.AppShortcutsRepository
import io.homeassistant.companion.android.settings.shortcuts.data.ShortcutInfoFactory
import io.homeassistant.companion.android.settings.shortcuts.data.ShortcutIntentSerializer
import io.homeassistant.companion.android.settings.shortcuts.data.entities.AppShortcuts
import io.homeassistant.companion.android.settings.shortcuts.data.entities.Shortcut
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDraft
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutError
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutResult
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

// Fallback used when the system query fails. 5 matches the historical maximum on pre-API-25
// launchers and is the de-facto static-launcher limit on devices that can't be queried.
private const val DEFAULT_MAX_APP_SHORTCUTS = 5

// Legacy dynamic app shortcuts use shortcut_N IDs. Keep that bounded ID pool so existing
// shortcuts remain updateable and other dynamic shortcuts are not managed by this data source.
private const val APP_SHORTCUT_ID_PREFIX = "shortcut"

/**
 * Requires a [ShortcutInfoFactory] binding — provided by `app`'s
 * [io.homeassistant.companion.android.settings.shortcuts.di.ShortcutsModule].
 */
internal class AppShortcutsDataSource @Inject constructor(
    @ApplicationContext private val app: Context,
    private val shortcutFactory: ShortcutInfoFactory,
    private val shortcutIntentSerializer: ShortcutIntentSerializer,
) : AppShortcutsRepository {
    private val maxShortcuts: Int =
        runCatching { ShortcutManagerCompat.getMaxShortcutCountPerActivity(app) }
            .onFailure { Timber.w(it, "Failed to query max shortcut count, using fallback value") }
            .getOrNull()
            ?.takeIf { it > 0 }
            ?: DEFAULT_MAX_APP_SHORTCUTS

    override suspend fun load(): ShortcutResult<AppShortcuts> = withContext(Dispatchers.IO) {
        runCatchingShortcut("Failed to load app shortcuts") {
            val appShortcutOrder = appShortcutIds()
                .withIndex()
                .associate { (index, id) -> id to index }
            val shortcuts = ShortcutManagerCompat.getShortcuts(
                app,
                ShortcutManagerCompat.FLAG_MATCH_DYNAMIC,
            )
                .filter { it.id in appShortcutOrder }
                .sortedBy { appShortcutOrder.getValue(it.id) }

            ShortcutResult.Success(
                AppShortcuts(
                    maxAppShortcuts = maxShortcuts,
                    items = shortcuts.mapNotNull { decodeListItemOrNull(it) },
                ),
            )
        }
    }

    override suspend fun create(draft: ShortcutDraft): ShortcutResult<Unit> = withContext(Dispatchers.IO) {
        val existingIds = when (val result = loadExistingIds()) {
            is ShortcutResult.Success -> result.data
            is ShortcutResult.Error -> return@withContext ShortcutResult.Error(result.error)
        }
        val firstEmptyId =
            firstEmptyId(existingIds) ?: return@withContext ShortcutResult.Error(ShortcutError.AppShortcutSlotsFull)

        add(id = firstEmptyId, draft = draft)
    }

    override suspend fun update(id: String, draft: ShortcutDraft): ShortcutResult<Unit> = withContext(Dispatchers.IO) {
        val existingIds = when (val result = loadExistingIds()) {
            is ShortcutResult.Success -> result.data
            is ShortcutResult.Error -> return@withContext ShortcutResult.Error(result.error)
        }
        val requestedId = id.trim()
        if (requestedId !in existingIds) {
            return@withContext ShortcutResult.Error(ShortcutError.ShortcutNotFound)
        }

        updateExisting(id = requestedId, draft = draft)
    }

    override suspend fun loadEditor(id: String, defaultServerId: Int): ShortcutResult<Shortcut> =
        withContext(Dispatchers.IO) {
            val requestedId = id.trim()
            if (requestedId !in appShortcutIds()) {
                return@withContext ShortcutResult.Error(ShortcutError.ShortcutNotFound)
            }

            runCatchingShortcut("Failed to load app shortcut id=$requestedId") {
                val existingShortcut = ShortcutManagerCompat.getShortcuts(
                    app,
                    ShortcutManagerCompat.FLAG_MATCH_DYNAMIC,
                ).firstOrNull { it.id == requestedId }
                    ?: return@runCatchingShortcut ShortcutResult.Error(ShortcutError.ShortcutNotFound)

                ShortcutResult.Success(shortcutIntentSerializer.decode(existingShortcut, defaultServerId))
            }
        }

    private suspend fun add(id: String, draft: ShortcutDraft): ShortcutResult<Unit> = withContext(Dispatchers.IO) {
        runCatchingShortcut("Failed to create app shortcut id=$id") {
            val shortcutInfo = shortcutFactory.create(id, draft)
            val added = ShortcutManagerCompat.addDynamicShortcuts(app, listOf(shortcutInfo))
            if (!added) {
                Timber.w("Failed to add dynamic shortcut id=%s", id)
                ShortcutResult.Error(ShortcutError.Unknown)
            } else {
                ShortcutResult.Success(Unit)
            }
        }
    }

    private suspend fun updateExisting(id: String, draft: ShortcutDraft): ShortcutResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatchingShortcut("Failed to update app shortcut id=$id") {
                val shortcutInfo = shortcutFactory.create(id, draft)
                val updated = ShortcutManagerCompat.updateShortcuts(app, listOf(shortcutInfo))
                if (!updated) {
                    Timber.w("Failed to update dynamic shortcut id=%s", id)
                    ShortcutResult.Error(ShortcutError.Unknown)
                } else {
                    ShortcutResult.Success(Unit)
                }
            }
        }

    override suspend fun delete(id: String): ShortcutResult<Unit> = withContext(Dispatchers.IO) {
        runCatchingShortcut("Failed to delete app shortcut id=$id") {
            ShortcutManagerCompat.removeDynamicShortcuts(app, listOf(id))
            ShortcutResult.Success(Unit)
        }
    }

    private suspend fun loadExistingIds(): ShortcutResult<Set<String>> = withContext(Dispatchers.IO) {
        runCatchingShortcut("Failed to load app shortcut ids") {
            val shortcuts = ShortcutManagerCompat.getShortcuts(
                app,
                ShortcutManagerCompat.FLAG_MATCH_DYNAMIC,
            )
            val appShortcutIds = appShortcutIds().toSet()

            ShortcutResult.Success(
                shortcuts.map { it.id }
                    .filter { it in appShortcutIds }
                    .toSet(),
            )
        }
    }

    private fun firstEmptyId(existingIds: Set<String>): String? = appShortcutIds().firstOrNull { it !in existingIds }

    private fun appShortcutIds(): List<String> = (0 until maxShortcuts).map(::appShortcutId)

    private fun appShortcutId(index: Int): String = "${APP_SHORTCUT_ID_PREFIX}_${index + 1}"

    /**
     * Decodes [shortcut] into a list item, returning `null` and logging when it cannot be decoded.
     *
     * A malformed or legacy shortcut is an expected, recoverable condition: it is skipped so the rest of
     * the list still loads, rather than failing the whole load. Cancellation is propagated untouched.
     */
    private suspend fun decodeListItemOrNull(shortcut: ShortcutInfoCompat) = try {
        shortcutIntentSerializer.decodeListItem(shortcut)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Failed to decode app shortcut id=%s, skipping it", shortcut.id)
        null
    }
}

package io.homeassistant.companion.android.common.data.shortcuts.impl

import android.content.Context
import androidx.core.content.pm.ShortcutManagerCompat
import io.homeassistant.companion.android.common.data.shortcuts.ShortcutFactory
import io.homeassistant.companion.android.common.data.shortcuts.ShortcutIntentCodec
import io.homeassistant.companion.android.common.data.shortcuts.entities.AppEditorData
import io.homeassistant.companion.android.common.data.shortcuts.entities.EditorMode
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDraft
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutError
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutResult
import io.homeassistant.companion.android.common.data.shortcuts.entities.empty
import io.homeassistant.companion.android.common.data.shortcuts.entities.toDraft
import kotlinx.coroutines.CancellationException
import timber.log.Timber

private const val DEFAULT_MAX_APP_SHORTCUTS = 5
private const val APP_SHORTCUT_PREFIX = "shortcut"

internal data class AppShortcutsData(val maxAppShortcuts: Int, val shortcuts: Map<Int, ShortcutDraft>) {
    val orderedShortcuts: List<Map.Entry<Int, ShortcutDraft>> = shortcuts.entries.sortedBy { it.key }
}

private object AppShortcutId {
    fun build(index: Int): String = "${APP_SHORTCUT_PREFIX}_${index + 1}"

    fun parse(shortcutId: String): Int? {
        if (!shortcutId.startsWith("${APP_SHORTCUT_PREFIX}_")) return null
        return shortcutId.substringAfterLast("_", missingDelimiterValue = "")
            .toIntOrNull()
            ?.minus(1)
            ?.takeIf { it >= 0 }
    }
}

internal class AppShortcutsDataSource(
    private val app: Context,
    private val shortcutFactory: ShortcutFactory,
    private val shortcutIntentCodec: ShortcutIntentCodec,
    private val iconIdToName: Map<Int, String>,
) {
    val maxShortcuts: Int by lazy {
        runCatching { ShortcutManagerCompat.getMaxShortcutCountPerActivity(app) }
            .onFailure { Timber.w(it, "Failed to query max shortcut count, using fallback value") }
            .getOrNull()
            ?.takeIf { it > 0 }
            ?: DEFAULT_MAX_APP_SHORTCUTS
    }

    fun load(defaultServerId: Int): AppShortcutsData {
        val shortcuts = ShortcutManagerCompat.getShortcuts(
            app,
            ShortcutManagerCompat.FLAG_MATCH_DYNAMIC,
        )

        val shortcutsByIndex = buildMap {
            for (item in shortcuts) {
                val index = AppShortcutId.parse(item.id)

                if (index == null) {
                    Timber.w("Skipping app shortcut with unexpected id=%s", item.id)
                    continue
                }

                if (index !in 0 until maxShortcuts) {
                    Timber.w("Skipping app shortcut with out-of-range index=%d id=%s", index, item.id)
                    continue
                }

                put(index, item.toDraft(defaultServerId, iconIdToName, shortcutIntentCodec))
            }
        }

        return AppShortcutsData(
            maxAppShortcuts = maxShortcuts,
            shortcuts = shortcutsByIndex,
        )
    }

    fun save(
        index: Int?,
        shortcut: ShortcutDraft,
        defaultServerId: Int,
    ): ShortcutResult<AppEditorData> {
        val existingShortcuts = load(defaultServerId).shortcuts
        val resolvedIndex = when (val resolved = resolveIndex(index, existingShortcuts)) {
            is ShortcutResult.Success -> resolved.data
            is ShortcutResult.Error -> return ShortcutResult.Error(resolved.error)
        }

        return saveAtIndex(
            index = resolvedIndex,
            shortcut = shortcut,
            existingShortcuts = existingShortcuts,
        )
    }

    fun loadEditor(index: Int, defaultServerId: Int): ShortcutResult<AppEditorData> {
        if (index !in 0 until maxShortcuts) {
            return ShortcutResult.Error(ShortcutError.InvalidIndex)
        }

        val shortcuts = load(defaultServerId).shortcuts
        val existingDraft = shortcuts[index]
        val draft = existingDraft ?: ShortcutDraft.empty().copy(serverId = defaultServerId)

        return ShortcutResult.Success(
            AppEditorData(
                index = index,
                draftSeed = draft,
                mode = if (existingDraft != null) EditorMode.EDIT else EditorMode.CREATE,
            ),
        )
    }

    private fun saveAtIndex(
        index: Int,
        shortcut: ShortcutDraft,
        existingShortcuts: Map<Int, ShortcutDraft>,
    ): ShortcutResult<AppEditorData> {
        val expectedId = AppShortcutId.build(index)
        val existingShortcut = existingShortcuts[index]
        if (existingShortcut != null && shortcut.id != expectedId) {
            return ShortcutResult.Error(ShortcutError.SlotsFull)
        }

        return try {
            val normalized = shortcut.copy(id = expectedId)
            val shortcutInfo = shortcutFactory.createShortcutInfo(normalized)
            val added = ShortcutManagerCompat.addDynamicShortcuts(app, listOf(shortcutInfo))
            check(added) { "addDynamicShortcuts returned false" }

            ShortcutResult.Success(
                AppEditorData(
                    index = index,
                    draftSeed = normalized,
                    mode = EditorMode.EDIT,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ShortcutResult.Error(ShortcutError.Unknown, e)
        }
    }

    fun delete(index: Int): ShortcutResult<Unit> {
        return try {
            ShortcutManagerCompat.removeDynamicShortcuts(
                app,
                listOf(AppShortcutId.build(index)),
            )
            ShortcutResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ShortcutResult.Error(ShortcutError.Unknown, e)
        }
    }

    private fun resolveIndex(index: Int?, existingShortcuts: Map<Int, ShortcutDraft>): ShortcutResult<Int> {
        if (index != null) {
            return if (index in 0 until maxShortcuts) {
                ShortcutResult.Success(index)
            } else {
                ShortcutResult.Error(ShortcutError.InvalidIndex)
            }
        }

        val firstEmpty = (0 until maxShortcuts).firstOrNull { candidate ->
            !existingShortcuts.containsKey(candidate)
        } ?: return ShortcutResult.Error(ShortcutError.SlotsFull)

        return ShortcutResult.Success(firstEmpty)
    }
}

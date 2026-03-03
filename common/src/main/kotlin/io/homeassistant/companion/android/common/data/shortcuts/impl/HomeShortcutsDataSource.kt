package io.homeassistant.companion.android.common.data.shortcuts.impl

import android.content.Context
import android.os.Build
import androidx.core.content.pm.ShortcutManagerCompat
import io.homeassistant.companion.android.common.data.shortcuts.ShortcutFactory
import io.homeassistant.companion.android.common.data.shortcuts.ShortcutIntentCodec
import io.homeassistant.companion.android.common.data.shortcuts.entities.EditorMode
import io.homeassistant.companion.android.common.data.shortcuts.entities.HomeEditorData
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDraft
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutError
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutResult
import io.homeassistant.companion.android.common.data.shortcuts.entities.empty
import io.homeassistant.companion.android.common.data.shortcuts.entities.toDraft
import java.util.UUID
import kotlinx.coroutines.CancellationException
import timber.log.Timber

private const val ASSIST_SHORTCUT_PREFIX = ".ha_assist_"
private const val HOME_SHORTCUT_PREFIX = "pinned"

internal class HomeShortcutsDataSource(
    private val app: Context,
    private val shortcutFactory: ShortcutFactory,
    private val shortcutIntentCodec: ShortcutIntentCodec,
    private val iconIdToName: Map<Int, String>,
) {
    val canPinShortcuts: Boolean by lazy {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            ShortcutManagerCompat.isRequestPinShortcutSupported(app)
    }

    fun load(defaultServerId: Int): List<ShortcutDraft> {
        val homeShortcuts = ShortcutManagerCompat.getShortcuts(app, ShortcutManagerCompat.FLAG_MATCH_PINNED)
            .filter { !it.id.startsWith(ASSIST_SHORTCUT_PREFIX) }

        return homeShortcuts
            .map { it.toDraft(defaultServerId, iconIdToName, shortcutIntentCodec) }
            .reversed()
    }

    fun upsert(shortcut: ShortcutDraft, defaultServerId: Int): ShortcutResult<Unit> {
        return try {
            val normalized = if (shortcut.id.isBlank()) {
                shortcut.copy(id = newHomeId())
            } else {
                shortcut
            }
            val shortcutInfo = shortcutFactory.createShortcutInfo(normalized)
            val exists = load(defaultServerId).any { it.id == normalized.id }

            if (exists) {
                Timber.d("Updating home shortcut: ${normalized.id}")
                val updated = ShortcutManagerCompat.updateShortcuts(app, listOf(shortcutInfo))
                check(updated) { "updateShortcuts returned false" }
                ShortcutResult.Success(Unit)
            } else {
                Timber.d("Requesting pin for shortcut: ${normalized.id}")
                val requested = ShortcutManagerCompat.requestPinShortcut(app, shortcutInfo, null)
                check(requested) { "requestPinShortcut returned false" }
                ShortcutResult.Success(Unit)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ShortcutResult.Error(ShortcutError.Unknown, e)
        }
    }

    fun loadEditor(shortcutId: String, defaultServerId: Int): ShortcutResult<HomeEditorData> {
        val requestedId = shortcutId.trim()
        if (requestedId.isEmpty()) {
            return ShortcutResult.Error(ShortcutError.InvalidInput)
        }

        val homeShortcut = load(defaultServerId).firstOrNull { it.id == requestedId }
        val draft = homeShortcut ?: ShortcutDraft.empty(requestedId).copy(serverId = defaultServerId)

        return ShortcutResult.Success(
            HomeEditorData(
                draftSeed = draft,
                mode = if (homeShortcut != null) EditorMode.EDIT else EditorMode.CREATE,
            ),
        )
    }

    fun delete(shortcutId: String): ShortcutResult<Unit> {
        return try {
            ShortcutManagerCompat.disableShortcuts(app, listOf(shortcutId), null)
            ShortcutResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ShortcutResult.Error(ShortcutError.Unknown, e)
        }
    }

    private fun newHomeId(): String = "${HOME_SHORTCUT_PREFIX}_${UUID.randomUUID()}"
}

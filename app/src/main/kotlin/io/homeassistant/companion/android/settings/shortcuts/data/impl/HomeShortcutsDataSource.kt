package io.homeassistant.companion.android.settings.shortcuts.data.impl

import android.content.Context
import android.os.Build
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.homeassistant.companion.android.common.util.SdkVersion
import io.homeassistant.companion.android.settings.shortcuts.data.HomeShortcutsRepository
import io.homeassistant.companion.android.settings.shortcuts.data.ShortcutInfoFactory
import io.homeassistant.companion.android.settings.shortcuts.data.ShortcutIntentSerializer
import io.homeassistant.companion.android.settings.shortcuts.data.entities.HomeShortcutListItem
import io.homeassistant.companion.android.settings.shortcuts.data.entities.Shortcut
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDraft
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutError
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutResult
import io.homeassistant.companion.android.util.sensitive
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

// Keep in sync with AssistShortcutActivity.SHORTCUT_PREFIX in the app module. This remains here
// until we decide whether the app should provide an explicit pinned-shortcut filter.
private const val ASSIST_SHORTCUT_PREFIX = ".ha_assist_"
private const val HOME_SHORTCUT_ID_PREFIX = "home_shortcut"

/**
 * Requires a [ShortcutInfoFactory] binding — provided by `app`'s
 * [io.homeassistant.companion.android.settings.shortcuts.di.ShortcutsModule].
 */
internal class HomeShortcutsDataSource @Inject constructor(
    @ApplicationContext private val app: Context,
    private val shortcutFactory: ShortcutInfoFactory,
    private val shortcutIntentSerializer: ShortcutIntentSerializer,
) : HomeShortcutsRepository {

    override fun canPinShortcuts(): Boolean = SdkVersion.isAtLeast(Build.VERSION_CODES.O) &&
        ShortcutManagerCompat.isRequestPinShortcutSupported(app)

    override suspend fun load(): ShortcutResult<List<HomeShortcutListItem>> = withContext(Dispatchers.IO) {
        runCatchingShortcut("Failed to load home shortcuts") {
            ShortcutResult.Success(
                pinnedHomeShortcuts().reversed().mapNotNull { shortcut ->
                    decodeListItemOrNull(shortcut)?.let { listItem ->
                        HomeShortcutListItem(shortcut = listItem, isEnabled = shortcut.isEnabled)
                    }
                },
            )
        }
    }

    override suspend fun create(draft: ShortcutDraft): ShortcutResult<Unit> = withContext(Dispatchers.IO) {
        runCatchingShortcut("Failed to create home shortcut") {
            val shortcutId = newHomeId()
            val shortcutInfo = shortcutFactory.create(shortcutId, draft)
            Timber.d("Requesting pin for shortcut id=%s", sensitive(shortcutId))
            val requestAccepted = ShortcutManagerCompat.requestPinShortcut(app, shortcutInfo, null)
            if (!requestAccepted) {
                Timber.w("Home shortcut pin request was not accepted id=%s", sensitive(shortcutId))
                ShortcutResult.Error(ShortcutError.Unknown)
            } else {
                ShortcutResult.Success(Unit)
            }
        }
    }

    override suspend fun update(id: String, draft: ShortcutDraft): ShortcutResult<Unit> = withContext(Dispatchers.IO) {
        val requestedId = id.trim()
        runCatchingShortcut("Failed to update home shortcut id=${sensitive(requestedId)}") {
            val shortcutExists = pinnedHomeShortcuts().any { it.id == requestedId }
            if (!shortcutExists) {
                return@runCatchingShortcut ShortcutResult.Error(ShortcutError.ShortcutNotFound)
            }

            val shortcutInfo = shortcutFactory.create(requestedId, draft)
            Timber.d("Updating home shortcut id=%s", sensitive(requestedId))
            val updated = ShortcutManagerCompat.updateShortcuts(app, listOf(shortcutInfo))
            if (!updated) {
                Timber.w("Failed to update home shortcut id=%s", sensitive(requestedId))
                ShortcutResult.Error(ShortcutError.Unknown)
            } else {
                ShortcutResult.Success(Unit)
            }
        }
    }

    override suspend fun loadEditor(id: String, defaultServerId: Int): ShortcutResult<Shortcut> =
        withContext(Dispatchers.IO) {
            val requestedId = id.trim()
            if (requestedId.isEmpty()) {
                return@withContext ShortcutResult.Error(ShortcutError.ShortcutNotFound)
            }

            runCatchingShortcut("Failed to load home shortcut id=${sensitive(requestedId)}") {
                val homeShortcut = pinnedHomeShortcuts()
                    .firstOrNull { it.id == requestedId }
                    ?: return@runCatchingShortcut ShortcutResult.Error(ShortcutError.ShortcutNotFound)

                ShortcutResult.Success(shortcutIntentSerializer.decode(homeShortcut, defaultServerId))
            }
        }

    override suspend fun disable(id: String): ShortcutResult<Unit> = withContext(Dispatchers.IO) {
        val trimmedId = id.trim()
        runCatchingShortcut("Failed to disable home shortcut id=${sensitive(trimmedId)}") {
            ShortcutManagerCompat.disableShortcuts(app, listOf(trimmedId), null)
            ShortcutResult.Success(Unit)
        }
    }

    private fun pinnedHomeShortcuts(): List<ShortcutInfoCompat> =
        ShortcutManagerCompat.getShortcuts(app, ShortcutManagerCompat.FLAG_MATCH_PINNED)
            .filter { !it.id.startsWith(ASSIST_SHORTCUT_PREFIX) }

    private fun newHomeId(): String = "${HOME_SHORTCUT_ID_PREFIX}_${UUID.randomUUID()}"

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
        Timber.e(e, "Failed to decode home shortcut id=%s, skipping it", sensitive(shortcut.id))
        null
    }
}

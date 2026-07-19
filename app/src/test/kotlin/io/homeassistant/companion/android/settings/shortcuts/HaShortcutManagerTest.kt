package io.homeassistant.companion.android.settings.shortcuts

import android.content.Context
import android.content.Intent
import android.util.NoSuchPropertyException
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.database.IconDialogCompat
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDestination
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDraft
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutIcon
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.assertNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class HaShortcutManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = HaShortcutManager(context, IconDialogCompat(context.assets))

    companion object {
        private const val DEFAULT_SERVER_ID = 1
        private const val SHORTCUT_ID = "shortcut_1"
        private const val SHORTCUT_LABEL = "Label"
        private const val SHORTCUT_DESCRIPTION = "Description"
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Given an entity path when buildShortcutInfo then it produces a package-scoped navigate intent`() {
        val info = manager.buildShortcutInfo(
            shortcutId = "shortcut_1",
            serverId = 2,
            label = "Label",
            longLabel = "Description",
            path = "entityId:light.kitchen",
            icon = null,
        )

        val intent = info.intent
        assertEquals(Intent.ACTION_VIEW, intent.action)
        // Scoped to our app rather than a hard-coded component.
        assertEquals(context.packageName, intent.`package`)
        assertNull(intent.component)
        assertEquals("homeassistant", intent.data?.scheme)
        assertEquals("navigate", intent.data?.host)
        assertEquals("light.kitchen", intent.data?.getQueryParameter("more-info-entity-id"))
        assertEquals("2", intent.data?.getQueryParameter("server_id"))
        // Primitive extras for the edit-form round-trip.
        assertEquals(2, intent.getIntExtra("server", -1))
        assertEquals("entityId:light.kitchen", intent.getStringExtra("path"))
    }

    @Test
    fun `Given shortcut draft when create then it produces shortcut info`() {
        val info = manager.buildShortcutInfo(
            shortcutId = SHORTCUT_ID,
            draft = ShortcutDraft(
                serverId = 2,
                icon = ShortcutIcon.Mdi("mdi:flash"),
                label = SHORTCUT_LABEL,
                description = SHORTCUT_DESCRIPTION,
                destination = ShortcutDestination.Entity("light.kitchen"),
            ),
        )

        val intent = info.intent
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("light.kitchen", intent.data?.getQueryParameter("more-info-entity-id"))
        assertEquals("2", intent.data?.getQueryParameter("server_id"))
        assertEquals(2, intent.getIntExtra(SHORTCUT_EXTRA_SERVER, -1))
        assertEquals("entityId:light.kitchen", intent.getStringExtra(SHORTCUT_EXTRA_PATH))
        assertEquals("mdi:flash", intent.getStringExtra(SHORTCUT_EXTRA_ICON_NAME))
    }

    @Test
    fun `Given legacy action path when decode then returns shortcut using action destination`() = runTest {
        val shortcut = shortcutInfo(
            action = "entityId:light.kitchen",
        ) {
            putExtra(SHORTCUT_EXTRA_ICON_NAME, "mdi:flash")
        }

        val decoded = manager.decode(shortcut, DEFAULT_SERVER_ID)

        assertEquals(SHORTCUT_ID, decoded.id)
        assertEquals(DEFAULT_SERVER_ID, decoded.serverId)
        assertEquals(ShortcutIcon.Mdi("mdi:flash"), decoded.icon)
        assertEquals(SHORTCUT_LABEL, decoded.label)
        assertEquals(SHORTCUT_DESCRIPTION, decoded.description)
        assertEquals(ShortcutDestination.Entity("light.kitchen"), decoded.destination)
    }

    @Test
    fun `Given path extra and action when decode then path extra wins`() = runTest {
        val shortcut = shortcutInfo(
            action = "entityId:light.kitchen",
        ) {
            putExtra(SHORTCUT_EXTRA_SERVER, 2)
            putExtra(SHORTCUT_EXTRA_PATH, "/lovelace/home")
        }

        val decoded = manager.decode(shortcut, DEFAULT_SERVER_ID)

        assertEquals(2, decoded.serverId)
        assertEquals(ShortcutDestination.Dashboard("/lovelace/home"), decoded.destination)
    }

    @Test
    fun `Given shortcut info when decodeListItem then returns list item without destination`() = runTest {
        val shortcut = shortcutInfo(action = Intent.ACTION_VIEW) {
            putExtra(SHORTCUT_EXTRA_ICON_NAME, "mdi:flash")
        }

        val decoded = manager.decodeListItem(shortcut)

        assertEquals(SHORTCUT_ID, decoded.id)
        assertEquals(SHORTCUT_LABEL, decoded.label)
        assertEquals(ShortcutIcon.Mdi("mdi:flash"), decoded.icon)
    }

    @Test
    fun `Given legacy icon id when decodeListItem then maps id through streaming lookup`() = runTest {
        val manager = managerWith(iconIdToName = mapOf(42 to "flash"))
        val shortcut = shortcutInfo(action = Intent.ACTION_VIEW) {
            putExtra(SHORTCUT_EXTRA_ICON_ID, 42)
        }

        val decoded = manager.decodeListItem(shortcut)

        assertEquals(ShortcutIcon.Mdi("mdi:flash"), decoded.icon)
    }

    @Test
    fun `Given modern icon name and legacy icon id when decodeListItem then modern value wins`() = runTest {
        val manager = managerWith(iconIdToName = mapOf(42 to "legacy"))
        val shortcut = shortcutInfo(action = Intent.ACTION_VIEW) {
            putExtra(SHORTCUT_EXTRA_ICON_NAME, "modern")
            putExtra(SHORTCUT_EXTRA_ICON_ID, 42)
        }

        val decoded = manager.decodeListItem(shortcut)

        assertEquals(ShortcutIcon.Mdi("mdi:modern"), decoded.icon)
    }

    @Test
    fun `Given ACTION_VIEW shortcut without path extra when decode then fails instead of treating action as path`() = runTest {
        val shortcut = shortcutInfo(action = Intent.ACTION_VIEW)

        val error = runCatching { manager.decode(shortcut, DEFAULT_SERVER_ID) }.exceptionOrNull()

        assertInstanceOf(IllegalArgumentException::class.java, error)
        assertEquals("Shortcut destination path is missing", error?.message)
    }

    @Test
    fun `Given blank path extra and ACTION_VIEW action when decode then fails instead of treating action as path`() = runTest {
        val shortcut = shortcutInfo(action = Intent.ACTION_VIEW) {
            putExtra(SHORTCUT_EXTRA_PATH, "")
        }

        val error = runCatching { manager.decode(shortcut, DEFAULT_SERVER_ID) }.exceptionOrNull()

        assertInstanceOf(IllegalArgumentException::class.java, error)
        assertEquals("Shortcut destination path is missing", error?.message)
    }

    @Test
    fun `Given an intent without icon extras when resolveIconFromIntent then returns null`() = runTest {
        assertNull(manager.resolveIconFromIntent(Intent()))
    }

    @Test
    fun `Given an unknown legacy iconId when resolveIconFromIntent then returns null instead of throwing`() = runTest {
        val manager = managerWith(iconIdToName = emptyMap())
        val intent = Intent().putExtra("iconId", Int.MAX_VALUE)
        assertNull(manager.resolveIconFromIntent(intent))
    }

    @Test
    fun `Given a legacy WebViewActivity shortcut when migrateLegacyShortcuts then it is rewritten to a navigate intent`() = runTest {
        mockkStatic(ShortcutManagerCompat::class)
        // Legacy shortcuts set the action to the path and stored path/server as extras.
        val legacyIntent = Intent("entityId:light.kitchen").apply {
            putExtra("server", 2)
            putExtra("path", "entityId:light.kitchen")
        }
        val legacy = ShortcutInfoCompat.Builder(context, "shortcut_1")
            .setShortLabel("Label")
            .setLongLabel("Description")
            .setIntent(legacyIntent)
            .build()
        every { ShortcutManagerCompat.getShortcuts(any(), any()) } returns listOf(legacy)
        val updated = slot<List<ShortcutInfoCompat>>()
        every { ShortcutManagerCompat.updateShortcuts(any(), capture(updated)) } returns true

        manager.migrateLegacyShortcuts()

        val migrated = updated.captured.single().intent
        assertEquals(Intent.ACTION_VIEW, migrated.action)
        assertEquals(context.packageName, migrated.`package`)
        assertEquals("light.kitchen", migrated.data?.getQueryParameter("more-info-entity-id"))
        assertEquals(2, migrated.getIntExtra("server", -1))
    }

    @Test
    fun `Given a shortcut already on the navigate format when migrateLegacyShortcuts then it is not updated`() = runTest {
        mockkStatic(ShortcutManagerCompat::class)
        val current = manager.buildShortcutInfo(
            shortcutId = "shortcut_1",
            serverId = 2,
            label = "Label",
            longLabel = "Description",
            path = "entityId:light.kitchen",
            icon = null,
        )
        every { ShortcutManagerCompat.getShortcuts(any(), any()) } returns listOf(current)

        manager.migrateLegacyShortcuts()

        verify(exactly = 0) { ShortcutManagerCompat.updateShortcuts(any(), any()) }
    }

    private fun shortcutInfo(
        action: String,
        applyExtras: Intent.() -> Unit = {},
    ): ShortcutInfoCompat {
        val intent = Intent(action).apply(applyExtras)
        return ShortcutInfoCompat.Builder(context, SHORTCUT_ID)
            .setShortLabel(SHORTCUT_LABEL)
            .setLongLabel(SHORTCUT_DESCRIPTION)
            .setIntent(intent)
            .build()
    }

    private fun managerWith(iconIdToName: Map<Int, String>): HaShortcutManager {
        val iconDialog = mockk<IconDialogCompat>()
        coEvery { iconDialog.streamingIconLookup(any()) } answers {
            iconIdToName[firstArg()] ?: throw NoSuchPropertyException("unknown")
        }
        return HaShortcutManager(context, iconDialog)
    }
}

package io.homeassistant.companion.android.settings.shortcuts.data.impl

import android.content.Context
import android.os.Build
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import io.homeassistant.companion.android.common.util.SdkVersion
import io.homeassistant.companion.android.settings.shortcuts.data.ShortcutInfoFactory
import io.homeassistant.companion.android.settings.shortcuts.data.ShortcutIntentSerializer
import io.homeassistant.companion.android.settings.shortcuts.data.entities.HomeShortcutListItem
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDraft
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutError
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutListItem
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutResult
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class HomeShortcutsDataSourceTest {
    private val context = mockk<Context>(relaxed = true)
    private val shortcutFactory = mockk<ShortcutInfoFactory>(relaxed = true)
    private val shortcutIntentSerializer = mockk<ShortcutIntentSerializer>(relaxed = true)

    @BeforeEach
    fun setUp() {
        mockkStatic(ShortcutManagerCompat::class)
        every { ShortcutManagerCompat.getShortcuts(context, any()) } returns emptyList()
    }

    @AfterEach
    fun tearDown() {
        SdkVersion.resetSdkInt()
        unmockkAll()
    }

    private fun dataSource() = HomeShortcutsDataSource(
        app = context,
        shortcutFactory = shortcutFactory,
        shortcutIntentSerializer = shortcutIntentSerializer,
    )

    @Test
    fun `Given home shortcut permission changes while app is open when checking support then returns current value`() {
        SdkVersion.sdkInt = Build.VERSION_CODES.O
        var pinningSupported = true
        every { ShortcutManagerCompat.isRequestPinShortcutSupported(context) } answers { pinningSupported }
        val dataSource = dataSource()

        assertEquals(true, dataSource.canPinShortcuts())

        pinningSupported = false

        assertEquals(false, dataSource.canPinShortcuts())
    }

    @Test
    fun `Given disabled home shortcut when loaded then preserves launcher state`() = runTest {
        val platformShortcut = testShortcutInfoCompat(
            id = "home_1",
            label = "Home",
            isEnabled = false,
        )
        val shortcut = ShortcutListItem(
            id = "home_1",
            label = "Home",
        )
        every {
            ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
        } returns listOf(platformShortcut)
        coEvery { shortcutIntentSerializer.decodeListItem(platformShortcut) } returns shortcut
        val dataSource = dataSource()

        val result = dataSource.load()

        assertEquals(false, (result as ShortcutResult.Success<List<HomeShortcutListItem>>).data.single().isEnabled)
    }

    @Test
    fun `Given one home shortcut fails to decode when load then skips it and returns the rest`() = runTest {
        val failing = testShortcutInfoCompat(id = "home_1")
        val working = testShortcutInfoCompat(id = "home_2")
        val workingItem = ShortcutListItem(id = "home_2", label = "home_2")
        every {
            ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
        } returns listOf(failing, working)
        coEvery { shortcutIntentSerializer.decodeListItem(failing) } throws IllegalArgumentException("bad shortcut")
        coEvery { shortcutIntentSerializer.decodeListItem(working) } returns workingItem
        val dataSource = dataSource()

        val result = dataSource.load()

        val items = (result as ShortcutResult.Success<List<HomeShortcutListItem>>).data
        assertEquals(listOf(workingItem), items.map { it.shortcut })
    }

    @Test
    fun `Given home shortcut draft when create then requests launcher pin`() = runTest {
        val draft = testDraft()
        val shortcutId = slot<String>()
        val shortcutInfo = mockk<ShortcutInfoCompat>()
        every { shortcutFactory.create(capture(shortcutId), draft) } returns shortcutInfo
        every { ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null) } returns true
        val dataSource = dataSource()

        val result = dataSource.create(draft)

        assertEquals(ShortcutResult.Success(Unit), result)
        assertEquals(true, shortcutId.captured.startsWith("home_shortcut_"))
        verify { ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null) }
    }

    @Test
    fun `Given home shortcut exists when update then updates the pinned shortcut`() = runTest {
        val draft = testDraft()
        val shortcutInfo = mockk<ShortcutInfoCompat>()
        every {
            ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
        } returns listOf(testShortcutInfoCompat(id = "home_1"))
        every { shortcutFactory.create("home_1", draft) } returns shortcutInfo
        every { ShortcutManagerCompat.updateShortcuts(context, listOf(shortcutInfo)) } returns true
        val dataSource = dataSource()

        val result = dataSource.update(id = "home_1", draft = draft)

        assertEquals(ShortcutResult.Success(Unit), result)
        verify { shortcutFactory.create("home_1", draft) }
        verify { ShortcutManagerCompat.updateShortcuts(context, listOf(shortcutInfo)) }
    }

    @Test
    fun `Given home shortcut id when disable then disables the pinned shortcut`() = runTest {
        every { ShortcutManagerCompat.disableShortcuts(context, listOf("home_1"), null) } just Runs
        val dataSource = dataSource()

        val result = dataSource.disable("home_1")

        assertEquals(ShortcutResult.Success(Unit), result)
        verify { ShortcutManagerCompat.disableShortcuts(context, listOf("home_1"), null) }
    }

    @Test
    fun `Given home shortcut id is missing when loadEditor then returns ShortcutNotFound`() = runTest {
        val dataSource = dataSource()

        val result = dataSource.loadEditor("missing", defaultServerId = 1)

        assertEquals(ShortcutError.ShortcutNotFound, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given home shortcut id is missing when update then returns ShortcutNotFound`() = runTest {
        val dataSource = dataSource()

        val result = dataSource.update(id = "missing", draft = testDraft())

        assertEquals(ShortcutError.ShortcutNotFound, (result as ShortcutResult.Error).error)
        verify(exactly = 0) { shortcutFactory.create(any(), any()) }
        verify(exactly = 0) { ShortcutManagerCompat.updateShortcuts(context, any()) }
    }

    @Test
    fun `Given home shortcut id is blank when loadEditor then returns ShortcutNotFound`() = runTest {
        val dataSource = dataSource()

        val result = dataSource.loadEditor(" ", defaultServerId = 1)

        assertEquals(ShortcutError.ShortcutNotFound, (result as ShortcutResult.Error).error)
    }
}

private fun testShortcutInfoCompat(
    id: String,
    label: CharSequence = id,
    isEnabled: Boolean = true,
): ShortcutInfoCompat {
    val shortcut = mockk<ShortcutInfoCompat>()
    every { shortcut.id } returns id
    every { shortcut.shortLabel } returns label
    every { shortcut.isEnabled } returns isEnabled
    return shortcut
}

private fun testDraft(): ShortcutDraft = ShortcutDraft.initial(serverId = 0)

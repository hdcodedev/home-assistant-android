package io.homeassistant.companion.android.settings.shortcuts.data.impl

import android.content.Context
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import io.homeassistant.companion.android.settings.shortcuts.data.ShortcutInfoFactory
import io.homeassistant.companion.android.settings.shortcuts.data.ShortcutIntentSerializer
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
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AppShortcutsDataSourceTest {
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
        unmockkAll()
    }

    private fun dataSource() = AppShortcutsDataSource(
        app = context,
        shortcutFactory = shortcutFactory,
        shortcutIntentSerializer = shortcutIntentSerializer,
    )

    @Test
    fun `Given app shortcuts when load then returns decoded list items in shortcut slot order`() = runTest {
        val shortcut1 = testShortcutInfoCompat(id = "shortcut_1")
        val shortcut2 = testShortcutInfoCompat(id = "shortcut_2")
        val ignored = testShortcutInfoCompat(id = "other")
        val item1 = ShortcutListItem(id = "shortcut_1", label = "First")
        val item2 = ShortcutListItem(id = "shortcut_2", label = "Second")
        every {
            ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_DYNAMIC)
        } returns listOf(shortcut2, ignored, shortcut1)
        coEvery { shortcutIntentSerializer.decodeListItem(shortcut1) } returns item1
        coEvery { shortcutIntentSerializer.decodeListItem(shortcut2) } returns item2
        val dataSource = dataSource()

        val result = dataSource.load()

        assertEquals(listOf(item1, item2), (result as ShortcutResult.Success).data.items)
    }

    @Test
    fun `Given one app shortcut fails to decode when load then skips it and returns the rest`() = runTest {
        val shortcut1 = testShortcutInfoCompat(id = "shortcut_1")
        val shortcut2 = testShortcutInfoCompat(id = "shortcut_2")
        val item2 = ShortcutListItem(id = "shortcut_2", label = "Second")
        every {
            ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_DYNAMIC)
        } returns listOf(shortcut1, shortcut2)
        coEvery { shortcutIntentSerializer.decodeListItem(shortcut1) } throws IllegalArgumentException("bad shortcut")
        coEvery { shortcutIntentSerializer.decodeListItem(shortcut2) } returns item2
        val dataSource = dataSource()

        val result = dataSource.load()

        assertEquals(listOf(item2), (result as ShortcutResult.Success).data.items)
    }

    @Test
    fun `Given app shortcut slots have a gap when create then adds first empty slot`() = runTest {
        val draft = testDraft()
        val shortcutInfo = mockk<ShortcutInfoCompat>()
        every {
            ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_DYNAMIC)
        } returns listOf(
            testShortcutInfoCompat(id = "shortcut_1"),
            testShortcutInfoCompat(id = "shortcut_3"),
        )
        every { shortcutFactory.create("shortcut_2", draft) } returns shortcutInfo
        every { ShortcutManagerCompat.addDynamicShortcuts(context, listOf(shortcutInfo)) } returns true
        val dataSource = dataSource()

        val result = dataSource.create(draft)

        assertEquals(ShortcutResult.Success(Unit), result)
        verify { shortcutFactory.create("shortcut_2", draft) }
        verify { ShortcutManagerCompat.addDynamicShortcuts(context, listOf(shortcutInfo)) }
    }

    @Test
    fun `Given app shortcut exists when update then updates the dynamic shortcut`() = runTest {
        val draft = testDraft()
        val shortcutInfo = mockk<ShortcutInfoCompat>()
        every {
            ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_DYNAMIC)
        } returns listOf(testShortcutInfoCompat(id = "shortcut_1"))
        every { shortcutFactory.create("shortcut_1", draft) } returns shortcutInfo
        every { ShortcutManagerCompat.updateShortcuts(context, listOf(shortcutInfo)) } returns true
        val dataSource = dataSource()

        val result = dataSource.update(id = "shortcut_1", draft = draft)

        assertEquals(ShortcutResult.Success(Unit), result)
        verify { shortcutFactory.create("shortcut_1", draft) }
        verify { ShortcutManagerCompat.updateShortcuts(context, listOf(shortcutInfo)) }
    }

    @Test
    fun `Given app shortcut id when delete then removes the dynamic shortcut`() = runTest {
        every { ShortcutManagerCompat.removeDynamicShortcuts(context, listOf("shortcut_1")) } just Runs
        val dataSource = dataSource()

        val result = dataSource.delete("shortcut_1")

        assertEquals(ShortcutResult.Success(Unit), result)
        verify { ShortcutManagerCompat.removeDynamicShortcuts(context, listOf("shortcut_1")) }
    }

    @Test
    fun `Given app shortcut slot is empty when loadEditor then returns ShortcutNotFound`() = runTest {
        val dataSource = dataSource()

        val result = dataSource.loadEditor("shortcut_1", defaultServerId = 1)

        assertEquals(ShortcutError.ShortcutNotFound, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given app shortcut index is out of range when loadEditor then returns ShortcutNotFound`() = runTest {
        val dataSource = dataSource()

        val result = dataSource.loadEditor("shortcut_6", defaultServerId = 1)

        assertEquals(ShortcutError.ShortcutNotFound, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given app shortcut index is out of range when update then returns ShortcutNotFound`() = runTest {
        val dataSource = dataSource()

        val result = dataSource.update(id = "shortcut_6", draft = testDraft())

        assertEquals(ShortcutError.ShortcutNotFound, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given app shortcut slot is empty when update then returns ShortcutNotFound`() = runTest {
        val dataSource = dataSource()

        val result = dataSource.update(id = "shortcut_1", draft = testDraft())

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

package io.homeassistant.companion.android.settings.shortcuts

import android.os.Build
import io.homeassistant.companion.android.common.util.SdkVersion
import io.homeassistant.companion.android.settings.shortcuts.data.AppShortcutsRepository
import io.homeassistant.companion.android.settings.shortcuts.data.HomeShortcutsRepository
import io.homeassistant.companion.android.settings.shortcuts.data.ShortcutServersRepository
import io.homeassistant.companion.android.settings.shortcuts.data.entities.AppShortcuts
import io.homeassistant.companion.android.settings.shortcuts.data.entities.HomeShortcutListItem
import io.homeassistant.companion.android.settings.shortcuts.data.entities.Shortcut
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutError
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutListItem
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutResult
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutServer
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutServersSnapshot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class LoadShortcutsUseCaseTest {
    private val appShortcutsRepository: AppShortcutsRepository = mockk(relaxed = true)
    private val homeShortcutsRepository: HomeShortcutsRepository = mockk(relaxed = true)
    private val shortcutServersRepository: ShortcutServersRepository = mockk(relaxed = true)
    private val defaultServerId = 1

    private lateinit var useCase: LoadShortcutsUseCase

    @BeforeEach
    fun setUp() {
        coEvery { shortcutServersRepository.loadServers() } returns ShortcutResult.Success(shortcutServers())
        useCase = newUseCase()
    }

    @AfterEach
    fun tearDown() {
        SdkVersion.resetSdkInt()
        unmockkAll()
    }

    @Test
    fun `Given Android version not supported when invoked then returns AndroidVersionNotSupported`() = runTest {
        useCase = newUseCase(sdkInt = Build.VERSION_CODES.M)

        val result = useCase()

        assertTrue(result is ShortcutResult.Error)
        assertEquals(ShortcutError.AndroidVersionNotSupported, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given no servers when invoked then returns NoServersConfigured`() = runTest {
        coEvery { shortcutServersRepository.loadServers() } returns ShortcutResult.Error(ShortcutError.NoServersConfigured)

        val result = useCase()

        assertTrue(result is ShortcutResult.Error)
        assertEquals(ShortcutError.NoServersConfigured, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given app shortcuts load fails when invoked then returns error`() = runTest {
        coEvery { appShortcutsRepository.load() } returns ShortcutResult.Error(ShortcutError.Unknown)

        val result = useCase()

        assertTrue(result is ShortcutResult.Error)
        assertEquals(ShortcutError.Unknown, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given pinning not supported when invoked then returns home shortcuts unsupported`() = runTest {
        coEvery { appShortcutsRepository.load() } returns ShortcutResult.Success(appShortcuts())
        every { homeShortcutsRepository.canPinShortcuts() } returns false

        val data = (useCase() as ShortcutResult.Success).data

        assertFalse(data.homeShortcuts.canPinShortcuts)
        assertTrue(data.homeShortcuts.items.isEmpty())
    }

    @Test
    fun `Given pinning supported when invoked then returns both shortcut lists`() = runTest {
        coEvery { appShortcutsRepository.load() } returns ShortcutResult.Success(appShortcuts())
        every { homeShortcutsRepository.canPinShortcuts() } returns true
        coEvery { homeShortcutsRepository.load() } returns ShortcutResult.Success(
            homeShortcuts(
                shortcuts = listOf(
                    HomeShortcutListItem(
                        shortcut = ShortcutListItem(id = "home_1", label = "Kitchen"),
                        isEnabled = true,
                    ),
                ),
            ),
        )

        val data = (useCase() as ShortcutResult.Success).data

        assertTrue(data.homeShortcuts.canPinShortcuts)
        assertEquals(listOf("home_1"), data.homeShortcuts.items.map { it.shortcut.id })
    }

    @Test
    fun `Given home shortcuts load fails when invoked then returns error`() = runTest {
        coEvery { appShortcutsRepository.load() } returns ShortcutResult.Success(appShortcuts())
        every { homeShortcutsRepository.canPinShortcuts() } returns true
        coEvery { homeShortcutsRepository.load() } returns ShortcutResult.Error(ShortcutError.Unknown)

        val result = useCase()

        assertTrue(result is ShortcutResult.Error)
        assertEquals(ShortcutError.Unknown, (result as ShortcutResult.Error).error)
    }

    private fun newUseCase(sdkInt: Int = Build.VERSION_CODES.N_MR1): LoadShortcutsUseCase {
        SdkVersion.sdkInt = sdkInt
        return LoadShortcutsUseCase(
            appShortcutsRepository = appShortcutsRepository,
            homeShortcutsRepository = homeShortcutsRepository,
            shortcutServersRepository = shortcutServersRepository,
        )
    }

    private fun shortcutServers() = ShortcutServersSnapshot(
        servers = listOf(shortcutServer(defaultServerId)),
        defaultServer = shortcutServer(defaultServerId),
    )

    private fun shortcutServer(id: Int) = ShortcutServer(
        id = id,
        name = "Home",
        supportsEntity = true,
    )
}

private fun appShortcuts(
    maxAppShortcuts: Int = 5,
    shortcuts: List<Shortcut> = emptyList(),
) = AppShortcuts(
    maxAppShortcuts = maxAppShortcuts,
    items = shortcuts.map {
        ShortcutListItem(
            id = it.id,
            label = it.label,
            icon = it.icon,
        )
    },
)

private fun homeShortcuts(
    shortcuts: List<HomeShortcutListItem> = emptyList(),
) = shortcuts

package io.homeassistant.companion.android.settings.shortcuts

import android.os.Build
import io.homeassistant.companion.android.common.util.SdkVersion
import io.homeassistant.companion.android.settings.shortcuts.data.AppShortcutsRepository
import io.homeassistant.companion.android.settings.shortcuts.data.HomeShortcutsRepository
import io.homeassistant.companion.android.settings.shortcuts.data.ShortcutServersRepository
import io.homeassistant.companion.android.settings.shortcuts.data.entities.Shortcut
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDestination
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDraft
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutError
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutResult
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutServer
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutServersSnapshot
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class LoadShortcutEditorUseCaseTest {
    private val appShortcutsRepository: AppShortcutsRepository = mockk(relaxed = true)
    private val homeShortcutsRepository: HomeShortcutsRepository = mockk(relaxed = true)
    private val shortcutServersRepository: ShortcutServersRepository = mockk(relaxed = true)
    private val defaultServerId = 1

    private lateinit var useCase: LoadShortcutEditorUseCase

    @BeforeEach
    fun setUp() {
        coEvery { shortcutServersRepository.loadServers() } returns ShortcutResult.Success(
            shortcutServers(defaultServerId),
        )
        useCase = newUseCase()
    }

    @AfterEach
    fun tearDown() {
        SdkVersion.resetSdkInt()
        unmockkAll()
    }

    @Test
    fun `Given Android version not supported when load new shortcut then returns AndroidVersionNotSupported`() = runTest {
        useCase = newUseCase(sdkInt = Build.VERSION_CODES.M)

        val result = useCase.loadNewShortcut()

        assertTrue(result is ShortcutResult.Error)
        assertEquals(ShortcutError.AndroidVersionNotSupported, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given no servers when load new shortcut then returns NoServersConfigured`() = runTest {
        coEvery { shortcutServersRepository.loadServers() } returns ShortcutResult.Error(ShortcutError.NoServersConfigured)

        val result = useCase.loadNewShortcut()

        assertTrue(result is ShortcutResult.Error)
        assertEquals(ShortcutError.NoServersConfigured, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given servers when load new shortcut then returns initial editor`() = runTest {
        val data = (useCase.loadNewShortcut() as ShortcutResult.Success).data

        assertEquals(ShortcutDraft.initial(defaultServerId), data.draft)
        assertEquals(listOf(defaultServerId), data.servers.map { it.id })
    }

    @Test
    fun `Given editor load fails when load app shortcut then returns error`() = runTest {
        coEvery {
            appShortcutsRepository.loadEditor(any(), any())
        } returns ShortcutResult.Error(ShortcutError.ShortcutNotFound)

        val result = useCase.loadAppShortcut("shortcut_1")

        assertTrue(result is ShortcutResult.Error)
        assertEquals(ShortcutError.ShortcutNotFound, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given valid app shortcut when loaded then returns editor data`() = runTest {
        val existing = persistedShortcut("shortcut_1").copy(label = "Existing")
        coEvery {
            appShortcutsRepository.loadEditor("shortcut_1", defaultServerId)
        } returns ShortcutResult.Success(existing)

        val result = useCase.loadAppShortcut("shortcut_1")
        val data = (result as ShortcutResult.Success).data

        assertEquals("Existing", data.draft.label)
    }

    @Test
    fun `Given shortcut server is unavailable when load app shortcut then uses default server`() = runTest {
        val existing = persistedShortcut("shortcut_1").copy(serverId = 99)
        coEvery {
            appShortcutsRepository.loadEditor("shortcut_1", defaultServerId)
        } returns ShortcutResult.Success(existing)

        val result = useCase.loadAppShortcut("shortcut_1")
        val data = (result as ShortcutResult.Success).data

        assertEquals(defaultServerId, data.draft.serverId)
        assertEquals(listOf(defaultServerId), data.servers.map { it.id })
    }

    @Test
    fun `Given entity shortcut on unsupported server when load app shortcut then preserves destination`() = runTest {
        coEvery { shortcutServersRepository.loadServers() } returns ShortcutResult.Success(
            ShortcutServersSnapshot(
                servers = listOf(shortcutServer(defaultServerId, supportsEntity = false)),
                defaultServer = shortcutServer(defaultServerId, supportsEntity = false),
            ),
        )
        val existing = persistedShortcut("shortcut_1").copy(
            destination = ShortcutDestination.Entity("light.kitchen"),
        )
        coEvery {
            appShortcutsRepository.loadEditor("shortcut_1", defaultServerId)
        } returns ShortcutResult.Success(existing)

        val result = useCase.loadAppShortcut("shortcut_1")
        val data = (result as ShortcutResult.Success).data

        assertEquals(ShortcutDestination.Entity("light.kitchen"), data.draft.destination)
    }

    @Test
    fun `Given valid home shortcut when loaded then returns editor data`() = runTest {
        val existing = persistedShortcut("pinned_abc").copy(label = "Pinned")
        coEvery {
            homeShortcutsRepository.loadEditor("pinned_abc", defaultServerId)
        } returns ShortcutResult.Success(existing)

        val result = useCase.loadHomeShortcut("pinned_abc")
        val data = (result as ShortcutResult.Success).data

        assertEquals("Pinned", data.draft.label)
    }

    @Test
    fun `Given home shortcut disappeared when loaded then returns ShortcutNotFound`() = runTest {
        coEvery {
            homeShortcutsRepository.loadEditor("missing", defaultServerId)
        } returns ShortcutResult.Error(ShortcutError.ShortcutNotFound)

        val result = useCase.loadHomeShortcut("missing")

        assertTrue(result is ShortcutResult.Error)
        assertEquals(ShortcutError.ShortcutNotFound, (result as ShortcutResult.Error).error)
    }

    private fun newUseCase(sdkInt: Int = Build.VERSION_CODES.N_MR1): LoadShortcutEditorUseCase {
        SdkVersion.sdkInt = sdkInt
        return LoadShortcutEditorUseCase(
            appShortcutsRepository = appShortcutsRepository,
            homeShortcutsRepository = homeShortcutsRepository,
            shortcutServersRepository = shortcutServersRepository,
        )
    }

    private fun shortcutServers(defaultServerId: Int) = ShortcutServersSnapshot(
        servers = listOf(shortcutServer(defaultServerId)),
        defaultServer = shortcutServer(defaultServerId),
    )

    private fun shortcutServer(
        id: Int,
        supportsEntity: Boolean = true,
    ) = ShortcutServer(
        id = id,
        name = "Home",
        supportsEntity = supportsEntity,
    )

    private fun persistedShortcut(id: String) = Shortcut(
        id = id,
        serverId = defaultServerId,
        label = "Test",
        description = "",
        destination = ShortcutDestination.Entity("light.kitchen"),
    )
}

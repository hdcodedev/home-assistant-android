package io.homeassistant.companion.android.settings.shortcuts.v2

import io.homeassistant.companion.android.common.data.shortcuts.ShortcutsRepository
import io.homeassistant.companion.android.common.data.shortcuts.entities.AppShortcutSummary
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDraft
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutError
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutResult
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDestination
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutsListData
import io.homeassistant.companion.android.common.data.shortcuts.entities.toSummary
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.database.server.ServerConnectionInfo
import io.homeassistant.companion.android.database.server.ServerSessionInfo
import io.homeassistant.companion.android.database.server.ServerUserInfo
import io.homeassistant.companion.android.testing.unit.ConsoleLogExtension
import io.homeassistant.companion.android.testing.unit.MainDispatcherJUnit5Extension
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherJUnit5Extension::class, ConsoleLogExtension::class)
class ManageShortcutsViewModelTest {

    private val shortcutsRepository: ShortcutsRepository = mockk()

    private val server = Server(
        id = 1,
        _name = "Home",
        connection = ServerConnectionInfo(externalUrl = "https://example.com"),
        session = ServerSessionInfo(),
        user = ServerUserInfo(),
    )

    @BeforeEach
    fun setup() {
        stubList(
            ShortcutsListData(
                maxAppShortcuts = 5,
                appShortcuts = listOf(
                    appShortcutSummary(index = 0, id = "shortcut_1", serverId = server.id),
                    appShortcutSummary(index = 2, id = "shortcut_3", serverId = server.id),
                ),
                homeShortcuts = listOf(buildDraft(id = "home_1", serverId = server.id).toSummary()),
            ),
        )
    }

    private fun stubList(data: ShortcutsListData) {
        coEvery { shortcutsRepository.loadShortcutsList() } returns ShortcutResult.Success(data)
    }

    private fun TestScope.createVm(): ManageShortcutsViewModel {
        val vm = ManageShortcutsViewModel(shortcutsRepository)
        advanceUntilIdle()
        return vm
    }

    @Test
    fun `Given shortcuts when init then state has items`() = runTest {
        val viewModel = createVm()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isEmpty)
        assertTrue(state.isHomeSupported)
        assertEquals(5, state.maxAppShortcuts)
        assertEquals(listOf(0, 2), state.appShortcutItems.map { it.index })
        assertEquals(1, state.homeShortcutItems.size)
    }

    @Test
    fun `Given empty shortcuts when init then empty state shown`() = runTest {
        stubList(
            ShortcutsListData(
                maxAppShortcuts = 5,
                appShortcuts = emptyList(),
                homeShortcuts = emptyList(),
            ),
        )

        val viewModel = createVm()
        assertTrue(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun `Given home not supported when init then home support is false`() = runTest {
        stubList(
            ShortcutsListData(
                maxAppShortcuts = 5,
                appShortcuts = emptyList(),
                homeShortcuts = emptyList(),
                homeShortcutsError = ShortcutError.HomeShortcutNotSupported,
            ),
        )

        val viewModel = createVm()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isHomeSupported)
    }

    @Test
    fun `Given home not supported but app has shortcuts when init then isEmpty is false`() = runTest {
        stubList(
            ShortcutsListData(
                maxAppShortcuts = 5,
                appShortcuts = listOf(appShortcutSummary(index = 0, id = "s1", serverId = server.id)),
                homeShortcuts = emptyList(),
                homeShortcutsError = ShortcutError.HomeShortcutNotSupported,
            ),
        )

        val viewModel = createVm()
        assertFalse(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun `Given load error when init then error state shown`() = runTest {
        coEvery { shortcutsRepository.loadShortcutsList() } returns ShortcutResult.Error(
            ShortcutError.NoServers,
        )

        val viewModel = createVm()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(ShortcutError.NoServers, state.error)
    }

    @Test
    fun `Given app shortcuts with gaps when init then items have correct indexes`() = runTest {
        stubList(
            ShortcutsListData(
                maxAppShortcuts = 5,
                appShortcuts = listOf(
                    appShortcutSummary(index = 0, id = "s0", serverId = server.id),
                    appShortcutSummary(index = 3, id = "s3", serverId = server.id),
                ),
                homeShortcuts = emptyList(),
            ),
        )

        val viewModel = createVm()
        assertEquals(2, viewModel.uiState.value.appShortcutItems.size)
        assertEquals(listOf(0, 3), viewModel.uiState.value.appShortcutItems.map { it.index })
    }

    private fun buildDraft(id: String, serverId: Int): ShortcutDraft {
        return ShortcutDraft(
            id = id,
            serverId = serverId,
            selectedIconName = null,
            label = id,
            description = "Description for $id",
            destination = ShortcutDestination.Lovelace("/lovelace/$id"),
        )
    }

    private fun appShortcutSummary(index: Int, id: String, serverId: Int): AppShortcutSummary {
        return AppShortcutSummary(
            index = index,
            summary = buildDraft(id = id, serverId = serverId).toSummary(),
        )
    }
}

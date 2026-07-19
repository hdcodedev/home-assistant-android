package io.homeassistant.companion.android.settings.shortcuts

import app.cash.turbine.test
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.database.server.ServerConnectionInfo
import io.homeassistant.companion.android.database.server.ServerSessionInfo
import io.homeassistant.companion.android.database.server.ServerUserInfo
import io.homeassistant.companion.android.settings.shortcuts.data.entities.AppShortcuts
import io.homeassistant.companion.android.settings.shortcuts.data.entities.HomeShortcutListItem
import io.homeassistant.companion.android.settings.shortcuts.data.entities.HomeShortcuts
import io.homeassistant.companion.android.settings.shortcuts.data.entities.Shortcut
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDestination
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutError
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutListItem
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutResult
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutsListData
import io.homeassistant.companion.android.testing.unit.MainDispatcherJUnit5Extension
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherJUnit5Extension::class)
class ManageShortcutsViewModelTest {

    private val loadShortcutsUseCase: LoadShortcutsUseCase = mockk()

    private val server = Server(
        id = 1,
        _name = "Home",
        connection = ServerConnectionInfo(externalUrl = "https://example.com"),
        session = ServerSessionInfo(),
        user = ServerUserInfo(),
    )

    private fun createViewModel(): ManageShortcutsViewModel {
        return ManageShortcutsViewModel(loadShortcutsUseCase)
    }

    private fun stubList(data: ShortcutsListData) {
        coEvery { loadShortcutsUseCase() } returns ShortcutResult.Success(data)
    }

    @BeforeEach
    fun setup() {
        stubList(
            ShortcutsListData(
                appShortcuts = AppShortcuts(
                    items = listOf(
                        appShortcutItem(id = "shortcut_1", serverId = server.id),
                        appShortcutItem(id = "shortcut_3", serverId = server.id),
                    ),
                    maxAppShortcuts = 5,
                ),
                homeShortcuts = HomeShortcuts(
                    items = listOf(
                        HomeShortcutListItem(
                            shortcut = ShortcutListItem(
                                id = "home_1",
                                label = "home_1",
                            ),
                            isEnabled = true,
                        ),
                    ),
                    canPinShortcuts = true,
                ),
            ),
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Given shortcuts when init then state has items`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertTrue(awaitItem().loadState is ManageShortcutsLoadState.Loading)

            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state.loadState is ManageShortcutsLoadState.Ready)
            assertFalse(state.isEmpty)
            assertTrue(state.isHomeSupported)
            assertEquals(5, state.appShortcuts.maxAppShortcuts)
            assertEquals(listOf("shortcut_1", "shortcut_3"), state.appShortcuts.items.map { it.id })
            assertEquals(1, state.homeShortcuts.items.size)
            expectNoEvents()
        }
    }

    @Test
    fun `Given empty shortcuts when init then empty state shown`() = runTest {
        stubList(
            ShortcutsListData(
                appShortcuts = AppShortcuts(items = emptyList(), maxAppShortcuts = 5),
                homeShortcuts = HomeShortcuts(items = emptyList(), canPinShortcuts = true),
            ),
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertTrue(awaitItem().loadState is ManageShortcutsLoadState.Loading)

            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state.loadState is ManageShortcutsLoadState.Ready)
            assertTrue(state.isEmpty)
            expectNoEvents()
        }
    }

    @Test
    fun `Given home not supported when init then home support is false`() = runTest {
        stubList(
            ShortcutsListData(
                appShortcuts = AppShortcuts(items = emptyList(), maxAppShortcuts = 5),
                homeShortcuts = HomeShortcuts(items = emptyList(), canPinShortcuts = false),
            ),
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertTrue(awaitItem().loadState is ManageShortcutsLoadState.Loading)

            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state.loadState is ManageShortcutsLoadState.Ready)
            assertFalse(state.isHomeSupported)
            expectNoEvents()
        }
    }

    @Test
    fun `Given home not supported but app has shortcuts when init then isEmpty is false`() = runTest {
        stubList(
            ShortcutsListData(
                appShortcuts = AppShortcuts(
                    items = listOf(appShortcutItem(id = "s1", serverId = server.id)),
                    maxAppShortcuts = 5,
                ),
                homeShortcuts = HomeShortcuts(items = emptyList(), canPinShortcuts = false),
            ),
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertTrue(awaitItem().loadState is ManageShortcutsLoadState.Loading)

            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state.loadState is ManageShortcutsLoadState.Ready)
            assertFalse(state.isEmpty)
            expectNoEvents()
        }
    }

    @Test
    fun `Given load error when init then error state shown`() = runTest {
        coEvery { loadShortcutsUseCase() } returns ShortcutResult.Error(
            ShortcutError.NoServersConfigured,
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertTrue(awaitItem().loadState is ManageShortcutsLoadState.Loading)

            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state.loadState is ManageShortcutsLoadState.Error)
            assertEquals(ShortcutError.NoServersConfigured, (state.loadState as ManageShortcutsLoadState.Error).error)
            expectNoEvents()
        }
    }

    @Test
    fun `Given initial load still in flight when refreshSilently called then initial error is shown`() = runTest {
        val initialResult = CompletableDeferred<ShortcutResult<ShortcutsListData>>()
        coEvery { loadShortcutsUseCase() } coAnswers { initialResult.await() }

        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertTrue(awaitItem().loadState is ManageShortcutsLoadState.Loading)

            advanceUntilIdle()
            viewModel.refreshSilently()
            expectNoEvents()

            initialResult.complete(ShortcutResult.Error(ShortcutError.NoServersConfigured))

            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state.loadState is ManageShortcutsLoadState.Error)
            assertEquals(
                ShortcutError.NoServersConfigured,
                (state.loadState as ManageShortcutsLoadState.Error).error,
            )
            expectNoEvents()
        }
    }

    @Test
    fun `Given loaded shortcuts when refresh called then state is updated`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertTrue(awaitItem().loadState is ManageShortcutsLoadState.Loading)
            advanceUntilIdle()
            assertEquals(2, awaitItem().appShortcuts.items.size)

            stubList(
                ShortcutsListData(
                    appShortcuts = AppShortcuts(
                        items = listOf(
                            appShortcutItem(id = "new_1", serverId = server.id),
                            appShortcutItem(id = "new_2", serverId = server.id),
                            appShortcutItem(id = "new_3", serverId = server.id),
                        ),
                        maxAppShortcuts = 5,
                    ),
                    homeShortcuts = HomeShortcuts(items = emptyList(), canPinShortcuts = true),
                ),
            )

            viewModel.refresh()

            advanceUntilIdle()
            val loadingState = awaitItem()
            assertTrue(loadingState.loadState is ManageShortcutsLoadState.Loading)
            assertEquals(2, loadingState.appShortcuts.items.size)

            val state = awaitItem()
            assertTrue(state.loadState is ManageShortcutsLoadState.Ready)
            assertFalse(state.hasError)
            assertEquals(3, state.appShortcuts.items.size)
            assertEquals(listOf("new_1", "new_2", "new_3"), state.appShortcuts.items.map { it.id })
            expectNoEvents()
        }
    }

    @Test
    fun `Given loaded shortcuts when refreshSilently called then loading is not shown`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertTrue(awaitItem().loadState is ManageShortcutsLoadState.Loading)
            advanceUntilIdle()
            val initialState = awaitItem()
            assertTrue(initialState.loadState is ManageShortcutsLoadState.Ready)
            assertEquals(2, initialState.appShortcuts.items.size)

            stubList(
                ShortcutsListData(
                    appShortcuts = AppShortcuts(
                        items = listOf(appShortcutItem(id = "s1", serverId = server.id)),
                        maxAppShortcuts = 5,
                    ),
                    homeShortcuts = HomeShortcuts(items = emptyList(), canPinShortcuts = true),
                ),
            )

            viewModel.refreshSilently()

            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state.loadState is ManageShortcutsLoadState.Ready)
            assertFalse(state.hasError)
            assertEquals(1, state.appShortcuts.items.size)
            expectNoEvents()
        }
    }

    @Test
    fun `Given loaded shortcuts when refreshSilently fails then error is not shown and existing data is preserved`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertTrue(awaitItem().loadState is ManageShortcutsLoadState.Loading)
            advanceUntilIdle()
            val initialState = awaitItem()
            assertEquals(2, initialState.appShortcuts.items.size)
            assertEquals(1, initialState.homeShortcuts.items.size)

            coEvery { loadShortcutsUseCase() } returns ShortcutResult.Error(
                ShortcutError.Unknown,
            )

            viewModel.refreshSilently()

            advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun `Given loaded shortcuts when refresh fails then error is shown and existing data is preserved`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertTrue(awaitItem().loadState is ManageShortcutsLoadState.Loading)
            advanceUntilIdle()
            assertEquals(2, awaitItem().appShortcuts.items.size)

            coEvery { loadShortcutsUseCase() } returns ShortcutResult.Error(
                ShortcutError.NoServersConfigured,
            )

            viewModel.refresh()

            advanceUntilIdle()
            val loadingState = awaitItem()
            assertTrue(loadingState.loadState is ManageShortcutsLoadState.Loading)
            assertEquals(2, loadingState.appShortcuts.items.size)
            assertEquals(1, loadingState.homeShortcuts.items.size)

            val state = awaitItem()
            assertTrue(state.loadState is ManageShortcutsLoadState.Error)
            assertEquals(ShortcutError.NoServersConfigured, (state.loadState as ManageShortcutsLoadState.Error).error)
            assertTrue(state.hasError)
            assertEquals(2, state.appShortcuts.items.size)
            assertEquals(1, state.homeShortcuts.items.size)
            expectNoEvents()
        }
    }

    private fun shortcut(id: String, serverId: Int): Shortcut {
        return Shortcut(
            id = id,
            serverId = serverId,
            label = id,
            description = "Description for $id",
            destination = ShortcutDestination.Dashboard("/lovelace/$id"),
        )
    }

    private fun appShortcutItem(id: String, serverId: Int): ShortcutListItem {
        val shortcut = shortcut(id = id, serverId = serverId)
        return ShortcutListItem(
            id = id,
            label = shortcut.label,
            icon = shortcut.icon,
        )
    }
}

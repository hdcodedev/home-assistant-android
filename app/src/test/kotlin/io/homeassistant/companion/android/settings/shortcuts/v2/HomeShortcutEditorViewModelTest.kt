package io.homeassistant.companion.android.settings.shortcuts.v2

import app.cash.turbine.turbineScope
import io.homeassistant.companion.android.common.data.shortcuts.ShortcutsRepository
import io.homeassistant.companion.android.common.data.shortcuts.entities.ServerData
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutServerItem
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDraft
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDestination
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutData
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutError
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutMode
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutResult
import io.homeassistant.companion.android.common.data.shortcuts.entities.empty
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.database.server.ServerConnectionInfo
import io.homeassistant.companion.android.database.server.ServerSessionInfo
import io.homeassistant.companion.android.database.server.ServerUserInfo
import io.homeassistant.companion.android.settings.shortcuts.v2.views.screens.ShortcutEditAction
import io.homeassistant.companion.android.testing.unit.ConsoleLogExtension
import io.homeassistant.companion.android.testing.unit.MainDispatcherJUnit5Extension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherJUnit5Extension::class, ConsoleLogExtension::class)
class HomeShortcutEditorViewModelTest {

    private val shortcutsRepository: ShortcutsRepository = mockk()

    private val server = Server(
        id = 1,
        _name = "Home",
        connection = ServerConnectionInfo(externalUrl = "https://example.com"),
        session = ServerSessionInfo(),
        user = ServerUserInfo(),
    )

    private val homeShortcutDraft = ShortcutDraft(
        id = "home_1",
        serverId = server.id,
        selectedIconName = null,
        label = "Home",
        description = "Home shortcut",
        destination = ShortcutDestination.Lovelace("/lovelace/home"),
    )

    @BeforeEach
    fun setup() {
        coEvery { shortcutsRepository.loadHomeShortcut(homeShortcutDraft.id) } returns ShortcutResult.Success(
            ShortcutData(servers = buildEditorServers(), draftSeed = homeShortcutDraft, mode = ShortcutMode.EDIT),
        )
        coEvery { shortcutsRepository.draftHomeShortcutEditor() } returns ShortcutResult.Success(
            ShortcutData(
                servers = buildEditorServers(),
                draftSeed = ShortcutDraft.empty().copy(serverId = server.id),
                mode = ShortcutMode.CREATE,
            ),
        )
        coEvery { shortcutsRepository.upsertHomeShortcut(any()) } returns ShortcutResult.Success(Unit)
        every { shortcutsRepository.deleteHomeShortcut(any()) } returns ShortcutResult.Success(Unit)
    }

    @Test
    fun `Given no servers when openCreateHomeShortcut then screen error is set`() = runTest {
        coEvery { shortcutsRepository.draftHomeShortcutEditor() } returns ShortcutResult.Error(
            ShortcutError.NoServers,
        )

        val viewModel = createVm()
        viewModel.draftHomeShortcutEditor()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.content.isLoading)
        assertEquals(ShortcutError.NoServers, viewModel.uiState.value.content.error)
    }

    @Test
    fun `Given home shortcut exists when openEditHomeShortcut then editor is HomeEdit with correct id`() = runTest {
        val viewModel = createVm()

        viewModel.openEditHomeShortcut(homeShortcutDraft.id)
        advanceUntilIdle()

        val editor = viewModel.uiState.value.editor as ShortcutEditorUiState.HomeEditState
        assertEquals(homeShortcutDraft.id, editor.initialDraft.id)
    }

    @Test
    fun `Given home editor load error when openEditHomeShortcut then screen error is set`() = runTest {
        coEvery { shortcutsRepository.loadHomeShortcut(homeShortcutDraft.id) } returns ShortcutResult.Error(
            ShortcutError.Unknown,
        )
        val viewModel = createVm()

        viewModel.openEditHomeShortcut(homeShortcutDraft.id)
        advanceUntilIdle()

        assertEquals(ShortcutError.Unknown, viewModel.uiState.value.content.error)
    }

    @Test
    fun `Given error when openCreateHomeShortcut then error is cleared`() = runTest {
        coEvery { shortcutsRepository.loadHomeShortcut(homeShortcutDraft.id) } returns ShortcutResult.Error(
            ShortcutError.Unknown,
        )
        val viewModel = createVm()
        viewModel.openEditHomeShortcut(homeShortcutDraft.id)
        advanceUntilIdle()
        assertEquals(ShortcutError.Unknown, viewModel.uiState.value.content.error)

        viewModel.draftHomeShortcutEditor()

        val editor = viewModel.uiState.value.editor as ShortcutEditorUiState.HomeCreateState
        assertEquals(null, viewModel.uiState.value.content.error)
        assertEquals(ShortcutDraft.empty().copy(serverId = server.id), editor.initialDraft)
    }

    @Test
    fun `Given home shortcut when submit then close event emitted`() = runTest {
        val viewModel = createVm()
        viewModel.openEditHomeShortcut(homeShortcutDraft.id)
        advanceUntilIdle()

        assertCloseEmitted(viewModel) {
            viewModel.dispatch(ShortcutEditAction.Submit(homeShortcutDraft))
        }

        coVerify { shortcutsRepository.upsertHomeShortcut(match { it.id == homeShortcutDraft.id }) }
    }

    @Test
    fun `Given home shortcut edit when delete then close event emitted`() = runTest {
        val viewModel = createVm()
        viewModel.openEditHomeShortcut(homeShortcutDraft.id)
        advanceUntilIdle()

        assertCloseEmitted(viewModel) {
            viewModel.dispatch(ShortcutEditAction.DeleteHomeShortcut(homeShortcutDraft.id))
        }

        verify { shortcutsRepository.deleteHomeShortcut(homeShortcutDraft.id) }
    }

    @Test
    fun `Given home shortcut delete error when delete then screen error set and no close event`() = runTest {
        every { shortcutsRepository.deleteHomeShortcut(any()) } returns ShortcutResult.Error(
            ShortcutError.Unknown,
        )
        val viewModel = createVm()
        viewModel.openEditHomeShortcut(homeShortcutDraft.id)
        advanceUntilIdle()

        turbineScope {
            val closeEvents = viewModel.closeEvents.testIn(backgroundScope)

            viewModel.dispatch(ShortcutEditAction.DeleteHomeShortcut(homeShortcutDraft.id))
            advanceUntilIdle()

            assertEquals(ShortcutError.Unknown, viewModel.uiState.value.content.error)
            closeEvents.expectNoEvents()
        }
    }

    private fun TestScope.createVm(): ShortcutEditorViewModel {
        val vm = ShortcutEditorViewModel(shortcutsRepository)
        advanceUntilIdle()
        return vm
    }

    private suspend fun TestScope.assertCloseEmitted(viewModel: ShortcutEditorViewModel, action: suspend () -> Unit) {
        turbineScope {
            val closeEvents = viewModel.closeEvents.testIn(backgroundScope)
            action()
            advanceUntilIdle()
            closeEvents.awaitItem()
        }
    }

    private fun buildEditorServers(): List<ShortcutServerItem> {
        return listOf(
            ShortcutServerItem(
                server = server,
                data = ServerData(),
            ),
        )
    }
}

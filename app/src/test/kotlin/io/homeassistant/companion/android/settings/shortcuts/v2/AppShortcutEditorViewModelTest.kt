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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherJUnit5Extension::class, ConsoleLogExtension::class)
class AppShortcutEditorViewModelTest {

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
        coEvery { shortcutsRepository.loadAppShortcut(0) } returns ShortcutResult.Success(
            ShortcutData(
                servers = buildEditorServers(),
                draftSeed = buildDraft(id = appShortcutId(0), serverId = server.id),
                mode = ShortcutMode.EDIT,
            ),
        )
        coEvery { shortcutsRepository.draftAppShortcutEditor() } returns ShortcutResult.Success(
            ShortcutData(
                servers = buildEditorServers(),
                draftSeed = ShortcutDraft.empty().copy(serverId = server.id),
                mode = ShortcutMode.CREATE,
            ),
        )
        coEvery { shortcutsRepository.createAppShortcut(any()) } returns ShortcutResult.Success(Unit)
        coEvery { shortcutsRepository.updateAppShortcut(any(), any()) } returns ShortcutResult.Success(Unit)
        every { shortcutsRepository.deleteAppShortcut(any()) } returns ShortcutResult.Success(Unit)
    }

    @Test
    fun `Given no servers when openCreateAppShortcut then screen error is set`() = runTest {
        coEvery { shortcutsRepository.draftAppShortcutEditor() } returns ShortcutResult.Error(
            ShortcutError.NoServers,
        )

        val viewModel = createVm()
        viewModel.draftAppShortcutEditor()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.content.isLoading)
        assertEquals(ShortcutError.NoServers, viewModel.uiState.value.content.error)
    }

    @Test
    fun `Given app shortcut exists when openEditAppShortcut then editor is AppEdit with correct index`() = runTest {
        val viewModel = createVm()

        viewModel.openEditAppShortcut(0)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val editor = state.editor as ShortcutEditorUiState.AppEditState
        assertFalse(state.content.isLoading)
        assertEquals(0, editor.appIndex)
    }

    @Test
    fun `Given app editor load error when openEditAppShortcut then screen error is set`() = runTest {
        coEvery { shortcutsRepository.loadAppShortcut(0) } returns ShortcutResult.Error(
            ShortcutError.SlotsFull,
        )
        val viewModel = createVm()

        viewModel.openEditAppShortcut(0)
        advanceUntilIdle()

        assertEquals(ShortcutError.SlotsFull, viewModel.uiState.value.content.error)
    }

    @Test
    fun `Given error when openCreateAppShortcut then error is cleared`() = runTest {
        coEvery { shortcutsRepository.loadAppShortcut(0) } returns ShortcutResult.Error(
            ShortcutError.SlotsFull,
        )
        val viewModel = createVm()
        viewModel.openEditAppShortcut(0)
        advanceUntilIdle()
        assertEquals(ShortcutError.SlotsFull, viewModel.uiState.value.content.error)

        viewModel.draftAppShortcutEditor()

        val editor = viewModel.uiState.value.editor as ShortcutEditorUiState.AppCreateState
        assertEquals(null, viewModel.uiState.value.content.error)
        assertEquals(ShortcutDraft.empty().copy(serverId = server.id), editor.initialDraft)
    }

    @ParameterizedTest
    @CsvSource("true", "false")
    fun `Given app shortcut when submit then save uses correct index and draft`(expectedIsEditing: Boolean) = runTest {
        val index = if (expectedIsEditing) 0 else 1
        val draft = buildDraft(
            id = appShortcutId(index),
            serverId = server.id,
        ).copy(
            label = "Updated",
            description = "Updated description",
            destination = ShortcutDestination.Entity("light.kitchen"),
        )
        val viewModel = createVm()
        if (expectedIsEditing) {
            viewModel.openEditAppShortcut(index)
        } else {
            viewModel.draftAppShortcutEditor()
            advanceUntilIdle()
        }

        assertCloseEmitted(viewModel) {
            viewModel.dispatch(ShortcutEditAction.Submit(draft))
        }

        if (expectedIsEditing) {
            coVerify { shortcutsRepository.updateAppShortcut(index, draft) }
        } else {
            coVerify { shortcutsRepository.createAppShortcut(draft) }
        }
    }

    @Test
    fun `Given app shortcut submit error when submit then screen error set`() = runTest {
        coEvery { shortcutsRepository.updateAppShortcut(any(), any()) } returns ShortcutResult.Error(
            ShortcutError.SlotsFull,
        )
        val viewModel = createVm()
        viewModel.openEditAppShortcut(0)
        advanceUntilIdle()

        viewModel.dispatch(
            ShortcutEditAction.Submit(
                buildDraft(
                    id = appShortcutId(0),
                    serverId = server.id,
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(ShortcutError.SlotsFull, viewModel.uiState.value.content.error)
    }

    @Test
    fun `Given app shortcut edit when delete then close event emitted`() = runTest {
        val viewModel = createVm()
        viewModel.openEditAppShortcut(0)
        advanceUntilIdle()

        assertCloseEmitted(viewModel) {
            viewModel.dispatch(ShortcutEditAction.DeleteAppShortcut(0))
        }

        verify { shortcutsRepository.deleteAppShortcut(0) }
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

    private fun appShortcutId(index: Int): String {
        return "shortcut_${index + 1}"
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

    private fun buildEditorServers(): List<ShortcutServerItem> {
        return listOf(
            ShortcutServerItem(
                server = server,
                data = ServerData(),
            ),
        )
    }
}

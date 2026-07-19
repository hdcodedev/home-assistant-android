package io.homeassistant.companion.android.settings.shortcuts

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.data.integration.display.EntityDisplayState
import io.homeassistant.companion.android.common.data.integration.display.GetEntitiesForDisplayUseCase
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.database.server.ServerConnectionInfo
import io.homeassistant.companion.android.database.server.ServerSessionInfo
import io.homeassistant.companion.android.database.server.ServerUserInfo
import io.homeassistant.companion.android.settings.shortcuts.data.entities.Shortcut
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDestination
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDraft
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutError
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutIcon
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutResult
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutServer
import io.homeassistant.companion.android.settings.shortcuts.data.entities.toDraft
import io.homeassistant.companion.android.testing.unit.MainDispatcherJUnit5Extension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherJUnit5Extension::class)
internal class ShortcutEditorViewModelTest {

    private val loadShortcutEditorUseCase: LoadShortcutEditorUseCase = mockk()
    private val modifyShortcutUseCase: ModifyShortcutUseCase = mockk()
    private val getEntitiesForDisplay: GetEntitiesForDisplayUseCase = mockk()
    private val server = testServer()
    private val appShortcut = shortcut("shortcut_1", "App")
    private val homeShortcut = shortcut("home_1", "Home")

    @BeforeEach
    fun setup() {
        givenCreateEditor()
        givenEditEditor(ShortcutKind.APP, appShortcut)
        givenEditEditor(ShortcutKind.HOME, homeShortcut)
        coEvery { modifyShortcutUseCase(any()) } returns ShortcutResult.Success(Unit)
        givenDisplayState(server.id, EntityDisplayState.Loaded(emptyList()))
        givenDisplayState(2, EntityDisplayState.Loaded(emptyList()))
    }

    @Nested
    inner class CreateLoad {

        @Test
        fun `Given create route when ViewModel starts then editor uses default server`() = runTest {
            val viewModel = newViewModel(EditorRoute.Create(ShortcutKind.HOME))
            runCurrent()

            assertEquals(ShortcutDraft.initial(server.id), viewModel.draft)
        }

        @Test
        fun `Given create load error when ViewModel starts then error is shown`() = runTest {
            coEvery { loadShortcutEditorUseCase.loadNewShortcut() } returns
                ShortcutResult.Error(ShortcutError.NoServersConfigured)
            val viewModel = newViewModel()

            runCurrent()

            assertEquals(
                ShortcutsUiState.LoadError(ShortcutError.NoServersConfigured),
                viewModel.uiState.value,
            )
        }

        @Test
        fun `Given dashboard-only server when started then entity display state is not requested`() = runTest {
            givenCreateEditor(servers = listOf(dashboardOnlyServer(server.id)))
            val viewModel = newViewModel()

            runCurrent()

            assertTrue(viewModel.editorState.entityDisplayStatesByServerId.isEmpty())
            coVerify(exactly = 0) { getEntitiesForDisplay(any()) }
        }

        @Test
        fun `Given draft update for another server when updateDraft then update is ignored`() = runTest {
            givenCreateEditor(draft = validShortcut(), servers = editorServers() + entityServer(2))
            val viewModel = loadedCreateVm()
            val originalDraft = viewModel.draft

            viewModel.updateDraft(
                validShortcut().copy(
                    serverId = 2,
                    destination = ShortcutDestination.Entity("light.kitchen"),
                ),
            )
            runCurrent()

            assertEquals(originalDraft, viewModel.draft)
        }
    }

    @Nested
    inner class CreateSubmit {

        @ParameterizedTest
        @EnumSource(ShortcutKind::class)
        fun `Given draft cannot submit when createShortcut then modification is not submitted`(
            kind: ShortcutKind,
        ) = runTest {
            val viewModel = loadedCreateVm()

            assertNoSubmission(viewModel) {
                createShortcut(kind)
            }
        }

        @ParameterizedTest
        @EnumSource(ShortcutKind::class)
        fun `Given shortcut when createShortcut then modification is submitted and close is emitted`(
            kind: ShortcutKind,
        ) = runTest {
            val shortcut = validShortcut(label = kind.name)
            val viewModel = loadedCreateVm()

            viewModel.updateDraft(shortcut)
            viewModel.closeEvents.test {
                viewModel.createShortcut(kind)
                runCurrent()
                assertEquals(createSuccessMessageRes(kind), awaitItem().messageRes)
            }

            coVerify {
                modifyShortcutUseCase(ShortcutModification.Create(kind = kind, draft = shortcut))
            }
        }

        @Test
        fun `Given create running when submitted again then modification is submitted once`() = runTest {
            val result = CompletableDeferred<ShortcutResult<Unit>>()
            coEvery { modifyShortcutUseCase(any()) } coAnswers { result.await() }
            val viewModel = loadedCreateVmWithValidDraft()

            viewModel.createAppShortcut()
            viewModel.createAppShortcut()
            runCurrent()

            coVerify(exactly = 1) {
                modifyShortcutUseCase(match { it is ShortcutModification.Create && it.kind == ShortcutKind.APP })
            }
            result.complete(ShortcutResult.Success(Unit))
            runCurrent()
        }

        @Test
        fun `Given valid draft when createShortcut then isSaving toggles true during save and back to false`() = runTest {
            val result = CompletableDeferred<ShortcutResult<Unit>>()
            coEvery { modifyShortcutUseCase(any()) } coAnswers { result.await() }
            val viewModel = loadedCreateVmWithValidDraft()

            viewModel.createAppShortcut()
            runCurrent()

            assertTrue(viewModel.editorState.isSaving)

            result.complete(ShortcutResult.Success(Unit))
            runCurrent()

            assertFalse(viewModel.editorState.isSaving)
        }

        @ParameterizedTest
        @EnumSource(ShortcutKind::class)
        fun `Given slots full error when createShortcut then specific error is emitted`(
            kind: ShortcutKind,
        ) = runTest {
            givenModifyError(ShortcutError.AppShortcutSlotsFull)
            val viewModel = loadedCreateVmWithValidDraft()

            assertSnackbar(viewModel, commonR.string.shortcut_dynamic_slots_full) { createShortcut(kind) }
        }

        @ParameterizedTest
        @EnumSource(ShortcutKind::class)
        fun `Given unknown error when createShortcut then generic create error is emitted`(
            kind: ShortcutKind,
        ) = runTest {
            givenModifyError(ShortcutError.Unknown)
            val viewModel = loadedCreateVmWithValidDraft()

            assertSnackbar(viewModel, commonR.string.shortcut_create_error) { createShortcut(kind) }
        }

        @Test
        fun `Given shortcut not found when createAppShortcut then not found error is emitted`() = runTest {
            givenModifyError(ShortcutError.ShortcutNotFound)
            val viewModel = loadedCreateVmWithValidDraft()

            assertSnackbar(viewModel, commonR.string.shortcut_not_found) { createShortcut(ShortcutKind.APP) }
        }

        @Test
        fun `Given home pinning unsupported when createHomeShortcut then pin not supported error is emitted`() = runTest {
            givenModifyError(ShortcutError.HomeShortcutPinningNotSupported)
            val viewModel = loadedCreateVmWithValidDraft()

            assertSnackbar(viewModel, commonR.string.shortcut_pin_not_supported) { createShortcut(ShortcutKind.HOME) }
        }
    }

    @Nested
    inner class EditLoad {

        @ParameterizedTest
        @EnumSource(ShortcutKind::class)
        fun `Given edit route when ViewModel starts then draft is loaded`(kind: ShortcutKind) = runTest {
            val shortcut = shortcutFor(kind)
            val viewModel = newViewModel(EditorRoute.Edit(kind = kind, id = shortcut.id))
            runCurrent()

            assertEquals(shortcut.toDraft(), viewModel.draft)
        }

        @ParameterizedTest
        @EnumSource(ShortcutKind::class)
        fun `Given missing shortcut when ViewModel starts then error is shown`(kind: ShortcutKind) = runTest {
            val shortcut = shortcutFor(kind)
            givenEditError(kind = kind, id = shortcut.id, error = ShortcutError.ShortcutNotFound)
            val viewModel = newViewModel(EditorRoute.Edit(kind = kind, id = shortcut.id))

            runCurrent()

            assertEquals(
                ShortcutsUiState.LoadError(ShortcutError.ShortcutNotFound),
                viewModel.uiState.value,
            )
        }

        @Test
        fun `Given overlapping loads when newer completes then stale result is ignored`() = runTest {
            val firstResult = CompletableDeferred<ShortcutResult<ShortcutEditor>>()
            val newer = appShortcut.copy(label = "Newer")
            var calls = 0
            coEvery {
                loadShortcutEditorUseCase.loadAppShortcut(appShortcut.id)
            } coAnswers {
                if (calls++ == 0) firstResult.await() else ShortcutResult.Success(editData(newer))
            }
            val viewModel = newViewModel(EditorRoute.Edit(kind = ShortcutKind.APP, id = appShortcut.id))

            runCurrent()
            viewModel.retry()
            runCurrent()
            firstResult.complete(ShortcutResult.Success(editData(appShortcut)))
            runCurrent()

            assertEquals(newer.toDraft(), viewModel.draft)
        }

        @ParameterizedTest
        @EnumSource(ShortcutKind::class)
        fun `Given draft cannot submit when updateShortcut then modification is not submitted`(
            kind: ShortcutKind,
        ) = runTest {
            val shortcut = shortcutFor(kind)
            val viewModel = loadedEditVm(kind)
            viewModel.updateDraft(shortcut.toDraft().copy(label = ""))

            assertNoSubmission(viewModel) {
                updateShortcut(kind, shortcut.id)
            }
        }
    }

    @Nested
    inner class EditSubmit {

        @ParameterizedTest
        @EnumSource(ShortcutKind::class)
        fun `Given edited draft when updateShortcut then modification receives draft`(kind: ShortcutKind) = runTest {
            val shortcut = shortcutFor(kind)
            val viewModel = loadedEditVm(kind)
            val editedDraft = shortcut.copy(label = "Updated").toDraft()
            viewModel.updateDraft(editedDraft)

            assertModificationAndClose(
                viewModel = viewModel,
                expectedModification = ShortcutModification.Update(
                    kind = kind,
                    id = shortcut.id,
                    draft = editedDraft,
                ),
            ) {
                updateShortcut(kind, shortcut.id)
            }
        }

        @ParameterizedTest
        @EnumSource(ShortcutKind::class)
        fun `Given shortcut when removeShortcut then remove modification is submitted`(kind: ShortcutKind) = runTest {
            val shortcut = shortcutFor(kind)
            val viewModel = loadedEditVm(kind)

            assertModificationAndClose(
                viewModel = viewModel,
                expectedModification = ShortcutModification.Remove(kind = kind, id = shortcut.id),
            ) {
                removeShortcut(kind, shortcut.id)
            }
        }

        @ParameterizedTest
        @EnumSource(ShortcutKind::class)
        fun `Given unknown error when updateShortcut then generic update error is emitted and editor remains visible`(
            kind: ShortcutKind,
        ) = runTest {
            val shortcut = shortcutFor(kind)
            givenModifyError(ShortcutError.Unknown)
            val viewModel = loadedEditVm(kind)

            assertSnackbar(viewModel, commonR.string.shortcut_update_error) { updateShortcut(kind, shortcut.id) }

            assertTrue(viewModel.uiState.value is ShortcutsUiState.Editor)
        }

        @ParameterizedTest
        @EnumSource(ShortcutKind::class)
        fun `Given remove error when removeShortcut then error is emitted`(kind: ShortcutKind) = runTest {
            val shortcut = shortcutFor(kind)
            givenModifyError(ShortcutError.Unknown)
            val viewModel = loadedEditVm(kind)

            assertSnackbar(viewModel, removeErrorRes(kind)) { removeShortcut(kind, shortcut.id) }
        }
    }

    @Nested
    inner class ServerSelection {

        @Test
        fun `Given entity server when selectServer then display state is loaded for that server`() = runTest {
            givenCreateEditor(draft = validShortcut(), servers = editorServers() + entityServer(2))
            val viewModel = loadedCreateVm()

            viewModel.selectServer(2)
            runCurrent()

            assertTrue(2 in viewModel.editorState.entityDisplayStatesByServerId)
        }

        @Test
        fun `Given dashboard-only server when selectServer then display state is not requested`() = runTest {
            givenCreateEditor(draft = validShortcut(), servers = editorServers() + dashboardOnlyServer(2))
            val viewModel = loadedCreateVm()

            viewModel.selectServer(2)
            runCurrent()

            assertEquals(2, viewModel.draft.serverId)
            coVerify(exactly = 0) { getEntitiesForDisplay(2) }
        }

        @Test
        fun `Given cached display state when server is reselected then it is not reloaded`() = runTest {
            givenCreateEditor(draft = validShortcut(), servers = editorServers() + entityServer(2))
            val viewModel = loadedCreateVm()

            viewModel.selectServer(2)
            runCurrent()

            viewModel.selectServer(server.id)
            runCurrent()

            // The display state for the initial server was already loaded during startup, so it is reused.
            coVerify(exactly = 1) { getEntitiesForDisplay(server.id) }
        }
    }

    @Nested
    inner class DisplayState {

        @Test
        fun `Given display state load fails when started then entity error is emitted`() = runTest {
            givenDisplayState(server.id, EntityDisplayState.Error)
            val viewModel = newViewModel()

            viewModel.errorSnackbar.test {
                runCurrent()
                assertTrue(viewModel.editorState.entityDisplayStatesByServerId[server.id] is EntityDisplayState.Error)
                assertEquals(commonR.string.shortcut_entity_error_title, awaitItem())
            }
        }
    }

    private val ShortcutEditorViewModel.editorState: EditorState
        get() = (uiState.value as ShortcutsUiState.Editor).state

    private val ShortcutEditorViewModel.draft: ShortcutDraft
        get() = editorState.draft

    private fun newViewModel(route: EditorRoute = EditorRoute.Create(ShortcutKind.APP)) = ShortcutEditorViewModel(
        editorRoute = route,
        loadShortcutEditorUseCase = loadShortcutEditorUseCase,
        modifyShortcutUseCase = modifyShortcutUseCase,
        getEntitiesForDisplay = getEntitiesForDisplay,
    )

    private fun givenCreateEditor(
        draft: ShortcutDraft = ShortcutDraft.initial(server.id),
        servers: List<ShortcutServer> = editorServers(),
    ) {
        coEvery { loadShortcutEditorUseCase.loadNewShortcut() } returns ShortcutResult.Success(
            ShortcutEditor(
                draft = draft,
                servers = servers,
            ),
        )
    }

    private fun givenEditEditor(
        kind: ShortcutKind,
        shortcut: Shortcut,
        servers: List<ShortcutServer> = editorServers(),
    ) {
        val result = ShortcutResult.Success(
            ShortcutEditor(
                draft = shortcut.toDraft(),
                servers = servers,
            ),
        )
        when (kind) {
            ShortcutKind.APP -> coEvery { loadShortcutEditorUseCase.loadAppShortcut(shortcut.id) } returns result
            ShortcutKind.HOME -> coEvery { loadShortcutEditorUseCase.loadHomeShortcut(shortcut.id) } returns result
        }
    }

    private fun givenEditError(kind: ShortcutKind, id: String, error: ShortcutError) {
        when (kind) {
            ShortcutKind.APP -> coEvery { loadShortcutEditorUseCase.loadAppShortcut(id) } returns ShortcutResult.Error(error)
            ShortcutKind.HOME -> coEvery { loadShortcutEditorUseCase.loadHomeShortcut(id) } returns ShortcutResult.Error(error)
        }
    }

    private fun givenDisplayState(serverId: Int, state: EntityDisplayState) {
        coEvery { getEntitiesForDisplay(serverId) } returns flow { emit(state) }
    }

    private fun givenModifyError(error: ShortcutError) {
        coEvery { modifyShortcutUseCase(any()) } returns ShortcutResult.Error(error)
    }

    private fun TestScope.loadedCreateVm() = newViewModel().also {
        runCurrent()
    }

    private fun TestScope.loadedCreateVmWithValidDraft() = loadedCreateVm().also {
        it.updateDraft(validShortcut())
    }

    private fun TestScope.loadedEditVm(kind: ShortcutKind) = newViewModel(
        EditorRoute.Edit(kind = kind, id = shortcutFor(kind).id),
    ).also {
        runCurrent()
    }

    private fun ShortcutEditorViewModel.createShortcut(kind: ShortcutKind) {
        when (kind) {
            ShortcutKind.APP -> createAppShortcut()
            ShortcutKind.HOME -> createHomeShortcut()
        }
    }

    private fun ShortcutEditorViewModel.updateShortcut(kind: ShortcutKind, id: String) {
        when (kind) {
            ShortcutKind.APP -> updateAppShortcut(id)
            ShortcutKind.HOME -> updateHomeShortcut(id)
        }
    }

    private fun ShortcutEditorViewModel.removeShortcut(kind: ShortcutKind, id: String) {
        when (kind) {
            ShortcutKind.APP -> deleteAppShortcut(id)
            ShortcutKind.HOME -> disableHomeShortcut(id)
        }
    }

    private suspend fun TestScope.assertSnackbar(
        viewModel: ShortcutEditorViewModel,
        expectedRes: Int,
        action: ShortcutEditorViewModel.() -> Unit,
    ) {
        viewModel.errorSnackbar.test {
            viewModel.action()
            runCurrent()
            assertEquals(expectedRes, awaitItem())
        }
    }

    private suspend fun TestScope.assertNoSubmission(
        viewModel: ShortcutEditorViewModel,
        action: ShortcutEditorViewModel.() -> Unit,
    ) {
        turbineScope {
            val closeEvents = viewModel.closeEvents.testIn(backgroundScope)
            val errorSnackbar = viewModel.errorSnackbar.testIn(backgroundScope)

            viewModel.action()
            advanceUntilIdle()

            closeEvents.expectNoEvents()
            errorSnackbar.expectNoEvents()
        }
        coVerify(exactly = 0) { modifyShortcutUseCase(any()) }
    }

    private suspend fun TestScope.assertModificationAndClose(
        viewModel: ShortcutEditorViewModel,
        expectedModification: ShortcutModification,
        action: ShortcutEditorViewModel.() -> Unit,
    ) {
        viewModel.closeEvents.test {
            viewModel.action()
            runCurrent()
            awaitItem()
        }

        coVerify {
            modifyShortcutUseCase(expectedModification)
        }
    }

    private fun shortcutFor(kind: ShortcutKind) = when (kind) {
        ShortcutKind.APP -> appShortcut
        ShortcutKind.HOME -> homeShortcut
    }

    private fun createSuccessMessageRes(kind: ShortcutKind) = when (kind) {
        ShortcutKind.APP -> null
        ShortcutKind.HOME -> commonR.string.shortcut_home_request_sent
    }

    private fun removeErrorRes(kind: ShortcutKind) = when (kind) {
        ShortcutKind.APP -> commonR.string.shortcut_delete_error
        ShortcutKind.HOME -> commonR.string.shortcut_disable_error
    }

    private fun validShortcut(label: String = "Shortcut") = ShortcutDraft(
        serverId = server.id,
        icon = ShortcutIcon.Default,
        label = label,
        description = "Description",
        destination = ShortcutDestination.Dashboard("/lovelace/home"),
    )

    private fun shortcut(id: String, label: String) = Shortcut(
        id = id,
        serverId = server.id,
        label = label,
        description = "Description",
        destination = ShortcutDestination.Dashboard("/lovelace/home"),
    )

    private fun editData(shortcut: Shortcut) = ShortcutEditor(
        draft = shortcut.toDraft(),
        servers = editorServers(),
    )

    private fun editorServers() = listOf(
        entityServer(
            serverId = server.id,
            name = server.friendlyName.ifBlank { server.id.toString() },
        ),
    )

    private fun entityServer(serverId: Int, name: String = "Office") = ShortcutServer(
        id = serverId,
        name = name,
        supportsEntity = true,
    )

    private fun dashboardOnlyServer(serverId: Int) = ShortcutServer(
        id = serverId,
        name = "Dashboard only",
        supportsEntity = false,
    )
}

private fun testServer() = Server(
    id = 1,
    _name = "Home",
    connection = ServerConnectionInfo(externalUrl = "https://example.com"),
    session = ServerSessionInfo(),
    user = ServerUserInfo(),
)

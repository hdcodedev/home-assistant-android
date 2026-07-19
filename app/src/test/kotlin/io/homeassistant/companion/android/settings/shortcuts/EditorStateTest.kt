package io.homeassistant.companion.android.settings.shortcuts

import io.homeassistant.companion.android.common.data.integration.display.EntityDisplayState
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDestination
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDraft
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutIcon
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class EditorStateTest {

    @Test
    fun `Given draft server id when selectedServer then matching server is returned`() {
        val state = editorState(
            servers = listOf(entityServer(serverId = 1), entityServer(serverId = 2)),
            draft = validDraft(serverId = 2),
        )

        assertEquals(entityServer(serverId = 2), state.selectedServer)
    }

    @Test
    fun `Given one server when isServerSelectionVisible then it is false`() {
        val state = editorState(servers = listOf(entityServer(serverId = 1)))

        assertFalse(state.isServerSelectionVisible)
    }

    @Test
    fun `Given multiple servers when isServerSelectionVisible then it is true`() {
        val state = editorState(
            servers = listOf(entityServer(serverId = 1), entityServer(serverId = 2)),
        )

        assertTrue(state.isServerSelectionVisible)
    }

    @Test
    fun `Given valid dashboard draft when canSubmit then it is true`() {
        val state = editorState(draft = validDraft())

        assertTrue(state.canSubmit)
    }

    @Test
    fun `Given blank label when canSubmit then it is false`() {
        val state = editorState(draft = validDraft(label = ""))

        assertFalse(state.canSubmit)
    }

    @Test
    fun `Given invalid dashboard path when canSubmit then it is false`() {
        val state = editorState(
            draft = validDraft(destination = ShortcutDestination.Dashboard(path = "lovelace/home")),
        )

        assertFalse(state.canSubmit)
    }

    @Test
    fun `Given entity target is loading when canSubmit then it is false`() {
        val state = editorState(
            draft = validDraft(destination = ShortcutDestination.Entity("light.kitchen")),
            entityDisplayStatesByServerId = mapOf(1 to EntityDisplayState.Loading),
        )

        assertFalse(state.canSubmit)
    }

    @Test
    fun `Given entity target on dashboard-only server when canSubmit then it is false`() {
        val state = editorState(
            servers = listOf(dashboardOnlyServer(serverId = 1)),
            draft = validDraft(destination = ShortcutDestination.Entity("light.kitchen")),
            entityDisplayStatesByServerId = mapOf(1 to EntityDisplayState.Loaded(emptyList())),
        )

        assertFalse(state.canSubmit)
    }

    @Test
    fun `Given entity-capable server with no display state when selectedEntityDisplayState then loading is returned`() {
        val state = editorState()

        assertEquals(EntityDisplayState.Loading, state.selectedEntityDisplayState)
        assertTrue(state.isDisplayStateLoading)
    }

    @Test
    fun `Given dashboard-only server with no display state when isDisplayStateLoading then it is false`() {
        val state = editorState(servers = listOf(dashboardOnlyServer(serverId = 1)))

        assertEquals(EntityDisplayState.Loading, state.selectedEntityDisplayState)
        assertFalse(state.isDisplayStateLoading)
    }

    @Test
    fun `Given supported destination when withServer then destination is kept`() {
        val destination = ShortcutDestination.Entity("light.kitchen")
        val state = editorState(
            servers = listOf(entityServer(serverId = 1), entityServer(serverId = 2)),
            draft = validDraft(destination = destination),
        )

        val updated = state.withServer(serverId = 2)

        assertEquals(2, updated.draft.serverId)
        assertEquals(destination, updated.draft.destination)
    }

    @Test
    fun `Given unsupported destination when withServer then destination resets to empty dashboard`() {
        val state = editorState(
            servers = listOf(entityServer(serverId = 1), dashboardOnlyServer(serverId = 2)),
            draft = validDraft(destination = ShortcutDestination.Entity("light.kitchen")),
        )

        val updated = state.withServer(serverId = 2)

        assertEquals(2, updated.draft.serverId)
        assertEquals(ShortcutDestination.Dashboard(path = ""), updated.draft.destination)
    }

    private fun editorState(
        servers: List<ShortcutServer> = listOf(entityServer(serverId = 1)),
        entityDisplayStatesByServerId: Map<Int, EntityDisplayState> = emptyMap(),
        draft: ShortcutDraft = validDraft(),
    ) = EditorState(
        servers = servers,
        entityDisplayStatesByServerId = entityDisplayStatesByServerId,
        draft = draft,
    )

    private fun validDraft(
        serverId: Int = 1,
        label: String = "Shortcut",
        destination: ShortcutDestination = ShortcutDestination.Dashboard(path = "/lovelace/home"),
    ) = ShortcutDraft(
        serverId = serverId,
        icon = ShortcutIcon.Default,
        label = label,
        description = "Description",
        destination = destination,
    )

    private fun entityServer(serverId: Int) = ShortcutServer(
        id = serverId,
        name = "Server $serverId",
        supportsEntity = true,
    )

    private fun dashboardOnlyServer(serverId: Int) = ShortcutServer(
        id = serverId,
        name = "Server $serverId",
        supportsEntity = false,
    )
}

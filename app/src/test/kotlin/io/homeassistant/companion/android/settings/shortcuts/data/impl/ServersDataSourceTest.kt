package io.homeassistant.companion.android.settings.shortcuts.data.impl

import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.database.server.ServerConnectionInfo
import io.homeassistant.companion.android.database.server.ServerSessionInfo
import io.homeassistant.companion.android.database.server.ServerUserInfo
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutError
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ServersDataSourceTest {
    private val serverManager: ServerManager = mockk(relaxed = true)
    private val defaultServerId = 1
    private val server = createServer(defaultServerId)

    private lateinit var dataSource: ServersDataSource

    @BeforeEach
    fun setUp() {
        coEvery { serverManager.servers() } returns listOf(server)
        coEvery { serverManager.getServer(any<Int>()) } returns server
        dataSource = ServersDataSource(
            serverManager = serverManager,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Given no servers when loadServers then returns NoServersConfigured`() = runTest {
        coEvery { serverManager.servers() } returns emptyList()

        val result = dataSource.loadServers()

        assertTrue(result is ShortcutResult.Error)
        assertEquals(ShortcutError.NoServersConfigured, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given server list loading fails when loadServers then returns Unknown`() = runTest {
        coEvery { serverManager.servers() } throws IllegalStateException("server failure")

        val result = dataSource.loadServers()

        assertTrue(result is ShortcutResult.Error)
        assertEquals(ShortcutError.Unknown, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given active server loading fails when loadServers then returns Unknown`() = runTest {
        coEvery { serverManager.getServer(any<Int>()) } throws IllegalStateException("active server failure")

        val result = dataSource.loadServers()

        assertTrue(result is ShortcutResult.Error)
        assertEquals(ShortcutError.Unknown, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given server list loading is cancelled when loadServers then cancellation propagates`() = runTest {
        coEvery { serverManager.servers() } throws CancellationException("cancelled")

        val failure = runCatching { dataSource.loadServers() }.exceptionOrNull()

        assertTrue(failure is CancellationException)
    }

    @Test
    fun `Given valid state when loadServers then returns lightweight servers`() = runTest {
        val data = (dataSource.loadServers() as ShortcutResult.Success).data

        assertTrue(data.servers.isNotEmpty())
        assertEquals(defaultServerId, data.defaultServer.id)
        coVerify(exactly = 0) { serverManager.integrationRepository(any()) }
    }

    private fun createServer(id: Int) = Server(
        id = id,
        _name = "Test Server",
        _version = "2025.6.0",
        connection = ServerConnectionInfo(externalUrl = "https://example.com"),
        session = ServerSessionInfo(),
        user = ServerUserInfo(),
    )
}

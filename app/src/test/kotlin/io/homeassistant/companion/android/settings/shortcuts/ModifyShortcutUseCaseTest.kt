package io.homeassistant.companion.android.settings.shortcuts

import android.os.Build
import io.homeassistant.companion.android.common.util.SdkVersion
import io.homeassistant.companion.android.settings.shortcuts.data.AppShortcutsRepository
import io.homeassistant.companion.android.settings.shortcuts.data.HomeShortcutsRepository
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDestination
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDraft
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutError
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutIcon
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ModifyShortcutUseCaseTest {
    private val appShortcutsRepository: AppShortcutsRepository = mockk(relaxed = true)
    private val homeShortcutsRepository: HomeShortcutsRepository = mockk(relaxed = true)
    private val defaultServerId = 1

    private lateinit var useCase: ModifyShortcutUseCase

    @BeforeEach
    fun setUp() {
        useCase = newUseCase()
    }

    @AfterEach
    fun tearDown() {
        SdkVersion.resetSdkInt()
        unmockkAll()
    }

    @Test
    fun `Given Android version not supported when create app shortcut then returns AndroidVersionNotSupported`() = runTest {
        useCase = newUseCase(sdkInt = Build.VERSION_CODES.M)

        val result = useCase(ShortcutModification.Create(kind = ShortcutKind.APP, draft = validShortcut()))

        assertTrue(result is ShortcutResult.Error)
        assertEquals(ShortcutError.AndroidVersionNotSupported, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given invalid dashboard path when create app shortcut then returns error`() = runTest {
        val shortcut = validShortcut().copy(
            destination = ShortcutDestination.Dashboard("https://evil.com"),
        )

        val result = useCase(ShortcutModification.Create(kind = ShortcutKind.APP, draft = shortcut))

        assertTrue(result is ShortcutResult.Error)
        assertEquals(ShortcutError.Unknown, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given invalid entity when update app shortcut then returns error`() = runTest {
        val shortcut = validShortcut().copy(
            destination = ShortcutDestination.Entity("invalid_entity"),
        )

        val result = useCase(
            ShortcutModification.Update(kind = ShortcutKind.APP, id = "shortcut_1", draft = shortcut),
        )

        assertTrue(result is ShortcutResult.Error)
        assertEquals(ShortcutError.Unknown, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given valid shortcut when create app shortcut then succeeds`() = runTest {
        coEvery { appShortcutsRepository.create(any()) } returns ShortcutResult.Success(Unit)

        val result = useCase(ShortcutModification.Create(kind = ShortcutKind.APP, draft = validShortcut()))

        assertTrue(result is ShortcutResult.Success)
    }

    @Test
    fun `Given app shortcut slots full when create app shortcut then returns AppShortcutSlotsFull`() = runTest {
        coEvery { appShortcutsRepository.create(any()) } returns ShortcutResult.Error(ShortcutError.AppShortcutSlotsFull)

        val result = useCase(ShortcutModification.Create(kind = ShortcutKind.APP, draft = validShortcut()))

        assertEquals(ShortcutError.AppShortcutSlotsFull, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given save fails when update app shortcut then returns error`() = runTest {
        coEvery { appShortcutsRepository.update("shortcut_1", any()) } returns
            ShortcutResult.Error(ShortcutError.ShortcutNotFound)

        val result = useCase(
            ShortcutModification.Update(kind = ShortcutKind.APP, id = "shortcut_1", draft = validShortcut()),
        )

        assertTrue(result is ShortcutResult.Error)
        assertEquals(ShortcutError.ShortcutNotFound, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given valid shortcut when update app shortcut then succeeds`() = runTest {
        coEvery { appShortcutsRepository.update("shortcut_1", any()) } returns ShortcutResult.Success(Unit)

        val result = useCase(
            ShortcutModification.Update(kind = ShortcutKind.APP, id = "shortcut_1", draft = validShortcut()),
        )

        assertTrue(result is ShortcutResult.Success)
    }

    @Test
    fun `Given pinning not supported when create home shortcut then returns HomeShortcutPinningNotSupported`() = runTest {
        every { homeShortcutsRepository.canPinShortcuts() } returns false

        val result = useCase(ShortcutModification.Create(kind = ShortcutKind.HOME, draft = validShortcut()))

        assertTrue(result is ShortcutResult.Error)
        assertEquals(ShortcutError.HomeShortcutPinningNotSupported, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given create fails when create home shortcut then returns error`() = runTest {
        every { homeShortcutsRepository.canPinShortcuts() } returns true
        coEvery { homeShortcutsRepository.create(any()) } returns ShortcutResult.Error(ShortcutError.Unknown)

        val result = useCase(ShortcutModification.Create(kind = ShortcutKind.HOME, draft = validShortcut()))

        assertTrue(result is ShortcutResult.Error)
        assertEquals(ShortcutError.Unknown, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given valid shortcut when create home shortcut then succeeds`() = runTest {
        every { homeShortcutsRepository.canPinShortcuts() } returns true
        coEvery { homeShortcutsRepository.create(any()) } returns ShortcutResult.Success(Unit)

        val result = useCase(ShortcutModification.Create(kind = ShortcutKind.HOME, draft = validShortcut()))

        assertTrue(result is ShortcutResult.Success)
    }

    @Test
    fun `Given update fails when update home shortcut then returns error`() = runTest {
        every { homeShortcutsRepository.canPinShortcuts() } returns true
        coEvery { homeShortcutsRepository.update(any(), any()) } returns ShortcutResult.Error(ShortcutError.Unknown)

        val result = useCase(
            ShortcutModification.Update(kind = ShortcutKind.HOME, id = "home_1", draft = validShortcut()),
        )

        assertTrue(result is ShortcutResult.Error)
        assertEquals(ShortcutError.Unknown, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given delete fails when remove app shortcut then returns error`() = runTest {
        coEvery { appShortcutsRepository.delete("shortcut_1") } returns ShortcutResult.Error(ShortcutError.Unknown)

        val result = useCase(ShortcutModification.Remove(kind = ShortcutKind.APP, id = "shortcut_1"))

        assertTrue(result is ShortcutResult.Error)
        assertEquals(ShortcutError.Unknown, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given valid id when remove app shortcut then succeeds`() = runTest {
        coEvery { appShortcutsRepository.delete("shortcut_1") } returns ShortcutResult.Success(Unit)

        val result = useCase(ShortcutModification.Remove(kind = ShortcutKind.APP, id = "shortcut_1"))

        assertTrue(result is ShortcutResult.Success)
    }

    @Test
    fun `Given disable fails when remove home shortcut then returns error`() = runTest {
        coEvery { homeShortcutsRepository.disable("test-id") } returns ShortcutResult.Error(ShortcutError.Unknown)

        val result = useCase(ShortcutModification.Remove(kind = ShortcutKind.HOME, id = "test-id"))

        assertTrue(result is ShortcutResult.Error)
        assertEquals(ShortcutError.Unknown, (result as ShortcutResult.Error).error)
    }

    @Test
    fun `Given valid id when remove home shortcut then succeeds`() = runTest {
        coEvery { homeShortcutsRepository.disable("test-id") } returns ShortcutResult.Success(Unit)

        val result = useCase(ShortcutModification.Remove(kind = ShortcutKind.HOME, id = "test-id"))

        assertTrue(result is ShortcutResult.Success)
    }

    private fun newUseCase(sdkInt: Int = Build.VERSION_CODES.N_MR1): ModifyShortcutUseCase {
        SdkVersion.sdkInt = sdkInt
        return ModifyShortcutUseCase(
            appShortcutsRepository = appShortcutsRepository,
            homeShortcutsRepository = homeShortcutsRepository,
        )
    }

    private fun validShortcut() = ShortcutDraft(
        serverId = defaultServerId,
        icon = ShortcutIcon.Default,
        label = "Test",
        description = "",
        destination = ShortcutDestination.Entity("light.kitchen"),
    )
}

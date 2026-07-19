package io.homeassistant.companion.android.settings.shortcuts

import io.homeassistant.companion.android.settings.shortcuts.data.AppShortcutsRepository
import io.homeassistant.companion.android.settings.shortcuts.data.HomeShortcutsRepository
import io.homeassistant.companion.android.settings.shortcuts.data.ShortcutServersRepository
import io.homeassistant.companion.android.settings.shortcuts.data.entities.HomeShortcuts
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutError
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutResult
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutsListData
import javax.inject.Inject

internal class LoadShortcutsUseCase @Inject constructor(
    private val appShortcutsRepository: AppShortcutsRepository,
    private val homeShortcutsRepository: HomeShortcutsRepository,
    private val shortcutServersRepository: ShortcutServersRepository,
) {
    suspend operator fun invoke(): ShortcutResult<ShortcutsListData> {
        if (!areShortcutsSupported()) {
            return ShortcutResult.Error(ShortcutError.AndroidVersionNotSupported)
        }

        when (val result = shortcutServersRepository.loadServers()) {
            is ShortcutResult.Success -> Unit
            is ShortcutResult.Error -> return ShortcutResult.Error(result.error)
        }

        val appShortcuts = when (val result = appShortcutsRepository.load()) {
            is ShortcutResult.Success -> result.data
            is ShortcutResult.Error -> return ShortcutResult.Error(result.error)
        }

        val canPinHomeShortcuts = homeShortcutsRepository.canPinShortcuts()
        val homeShortcutItems = when {
            !canPinHomeShortcuts -> emptyList()
            else -> when (val result = homeShortcutsRepository.load()) {
                is ShortcutResult.Success -> result.data
                // Fail the whole screen until the UI supports per-section errors and retry.
                is ShortcutResult.Error -> return ShortcutResult.Error(result.error)
            }
        }

        return ShortcutResult.Success(
            ShortcutsListData(
                appShortcuts = appShortcuts,
                homeShortcuts = HomeShortcuts(
                    items = homeShortcutItems,
                    canPinShortcuts = canPinHomeShortcuts,
                ),
            ),
        )
    }
}

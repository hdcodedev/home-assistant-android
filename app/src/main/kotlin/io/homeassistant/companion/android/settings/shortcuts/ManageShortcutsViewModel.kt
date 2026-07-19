package io.homeassistant.companion.android.settings.shortcuts

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.homeassistant.companion.android.settings.shortcuts.data.entities.AppShortcuts
import io.homeassistant.companion.android.settings.shortcuts.data.entities.HomeShortcuts
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutError
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutResult
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Loading / error / ready states for the shortcuts list screen. */
@Immutable
internal sealed interface ManageShortcutsLoadState {
    data object Loading : ManageShortcutsLoadState

    data class Error(val error: ShortcutError) : ManageShortcutsLoadState

    data object Ready : ManageShortcutsLoadState
}

@Immutable
internal data class ManageShortcutsUiState(
    val loadState: ManageShortcutsLoadState = ManageShortcutsLoadState.Loading,
    val appShortcuts: AppShortcuts,
    val homeShortcuts: HomeShortcuts,
) {
    val hasError: Boolean get() = loadState is ManageShortcutsLoadState.Error
    val isHomeSupported: Boolean get() = homeShortcuts.canPinShortcuts
    val isEmpty: Boolean get() = appShortcuts.items.isEmpty() && homeShortcuts.items.isEmpty()
    val canCreateAppShortcut: Boolean get() = appShortcuts.items.size < appShortcuts.maxAppShortcuts
}

@HiltViewModel
internal class ManageShortcutsViewModel @Inject constructor(private val loadShortcutsUseCase: LoadShortcutsUseCase) :
    ViewModel() {
    private val _uiState = MutableStateFlow(
        ManageShortcutsUiState(
            appShortcuts = AppShortcuts(emptyList(), maxAppShortcuts = 0),
            homeShortcuts = HomeShortcuts(emptyList(), canPinShortcuts = true),
        ),
    )
    val uiState: StateFlow<ManageShortcutsUiState> = _uiState.asStateFlow()
    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        refreshInternal(true)
    }

    fun refreshSilently() {
        if (_uiState.value.loadState is ManageShortcutsLoadState.Loading) return
        refreshInternal(false)
    }

    private fun refreshInternal(showLoading: Boolean) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            if (showLoading) {
                _uiState.update {
                    it.copy(loadState = ManageShortcutsLoadState.Loading)
                }
            }

            val listData = when (val result = loadShortcutsUseCase()) {
                is ShortcutResult.Success -> result.data
                is ShortcutResult.Error -> {
                    if (showLoading) {
                        _uiState.update {
                            it.copy(loadState = ManageShortcutsLoadState.Error(result.error))
                        }
                    }
                    return@launch
                }
            }

            _uiState.update {
                ManageShortcutsUiState(
                    loadState = ManageShortcutsLoadState.Ready,
                    appShortcuts = listData.appShortcuts,
                    homeShortcuts = listData.homeShortcuts,
                )
            }
        }
    }
}

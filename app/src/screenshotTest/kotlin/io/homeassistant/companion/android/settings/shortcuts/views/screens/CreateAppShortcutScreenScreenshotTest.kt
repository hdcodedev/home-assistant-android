package io.homeassistant.companion.android.settings.shortcuts.views.screens

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.settings.shortcuts.ShortcutsUiState
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDestination
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDraft
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutError
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutServer
import io.homeassistant.companion.android.settings.shortcuts.views.preview.ShortcutPreviewData

class CreateAppShortcutScreenScreenshotTest {

    @PreviewTest
    @ShortcutSingleDevicePreview
    @Composable
    fun `CreateAppShortcutScreen shortcut not found`() {
        HAThemeForPreview {
            CreateAppShortcutScreen(
                uiState = ShortcutsUiState.LoadError(ShortcutError.ShortcutNotFound),
                onSubmit = {},
                onUpdateDraft = {},
                onRetry = {},
            )
        }
    }

    @PreviewTest
    @ShortcutSingleDevicePreview
    @Composable
    fun `CreateAppShortcutScreen empty`() {
        HAThemeForPreview {
            CreateAppShortcutScreen(
                uiState = ShortcutsUiState.Editor(
                    ShortcutPreviewData.buildEditorState(
                        draft = ShortcutDraft.initial(1),
                    ),
                ),
                onSubmit = {},
                onUpdateDraft = {},
                onRetry = {},
            )
        }
    }

    @PreviewTest
    @ShortcutSingleDevicePreview
    @Composable
    fun `CreateAppShortcutScreen populated`() {
        HAThemeForPreview {
            CreateAppShortcutScreen(
                uiState = ShortcutsUiState.Editor(
                    ShortcutPreviewData.buildEditorState(
                        draft = ShortcutPreviewData.buildDraft(),
                    ),
                ),
                onSubmit = {},
                onUpdateDraft = {},
                onRetry = {},
            )
        }
    }

    @PreviewTest
    @ShortcutSingleDevicePreview
    @Composable
    fun `CreateAppShortcutScreen old server version`() {
        HAThemeForPreview {
            CreateAppShortcutScreen(
                uiState = ShortcutsUiState.Editor(
                    ShortcutPreviewData.buildEditorState(
                        servers = listOf(
                            ShortcutServer(
                                id = 1,
                                name = "Old Home",
                                supportsEntity = false,
                            ),
                        ),
                        draft = ShortcutDraft.initial(1)
                            .copy(destination = ShortcutDestination.Dashboard("/lovelace/shortcut")),
                    ),
                ),
                onSubmit = {},
                onUpdateDraft = {},
                onRetry = {},
            )
        }
    }
}

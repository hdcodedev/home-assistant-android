package io.homeassistant.companion.android.settings.shortcuts.views.screens

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.data.integration.display.EntityDisplayState
import io.homeassistant.companion.android.settings.shortcuts.ShortcutsUiState
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDraft
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutServer
import io.homeassistant.companion.android.settings.shortcuts.views.preview.ShortcutPreviewData

class CreateHomeShortcutScreenScreenshotTest {

    @PreviewTest
    @ShortcutSingleDevicePreview
    @Composable
    fun `CreateHomeShortcutScreen empty`() {
        HAThemeForPreview {
            CreateHomeShortcutScreen(
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
    fun `CreateHomeShortcutScreen populated`() {
        HAThemeForPreview {
            CreateHomeShortcutScreen(
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
    fun `CreateHomeShortcutScreen old server version`() {
        HAThemeForPreview {
            CreateHomeShortcutScreen(
                uiState = ShortcutsUiState.Editor(
                    ShortcutPreviewData.buildEditorState(
                        servers = listOf(
                            ShortcutServer(
                                id = 1,
                                name = "Old Home",
                                supportsEntity = false,
                            ),
                        ),
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
    fun `CreateHomeShortcutScreen entity catalog loading`() {
        HAThemeForPreview {
            CreateHomeShortcutScreen(
                uiState = ShortcutsUiState.Editor(
                    ShortcutPreviewData.buildEditorState(
                        entityDisplayStatesByServerId = mapOf(1 to EntityDisplayState.Loading),
                        draft = ShortcutPreviewData.previewEntityDraft,
                    ),
                ),
                onSubmit = {},
                onUpdateDraft = {},
                onRetry = {},
            )
        }
    }
}

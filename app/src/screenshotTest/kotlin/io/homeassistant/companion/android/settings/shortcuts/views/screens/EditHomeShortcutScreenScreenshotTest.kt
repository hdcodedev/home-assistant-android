package io.homeassistant.companion.android.settings.shortcuts.views.screens

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.data.integration.display.EntityDisplayState
import io.homeassistant.companion.android.settings.shortcuts.ShortcutsUiState
import io.homeassistant.companion.android.settings.shortcuts.views.preview.ShortcutPreviewData

class EditHomeShortcutScreenScreenshotTest {

    @PreviewTest
    @ShortcutSingleDevicePreview
    @Composable
    fun `EditHomeShortcutScreen populated`() {
        HAThemeForPreview {
            EditHomeShortcutScreen(
                uiState = ShortcutsUiState.Editor(
                    ShortcutPreviewData.buildEditorState(
                        draft = ShortcutPreviewData.previewHomeDraft,
                    ),
                ),
                onSubmit = {},
                onUpdateDraft = {},
                onDelete = {},
                onRetry = {},
            )
        }
    }

    @PreviewTest
    @ShortcutSingleDevicePreview
    @Composable
    fun `EditHomeShortcutScreen entity catalog loading`() {
        HAThemeForPreview {
            EditHomeShortcutScreen(
                uiState = ShortcutsUiState.Editor(
                    ShortcutPreviewData.buildEditorState(
                        entityDisplayStatesByServerId = mapOf(1 to EntityDisplayState.Loading),
                        draft = ShortcutPreviewData.previewEntityDraft,
                    ),
                ),
                onSubmit = {},
                onUpdateDraft = {},
                onDelete = {},
                onRetry = {},
            )
        }
    }
}

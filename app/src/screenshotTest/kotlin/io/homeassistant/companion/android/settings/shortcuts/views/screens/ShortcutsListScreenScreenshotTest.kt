package io.homeassistant.companion.android.settings.shortcuts.views.screens

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.settings.shortcuts.ManageShortcutsLoadState
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDestination
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutError
import io.homeassistant.companion.android.settings.shortcuts.views.preview.ShortcutPreviewData

class ShortcutsListScreenScreenshotTest {

    @PreviewTest
    @ShortcutSingleDevicePreview
    @Composable
    fun `ShortcutsListScreen loading`() {
        HAThemeForPreview {
            ShortcutsListScreen(
                state = ShortcutPreviewData.buildListState(
                    loadState = ManageShortcutsLoadState.Loading,
                    appSummaries = emptyList(),
                    homeSummaries = emptyList(),
                ),
                onAction = {},
                onRetry = {},
            )
        }
    }

    @PreviewTest
    @ShortcutSingleDevicePreview
    @Composable
    fun `ShortcutsListScreen empty`() {
        HAThemeForPreview {
            ShortcutsListScreen(
                state = ShortcutPreviewData.buildListState(
                    loadState = ManageShortcutsLoadState.Ready,
                    appSummaries = emptyList(),
                    homeSummaries = emptyList(),
                ),
                onAction = {},
                onRetry = {},
            )
        }
    }

    @PreviewTest
    @ShortcutSingleDevicePreview
    @Composable
    fun `ShortcutsListScreen error`() {
        HAThemeForPreview {
            ShortcutsListScreen(
                state = ShortcutPreviewData.buildListState(
                    loadState = ManageShortcutsLoadState.Error(ShortcutError.Unknown),
                    appSummaries = emptyList(),
                    homeSummaries = emptyList(),
                ),
                onAction = {},
                onRetry = {},
            )
        }
    }

    @PreviewTest
    @ShortcutSingleDevicePreview
    @Composable
    fun `ShortcutsListScreen not supported`() {
        HAThemeForPreview {
            ShortcutsListScreen(
                state = ShortcutPreviewData.buildListState(
                    loadState = ManageShortcutsLoadState.Error(ShortcutError.AndroidVersionNotSupported),
                    appSummaries = emptyList(),
                    homeSummaries = emptyList(),
                ),
                onAction = {},
                onRetry = {},
            )
        }
    }

    @PreviewTest
    @ShortcutSingleDevicePreview
    @Composable
    fun `ShortcutsListScreen no servers`() {
        HAThemeForPreview {
            ShortcutsListScreen(
                state = ShortcutPreviewData.buildListState(
                    loadState = ManageShortcutsLoadState.Error(ShortcutError.NoServersConfigured),
                    appSummaries = emptyList(),
                    homeSummaries = emptyList(),
                ),
                onAction = {},
                onRetry = {},
            )
        }
    }

    @PreviewTest
    @ShortcutSingleDevicePreview
    @Composable
    fun `ShortcutsListScreen content max`() {
        HAThemeForPreview {
            ShortcutsListScreen(
                state = ShortcutPreviewData.buildListState(
                    appSummaries = ShortcutPreviewData.buildAppSummaries(
                        count = 5,
                        destination = ShortcutDestination.Dashboard("/lovelace/shortcut"),
                    ),
                    homeSummaries = ShortcutPreviewData.buildHomeSummaries(count = 20),
                ),
                onAction = {},
                onRetry = {},
            )
        }
    }

    @PreviewTest
    @ShortcutSingleDevicePreview
    @Composable
    fun `ShortcutsListScreen content disabled home shortcut`() {
        HAThemeForPreview {
            ShortcutsListScreen(
                state = ShortcutPreviewData.buildListState(
                    appSummaries = emptyList(),
                    homeSummaries = ShortcutPreviewData.buildDisabledHomeSummaries(),
                ),
                onAction = {},
                onRetry = {},
            )
        }
    }
}

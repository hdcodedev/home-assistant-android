package io.homeassistant.companion.android.settings.shortcuts.views.screens

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview

class CreateShortcutDialogScreenshotTest {

    @PreviewTest
    @ShortcutSingleDevicePreview
    @Composable
    fun `CreateShortcutDialog available`() {
        HAThemeForPreview {
            CreateShortcutDialog(
                canCreateAppShortcut = true,
                canCreateHomeShortcut = true,
                onCreateAppShortcut = {},
                onCreateHomeShortcut = {},
                onDismissRequest = {},
            )
        }
    }

    @PreviewTest
    @ShortcutSingleDevicePreview
    @Composable
    fun `CreateShortcutDialog app slots full`() {
        HAThemeForPreview {
            CreateShortcutDialog(
                canCreateAppShortcut = false,
                canCreateHomeShortcut = true,
                onCreateAppShortcut = {},
                onCreateHomeShortcut = {},
                onDismissRequest = {},
            )
        }
    }

    @PreviewTest
    @ShortcutSingleDevicePreview
    @Composable
    fun `CreateShortcutDialog home not supported`() {
        HAThemeForPreview {
            CreateShortcutDialog(
                canCreateAppShortcut = true,
                canCreateHomeShortcut = false,
                onCreateAppShortcut = {},
                onCreateHomeShortcut = {},
                onDismissRequest = {},
            )
        }
    }
}

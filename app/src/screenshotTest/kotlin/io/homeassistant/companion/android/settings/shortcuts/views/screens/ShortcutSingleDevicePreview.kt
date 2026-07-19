package io.homeassistant.companion.android.settings.shortcuts.views.screens

import androidx.compose.ui.tooling.preview.Preview

/**
 * Single-device preview for low-variance shortcut states, such as loading screens, where the full
 * device matrix does not add meaningful coverage.
 */
@Preview(name = "phone", device = "spec:width=411.4dp,height=923.4dp", group = "phone")
internal annotation class ShortcutSingleDevicePreview

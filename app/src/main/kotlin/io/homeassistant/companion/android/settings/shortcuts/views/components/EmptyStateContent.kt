package io.homeassistant.companion.android.settings.shortcuts.views.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.settings.views.EmptyState
import io.homeassistant.companion.android.util.compose.HAPreviews

@Composable
internal fun EmptyStateContent() {
    EmptyState(
        icon = CommunityMaterial.Icon2.cmd_flash,
        title = stringResource(R.string.shortcut_empty_title),
        subtitle = stringResource(R.string.shortcut_empty_subtitle),
    )
}

@Composable
internal fun EmptyStateNoServers() {
    EmptyState(
        icon = CommunityMaterial.Icon2.cmd_flash,
        title = stringResource(R.string.shortcut_empty_title),
        subtitle = stringResource(R.string.shortcut_no_servers),
    )
}

@Composable
internal fun EmptyStateContentSlots() {
    EmptyState(
        icon = CommunityMaterial.Icon2.cmd_flash,
        title = stringResource(R.string.state_unavailable),
        subtitle = stringResource(R.string.shortcut_dynamic_slots_full),
    )
}

@Composable
internal fun NotSupportedStateContent() {
    EmptyState(
        icon = CommunityMaterial.Icon.cmd_alert,
        title = stringResource(R.string.failed_unsupported),
        subtitle = stringResource(R.string.shortcut_not_supported_subtitle),
    )
}

@Composable
internal fun HomeShortcutsNotSupportedStateContent() {
    EmptyState(
        icon = CommunityMaterial.Icon.cmd_alert,
        title = stringResource(R.string.failed_unsupported),
        subtitle = stringResource(R.string.shortcut_pin_not_supported),
    )
}

@HAPreviews
@Composable
private fun EmptyStateContentPreview() {
    HAThemeForPreview {
        EmptyStateContent()
    }
}

@HAPreviews
@Composable
private fun EmptyStateContentNoServersPreview() {
    HAThemeForPreview {
        EmptyStateNoServers()
    }
}

@HAPreviews
@Composable
private fun EmptyStateContentSlotsPreview() {
    HAThemeForPreview {
        EmptyStateContentSlots()
    }
}

@HAPreviews
@Composable
private fun NotSupportedStateContentPreview() {
    HAThemeForPreview {
        NotSupportedStateContent()
    }
}

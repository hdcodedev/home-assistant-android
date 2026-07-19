package io.homeassistant.companion.android.settings.shortcuts.views.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.common.compose.composable.HAFilledButton
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutError
import io.homeassistant.companion.android.settings.views.EmptyState
import io.homeassistant.companion.android.util.compose.HAPreviews

private data class ShortcutErrorContent(val icon: IIcon, val titleRes: Int, val subtitleRes: Int)

/**
 * Displays the shortcut error state for a given [ShortcutError], with an optional retry action.
 *
 * Load-specific errors get dedicated copy. Operation-only errors fall back to the generic message
 * because they are normally shown as snackbars instead.
 *
 * @param error The error that occurred.
 * @param onRetry Called when the user taps the retry button. When `null` no button is shown.
 */
@Composable
internal fun ErrorStateContent(error: ShortcutError, onRetry: (() -> Unit)? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val content = when (error) {
            ShortcutError.AndroidVersionNotSupported -> ShortcutErrorContent(
                icon = CommunityMaterial.Icon.cmd_alert,
                titleRes = R.string.failed_unsupported,
                subtitleRes = R.string.shortcut_android_version_not_supported,
            )
            ShortcutError.NoServersConfigured -> ShortcutErrorContent(
                icon = CommunityMaterial.Icon2.cmd_flash,
                titleRes = R.string.shortcut_no_servers_title,
                subtitleRes = R.string.shortcut_no_servers_subtitle,
            )
            ShortcutError.ShortcutNotFound -> ShortcutErrorContent(
                icon = CommunityMaterial.Icon.cmd_alert,
                titleRes = R.string.shortcut_not_found_title,
                subtitleRes = R.string.shortcut_not_found,
            )
            else -> ShortcutErrorContent(
                icon = CommunityMaterial.Icon.cmd_alert,
                titleRes = R.string.shortcut_error_title,
                subtitleRes = R.string.shortcut_error_subtitle,
            )
        }
        EmptyState(
            icon = content.icon,
            title = stringResource(content.titleRes),
            subtitle = stringResource(content.subtitleRes),
        )
        if (error == ShortcutError.Unknown && onRetry != null) {
            Spacer(modifier = Modifier.height(HADimens.SPACE3))
            HAFilledButton(
                text = stringResource(R.string.retry),
                onClick = onRetry,
            )
        }
    }
}

@HAPreviews
@Composable
private fun ErrorStateContentUnknownPreview() {
    HAThemeForPreview {
        ErrorStateContent(error = ShortcutError.Unknown, onRetry = {})
    }
}

@HAPreviews
@Composable
private fun ErrorStateContentAndroidVersionNotSupportedPreview() {
    HAThemeForPreview {
        ErrorStateContent(error = ShortcutError.AndroidVersionNotSupported)
    }
}

@HAPreviews
@Composable
private fun ErrorStateContentNoServersConfiguredPreview() {
    HAThemeForPreview {
        ErrorStateContent(error = ShortcutError.NoServersConfigured)
    }
}

@HAPreviews
@Composable
private fun ErrorStateContentShortcutNotFoundPreview() {
    HAThemeForPreview {
        ErrorStateContent(error = ShortcutError.ShortcutNotFound)
    }
}

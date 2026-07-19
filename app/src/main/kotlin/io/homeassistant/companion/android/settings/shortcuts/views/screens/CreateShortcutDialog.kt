package io.homeassistant.companion.android.settings.shortcuts.views.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.common.compose.composable.HAPlainButton
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme

/**
 * Presents the choice between creating an application dynamic shortcut or a pinned home screen shortcut.
 *
 * @param canCreateAppShortcut Whether application dynamic shortcut creation is available.
 * @param canCreateHomeShortcut Whether pinned home screen shortcut creation is available.
 * @param onCreateAppShortcut Called when the user chooses to create an application shortcut.
 * @param onCreateHomeShortcut Called when the user chooses to create a home screen shortcut.
 * @param onDismissRequest Called when the dialog is dismissed without a selection.
 */
@Composable
internal fun CreateShortcutDialog(
    canCreateAppShortcut: Boolean,
    canCreateHomeShortcut: Boolean,
    onCreateAppShortcut: () -> Unit,
    onCreateHomeShortcut: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(R.string.shortcut_create_dialog_subtitle),
                style = HATextStyle.HeadlineMedium,
                color = LocalHAColorScheme.current.colorTextPrimary,
            )
        },
        text = {
            Column {
                ShortcutTypeOptionRow(
                    icon = CommunityMaterial.Icon2.cmd_flash,
                    label = stringResource(R.string.shortcut_add_to_app_shortcuts),
                    description = if (canCreateAppShortcut) {
                        stringResource(R.string.shortcut_add_to_app_shortcuts_subtitle)
                    } else {
                        stringResource(R.string.shortcut_dynamic_slots_full)
                    },
                    enabled = canCreateAppShortcut,
                    onClick = onCreateAppShortcut,
                )
                ShortcutTypeOptionRow(
                    icon = CommunityMaterial.Icon3.cmd_view_dashboard,
                    label = stringResource(R.string.shortcut_add_to_home_screen),
                    description = if (canCreateHomeShortcut) {
                        stringResource(R.string.shortcut_add_to_home_screen_subtitle)
                    } else {
                        stringResource(R.string.shortcut_pin_not_supported)
                    },
                    enabled = canCreateHomeShortcut,
                    onClick = onCreateHomeShortcut,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            HAPlainButton(
                text = stringResource(R.string.cancel),
                onClick = onDismissRequest,
            )
        },
    )
}

@Composable
private fun ShortcutTypeOptionRow(
    icon: IIcon,
    label: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalHAColorScheme.current
    val primaryTextColor = if (enabled) colors.colorTextPrimary else colors.colorTextDisabled
    val secondaryTextColor = if (enabled) colors.colorTextSecondary else colors.colorTextDisabled
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = HADimens.SPACE4, vertical = HADimens.SPACE3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HADimens.SPACE3),
    ) {
        Image(
            asset = icon,
            colorFilter = ColorFilter.tint(primaryTextColor),
            contentDescription = label,
            modifier = Modifier.size(HADimens.SPACE5),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(HADimens.SPACE1),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = label,
                style = HATextStyle.BodyMedium,
                color = primaryTextColor,
            )
            Text(
                text = description,
                style = HATextStyle.BodyMedium,
                color = secondaryTextColor,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

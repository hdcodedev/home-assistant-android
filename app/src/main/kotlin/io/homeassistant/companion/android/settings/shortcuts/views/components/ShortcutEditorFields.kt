package io.homeassistant.companion.android.settings.shortcuts.views.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.mikepenz.iconics.compose.IconicsPainter
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.common.compose.theme.HABorderWidth
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HARadius
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.settings.shortcuts.EditorState
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDraft
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutIcon
import io.homeassistant.companion.android.settings.shortcuts.views.preview.ShortcutPreviewData
import io.homeassistant.companion.android.util.compose.HAPreviews
import io.homeassistant.companion.android.common.util.getIconByMdiName

/**
 * Renders the shortcut editor header (title and icon picker) and the editable [ShortcutEditorForm].
 *
 * @param state The current editor state.
 * @param title The title to display for the editor.
 * @param onDraftChange Called with an updated [ShortcutDraft] whenever a field changes.
 * @param onServerSelected Called when the user selects a server.
 * @param onIconClick Called when the user taps the icon picker to choose a new icon.
 * @param onSubmit Called when the user confirms the shortcut.
 * @param onDelete Called when the user requests deletion of an existing shortcut, or `null` when not applicable.
 */
@Composable
internal fun ShortcutEditorFields(
    state: EditorState,
    title: String,
    onDraftChange: (ShortcutDraft) -> Unit,
    onServerSelected: (Int) -> Unit,
    onIconClick: () -> Unit,
    onSubmit: () -> Unit,
    onDelete: (() -> Unit)?,
    deleteLabelRes: Int = R.string.delete,
) {
    Column(verticalArrangement = Arrangement.spacedBy(HADimens.SPACE2)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HADimens.SPACE3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = HATextStyle.HeadlineMedium,
                color = LocalHAColorScheme.current.colorFillPrimaryLoudResting,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f),
            )

            ShortcutIconButton(
                selectedIcon = state.draft.icon,
                onIconClick = onIconClick,
            )
        }

        ShortcutEditorForm(
            state = state,
            labelText = stringResource(R.string.shortcut_label),
            descriptionText = stringResource(R.string.shortcut_description),
            onDraftChange = onDraftChange,
            onServerSelected = onServerSelected,
            onSubmit = onSubmit,
            onDelete = onDelete,
            deleteLabelRes = deleteLabelRes,
        )
    }
}

/**
 * A tappable button that displays the currently selected shortcut icon and opens the icon picker on tap.
 *
 * This is the trigger for the picker (the actual picker is the [io.homeassistant.companion.android.util.icondialog.IconDialog]
 * launched by the editor screen), not the picker itself. The control is an icon-only button drawn as a pill with an HA
 * outline, reusing the design-system tokens ([HABorderWidth], [HARadius], [LocalHAColorScheme]) so its colors match the
 * rest of the UI.
 *
 * @param selectedIcon Currently selected [ShortcutIcon], or [ShortcutIcon.Default] to fall back to the default app icon.
 * @param onIconClick Called when the user taps the button to choose a new icon.
 */
@Composable
private fun ShortcutIconButton(selectedIcon: ShortcutIcon, onIconClick: () -> Unit) {
    val colorScheme = LocalHAColorScheme.current
    val icon = remember(selectedIcon) {
        (selectedIcon as? ShortcutIcon.Mdi)?.name?.let(CommunityMaterial::getIconByMdiName)
    }
    val painter = if (icon != null) {
        remember(icon) { IconicsPainter(icon) }
    } else {
        painterResource(R.drawable.ic_stat_ic_notification_blue)
    }
    Box(
        modifier = Modifier
            .size(width = HADimens.SPACE14, height = HADimens.SPACE10)
            .clip(RoundedCornerShape(HARadius.Pill))
            .border(
                width = HABorderWidth.S,
                color = colorScheme.colorBorderNeutralQuiet,
                shape = RoundedCornerShape(HARadius.Pill),
            )
            .clickable(role = Role.Button, onClick = onIconClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = stringResource(R.string.shortcut_icon),
            tint = colorScheme.colorFillPrimaryLoudResting,
            modifier = Modifier.size(HADimens.SPACE6),
        )
    }
}

@HAPreviews
@Composable
private fun ShortcutEditorFieldsAppPreview() {
    HAThemeForPreview {
        ShortcutEditorFields(
            state = ShortcutPreviewData.buildEditorState(draft = ShortcutPreviewData.previewAppDraft),
            title = "Add App Shortcut",
            onDraftChange = {},
            onServerSelected = {},
            onIconClick = {},
            onSubmit = {},
            onDelete = null,
        )
    }
}

@HAPreviews
@Composable
private fun ShortcutEditorFieldsHomePreview() {
    HAThemeForPreview {
        ShortcutEditorFields(
            state = ShortcutPreviewData.buildEditorState(draft = ShortcutPreviewData.previewHomeDraft),
            title = "Edit Home Shortcut",
            onDraftChange = {},
            onServerSelected = {},
            onIconClick = {},
            onSubmit = {},
            onDelete = {},
        )
    }
}

@HAPreviews
@Composable
private fun ShortcutIconButtonPreview() {
    HAThemeForPreview {
        ShortcutIconButton(
            selectedIcon = ShortcutIcon.Default,
            onIconClick = {},
        )
    }
}

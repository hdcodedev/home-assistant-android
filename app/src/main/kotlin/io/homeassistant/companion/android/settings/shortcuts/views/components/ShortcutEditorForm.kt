package io.homeassistant.companion.android.settings.shortcuts.views.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.common.compose.composable.ButtonVariant
import io.homeassistant.companion.android.common.compose.composable.HAFilledButton
import io.homeassistant.companion.android.common.compose.composable.HARadioGroup
import io.homeassistant.companion.android.common.compose.composable.HATextField
import io.homeassistant.companion.android.common.compose.composable.RadioOption
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.data.integration.display.EntityDisplayState
import io.homeassistant.companion.android.settings.shortcuts.EditorState
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDestination
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDraft
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutServer
import io.homeassistant.companion.android.settings.shortcuts.views.preview.ShortcutPreviewData
import io.homeassistant.companion.android.util.compose.HAPreviews
import io.homeassistant.companion.android.util.compose.entity.EntityPicker

@Composable
internal fun ShortcutEditorForm(
    state: EditorState,
    labelText: String,
    descriptionText: String,
    onDraftChange: (ShortcutDraft) -> Unit,
    onServerSelected: (Int) -> Unit,
    onSubmit: () -> Unit,
    onDelete: (() -> Unit)? = null,
    deleteLabelRes: Int = R.string.delete,
) {
    val draft = state.draft

    Column(verticalArrangement = Arrangement.spacedBy(HADimens.SPACE4)) {
        ShortcutMetadataFields(
            draft = draft,
            servers = state.servers,
            isServerSelectionVisible = state.isServerSelectionVisible,
            labelText = labelText,
            descriptionText = descriptionText,
            onLabelChange = { onDraftChange(draft.copy(label = it)) },
            onDescriptionChange = { onDraftChange(draft.copy(description = it)) },
            onServerChange = onServerSelected,
        )

        ShortcutTypeSelector(
            destination = draft.destination,
            onTypeChange = { onDraftChange(draft.copy(destination = it)) },
            supportsEntity = state.selectedServer.supportsEntity,
        )

        ShortcutTargetInput(
            destination = draft.destination,
            entityDisplayState = state.selectedEntityDisplayState,
            onDestinationChange = { onDraftChange(draft.copy(destination = it)) },
        )

        PrimaryActionButtons(
            canSubmit = state.canSubmit,
            isSaving = state.isSaving,
            onSubmit = onSubmit,
            onDelete = onDelete,
            deleteLabelRes = deleteLabelRes,
        )
    }
}

@Composable
private fun ShortcutTypeSelector(
    destination: ShortcutDestination,
    onTypeChange: (ShortcutDestination) -> Unit,
    supportsEntity: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(HADimens.SPACE2)) {
        Text(
            text = stringResource(R.string.shortcut_target_type),
            style = HATextStyle.Body.copy(textAlign = TextAlign.Start),
        )

        val options = buildList<RadioOption<ShortcutDestination>> {
            add(
                RadioOption(
                    selectionKey = ShortcutDestination.Dashboard(""),
                    headline = stringResource(R.string.shortcut_target_open_dashboard),
                ),
            )
            if (supportsEntity) {
                add(
                    RadioOption(
                        selectionKey = ShortcutDestination.Entity(""),
                        headline = stringResource(R.string.shortcut_target_open_entity),
                    ),
                )
            }
        }

        HARadioGroup(
            spaceBy = HADimens.SPACE3,
            options = options,
            selectionKey = if (destination is ShortcutDestination.Entity) {
                ShortcutDestination.Entity("")
            } else {
                ShortcutDestination.Dashboard("")
            },
            onSelect = { selected ->
                onTypeChange(
                    when (selected.selectionKey) {
                        is ShortcutDestination.Dashboard -> ShortcutDestination.Dashboard("")
                        is ShortcutDestination.Entity -> ShortcutDestination.Entity("")
                    },
                )
            },
        )
    }
}

@Composable
private fun ShortcutMetadataFields(
    draft: ShortcutDraft,
    servers: List<ShortcutServer>,
    isServerSelectionVisible: Boolean,
    labelText: String,
    descriptionText: String,
    onLabelChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onServerChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(HADimens.SPACE2)) {
        HATextField(
            value = draft.label,
            onValueChange = onLabelChange,
            label = { Text(labelText) },
            modifier = Modifier.fillMaxWidth(),
        )

        HATextField(
            value = draft.description,
            onValueChange = onDescriptionChange,
            label = { Text(descriptionText) },
            supportingText = {
                Text(stringResource(R.string.shortcut_description_support))
            },
            modifier = Modifier.fillMaxWidth(),
        )

        if (isServerSelectionVisible) {
            ServerPicker(
                servers = servers,
                selectedServerId = draft.serverId,
                onServerSelected = onServerChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ShortcutTargetInput(
    destination: ShortcutDestination,
    entityDisplayState: EntityDisplayState,
    onDestinationChange: (ShortcutDestination) -> Unit,
) {
    when (destination) {
        is ShortcutDestination.Dashboard -> {
            HATextField(
                value = destination.path,
                onValueChange = { onDestinationChange(ShortcutDestination.Dashboard(it)) },
                label = { Text(stringResource(R.string.shortcut_dashboard_path_label)) },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Uri,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is ShortcutDestination.Entity -> {
            EntityPicker(
                displayState = entityDisplayState,
                selectedEntityId = destination.entityId.takeIf { it.isNotBlank() },
                onSelectionChanged = { entityId ->
                    onDestinationChange(ShortcutDestination.Entity(entityId ?: ""))
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PrimaryActionButtons(
    canSubmit: Boolean,
    isSaving: Boolean,
    onSubmit: () -> Unit,
    onDelete: (() -> Unit)?,
    deleteLabelRes: Int = R.string.delete,
) {
    val isEditing = onDelete != null
    val submitLabelRes = if (isEditing) R.string.update else R.string.add_shortcut

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HADimens.SPACE2),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HADimens.SPACE2),
        ) {
            if (onDelete != null) {
                HAFilledButton(
                    text = stringResource(deleteLabelRes),
                    onClick = onDelete,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                    variant = ButtonVariant.DANGER,
                )
            }

            HAFilledButton(
                text = stringResource(submitLabelRes),
                onClick = onSubmit,
                enabled = canSubmit && !isSaving,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@HAPreviews
@Composable
private fun ShortcutEditorFormPreview() {
    HAThemeForPreview {
        ShortcutEditorForm(
            state = EditorState(
                servers = ShortcutPreviewData.previewServers,
                entityDisplayStatesByServerId = ShortcutPreviewData.previewEntityDisplayStates,
                draft = ShortcutPreviewData.previewAppDraft,
            ),
            labelText = "Label",
            descriptionText = "Description",
            onDraftChange = {},
            onServerSelected = {},
            onSubmit = {},
            onDelete = {},
        )
    }
}

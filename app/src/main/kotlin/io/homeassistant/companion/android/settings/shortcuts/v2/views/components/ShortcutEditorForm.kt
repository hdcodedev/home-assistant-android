package io.homeassistant.companion.android.settings.shortcuts.v2.views.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.common.compose.composable.ButtonVariant
import io.homeassistant.companion.android.common.compose.composable.HAFilledButton
import io.homeassistant.companion.android.common.compose.composable.HARadioGroup
import io.homeassistant.companion.android.common.compose.composable.HATextField
import io.homeassistant.companion.android.common.compose.composable.RadioOption
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDestination
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDraft
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutType
import io.homeassistant.companion.android.settings.shortcuts.v2.views.preview.ShortcutPreviewData
import io.homeassistant.companion.android.settings.shortcuts.v2.views.screens.ShortcutEditorScreenState
import io.homeassistant.companion.android.util.compose.ServerExposedDropdownMenu
import io.homeassistant.companion.android.util.compose.entity.EntityPicker

@Composable
internal fun ShortcutEditorForm(
    draft: ShortcutDraft,
    labelText: String,
    descriptionText: String,
    screen: ShortcutEditorScreenState,
    onDraftChange: (ShortcutDraft) -> Unit,
    isEditing: Boolean,
    canSubmit: Boolean,
    isSaving: Boolean,
    onSubmit: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(HADimens.SPACE4)) {
        ShortcutMetadataFields(
            draft = draft,
            labelText = labelText,
            descriptionText = descriptionText,
            screen = screen,
            onLabelChange = { onDraftChange(draft.copy(label = it)) },
            onDescriptionChange = { onDraftChange(draft.copy(description = it)) },
            onServerChange = { onDraftChange(draft.copy(serverId = it)) },
        )

        ShortcutTypeSelector(
            type = when (draft.destination) {
                is ShortcutDestination.Lovelace -> ShortcutType.LOVELACE
                is ShortcutDestination.Entity -> ShortcutType.ENTITY_ID
            },
            onTypeChange = { onDraftChange(draft.withType(it)) },
        )

        ShortcutTargetInput(
            destination = draft.destination,
            screen = screen,
            serverId = draft.serverId,
            onDestinationChange = { onDraftChange(draft.copy(destination = it)) },
        )

        PrimaryActionButtons(
            isEditing = isEditing,
            canSubmit = canSubmit,
            isSaving = isSaving,
            onSubmit = onSubmit,
            onDelete = onDelete,
        )
    }
}

internal fun isDraftValidForSubmit(draft: ShortcutDraft, screen: ShortcutEditorScreenState): Boolean {
    val hasValidTarget = when (val destination = draft.destination) {
        is ShortcutDestination.Lovelace -> destination.path.isNotBlank()
        is ShortcutDestination.Entity -> destination.entityId.isNotBlank()
    }
    val hasValidServer = screen.servers.size == 1 ||
        draft.serverId?.let { selectedId -> screen.servers.any { it.id == selectedId } } == true
    return draft.label.isNotEmpty() &&
        draft.description.isNotEmpty() &&
        hasValidTarget &&
        hasValidServer
}

private fun ShortcutDraft.withType(type: ShortcutType): ShortcutDraft {
    val newTarget = when (type) {
        ShortcutType.LOVELACE -> {
            destination as? ShortcutDestination.Lovelace ?: ShortcutDestination.Lovelace("")
        }

        ShortcutType.ENTITY_ID -> {
            destination as? ShortcutDestination.Entity ?: ShortcutDestination.Entity("")
        }
    }
    return copy(destination = newTarget)
}

private fun resolveServerIdForEditor(serverId: Int?, screen: ShortcutEditorScreenState): Int? {
    if (serverId != null && screen.servers.any { it.id == serverId }) return serverId
    return if (screen.servers.size == 1) screen.servers.first().id else null
}

@Composable
private fun ShortcutMetadataFields(
    draft: ShortcutDraft,
    labelText: String,
    descriptionText: String,
    screen: ShortcutEditorScreenState,
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
                Text(stringResource(R.string.shortcut_v2_description_support))
            },
            modifier = Modifier.fillMaxWidth(),
        )

        if (screen.servers.size > 1) {
            ServerExposedDropdownMenu(
                servers = screen.servers,
                current = draft.serverId,
                onSelected = onServerChange,
            )
        }
    }
}

@Composable
private fun ShortcutTypeSelector(type: ShortcutType, onTypeChange: (ShortcutType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(HADimens.SPACE2)) {
        Text(
            text = stringResource(R.string.shortcut_v2_target_type),
            style = HATextStyle.Body.copy(textAlign = TextAlign.Start),
        )

        HARadioGroup(
            spaceBy = HADimens.SPACE3,
            options = listOf(
                RadioOption(
                    selectionKey = ShortcutType.LOVELACE,
                    headline = stringResource(R.string.shortcut_v2_target_open_dashboard),
                ),
                RadioOption(
                    selectionKey = ShortcutType.ENTITY_ID,
                    headline = stringResource(R.string.shortcut_v2_target_open_entity),
                ),
            ),
            selectionKey = type,
            onSelect = { selected -> onTypeChange(selected.selectionKey) },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortcutTargetInput(
    destination: ShortcutDestination,
    screen: ShortcutEditorScreenState,
    serverId: Int?,
    onDestinationChange: (ShortcutDestination) -> Unit,
) {
    when (destination) {
        is ShortcutDestination.Lovelace -> {
            val bringIntoViewRequester = remember { BringIntoViewRequester() }

            HATextField(
                value = destination.path,
                onValueChange = { onDestinationChange(ShortcutDestination.Lovelace(it)) },
                label = { Text(stringResource(R.string.shortcut_v2_dashboard_path_label)) },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Uri,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(bringIntoViewRequester),
            )
        }

        is ShortcutDestination.Entity -> {
            val resolvedServerId = resolveServerIdForEditor(serverId, screen)
            val selectedEntityId = destination.entityId.takeIf { it.isNotBlank() }
            val entities = resolvedServerId?.let { screen.entities[it] } ?: emptyList()
            val entityRegistry = resolvedServerId?.let { screen.entityRegistry[it] }
            val deviceRegistry = resolvedServerId?.let { screen.deviceRegistry[it] }
            val areaRegistry = resolvedServerId?.let { screen.areaRegistry[it] }
            EntityPicker(
                entities = entities,
                entityRegistry = entityRegistry,
                deviceRegistry = deviceRegistry,
                areaRegistry = areaRegistry,
                selectedEntityId = selectedEntityId,
                onEntitySelectedId = { entityId ->
                    onDestinationChange(ShortcutDestination.Entity(entityId))
                },
                onEntityCleared = {
                    onDestinationChange(ShortcutDestination.Entity(""))
                },
            )
        }
    }
}

@Composable
private fun PrimaryActionButtons(
    isEditing: Boolean,
    canSubmit: Boolean,
    isSaving: Boolean,
    onSubmit: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val submitLabelRes = if (isEditing) R.string.update else R.string.add_shortcut

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HADimens.SPACE2),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HADimens.SPACE2),
        ) {
            if (isEditing && onDelete != null) {
                HAFilledButton(
                    text = stringResource(R.string.delete),
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

@Preview(name = "Shortcut Editor Form")
@Composable
private fun ShortcutEditorFormPreview() {
    HAThemeForPreview {
        ShortcutEditorForm(
            draft = ShortcutPreviewData.buildDraft(),
            labelText = "Label",
            descriptionText = "Description",
            screen = ShortcutPreviewData.buildScreenState(servers = ShortcutPreviewData.previewServers),
            onDraftChange = {},
            isEditing = true,
            canSubmit = true,
            isSaving = false,
            onSubmit = {},
            onDelete = {},
        )
    }
}

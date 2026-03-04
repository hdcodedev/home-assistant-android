package io.homeassistant.companion.android.settings.shortcuts.v2.views.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.homeassistant.companion.android.common.compose.composable.HALoading
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutDraft
import io.homeassistant.companion.android.common.data.shortcuts.entities.ShortcutError
import io.homeassistant.companion.android.settings.shortcuts.v2.ShortcutEditorUiState
import io.homeassistant.companion.android.settings.shortcuts.v2.views.components.AppShortcutEditor
import io.homeassistant.companion.android.settings.shortcuts.v2.views.components.EmptyStateContentSlots
import io.homeassistant.companion.android.settings.shortcuts.v2.views.components.EmptyStateNoServers
import io.homeassistant.companion.android.settings.shortcuts.v2.views.components.ErrorStateContent
import io.homeassistant.companion.android.settings.shortcuts.v2.views.components.HomeShortcutEditor
import io.homeassistant.companion.android.settings.shortcuts.v2.views.components.HomeShortcutsNotSupportedStateContent
import io.homeassistant.companion.android.settings.shortcuts.v2.views.components.NotSupportedStateContent
import io.homeassistant.companion.android.settings.shortcuts.v2.views.preview.ShortcutPreviewData
import io.homeassistant.companion.android.util.icondialog.IconDialog
import io.homeassistant.companion.android.util.icondialog.mdiName
import io.homeassistant.companion.android.util.plus
import io.homeassistant.companion.android.util.safeBottomPaddingValues

@Composable
internal fun ShortcutEditorScreen(
    state: ShortcutEditorUiState,
    dispatch: (ShortcutEditAction) -> Unit,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit),
) {
    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            HALoading()
        }
        return
    }

    val editor = state.editor ?: return
    val noServers = state.content.servers.isEmpty()
    val notSupported = state.content.error == ShortcutError.ApiNotSupported
    val homeShortcutsNotSupported = state.content.error == ShortcutError.HomeShortcutNotSupported
    when (editor) {
        is ShortcutEditorUiState.AppEditorState -> {
            when {
                notSupported -> NotSupportedStateContent()
                noServers -> EmptyStateNoServers()
                state.content.error == ShortcutError.SlotsFull -> EmptyStateContentSlots()
                state.content.error != null -> ErrorStateContent(onRetry = onRetry)
                else -> ShortcutEditorContent(
                    initialDraft = editor.initialDraft,
                    dispatch = dispatch,
                    onDelete = when (editor) {
                        is ShortcutEditorUiState.AppEditState ->
                            { { dispatch(ShortcutEditAction.DeleteAppShortcut(editor.appIndex)) } }
                        is ShortcutEditorUiState.AppCreateState -> null
                    },
                    modifier = modifier,
                ) { draft, onDraftChange, onIconClick, onSubmit, onDelete ->
                    AppShortcutEditor(
                        draft = draft,
                        state = editor,
                        screen = state.content,
                        onDraftChange = onDraftChange,
                        onIconClick = onIconClick,
                        onSubmit = onSubmit,
                        onDelete = onDelete,
                    )
                }
            }
        }

        is ShortcutEditorUiState.HomeEditorState -> {
            when {
                notSupported -> NotSupportedStateContent()
                homeShortcutsNotSupported -> HomeShortcutsNotSupportedStateContent()
                noServers -> EmptyStateNoServers()
                state.content.error != null -> ErrorStateContent(onRetry = onRetry)
                else -> {
                    ShortcutEditorContent(
                        initialDraft = editor.initialDraft,
                        dispatch = dispatch,
                        onDelete = when (editor) {
                            is ShortcutEditorUiState.HomeEditState ->
                                { { dispatch(ShortcutEditAction.DeleteHomeShortcut(editor.shortcutId)) } }
                            is ShortcutEditorUiState.HomeCreateState -> null
                        },
                        modifier = modifier,
                    ) { draft, onDraftChange, onIconClick, onSubmit, onDelete ->
                        HomeShortcutEditor(
                            draft = draft,
                            state = editor,
                            screen = state.content,
                            onDraftChange = onDraftChange,
                            onIconClick = onIconClick,
                            onSubmit = onSubmit,
                            onDelete = onDelete,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutEditorContent(
    initialDraft: ShortcutDraft,
    dispatch: (ShortcutEditAction) -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    editor: @Composable (
        draft: ShortcutDraft,
        onDraftChange: (ShortcutDraft) -> Unit,
        onIconClick: () -> Unit,
        onSubmit: () -> Unit,
        onDelete: (() -> Unit)?,
    ) -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(PaddingValues(all = HADimens.SPACE4) + safeBottomPaddingValues(applyHorizontal = false)),
        verticalArrangement = Arrangement.spacedBy(HADimens.SPACE4),
    ) {
        var draft by rememberSaveable(
            initialDraft.id,
            initialDraft.serverId,
            stateSaver = ShortcutDraftSaver,
        ) {
            mutableStateOf(initialDraft)
        }
        var showIconDialog by rememberSaveable { mutableStateOf(false) }
        val updateDraft: (ShortcutDraft) -> Unit = { updated ->
            draft = updated.copy()
        }

        if (showIconDialog) {
            IconDialog(
                onSelect = {
                    updateDraft(draft.copy(selectedIconName = it.mdiName))
                    showIconDialog = false
                },
                onDismissRequest = { showIconDialog = false },
            )
        }
        editor(
            draft,
            updateDraft,
            { showIconDialog = true },
            { dispatch(ShortcutEditAction.Submit(draft)) },
            onDelete,
        )
    }
}

@Preview(name = "Shortcut Editor App")
@Composable
private fun ShortcutEditorScreenAppPreview() {
    HAThemeForPreview {
        ShortcutEditorScreen(
            state = ShortcutEditorUiState(
                content = ShortcutPreviewData.buildScreenState(
                    servers = ShortcutPreviewData.previewServers,
                ),
                editor = ShortcutPreviewData.buildAppEditorState(),
            ),
            dispatch = {},
            onRetry = {},
        )
    }
}

@Preview(name = "Shortcut Editor Home")
@Composable
private fun ShortcutEditorScreenHomePreview() {
    HAThemeForPreview {
        ShortcutEditorScreen(
            state = ShortcutEditorUiState(
                content = ShortcutPreviewData.buildScreenState(
                    servers = ShortcutPreviewData.previewServers,
                ),
                editor = ShortcutPreviewData.buildHomeEditorState(),
            ),
            dispatch = {},
            onRetry = {},
        )
    }
}

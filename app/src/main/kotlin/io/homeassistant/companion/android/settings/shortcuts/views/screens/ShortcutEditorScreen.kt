package io.homeassistant.companion.android.settings.shortcuts.views.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.common.compose.composable.HALoading
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.settings.shortcuts.EditorState
import io.homeassistant.companion.android.settings.shortcuts.ShortcutsUiState
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDraft
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutError
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutIcon
import io.homeassistant.companion.android.settings.shortcuts.views.components.ErrorStateContent
import io.homeassistant.companion.android.settings.shortcuts.views.components.ShortcutEditorFields
import io.homeassistant.companion.android.settings.shortcuts.views.preview.ShortcutPreviewData
import io.homeassistant.companion.android.util.compose.HAPreviews
import io.homeassistant.companion.android.util.icondialog.IconDialog
import io.homeassistant.companion.android.common.util.mdiName
import io.homeassistant.companion.android.util.plus
import io.homeassistant.companion.android.util.safeBottomPaddingValues

// region Public type-safe wrappers

/**
 * Screen for creating a new app shortcut.
 */
@Composable
internal fun CreateAppShortcutScreen(
    uiState: ShortcutsUiState,
    onSubmit: () -> Unit,
    onUpdateDraft: (ShortcutDraft) -> Unit,
    onServerSelected: (Int) -> Unit = {},
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShortcutEditorScreen(
        uiState = uiState,
        title = stringResource(R.string.shortcut_add_app_shortcut_title),
        onSubmit = onSubmit,
        onUpdateDraft = onUpdateDraft,
        onServerSelected = onServerSelected,
        onRetry = onRetry,
        modifier = modifier,
    )
}

/**
 * Screen for editing an existing app shortcut.
 */
@Composable
internal fun EditAppShortcutScreen(
    uiState: ShortcutsUiState,
    onSubmit: () -> Unit,
    onUpdateDraft: (ShortcutDraft) -> Unit,
    onServerSelected: (Int) -> Unit = {},
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShortcutEditorScreen(
        uiState = uiState,
        title = stringResource(R.string.shortcut_edit_app_shortcut_title),
        onSubmit = onSubmit,
        onUpdateDraft = onUpdateDraft,
        onServerSelected = onServerSelected,
        onRetry = onRetry,
        onDelete = onDelete,
        deleteLabelRes = R.string.delete,
        modifier = modifier,
    )
}

/**
 * Screen for creating a new home shortcut.
 */
@Composable
internal fun CreateHomeShortcutScreen(
    uiState: ShortcutsUiState,
    onSubmit: () -> Unit,
    onUpdateDraft: (ShortcutDraft) -> Unit,
    onServerSelected: (Int) -> Unit = {},
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShortcutEditorScreen(
        uiState = uiState,
        title = stringResource(R.string.shortcut_add_home_shortcut_title),
        onSubmit = onSubmit,
        onUpdateDraft = onUpdateDraft,
        onServerSelected = onServerSelected,
        onRetry = onRetry,
        modifier = modifier,
    )
}

/**
 * Screen for editing an existing home shortcut.
 */
@Composable
internal fun EditHomeShortcutScreen(
    uiState: ShortcutsUiState,
    onSubmit: () -> Unit,
    onUpdateDraft: (ShortcutDraft) -> Unit,
    onServerSelected: (Int) -> Unit = {},
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShortcutEditorScreen(
        uiState = uiState,
        title = stringResource(R.string.shortcut_edit_home_shortcut_title),
        onSubmit = onSubmit,
        onUpdateDraft = onUpdateDraft,
        onServerSelected = onServerSelected,
        onRetry = onRetry,
        onDelete = onDelete,
        deleteLabelRes = R.string.disable,
        modifier = modifier,
    )
}

// endregion

// region Private implementation

/**
 * Displays the shortcut editor, or an appropriate empty, error or unsupported state when editing is not possible.
 */
@Composable
private fun ShortcutEditorScreen(
    uiState: ShortcutsUiState,
    title: String,
    onSubmit: () -> Unit,
    onUpdateDraft: (ShortcutDraft) -> Unit,
    onServerSelected: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    deleteLabelRes: Int = R.string.delete,
) {
    val screenModifier = modifier.fillMaxSize()
    when (uiState) {
        is ShortcutsUiState.Loading -> {
            Box(
                modifier = screenModifier,
                contentAlignment = Alignment.Center,
            ) {
                HALoading()
            }
        }
        is ShortcutsUiState.LoadError -> ScreenErrorContent(
            error = uiState.error,
            onRetry = onRetry,
            modifier = screenModifier,
        )
        is ShortcutsUiState.Editor -> {
            ReadyEditorContent(
                editor = uiState.state,
                title = title,
                onSubmit = onSubmit,
                onUpdateDraft = onUpdateDraft,
                onServerSelected = onServerSelected,
                onDelete = onDelete,
                deleteLabelRes = deleteLabelRes,
                modifier = screenModifier,
            )
        }
    }
}

@Composable
private fun ScreenErrorContent(error: ShortcutError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        ErrorStateContent(error = error, onRetry = onRetry)
    }
}

@Composable
private fun ReadyEditorContent(
    editor: EditorState,
    title: String,
    onSubmit: () -> Unit,
    onUpdateDraft: (ShortcutDraft) -> Unit,
    onServerSelected: (Int) -> Unit,
    onDelete: (() -> Unit)?,
    deleteLabelRes: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(PaddingValues(all = HADimens.SPACE4) + safeBottomPaddingValues(applyHorizontal = false)),
        verticalArrangement = Arrangement.spacedBy(HADimens.SPACE4),
    ) {
        var showIconDialog by rememberSaveable { mutableStateOf(false) }

        if (showIconDialog) {
            IconDialog(
                onSelect = {
                    onUpdateDraft(editor.draft.copy(icon = ShortcutIcon.Mdi(it.mdiName)))
                    showIconDialog = false
                },
                onDismissRequest = { showIconDialog = false },
            )
        }

        ShortcutEditorFields(
            state = editor,
            title = title,
            onDraftChange = onUpdateDraft,
            onServerSelected = onServerSelected,
            onIconClick = { showIconDialog = true },
            onSubmit = onSubmit,
            onDelete = onDelete,
            deleteLabelRes = deleteLabelRes,
        )
    }
}

// endregion

// region Previews

@HAPreviews
@Composable
private fun CreateAppShortcutScreenPreview() {
    HAThemeForPreview {
        CreateAppShortcutScreen(
            uiState = ShortcutsUiState.Editor(
                ShortcutPreviewData.buildEditorState(draft = ShortcutPreviewData.previewAppDraft),
            ),
            onSubmit = {},
            onUpdateDraft = {},
            onRetry = {},
        )
    }
}

@HAPreviews
@Composable
private fun EditAppShortcutScreenPreview() {
    HAThemeForPreview {
        EditAppShortcutScreen(
            uiState = ShortcutsUiState.Editor(
                ShortcutPreviewData.buildEditorState(draft = ShortcutPreviewData.previewEditAppDraft),
            ),
            onSubmit = {},
            onUpdateDraft = {},
            onDelete = {},
            onRetry = {},
        )
    }
}

@HAPreviews
@Composable
private fun CreateHomeShortcutScreenPreview() {
    HAThemeForPreview {
        CreateHomeShortcutScreen(
            uiState = ShortcutsUiState.Editor(
                ShortcutPreviewData.buildEditorState(draft = ShortcutDraft.initial(1)),
            ),
            onSubmit = {},
            onUpdateDraft = {},
            onRetry = {},
        )
    }
}

@HAPreviews
@Composable
private fun EditHomeShortcutScreenPreview() {
    HAThemeForPreview {
        EditHomeShortcutScreen(
            uiState = ShortcutsUiState.Editor(
                ShortcutPreviewData.buildEditorState(draft = ShortcutPreviewData.previewHomeDraft),
            ),
            onSubmit = {},
            onUpdateDraft = {},
            onDelete = {},
            onRetry = {},
        )
    }
}

@HAPreviews
@Composable
private fun ShortcutEditorScreenLoadingPreview() {
    HAThemeForPreview {
        CreateAppShortcutScreen(
            uiState = ShortcutsUiState.Loading,
            onSubmit = {},
            onUpdateDraft = {},
            onRetry = {},
        )
    }
}

@HAPreviews
@Composable
private fun ShortcutEditorScreenNoServersPreview() {
    HAThemeForPreview {
        CreateAppShortcutScreen(
            uiState = ShortcutsUiState.LoadError(ShortcutError.NoServersConfigured),
            onSubmit = {},
            onUpdateDraft = {},
            onRetry = {},
        )
    }
}

@HAPreviews
@Composable
private fun ShortcutEditorScreenUnsupportedPreview() {
    HAThemeForPreview {
        CreateAppShortcutScreen(
            uiState = ShortcutsUiState.LoadError(ShortcutError.AndroidVersionNotSupported),
            onSubmit = {},
            onUpdateDraft = {},
            onRetry = {},
        )
    }
}

// endregion

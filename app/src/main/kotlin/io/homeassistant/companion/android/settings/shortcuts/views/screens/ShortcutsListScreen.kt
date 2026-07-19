package io.homeassistant.companion.android.settings.shortcuts.views.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mikepenz.iconics.compose.IconicsPainter
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.common.compose.composable.HAFloatingActionButton
import io.homeassistant.companion.android.common.compose.composable.HALoading
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HARadius
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.settings.shortcuts.ManageShortcutsLoadState
import io.homeassistant.companion.android.settings.shortcuts.ManageShortcutsUiState
import io.homeassistant.companion.android.settings.shortcuts.ShortcutKind
import io.homeassistant.companion.android.settings.shortcuts.data.entities.AppShortcuts
import io.homeassistant.companion.android.settings.shortcuts.data.entities.HomeShortcuts
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDestination
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutError
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutIcon
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutListItem
import io.homeassistant.companion.android.settings.shortcuts.views.components.ErrorStateContent
import io.homeassistant.companion.android.settings.shortcuts.views.preview.ShortcutPreviewData
import io.homeassistant.companion.android.settings.views.EmptyState
import io.homeassistant.companion.android.util.compose.HAPreviews
import io.homeassistant.companion.android.util.compose.screenWidth
import io.homeassistant.companion.android.common.util.getIconByMdiName
import io.homeassistant.companion.android.util.plus
import io.homeassistant.companion.android.util.safeBottomPaddingValues
import io.homeassistant.companion.android.util.safeBottomWindowInsets

private val COMPACT_WIDTH_BREAKPOINT = 600.dp
private val DIVIDER_HEIGHT = 1.dp

internal sealed interface ShortcutsListAction {
    data class CreateShortcut(val kind: ShortcutKind) : ShortcutsListAction
    data class EditShortcut(val kind: ShortcutKind, val id: String) : ShortcutsListAction
}

/**
 * Displays the list of configured shortcuts (application dynamic shortcuts and pinned home screen shortcuts),
 * or an appropriate empty, error or unsupported state.
 *
 * @param state The current list state driving which content is shown.
 * @param onAction Called when the user chooses to create or edit a shortcut.
 * @param onRetry Called when the user requests to retry after an error.
 * @param modifier Optional [Modifier] applied to the screen root.
 */
@Composable
internal fun ShortcutsListScreen(
    state: ManageShortcutsUiState,
    onAction: (ShortcutsListAction) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    val dismissCreateDialog: () -> Unit = { showCreateDialog = false }
    Scaffold(
        floatingActionButton = {
            if (state.loadState is ManageShortcutsLoadState.Ready) {
                HAFloatingActionButton(
                    icon = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_shortcut),
                    modifier = Modifier.padding(safeBottomPaddingValues(applyHorizontal = false)),
                    onClick = { showCreateDialog = true },
                )
            }
        },
        contentWindowInsets = safeBottomWindowInsets(),
        modifier = modifier,
    ) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            when (val load = state.loadState) {
                is ManageShortcutsLoadState.Loading -> LoadingState()
                is ManageShortcutsLoadState.Error -> ErrorStateContent(
                    error = load.error,
                    onRetry = onRetry,
                )
                is ManageShortcutsLoadState.Ready -> if (state.isEmpty) {
                    EmptyStateContent()
                } else {
                    ShortcutsList(
                        appShortcuts = state.appShortcuts,
                        homeShortcuts = state.homeShortcuts,
                        onAction = onAction,
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateShortcutDialog(
            canCreateAppShortcut = state.canCreateAppShortcut,
            canCreateHomeShortcut = state.isHomeSupported,
            onCreateAppShortcut = {
                if (state.canCreateAppShortcut) {
                    dismissCreateDialog()
                    onAction(ShortcutsListAction.CreateShortcut(kind = ShortcutKind.APP))
                }
            },
            onCreateHomeShortcut = {
                if (state.isHomeSupported) {
                    dismissCreateDialog()
                    onAction(ShortcutsListAction.CreateShortcut(kind = ShortcutKind.HOME))
                }
            },
            onDismissRequest = dismissCreateDialog,
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        HALoading()
    }
}

@Composable
private fun ShortcutsList(
    appShortcuts: AppShortcuts,
    homeShortcuts: HomeShortcuts,
    onAction: (ShortcutsListAction) -> Unit,
) {
    val homeItems = homeShortcuts.items
    val isCompactScreen = screenWidth() < COMPACT_WIDTH_BREAKPOINT
    val columnsCount = if (isCompactScreen) 3 else 4

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnsCount),
        contentPadding = PaddingValues(all = HADimens.SPACE4) +
            safeBottomPaddingValues(applyHorizontal = false) +
            PaddingValues(bottom = HADimens.SPACE18),
        verticalArrangement = Arrangement.spacedBy(HADimens.SPACE3),
        horizontalArrangement = Arrangement.spacedBy(HADimens.SPACE3),
    ) {
        if (appShortcuts.items.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    text = stringResource(R.string.shortcut_app_shortcuts_header),
                    subtitle = stringResource(R.string.shortcut_dynamic_section_subtitle),
                    showAppIcon = true,
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                AppShortcutsOverview(
                    items = appShortcuts.items,
                    maxAppShortcuts = appShortcuts.maxAppShortcuts,
                    onEditAppShortcut = { id ->
                        onAction(ShortcutsListAction.EditShortcut(kind = ShortcutKind.APP, id = id))
                    },
                )
            }
        }

        if (homeItems.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    text = stringResource(R.string.shortcut_home_screen_shortcuts_header),
                    subtitle = stringResource(R.string.shortcut_pinned_section_subtitle),
                )
            }
            items(items = homeItems, key = { it.shortcut.id }) { summary ->
                val shortcut = summary.shortcut
                val label = shortcut.label.ifBlank { shortcut.id }
                ShortcutGridItem(
                    label = label,
                    icon = shortcut.icon,
                    isCompactScreen = isCompactScreen,
                    isDisabled = !summary.isEnabled,
                    onClick = {
                        onAction(ShortcutsListAction.EditShortcut(kind = ShortcutKind.HOME, id = shortcut.id))
                    },
                )
            }
        }
    }
}

/**
 * Renders the application (dynamic) shortcuts panel: the slot capacity, per-slot numbers, and the
 * list of configured shortcuts that can be tapped to edit.
 */
@Composable
private fun AppShortcutsOverview(
    items: List<ShortcutListItem>,
    maxAppShortcuts: Int,
    onEditAppShortcut: (String) -> Unit,
) {
    val colors = LocalHAColorScheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = HADimens.SPACE1),
        verticalArrangement = Arrangement.spacedBy(HADimens.SPACE3),
    ) {
        Text(
            text = pluralStringResource(R.plurals.shortcut_dynamic_slots_capacity, maxAppShortcuts, maxAppShortcuts),
            style = HATextStyle.BodyMedium,
            color = colors.colorTextSecondary,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HADimens.SPACE2),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.width(HADimens.SPACE6)) {
                items.forEachIndexed { position, item ->
                    val slotNumber = position + 1
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = HADimens.SPACE12),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = slotNumber.toString(),
                            style = HATextStyle.BodyMedium,
                            textAlign = TextAlign.Center,
                            color = colors.colorTextPrimary,
                            maxLines = 1,
                        )
                    }
                    if (position != items.lastIndex) {
                        Spacer(modifier = Modifier.height(DIVIDER_HEIGHT))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(HARadius.M))
                    .background(
                        color = colors.colorFillPrimaryQuietResting,
                    ),
            ) {
                items.forEachIndexed { position, item ->
                    val label = item.label.ifBlank {
                        stringResource(R.string.shortcut_n, position + 1)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = HADimens.SPACE12)
                            .clickable(role = Role.Button, onClick = { onEditAppShortcut(item.id) })
                            .padding(horizontal = HADimens.SPACE3, vertical = HADimens.SPACE2),
                        horizontalArrangement = Arrangement.spacedBy(HADimens.SPACE2),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ShortcutIcon(
                            icon = item.icon,
                            modifier = Modifier.size(HADimens.SPACE5),
                            tint = colors.colorFillPrimaryLoudResting,
                        )
                        Text(
                            text = label,
                            style = HATextStyle.BodyMedium,
                            color = colors.colorTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (position != items.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(DIVIDER_HEIGHT)
                                .background(colors.colorBorderNeutralQuiet),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, subtitle: String? = null, showAppIcon: Boolean = false) {
    val colors = LocalHAColorScheme.current
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HADimens.SPACE2),
        ) {
            if (showAppIcon) {
                Box(
                    modifier = Modifier
                        .size(HADimens.SPACE14)
                        .background(
                            color = colors.colorFillPrimaryLoudResting,
                            shape = RoundedCornerShape(HARadius.XL),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_stat_ic_notification_blue),
                        contentDescription = null,
                        tint = colors.colorOnPrimaryLoud,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(HADimens.SPACE1)) {
                Text(
                    text = text,
                    style = HATextStyle.HeadlineMedium,
                    color = colors.colorFillPrimaryLoudResting,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = HATextStyle.BodyMedium,
                        color = colors.colorTextSecondary,
                    )
                }
            }
        }
    }
}

/**
 * Renders the visual representation of a [ShortcutIcon], falling back to the default app icon
 * when the icon is not an MDI icon.
 *
 * @param icon The shortcut icon to render. This is decorative because the row or grid item carries the action.
 * @param tint Color applied to the icon.
 * @param modifier Optional [Modifier] applied to the icon.
 */
@Composable
private fun ShortcutIcon(icon: ShortcutIcon, tint: Color, modifier: Modifier = Modifier) {
    val mdiIcon = remember(icon) {
        (icon as? ShortcutIcon.Mdi)?.name?.let(CommunityMaterial::getIconByMdiName)
    }
    val painter = when {
        mdiIcon != null -> remember(mdiIcon) { IconicsPainter(mdiIcon) }
        else -> painterResource(R.drawable.ic_stat_ic_notification_blue)
    }
    Icon(
        painter = painter,
        contentDescription = null,
        modifier = modifier,
        tint = tint,
    )
}

/**
 * A single shortcut shown as a centered icon badge with a label below it.
 *
 * @param label Text displayed under the icon.
 * @param icon The shortcut icon to render.
 * @param isCompactScreen Whether the layout is in compact width mode, used to pick sizes.
 * @param isDisabled When true the item is dimmed and not clickable.
 * @param onClick Called when the user taps the item.
 */
@Composable
private fun ShortcutGridItem(
    label: String,
    icon: ShortcutIcon,
    isCompactScreen: Boolean,
    isDisabled: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = LocalHAColorScheme.current
    val badgeSize = if (isCompactScreen) HADimens.SPACE14 else HADimens.SPACE16
    val iconSize = if (isCompactScreen) HADimens.SPACE7 else HADimens.SPACE8
    val labelGap = HADimens.SPACE2
    val textColor = if (isDisabled) colors.colorTextDisabled else colors.colorTextPrimary
    val iconTint = if (isDisabled) colors.colorTextDisabled else colors.colorFillPrimaryLoudResting
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(HARadius.M))
            .clickable(enabled = !isDisabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = HADimens.SPACE2, vertical = HADimens.SPACE2),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(badgeSize)
                .background(
                    color = colors.colorFillPrimaryQuietResting,
                    shape = RoundedCornerShape(HARadius.XL),
                ),
            contentAlignment = Alignment.Center,
        ) {
            ShortcutIcon(
                icon = icon,
                modifier = Modifier.size(iconSize),
                tint = iconTint,
            )
        }
        Spacer(modifier = Modifier.size(labelGap))
        Text(
            text = label,
            style = HATextStyle.BodyMedium,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Placeholder shown when the user has no shortcuts configured yet.
 */
@Composable
private fun EmptyStateContent() {
    EmptyState(
        icon = CommunityMaterial.Icon2.cmd_flash,
        title = stringResource(R.string.shortcut_empty_title),
        subtitle = stringResource(R.string.shortcut_empty_subtitle),
    )
}

@HAPreviews
@Composable
private fun ShortcutsListScreenPreview() {
    val appSummaries = ShortcutPreviewData.buildAppSummaries(
        count = 4,
        destination = ShortcutDestination.Dashboard("/lovelace/shortcut"),
    ).toList()
    val baseHome = ShortcutPreviewData.buildHomeSummaries().first()
    val homeSummaries = listOf(
        baseHome,
        baseHome.copy(
            shortcut = baseHome.shortcut.copy(
                id = "shortcut_2",
                label = "Home 2",
            ),
        ),
        baseHome.copy(
            shortcut = baseHome.shortcut.copy(
                id = "shortcut_3",
                label = "Home 3",
            ),
        ),
    ).toList()
    HAThemeForPreview {
        ShortcutsListScreen(
            state = ShortcutPreviewData.buildListState(
                appSummaries = appSummaries,
                homeSummaries = homeSummaries,
            ),
            onAction = {},
            onRetry = {},
        )
    }
}

@HAPreviews
@Composable
private fun ShortcutsListScreenLoadingPreview() {
    HAThemeForPreview {
        ShortcutsListScreen(
            state = ShortcutPreviewData.buildListState(loadState = ManageShortcutsLoadState.Loading),
            onAction = {},
            onRetry = {},
        )
    }
}

@HAPreviews
@Composable
private fun ShortcutsListScreenEmptyPreview() {
    HAThemeForPreview {
        ShortcutsListScreen(
            state = ShortcutPreviewData.buildListState(
                appSummaries = emptyList(),
                homeSummaries = emptyList(),
            ),
            onAction = {},
            onRetry = {},
        )
    }
}

@HAPreviews
@Composable
private fun ShortcutsListScreenErrorPreview() {
    HAThemeForPreview {
        ShortcutsListScreen(
            state = ShortcutPreviewData.buildListState(
                loadState = ManageShortcutsLoadState.Error(ShortcutError.Unknown),
            ),
            onAction = {},
            onRetry = {},
        )
    }
}

@HAPreviews
@Composable
private fun ShortcutsListScreenNotSupportedPreview() {
    HAThemeForPreview {
        ShortcutsListScreen(
            state = ManageShortcutsUiState(
                loadState = ManageShortcutsLoadState.Error(ShortcutError.AndroidVersionNotSupported),
                appShortcuts = AppShortcuts(emptyList(), maxAppShortcuts = 0),
                homeShortcuts = HomeShortcuts(emptyList(), canPinShortcuts = true),
            ),
            onAction = {},
            onRetry = {},
        )
    }
}

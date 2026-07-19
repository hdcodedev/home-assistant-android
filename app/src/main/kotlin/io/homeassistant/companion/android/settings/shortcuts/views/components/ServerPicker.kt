package io.homeassistant.companion.android.settings.shortcuts.views.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.common.compose.composable.ButtonSize
import io.homeassistant.companion.android.common.compose.composable.HAFilledButton
import io.homeassistant.companion.android.common.compose.composable.HAModalBottomSheet
import io.homeassistant.companion.android.common.compose.composable.rememberHAModalBottomSheetState
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HARadius
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutServer
import io.homeassistant.companion.android.util.compose.HAPreviews
import kotlinx.coroutines.launch

/**
 * Server selector rendered as a bottom action sheet.
 *
 * Takes [ShortcutServer] (a common data-layer projection) so no Room entities reach the UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ServerPicker(
    servers: List<ShortcutServer>,
    selectedServerId: Int,
    onServerSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val bottomSheetState = rememberHAModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val colorScheme = LocalHAColorScheme.current

    Column(modifier = modifier.fillMaxWidth()) {
        val selectedServer = servers.firstOrNull { it.id == selectedServerId }
        if (selectedServer != null) {
            val cornerShape = RoundedCornerShape(HARadius.XL)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.colorSurfaceLow, shape = cornerShape)
                    .clip(cornerShape)
                    .clickable(role = Role.DropdownList, onClick = { isExpanded = true })
                    .padding(horizontal = HADimens.SPACE4, vertical = HADimens.SPACE4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(HADimens.SPACE1),
                ) {
                    Text(
                        text = stringResource(R.string.server),
                        style = HATextStyle.BodyMedium.copy(textAlign = TextAlign.Start),
                        color = colorScheme.colorTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = selectedServer.name,
                        style = HATextStyle.Body.copy(textAlign = TextAlign.Start),
                        color = colorScheme.colorTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colorScheme.colorTextSecondary,
                    modifier = Modifier
                        .size(HADimens.SPACE5)
                        .rotate(if (isExpanded) 180f else 0f),
                )
            }
        } else {
            HAFilledButton(
                text = stringResource(R.string.server_select),
                onClick = { isExpanded = true },
                size = ButtonSize.SMALL,
            )
        }

        if (isExpanded) {
            HAModalBottomSheet(
                bottomSheetState = bottomSheetState,
                onDismissRequest = { isExpanded = false },
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                        .padding(HADimens.SPACE3),
                    verticalArrangement = Arrangement.spacedBy(HADimens.SPACE1),
                ) {
                    items(servers, key = { it.id }) { server ->
                        val isSelected = server.id == selectedServerId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = isSelected,
                                    role = Role.RadioButton,
                                ) {
                                    scope.launch {
                                        bottomSheetState.hide()
                                        onServerSelected(server.id)
                                        isExpanded = false
                                    }
                                }
                                .padding(HADimens.SPACE3),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(HADimens.SPACE3),
                        ) {
                            Text(
                                text = server.name,
                                style = HATextStyle.Body,
                                color = colorScheme.colorTextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = colorScheme.colorTextSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@HAPreviews
@Composable
private fun ServerPickerPreview() {
    HAThemeForPreview {
        ServerPicker(
            servers = listOf(
                ShortcutServer(
                    id = 1,
                    name = "Home",
                    supportsEntity = true,
                ),
                ShortcutServer(
                    id = 2,
                    name = "Office",
                    supportsEntity = true,
                ),
            ),
            selectedServerId = 1,
            onServerSelected = {},
        )
    }
}

package io.homeassistant.companion.android.settings.shortcuts.views.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.HiltComponentActivity
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.settings.shortcuts.ShortcutsUiState
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDraft
import io.homeassistant.companion.android.settings.shortcuts.views.preview.ShortcutPreviewData
import io.homeassistant.companion.android.testing.unit.stringResource
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
@HiltAndroidTest
class ShortcutEditorScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    @Test
    fun `Given create app shortcut when screen is shown then delete actions are hidden`() {
        composeTestRule.apply {
            setCreateAppContent()

            onNodeWithText(stringResource(commonR.string.add_shortcut)).performScrollTo().assertIsDisplayed()
            onNodeWithText(stringResource(commonR.string.delete)).assertDoesNotExist()
            onNodeWithText(stringResource(commonR.string.disable)).assertDoesNotExist()
        }
    }

    @Test
    fun `Given edit app shortcut when screen is shown then delete action is displayed`() {
        composeTestRule.apply {
            setEditAppContent()

            onNodeWithText(stringResource(commonR.string.update)).performScrollTo().assertIsDisplayed()
            onNodeWithText(stringResource(commonR.string.delete)).performScrollTo().assertIsDisplayed()
            onNodeWithText(stringResource(commonR.string.disable)).assertDoesNotExist()
        }
    }

    @Test
    fun `Given edit home shortcut when screen is shown then disable action is displayed`() {
        composeTestRule.apply {
            setEditHomeContent()

            onNodeWithText(stringResource(commonR.string.update)).performScrollTo().assertIsDisplayed()
            onNodeWithText(stringResource(commonR.string.disable)).performScrollTo().assertIsDisplayed()
            onNodeWithText(stringResource(commonR.string.delete)).assertDoesNotExist()
        }
    }

    @Test
    fun `Given create app shortcut when submit is selected then submit callback is invoked`() {
        var submitCount = 0

        composeTestRule.apply {
            setCreateAppContent(onSubmit = { submitCount++ })

            onNodeWithText(stringResource(commonR.string.add_shortcut))
                .performScrollTo()
                .assertIsEnabled()
                .performClick()

            assertEquals(1, submitCount)
        }
    }

    @Test
    fun `Given edit app shortcut when update is selected then submit callback is invoked`() {
        var submitCount = 0

        composeTestRule.apply {
            setEditAppContent(onSubmit = { submitCount++ })

            onNodeWithText(stringResource(commonR.string.update))
                .performScrollTo()
                .assertIsEnabled()
                .performClick()

            assertEquals(1, submitCount)
        }
    }

    @Test
    fun `Given edit app shortcut when delete is selected then delete callback is invoked`() {
        var deleteCount = 0

        composeTestRule.apply {
            setEditAppContent(onDelete = { deleteCount++ })

            onNodeWithText(stringResource(commonR.string.delete))
                .performScrollTo()
                .assertIsEnabled()
                .performClick()

            assertEquals(1, deleteCount)
        }
    }

    @Test
    fun `Given edit home shortcut when disable is selected then delete callback is invoked`() {
        var deleteCount = 0

        composeTestRule.apply {
            setEditHomeContent(onDelete = { deleteCount++ })

            onNodeWithText(stringResource(commonR.string.disable))
                .performScrollTo()
                .assertIsEnabled()
                .performClick()

            assertEquals(1, deleteCount)
        }
    }

    @Test
    fun `Given another server is selected when editor is shown then server selection callback is invoked`() {
        var selectedServerId: Int? = null

        composeTestRule.apply {
            setCreateAppContent(onServerSelected = { selectedServerId = it })

            onNodeWithText("Home").performScrollTo().performClick()
            onNodeWithText("Office").performClick()
            waitForIdle()

            assertEquals(2, selectedServerId)
        }
    }

    @Test
    fun `Given shortcut cannot be submitted when screen is shown then submit action is disabled`() {
        var submitCount = 0

        composeTestRule.apply {
            setCreateAppContent(
                uiState = ShortcutsUiState.Editor(
                    ShortcutPreviewData.buildEditorState(
                        draft = ShortcutPreviewData.previewAppDraft.copy(label = ""),
                    ),
                ),
                onSubmit = { submitCount++ },
            )

            onNodeWithText(stringResource(commonR.string.add_shortcut))
                .performScrollTo()
                .assertIsNotEnabled()

            assertEquals(0, submitCount)
        }
    }

    private fun setCreateAppContent(
        uiState: ShortcutsUiState = appEditorState,
        onSubmit: () -> Unit = {},
        onUpdateDraft: (ShortcutDraft) -> Unit = {},
        onServerSelected: (Int) -> Unit = {},
    ) {
        composeTestRule.setContent {
            HAThemeForPreview {
                CreateAppShortcutScreen(
                    uiState = uiState,
                    onSubmit = onSubmit,
                    onUpdateDraft = onUpdateDraft,
                    onServerSelected = onServerSelected,
                    onRetry = {},
                )
            }
        }
    }

    private fun setEditAppContent(
        uiState: ShortcutsUiState = appEditorState,
        onSubmit: () -> Unit = {},
        onUpdateDraft: (ShortcutDraft) -> Unit = {},
        onServerSelected: (Int) -> Unit = {},
        onDelete: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            HAThemeForPreview {
                EditAppShortcutScreen(
                    uiState = uiState,
                    onSubmit = onSubmit,
                    onUpdateDraft = onUpdateDraft,
                    onServerSelected = onServerSelected,
                    onDelete = onDelete,
                    onRetry = {},
                )
            }
        }
    }

    private fun setEditHomeContent(
        uiState: ShortcutsUiState = homeEditorState,
        onSubmit: () -> Unit = {},
        onUpdateDraft: (ShortcutDraft) -> Unit = {},
        onServerSelected: (Int) -> Unit = {},
        onDelete: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            HAThemeForPreview {
                EditHomeShortcutScreen(
                    uiState = uiState,
                    onSubmit = onSubmit,
                    onUpdateDraft = onUpdateDraft,
                    onServerSelected = onServerSelected,
                    onDelete = onDelete,
                    onRetry = {},
                )
            }
        }
    }

    private val appEditorState: ShortcutsUiState
        get() = ShortcutsUiState.Editor(
            ShortcutPreviewData.buildEditorState(draft = ShortcutPreviewData.previewEditAppDraft),
        )

    private val homeEditorState: ShortcutsUiState
        get() = ShortcutsUiState.Editor(
            ShortcutPreviewData.buildEditorState(draft = ShortcutPreviewData.previewHomeDraft),
        )
}

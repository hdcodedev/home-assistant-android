package io.homeassistant.companion.android.settings.shortcuts.views.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.HiltComponentActivity
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.settings.shortcuts.ManageShortcutsUiState
import io.homeassistant.companion.android.settings.shortcuts.ShortcutKind
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDestination
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
class ShortcutsListScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    @Test
    fun `Given only app shortcuts when screen is shown then only app section is displayed`() {
        composeTestRule.apply {
            setShortcutListContent(
                state = ShortcutPreviewData.buildListState(
                    appSummaries = ShortcutPreviewData.buildAppSummaries(
                        count = 2,
                        destination = ShortcutDestination.Dashboard("/lovelace/shortcut"),
                    ),
                    homeSummaries = emptyList(),
                ),
            )

            onNodeWithText(stringResource(commonR.string.shortcut_app_shortcuts_header)).assertIsDisplayed()
            onNodeWithText(stringResource(commonR.string.shortcut_home_screen_shortcuts_header)).assertDoesNotExist()
        }
    }

    @Test
    fun `Given only home shortcuts when screen is shown then only home section is displayed`() {
        composeTestRule.apply {
            setShortcutListContent(
                state = ShortcutPreviewData.buildListState(
                    appSummaries = emptyList(),
                    homeSummaries = ShortcutPreviewData.buildHomeSummaries(count = 2),
                ),
            )

            onNodeWithText(stringResource(commonR.string.shortcut_app_shortcuts_header)).assertDoesNotExist()
            onNodeWithText(stringResource(commonR.string.shortcut_home_screen_shortcuts_header)).assertIsDisplayed()
        }
    }

    @Test
    fun `Given create app shortcut selected when dialog is shown then create app action is emitted`() {
        val actions = mutableListOf<ShortcutsListAction>()

        composeTestRule.apply {
            setShortcutListContent(onAction = actions::add)

            onNodeWithContentDescription(stringResource(commonR.string.add_shortcut)).performClick()
            onNodeWithText(stringResource(commonR.string.shortcut_add_to_app_shortcuts)).performClick()

            assertEquals(listOf(ShortcutsListAction.CreateShortcut(kind = ShortcutKind.APP)), actions)
        }
    }

    @Test
    fun `Given create home shortcut selected when dialog is shown then create home action is emitted`() {
        val actions = mutableListOf<ShortcutsListAction>()

        composeTestRule.apply {
            setShortcutListContent(onAction = actions::add)

            onNodeWithContentDescription(stringResource(commonR.string.add_shortcut)).performClick()
            onNodeWithText(stringResource(commonR.string.shortcut_add_to_home_screen)).performClick()

            assertEquals(listOf(ShortcutsListAction.CreateShortcut(kind = ShortcutKind.HOME)), actions)
        }
    }

    @Test
    fun `Given app shortcut item selected when list is shown then edit app action is emitted`() {
        val actions = mutableListOf<ShortcutsListAction>()

        composeTestRule.apply {
            setShortcutListContent(onAction = actions::add)

            onNodeWithText("Shortcut 1").performClick()

            assertEquals(listOf(ShortcutsListAction.EditShortcut(kind = ShortcutKind.APP, id = "shortcut_1")), actions)
        }
    }

    @Test
    fun `Given home shortcut item selected when list is shown then edit home action is emitted`() {
        val actions = mutableListOf<ShortcutsListAction>()

        composeTestRule.apply {
            setShortcutListContent(onAction = actions::add)

            onNodeWithText("Home").performClick()

            assertEquals(listOf(ShortcutsListAction.EditShortcut(kind = ShortcutKind.HOME, id = "home_1")), actions)
        }
    }

    private fun setShortcutListContent(
        state: ManageShortcutsUiState = ShortcutPreviewData.buildListState(),
        onAction: (ShortcutsListAction) -> Unit = {},
    ) {
        composeTestRule.setContent {
            HAThemeForPreview {
                ShortcutsListScreen(
                    state = state,
                    onAction = onAction,
                    onRetry = {},
                )
            }
        }
    }
}

package io.homeassistant.companion.android.settings.shortcuts.navigation

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.toRoute
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.HiltComponentActivity
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.settings.shortcuts.ManageShortcutsUiState
import io.homeassistant.companion.android.settings.shortcuts.ManageShortcutsViewModel
import io.homeassistant.companion.android.settings.shortcuts.ShortcutKind
import io.homeassistant.companion.android.settings.shortcuts.data.entities.AppShortcuts
import io.homeassistant.companion.android.settings.shortcuts.data.entities.HomeShortcuts
import io.homeassistant.companion.android.settings.shortcuts.views.screens.ShortcutsListAction
import io.homeassistant.companion.android.testing.unit.seedFakeAndroidId
import io.homeassistant.companion.android.testing.unit.stringResource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
@HiltAndroidTest
internal class ShortcutsNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    @BindValue
    @JvmField
    val manageShortcutsViewModel: ManageShortcutsViewModel = mockk(relaxed = true) {
        every { uiState } returns MutableStateFlow(
            ManageShortcutsUiState(
                appShortcuts = AppShortcuts(emptyList(), maxAppShortcuts = 0),
                homeShortcuts = HomeShortcuts(emptyList(), canPinShortcuts = true),
            ),
        )
    }

    private lateinit var navController: TestNavHostController

    private val toolbarTitles = mutableListOf<String>()

    @Before
    fun setup() {
        ApplicationProvider.getApplicationContext<Context>().seedFakeAndroidId()
    }

    @Test
    fun `Given shortcuts list route when navigating to create app then create app route is shown`() {
        setContent()

        composeTestRule.runOnIdle {
            navController.navigate(ShortcutCreateRoute(kind = ShortcutKind.APP))
        }

        composeTestRule.runOnIdle {
            assertTrue(navController.currentBackStackEntry?.destination?.hasRoute<ShortcutCreateRoute>() == true)
            assertEquals(
                ShortcutKind.APP,
                navController.currentBackStackEntry?.toRoute<ShortcutCreateRoute>()?.kind,
            )
        }
    }

    @Test
    fun `Given shortcuts list route when navigating to edit home then edit home route is shown with id`() {
        setContent()

        composeTestRule.runOnIdle {
            navController.navigate(ShortcutEditRoute(kind = ShortcutKind.HOME, id = "home_1"))
        }

        composeTestRule.runOnIdle {
            assertTrue(navController.currentBackStackEntry?.destination?.hasRoute<ShortcutEditRoute>() == true)
            val route = navController.currentBackStackEntry?.toRoute<ShortcutEditRoute>()
            assertEquals(ShortcutKind.HOME, route?.kind)
            assertEquals("home_1", route?.id)
        }
    }

    @Test
    fun `Given edit app route when navigating back then shortcuts list route is shown`() {
        setContent()

        composeTestRule.runOnIdle {
            navController.navigate(ShortcutEditRoute(kind = ShortcutKind.APP, id = "shortcut_1"))
            navController.popBackStack()
        }

        composeTestRule.runOnIdle {
            assertTrue(navController.currentBackStackEntry?.destination?.hasRoute<ShortcutsListRoute>() == true)
        }
    }

    @Test
    fun `Given shortcuts list action when creating app shortcut then create app route is shown`() {
        setContent()

        composeTestRule.runOnIdle {
            navController.navigateToShortcut(ShortcutsListAction.CreateShortcut(kind = ShortcutKind.APP))
        }

        composeTestRule.runOnIdle {
            assertTrue(navController.currentBackStackEntry?.destination?.hasRoute<ShortcutCreateRoute>() == true)
            assertEquals(
                ShortcutKind.APP,
                navController.currentBackStackEntry?.toRoute<ShortcutCreateRoute>()?.kind,
            )
        }
    }

    @Test
    fun `Given shortcuts list action when editing home shortcut then edit home route is shown with id`() {
        setContent()

        composeTestRule.runOnIdle {
            navController.navigateToShortcut(ShortcutsListAction.EditShortcut(kind = ShortcutKind.HOME, id = "home_1"))
        }

        composeTestRule.runOnIdle {
            assertTrue(navController.currentBackStackEntry?.destination?.hasRoute<ShortcutEditRoute>() == true)
            val route = navController.currentBackStackEntry?.toRoute<ShortcutEditRoute>()
            assertEquals(ShortcutKind.HOME, route?.kind)
            assertEquals("home_1", route?.id)
        }
    }

    @Test
    fun `Given shortcuts list action when editing app shortcut then edit app route is shown with id`() {
        setContent()

        composeTestRule.runOnIdle {
            navController.navigateToShortcut(ShortcutsListAction.EditShortcut(kind = ShortcutKind.APP, id = "shortcut_1"))
        }

        composeTestRule.runOnIdle {
            assertTrue(navController.currentBackStackEntry?.destination?.hasRoute<ShortcutEditRoute>() == true)
            val route = navController.currentBackStackEntry?.toRoute<ShortcutEditRoute>()
            assertEquals(ShortcutKind.APP, route?.kind)
            assertEquals("shortcut_1", route?.id)
        }
    }

    @Test
    fun `Given shortcuts list route when navigating to create home then create home route is shown`() {
        setContent()

        composeTestRule.runOnIdle {
            navController.navigate(ShortcutCreateRoute(kind = ShortcutKind.HOME))
        }

        composeTestRule.runOnIdle {
            assertTrue(navController.currentBackStackEntry?.destination?.hasRoute<ShortcutCreateRoute>() == true)
            assertEquals(
                ShortcutKind.HOME,
                navController.currentBackStackEntry?.toRoute<ShortcutCreateRoute>()?.kind,
            )
        }
    }

    @Test
    fun `Given list route when composing host then toolbar title is shortcuts`() {
        setContent()

        composeTestRule.runOnIdle {
            assertEquals(composeTestRule.stringResource(commonR.string.shortcuts), toolbarTitles.firstOrNull())
        }
    }

    @Test
    fun `Given list route when navigating to create app then toolbar title updates to add app title`() {
        setContent()

        composeTestRule.runOnIdle {
            navController.navigate(ShortcutCreateRoute(kind = ShortcutKind.APP))
        }

        composeTestRule.runOnIdle {
            assertEquals(
                composeTestRule.stringResource(commonR.string.shortcut_add_app_shortcut_title),
                toolbarTitles.last(),
            )
        }
    }

    @Test
    fun `Given list route when navigating to edit home then toolbar title updates to edit home title`() {
        setContent()

        composeTestRule.runOnIdle {
            navController.navigate(ShortcutEditRoute(kind = ShortcutKind.HOME, id = "home_1"))
        }

        composeTestRule.runOnIdle {
            assertEquals(
                composeTestRule.stringResource(commonR.string.shortcut_edit_home_shortcut_title),
                toolbarTitles.last(),
            )
        }
    }

    private fun setContent() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            val snackbarHostState = remember { SnackbarHostState() }
            NavHost(
                navController = navController,
                startDestination = ShortcutsListRoute,
            ) {
                shortcuts(
                    navController = navController,
                    snackbarHostState = snackbarHostState,
                    onShowShortcutSnackbar = {},
                )
            }
            ShortcutsToolbarTitleEffect(navController) { toolbarTitles.add(it) }
        }
    }
}

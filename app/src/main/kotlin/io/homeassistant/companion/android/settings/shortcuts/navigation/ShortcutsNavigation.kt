package io.homeassistant.companion.android.settings.shortcuts.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.settings.shortcuts.EditorRoute
import io.homeassistant.companion.android.settings.shortcuts.ManageShortcutsViewModel
import io.homeassistant.companion.android.settings.shortcuts.ShortcutCloseEvent
import io.homeassistant.companion.android.settings.shortcuts.ShortcutEditorViewModel
import io.homeassistant.companion.android.settings.shortcuts.ShortcutEditorViewModelFactory
import io.homeassistant.companion.android.settings.shortcuts.ShortcutKind
import io.homeassistant.companion.android.settings.shortcuts.views.screens.CreateAppShortcutScreen
import io.homeassistant.companion.android.settings.shortcuts.views.screens.CreateHomeShortcutScreen
import io.homeassistant.companion.android.settings.shortcuts.views.screens.EditAppShortcutScreen
import io.homeassistant.companion.android.settings.shortcuts.views.screens.EditHomeShortcutScreen
import io.homeassistant.companion.android.settings.shortcuts.views.screens.ShortcutsListAction
import io.homeassistant.companion.android.settings.shortcuts.views.screens.ShortcutsListScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
internal data object ShortcutsListRoute

@Serializable
internal data class ShortcutCreateRoute(val kind: ShortcutKind)

@Serializable
internal data class ShortcutEditRoute(val kind: ShortcutKind, val id: String)

internal fun NavController.navigateToShortcut(action: ShortcutsListAction) {
    when (action) {
        is ShortcutsListAction.CreateShortcut -> navigate(ShortcutCreateRoute(kind = action.kind))
        is ShortcutsListAction.EditShortcut -> navigate(ShortcutEditRoute(kind = action.kind, id = action.id))
    }
}

internal fun NavGraphBuilder.shortcuts(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    onShowShortcutSnackbar: (Int?) -> Unit,
) {
    composable<ShortcutsListRoute> {
        ShortcutsListRouteScreen(
            onAction = navController::navigateToShortcut,
        )
    }

    composable<ShortcutCreateRoute> { backStackEntry ->
        val route: ShortcutCreateRoute = backStackEntry.toRoute()
        ShortcutCreateScreen(
            kind = route.kind,
            onClose = { event ->
                navController.popBackStack()
                onShowShortcutSnackbar(event.messageRes)
            },
            snackbarHostState = snackbarHostState,
        )
    }

    composable<ShortcutEditRoute> { backStackEntry ->
        val route: ShortcutEditRoute = backStackEntry.toRoute()
        ShortcutEditScreen(
            kind = route.kind,
            id = route.id,
            onClose = { event ->
                navController.popBackStack()
                onShowShortcutSnackbar(event.messageRes)
            },
            snackbarHostState = snackbarHostState,
        )
    }
}

@Composable
internal fun ShortcutsNavHost(onToolbarTitleChanged: (String) -> Unit) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val resources = LocalResources.current
    val showShortcutSnackbar: (Int?) -> Unit = { messageRes ->
        messageRes?.let {
            snackbarScope.launch {
                snackbarHostState.showSnackbar(resources.getString(it))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = ShortcutsListRoute,
        ) {
            shortcuts(
                navController = navController,
                snackbarHostState = snackbarHostState,
                onShowShortcutSnackbar = showShortcutSnackbar,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }

    ShortcutsToolbarTitleEffect(navController, onToolbarTitleChanged)
}

@Composable
internal fun ShortcutsToolbarTitleEffect(navController: NavController, onToolbarTitleChanged: (String) -> Unit) {
    val shortcutsTitle = stringResource(R.string.shortcuts)
    val addAppShortcutTitle = stringResource(R.string.shortcut_add_app_shortcut_title)
    val addHomeShortcutTitle = stringResource(R.string.shortcut_add_home_shortcut_title)
    val editAppShortcutTitle = stringResource(R.string.shortcut_edit_app_shortcut_title)
    val editHomeShortcutTitle = stringResource(R.string.shortcut_edit_home_shortcut_title)
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            when {
                backStackEntry.destination.hasRoute(route = ShortcutsListRoute::class) -> {
                    onToolbarTitleChanged(shortcutsTitle)
                }

                backStackEntry.destination.hasRoute(route = ShortcutCreateRoute::class) -> {
                    onToolbarTitleChanged(
                        when (backStackEntry.toRoute<ShortcutCreateRoute>().kind) {
                            ShortcutKind.APP -> addAppShortcutTitle
                            ShortcutKind.HOME -> addHomeShortcutTitle
                        },
                    )
                }

                backStackEntry.destination.hasRoute(route = ShortcutEditRoute::class) -> {
                    onToolbarTitleChanged(
                        when (backStackEntry.toRoute<ShortcutEditRoute>().kind) {
                            ShortcutKind.APP -> editAppShortcutTitle
                            ShortcutKind.HOME -> editHomeShortcutTitle
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortcutsListRouteScreen(
    onAction: (ShortcutsListAction) -> Unit,
    viewModel: ManageShortcutsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSilently()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ShortcutsListScreen(
        state = uiState,
        onAction = onAction,
        onRetry = viewModel::refresh,
    )
}

@Composable
private fun ShortcutCreateScreen(
    kind: ShortcutKind,
    onClose: (ShortcutCloseEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: ShortcutEditorViewModel = hiltViewModel<
        ShortcutEditorViewModel,
        ShortcutEditorViewModelFactory,
        >(
        creationCallback = { factory -> factory.create(EditorRoute.Create(kind = kind)) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ErrorSnackbarEffect(viewModel.errorSnackbar, snackbarHostState)
    CloseEffect(closeEvents = viewModel.closeEvents, onClose = onClose)

    when (kind) {
        ShortcutKind.APP -> CreateAppShortcutScreen(
            uiState = uiState,
            onSubmit = viewModel::createAppShortcut,
            onUpdateDraft = viewModel::updateDraft,
            onServerSelected = viewModel::selectServer,
            onRetry = viewModel::retry,
        )

        ShortcutKind.HOME -> CreateHomeShortcutScreen(
            uiState = uiState,
            onSubmit = viewModel::createHomeShortcut,
            onUpdateDraft = viewModel::updateDraft,
            onServerSelected = viewModel::selectServer,
            onRetry = viewModel::retry,
        )
    }
}

@Composable
private fun ShortcutEditScreen(
    kind: ShortcutKind,
    id: String,
    onClose: (ShortcutCloseEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: ShortcutEditorViewModel = hiltViewModel<
        ShortcutEditorViewModel,
        ShortcutEditorViewModelFactory,
        >(
        creationCallback = { factory -> factory.create(EditorRoute.Edit(kind = kind, id = id)) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ErrorSnackbarEffect(viewModel.errorSnackbar, snackbarHostState)
    CloseEffect(closeEvents = viewModel.closeEvents, onClose = onClose)

    when (kind) {
        ShortcutKind.APP -> EditAppShortcutScreen(
            uiState = uiState,
            onSubmit = { viewModel.updateAppShortcut(id) },
            onUpdateDraft = viewModel::updateDraft,
            onServerSelected = viewModel::selectServer,
            onDelete = { viewModel.deleteAppShortcut(id) },
            onRetry = viewModel::retry,
        )

        ShortcutKind.HOME -> EditHomeShortcutScreen(
            uiState = uiState,
            onSubmit = { viewModel.updateHomeShortcut(id) },
            onUpdateDraft = viewModel::updateDraft,
            onServerSelected = viewModel::selectServer,
            onDelete = { viewModel.disableHomeShortcut(id) },
            onRetry = viewModel::retry,
        )
    }
}

@Composable
private fun ErrorSnackbarEffect(errorSnackbar: Flow<Int>, snackbarHostState: SnackbarHostState) {
    val resources = LocalResources.current
    LaunchedEffect(errorSnackbar) {
        errorSnackbar.collect { message ->
            val text = resources.getString(message)
            snackbarHostState.showSnackbar(text)
        }
    }
}

@Composable
private fun CloseEffect(closeEvents: Flow<ShortcutCloseEvent>, onClose: (ShortcutCloseEvent) -> Unit) {
    LaunchedEffect(closeEvents) {
        closeEvents.collect { event ->
            onClose(event)
        }
    }
}

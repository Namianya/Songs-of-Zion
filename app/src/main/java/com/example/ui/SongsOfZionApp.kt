package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.SongsOfZionApplication
import com.example.ui.screens.HymnReaderScreen
import com.example.ui.screens.HymnsScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SetlistsScreen
import com.example.ui.theme.SongsOfZionTheme
import com.example.ui.viewmodel.HymnsViewModel
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.ReaderViewModel
import com.example.ui.viewmodel.SetlistsViewModel

private object Routes {
    const val HYMNS = "hymns"
    const val SETLISTS = "setlists"
    const val LIBRARY = "library"
    const val READER_PATTERN = "reader/{hymnId}?setlistId={setlistId}"

    fun reader(hymnId: Long, setlistId: Long? = null): String {
        return if (setlistId != null) {
            "reader/$hymnId?setlistId=$setlistId"
        } else {
            "reader/$hymnId"
        }
    }
}

private data class TopDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topDestinations = listOf(
    TopDestination(
        route = Routes.HYMNS,
        labelRes = R.string.nav_hymns,
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook,
        testTag = "nav_hymns"
    ),
    TopDestination(
        route = Routes.SETLISTS,
        labelRes = R.string.nav_setlists,
        selectedIcon = Icons.AutoMirrored.Filled.QueueMusic,
        unselectedIcon = Icons.AutoMirrored.Outlined.QueueMusic,
        testTag = "nav_setlists"
    ),
    TopDestination(
        route = Routes.LIBRARY,
        labelRes = R.string.nav_library,
        selectedIcon = Icons.Filled.LibraryMusic,
        unselectedIcon = Icons.Outlined.LibraryMusic,
        testTag = "nav_library"
    )
)

@Composable
fun SongsOfZionApp() {
    val context = LocalContext.current
    val app = context.applicationContext as SongsOfZionApplication
    val preferences by app.preferencesRepository.preferencesFlow.collectAsStateWithLifecycle(
        initialValue = com.example.data.local.ReaderPreferences()
    )

    SongsOfZionTheme(themeMode = preferences.themeMode) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val isReaderRoute = currentRoute?.startsWith("reader/") == true

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!isReaderRoute && !useNavigationRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            topDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(dest.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isReaderRoute) Modifier else Modifier.padding(innerPadding)
                        )
                ) {
                    if (!isReaderRoute && useNavigationRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.statusBarsPadding()
                        ) {
                            topDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(dest.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = Routes.HYMNS,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable(Routes.HYMNS) {
                            val vm: HymnsViewModel = viewModel(
                                factory = HymnsViewModel.Factory(
                                    app.hymnalRepository,
                                    app.preferencesRepository
                                )
                            )
                            HymnsScreen(
                                viewModel = vm,
                                onOpenHymn = { hymnId ->
                                    navController.navigate(Routes.reader(hymnId))
                                }
                            )
                        }

                        composable(Routes.SETLISTS) {
                            val vm: SetlistsViewModel = viewModel(
                                factory = SetlistsViewModel.Factory(
                                    app.hymnalRepository,
                                    app.preferencesRepository
                                )
                            )
                            SetlistsScreen(
                                viewModel = vm,
                                onOpenHymn = { hymnId, setlistId ->
                                    navController.navigate(Routes.reader(hymnId, setlistId))
                                }
                            )
                        }

                        composable(Routes.LIBRARY) {
                            val vm: LibraryViewModel = viewModel(
                                factory = LibraryViewModel.Factory(
                                    app.hymnalRepository,
                                    app.preferencesRepository
                                )
                            )
                            LibraryScreen(viewModel = vm)
                        }

                        composable(
                            route = Routes.READER_PATTERN,
                            arguments = listOf(
                                navArgument("hymnId") { type = NavType.LongType },
                                navArgument("setlistId") {
                                    type = NavType.LongType
                                    defaultValue = -1L
                                }
                            )
                        ) { backStackEntry ->
                            val hymnId = backStackEntry.arguments?.getLong("hymnId") ?: 1L
                            val rawSetlistId = backStackEntry.arguments?.getLong("setlistId") ?: -1L
                            val setlistId = if (rawSetlistId > 0L) rawSetlistId else null

                            val vm: ReaderViewModel = viewModel(
                                key = "reader_${hymnId}_${setlistId ?: 0L}",
                                factory = ReaderViewModel.Factory(
                                    hymnId = hymnId,
                                    setlistId = setlistId,
                                    repository = app.hymnalRepository,
                                    preferencesRepository = app.preferencesRepository,
                                    tamilTranslator = app.tamilTranslator
                                )
                            )
                            HymnReaderScreen(
                                viewModel = vm,
                                onBack = { navController.popBackStack() },
                                onNavigateToHymn = { targetHymnId ->
                                    navController.navigate(Routes.reader(targetHymnId, setlistId)) {
                                        popUpTo(Routes.READER_PATTERN) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

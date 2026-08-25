package com.retimebox.lite.ui.home

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.retimebox.lite.R
import com.retimebox.lite.ui.album.AlbumScreen
import com.retimebox.lite.ui.navigation.BottomNavItem
import com.retimebox.lite.ui.settings.AboutDialog
import com.retimebox.lite.ui.settings.SettingsScreen
import com.retimebox.lite.ui.space.SpaceScreen
import com.retimebox.lite.ui.video.VideoScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenRecord: (Long) -> Unit,
    onOpenRecordEditor: (Long?, Long?) -> Unit,
    onOpenImage: (Long) -> Unit = {},
    onOpenVideo: (Long) -> Unit = {},
    onOpenSpaceLink: (Long) -> Unit = {},
    onOpenFolderManager: () -> Unit = {}
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)

    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var showAboutDialog by remember { mutableStateOf(!prefs.getBoolean(KEY_HAS_SEEN_ABOUT, false)) }

    val tabs = listOf(
        BottomNavItem.Home,
        BottomNavItem.Album,
        BottomNavItem.Video,
        BottomNavItem.Space,
        BottomNavItem.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                        selected = currentDestination?.hierarchy?.any {
                            it.route == tab.route
                        } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                EntryView(
                    onOpenRecord = onOpenRecord,
                    onOpenRecordEditor = onOpenRecordEditor
                )
            }
            composable(BottomNavItem.Album.route) {
                AlbumScreen(
                    onOpenImage = onOpenImage,
                    onOpenRecordEditor = onOpenRecordEditor
                )
            }
            composable(BottomNavItem.Video.route) {
                VideoScreen(
                    onOpenVideo = onOpenVideo,
                    onOpenRecordEditor = onOpenRecordEditor
                )
            }
            composable(BottomNavItem.Space.route) {
                SpaceScreen(
                    onOpenSpaceLink = onOpenSpaceLink
                )
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(
                    onOpenFolderManager = onOpenFolderManager
                )
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(
            appName = appName,
            onDismiss = { showAboutDialog = false },
            onConfirm = {
                prefs.edit().putBoolean(KEY_HAS_SEEN_ABOUT, true).apply()
                showAboutDialog = false
            }
        )
    }
}

private const val PREFS_NAME = "app_prefs"
private const val KEY_HAS_SEEN_ABOUT = "has_seen_about"

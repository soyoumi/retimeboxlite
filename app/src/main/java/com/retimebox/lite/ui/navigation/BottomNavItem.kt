package com.retimebox.lite.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import com.retimebox.lite.R

sealed class BottomNavItem(
    val index: Int,
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
) {
    data object Home : BottomNavItem(0, "home", R.string.tab_home, Icons.Filled.Home)
    data object Album : BottomNavItem(1, "album", R.string.tab_album, Icons.Filled.Image)
    data object Video : BottomNavItem(2, "video", R.string.tab_video, Icons.Filled.VideoLibrary)
    data object Space : BottomNavItem(3, "space", R.string.tab_space, Icons.Filled.Folder)
    data object Settings : BottomNavItem(4, "settings", R.string.tab_settings, Icons.Filled.Settings)
}

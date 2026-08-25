package com.retimebox.lite.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.retimebox.lite.ui.home.HomeScreen
import com.retimebox.lite.ui.media.ImagePreview
import com.retimebox.lite.ui.media.VideoPlayer
import com.retimebox.lite.ui.record.RecordDetailScreen
import com.retimebox.lite.ui.record.RecordEditorScreen
import com.retimebox.lite.ui.settings.FolderManagerScreen
import com.retimebox.lite.ui.spacelink.SpaceLinkWebView

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onOpenRecord = { recordId ->
                    navController.navigate(Screen.RecordDetail.createRoute(recordId))
                },
                onOpenRecordEditor = { recordId, folderId ->
                    navController.navigate(Screen.RecordEditor.createRoute(recordId, folderId))
                },
                onOpenImage = { imageId ->
                    navController.navigate(Screen.ImagePreview.createRoute(imageId))
                },
                onOpenVideo = { videoId ->
                    navController.navigate(Screen.VideoPlayer.createRoute(videoId))
                },
                onOpenSpaceLink = { linkId ->
                    navController.navigate(Screen.SpaceLinkWebView.createRoute(linkId))
                },
                onOpenFolderManager = {
                    navController.navigate(Screen.FolderManager.route)
                }
            )
        }

        composable(
            route = Screen.RecordDetail.route,
            arguments = listOf(
                navArgument(Screen.RecordDetail.ARG_RECORD_ID) { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getLong(Screen.RecordDetail.ARG_RECORD_ID) ?: 0L
            RecordDetailScreen(
                recordId = recordId,
                onBack = { navController.popBackStack() },
                onEdit = { rid ->
                    navController.navigate(Screen.RecordEditor.createRoute(rid))
                },
                onDeleted = { navController.popBackStack() },
                onOpenImage = { imageId ->
                    navController.navigate(Screen.ImagePreview.createRoute(imageId))
                },
                onOpenVideo = { videoId ->
                    navController.navigate(Screen.VideoPlayer.createRoute(videoId))
                },
                onOpenSpaceLink = { linkId ->
                    navController.navigate(Screen.SpaceLinkWebView.createRoute(linkId))
                }
            )
        }

        composable(
            route = Screen.RecordEditor.route,
            arguments = listOf(
                navArgument(Screen.RecordEditor.ARG_RECORD_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument(Screen.RecordEditor.ARG_FOLDER_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getLong(Screen.RecordEditor.ARG_RECORD_ID) ?: -1L
            val folderId = backStackEntry.arguments?.getLong(Screen.RecordEditor.ARG_FOLDER_ID) ?: -1L

            RecordEditorScreen(
                recordId = if (recordId > 0) recordId else null,
                folderId = if (folderId > 0) folderId else null,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ImagePreview.route,
            arguments = listOf(
                navArgument(Screen.ImagePreview.ARG_IMAGE_ID) { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val imageId = backStackEntry.arguments?.getLong(Screen.ImagePreview.ARG_IMAGE_ID) ?: 0L
            ImagePreview(
                imageId = imageId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.VideoPlayer.route,
            arguments = listOf(
                navArgument(Screen.VideoPlayer.ARG_VIDEO_ID) { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getLong(Screen.VideoPlayer.ARG_VIDEO_ID) ?: 0L
            VideoPlayer(
                videoId = videoId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SpaceLinkWebView.route,
            arguments = listOf(
                navArgument(Screen.SpaceLinkWebView.ARG_SPACE_LINK_ID) { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val spaceLinkId = backStackEntry.arguments?.getLong(Screen.SpaceLinkWebView.ARG_SPACE_LINK_ID) ?: 0L
            SpaceLinkWebView(
                spaceLinkId = spaceLinkId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.FolderManager.route) {
            FolderManagerScreen(onBack = { navController.popBackStack() })
        }
    }
}

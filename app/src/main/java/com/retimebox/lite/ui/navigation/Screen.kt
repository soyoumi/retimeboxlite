package com.retimebox.lite.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")

    data object RecordDetail : Screen("record/{recordId}") {
        fun createRoute(recordId: Long) = "record/$recordId"
        const val ARG_RECORD_ID = "recordId"
    }

    data object RecordEditor : Screen("record_editor/{recordId}/{folderId}") {
        fun createRoute(recordId: Long? = null, folderId: Long? = null): String {
            val rid = recordId ?: -1L
            val fid = folderId ?: -1L
            return "record_editor/$rid/$fid"
        }
        const val ARG_RECORD_ID = "recordId"
        const val ARG_FOLDER_ID = "folderId"
    }

    data object ImagePreview : Screen("image_preview/{imageId}") {
        fun createRoute(imageId: Long) = "image_preview/$imageId"
        const val ARG_IMAGE_ID = "imageId"
    }

    data object VideoPlayer : Screen("video_player/{videoId}") {
        fun createRoute(videoId: Long) = "video_player/$videoId"
        const val ARG_VIDEO_ID = "videoId"
    }

    data object SpaceLinkWebView : Screen("space_webview/{spaceLinkId}") {
        fun createRoute(spaceLinkId: Long) = "space_webview/$spaceLinkId"
        const val ARG_SPACE_LINK_ID = "spaceLinkId"
    }

    data object FolderManager : Screen("folder_manager")
}

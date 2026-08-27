package com.retimebox.lite.ui.media

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as Media3MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.retimebox.lite.R
import com.retimebox.lite.RetimeboxApplication
import com.retimebox.lite.data.local.entity.MediaItem
import com.retimebox.lite.data.local.entity.SourceType
import com.retimebox.lite.util.FileHelper
import kotlinx.coroutines.launch

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoId: Long,
    onBack: () -> Unit,
    onOpenRecord: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as RetimeboxApplication
    val mediaRepository = app.mediaRepository

    var item by remember { mutableStateOf<MediaItem?>(null) }
    var videoTitle by remember { mutableStateOf("视频播放") }
    var isFullscreen by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    val playerView = remember(context) {
        PlayerView(context).apply {
            useController = true
            setShutterBackgroundColor(android.graphics.Color.BLACK)
        }
    }

    LaunchedEffect(videoId) {
        item = mediaRepository.findById(videoId)
    }

    LaunchedEffect(item) {
        item?.let {
            val file = FileHelper.getFileFromRelativePath(context, it.fileRelativePath)
            if (file != null) {
                videoTitle = file.name
                exoPlayer.setMediaItem(Media3MediaItem.fromUri(Uri.fromFile(file)))
                exoPlayer.prepare()
                playerView.setPlayer(exoPlayer)
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        activity?.let {
            if (isFullscreen) {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                it.window?.decorView?.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            } else {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                it.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = {
                        Text(
                            text = videoTitle,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            val current = item
                            if (current != null && current.sourceType == SourceType.FROM_RECORD_INDEX && current.bindRecordId != null && current.bindRecordId > 0) {
                                onOpenRecord(current.bindRecordId)
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("非索引条目没有笔记来源") }
                            }
                        }) {
                            Text("来源")
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreen) PaddingValues(0.dp) else innerPadding)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val file = item?.let { FileHelper.getFileFromRelativePath(context, it.fileRelativePath) }
            if (file != null && file.exists()) {
                AndroidView(
                    factory = { ctx ->
                        playerView.apply {
                            setFullscreenButtonClickListener {
                                toggleFullscreen()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                if (isFullscreen) {
                    IconButton(
                        onClick = { toggleFullscreen() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 48.dp, end = 16.dp)
                    ) {
                        Icon(
                            Icons.Filled.FullscreenExit,
                            contentDescription = "退出全屏",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 48.dp, start = 16.dp)
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                }
            } else {
                Text(
                    text = "视频加载失败",
                    color = Color.White
                )
            }
        }
    }
}

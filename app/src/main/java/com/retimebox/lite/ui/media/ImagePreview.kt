package com.retimebox.lite.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.retimebox.lite.R
import com.retimebox.lite.RetimeboxApplication
import com.retimebox.lite.data.local.entity.MediaItem
import com.retimebox.lite.data.local.entity.SourceType
import com.retimebox.lite.util.FileHelper
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePreview(
    imageId: Long,
    onBack: () -> Unit,
    onOpenRecord: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as RetimeboxApplication
    val mediaRepository = app.mediaRepository

    var item by remember { mutableStateOf<MediaItem?>(null) }
    var imageTitle by remember { mutableStateOf("图片预览") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(imageId) {
        item = mediaRepository.findById(imageId)
    }

    LaunchedEffect(item) {
        item?.let {
            val file = FileHelper.getFileFromRelativePath(context, it.fileRelativePath)
            if (file != null) {
                imageTitle = file.name
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = imageTitle,
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
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        var scale by remember { mutableStateOf(1f) }
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val file = item?.let { FileHelper.getFileFromRelativePath(context, it.fileRelativePath) }
            if (file != null && file.exists()) {
                AsyncImage(
                    model = file,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 8f)
                                if (newScale > 1f) {
                                    scale = newScale
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        },
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = "图片加载失败",
                    color = Color.White
                )
            }
        }
    }
}

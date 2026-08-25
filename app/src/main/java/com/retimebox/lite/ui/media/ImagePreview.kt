package com.retimebox.lite.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.retimebox.lite.R
import com.retimebox.lite.RetimeboxApplication
import com.retimebox.lite.data.local.entity.MediaItem
import com.retimebox.lite.util.FileHelper
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePreview(
    imageId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as RetimeboxApplication
    val mediaRepository = app.mediaRepository

    var item by remember { mutableStateOf<MediaItem?>(null) }
    var imageTitle by remember { mutableStateOf("图片预览") }

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
                title = { Text(imageTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
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
                    modifier = Modifier.fillMaxSize(),
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

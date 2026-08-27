package com.retimebox.lite.ui.record

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.retimebox.lite.R
import com.retimebox.lite.data.local.entity.MediaItem
import com.retimebox.lite.data.local.entity.SpaceFileItem
import com.retimebox.lite.data.local.entity.SpaceLinkItem
import com.retimebox.lite.ui.components.VideoThumbnail
import com.retimebox.lite.util.FileHelper
import com.retimebox.lite.viewmodel.RecordDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(
    recordId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
    onOpenImage: (Long) -> Unit = {},
    onOpenVideo: (Long) -> Unit = {},
    onOpenSpaceLink: (Long) -> Unit = {},
    onOpenSpaceFile: (Long) -> Unit = {},
    viewModel: RecordDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val record by viewModel.record.collectAsStateWithLifecycle()
    val mediaItems by viewModel.mediaItems.collectAsStateWithLifecycle()
    val spaceLinks by viewModel.spaceLinks.collectAsStateWithLifecycle()
    val spaceFiles by viewModel.spaceFiles.collectAsStateWithLifecycle()
    val relatedFolders by viewModel.relatedFolders.collectAsStateWithLifecycle()
    val primaryFolder by viewModel.primaryFolder.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(recordId) {
        viewModel.loadRecord(recordId)
    }

    LaunchedEffect(deleted) {
        if (deleted) onDeleted()
    }

    if (record == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.loading),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    val dateFormat = remember { SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(record?.title ?: stringResource(R.string.record_detail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(recordId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.record_edit))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            val currentRecord = record!!

            // 日期
            Text(
                text = dateFormat.format(Date(currentRecord.recordDate)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 关联文件夹
            if (relatedFolders.isNotEmpty()) {
                Text(
                    text = "关联文件夹：" + relatedFolders.joinToString("、") { it.folderName },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (primaryFolder != null) {
                    Text(
                        text = "主文件夹：${primaryFolder!!.folderName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 内容渲染
            RenderContent(
                markdown = currentRecord.contentMarkdown,
                mediaItems = mediaItems,
                spaceLinks = spaceLinks,
                spaceFiles = spaceFiles,
                context = context,
                onOpenImage = onOpenImage,
                onOpenVideo = onOpenVideo,
                onOpenSpaceLink = onOpenSpaceLink,
                onOpenSpaceFile = onOpenSpaceFile
            )
        }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text(stringResource(R.string.confirm_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecord()
                    showDeleteDialog = false
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun RenderContent(
    markdown: String,
    mediaItems: List<MediaItem>,
    spaceLinks: List<SpaceLinkItem>,
    spaceFiles: List<SpaceFileItem>,
    context: android.content.Context,
    onOpenImage: (Long) -> Unit,
    onOpenVideo: (Long) -> Unit,
    onOpenSpaceLink: (Long) -> Unit,
    onOpenSpaceFile: (Long) -> Unit
) {
    val lines = markdown.split("\n")

    Column {
        for (line in lines) {
            when {
                line.startsWith("[image][") -> {
                    val idMatch = Regex("""\[image]\[(\d+)]""").find(line)
                    val id = idMatch?.groupValues?.get(1)?.toLongOrNull()
                    val media = id?.let { targetId -> mediaItems.find { it.id == targetId } }
                    if (media != null) {
                        ImageCard(
                            media = media,
                            context = context,
                            onClick = { onOpenImage(id) }
                        )
                    }
                }
                line.startsWith("[video][") -> {
                    val idMatch = Regex("""\[video]\[(\d+)]""").find(line)
                    val id = idMatch?.groupValues?.get(1)?.toLongOrNull()
                    val media = id?.let { targetId -> mediaItems.find { it.id == targetId } }
                    if (media != null) {
                        VideoCard(
                            media = media,
                            context = context,
                            onClick = { onOpenVideo(id) }
                        )
                    }
                }
                line.startsWith("[voice][") -> {
                    val idMatch = Regex("""\[voice]\[(\d+)]""").find(line)
                    val id = idMatch?.groupValues?.get(1)?.toLongOrNull()
                    val media = id?.let { targetId -> mediaItems.find { it.id == targetId } }
                    if (media != null) {
                        VoiceCard(media = media, context = context)
                    }
                }
                line.startsWith("[spacelink][") -> {
                    val idMatch = Regex("""\[spacelink]\[(\d+)]""").find(line)
                    val id = idMatch?.groupValues?.get(1)?.toLongOrNull()
                    val link = id?.let { targetId -> spaceLinks.find { it.id == targetId } }
                    if (link != null) {
                        SpaceLinkCard(
                            link = link,
                            onClick = { onOpenSpaceLink(id) }
                        )
                    }
                }
                line.startsWith("[spacefile][") -> {
                    val idMatch = Regex("""\[spacefile]\[(\d+)]""").find(line)
                    val id = idMatch?.groupValues?.get(1)?.toLongOrNull()
                    val file = id?.let { targetId -> spaceFiles.find { it.id == targetId } }
                    if (file != null) {
                        SpaceFileCard(
                            file = file,
                            onClick = { onOpenSpaceFile(id) }
                        )
                    }
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else -> {
                    val cleanLine = line.replace(Regex("<br\\s*/?>"), "").trim()
                    if (cleanLine.isNotEmpty() && !cleanLine.startsWith("[")) {
                        Text(
                            text = cleanLine,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageCard(
    media: MediaItem,
    context: android.content.Context,
    onClick: () -> Unit = {}
) {
    val file = FileHelper.getFileFromRelativePath(context, media.fileRelativePath)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        if (file.exists()) {
            AsyncImage(
                model = Uri.fromFile(file),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("图片加载失败", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun VideoCard(
    media: MediaItem,
    context: android.content.Context,
    onClick: () -> Unit = {}
) {
    val file = FileHelper.getFileFromRelativePath(context, media.fileRelativePath)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        if (file.exists()) {
            VideoThumbnail(
                videoFile = file,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(
                            color = Color(0x88000000),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(8.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text("视频加载失败", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun VoiceCard(media: MediaItem, context: android.content.Context) {
    val file = FileHelper.getFileFromRelativePath(context, media.fileRelativePath)
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(media.id) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun togglePlay() {
        if (!file.exists()) return
        if (isPlaying) {
            mediaPlayer?.pause()
            isPlaying = false
        } else {
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    setOnCompletionListener {
                        isPlaying = false
                        mediaPlayer?.seekTo(0)
                    }
                    prepare()
                    start()
                }
                isPlaying = true
            } catch (e: Exception) {
                isPlaying = false
                mediaPlayer = null
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { togglePlay() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50)
                    )
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "语音记录",
                    style = MaterialTheme.typography.titleMedium
                )
                if (file.exists()) {
                    Text(
                        text = "${file.length() / 1024} KB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SpaceLinkCard(
    link: SpaceLinkItem,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val typeLabel = when (link.spaceType) {
        com.retimebox.lite.data.local.entity.SpaceType.PANORAMA_IMAGE -> "全景图片"
        com.retimebox.lite.data.local.entity.SpaceType.PANORAMA_VIDEO -> "全景视频"
        com.retimebox.lite.data.local.entity.SpaceType.GSPLAT -> "高斯泼溅"
    }

    val bgColor = when (link.spaceType) {
        com.retimebox.lite.data.local.entity.SpaceType.PANORAMA_IMAGE -> Color(0xFF6897BB)
        com.retimebox.lite.data.local.entity.SpaceType.PANORAMA_VIDEO -> Color(0xFF808080)
        com.retimebox.lite.data.local.entity.SpaceType.GSPLAT -> Color(0xFF6A8759)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                if (link.thumbnailUrl != null && link.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = FileHelper.getFileFromRelativePath(context, link.thumbnailUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (link.spaceType) {
                                com.retimebox.lite.data.local.entity.SpaceType.PANORAMA_IMAGE -> Icons.Filled.Image
                                com.retimebox.lite.data.local.entity.SpaceType.PANORAMA_VIDEO -> Icons.Filled.Movie
                                com.retimebox.lite.data.local.entity.SpaceType.GSPLAT -> Icons.Filled.Public
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            color = Color(0xFF1565C0),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = link.name.ifEmpty { link.webUrl },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SpaceFileCard(
    file: SpaceFileItem,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val typeLabel = when (file.spaceType) {
        com.retimebox.lite.data.local.entity.SpaceType.PANORAMA_IMAGE -> "全景图片"
        com.retimebox.lite.data.local.entity.SpaceType.PANORAMA_VIDEO -> "全景视频"
        com.retimebox.lite.data.local.entity.SpaceType.GSPLAT -> "高斯泼溅"
    }

    val bgColor = when (file.spaceType) {
        com.retimebox.lite.data.local.entity.SpaceType.PANORAMA_IMAGE -> Color(0xFF6897BB)
        com.retimebox.lite.data.local.entity.SpaceType.PANORAMA_VIDEO -> Color(0xFF808080)
        com.retimebox.lite.data.local.entity.SpaceType.GSPLAT -> Color(0xFF6A8759)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                if (file.thumbnailUrl != null && file.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = FileHelper.getFileFromRelativePath(context, file.thumbnailUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (file.spaceType) {
                                com.retimebox.lite.data.local.entity.SpaceType.PANORAMA_IMAGE -> Icons.Filled.Image
                                com.retimebox.lite.data.local.entity.SpaceType.PANORAMA_VIDEO -> Icons.Filled.Movie
                                com.retimebox.lite.data.local.entity.SpaceType.GSPLAT -> Icons.Filled.Public
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            color = Color(0xFF2E7D32),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = file.filePath.substringAfterLast('/'),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

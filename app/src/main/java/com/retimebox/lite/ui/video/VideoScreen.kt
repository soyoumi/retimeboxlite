package com.retimebox.lite.ui.video

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.defaultMinSize

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import com.retimebox.lite.ui.components.VideoThumbnail
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.retimebox.lite.R
import com.retimebox.lite.data.local.entity.MediaItem
import com.retimebox.lite.data.local.entity.SourceType
import com.retimebox.lite.ui.folder.FolderPicker
import com.retimebox.lite.util.FileHelper
import com.retimebox.lite.viewmodel.VideoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(
    onOpenVideo: (Long) -> Unit,
    onOpenRecordEditor: (Long?, Long?) -> Unit,
    viewModel: VideoViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentFolderId by viewModel.currentFolderId.collectAsStateWithLifecycle()
    val batchMode by viewModel.batchMode.collectAsStateWithLifecycle()
    val forceDeleteMode by viewModel.forceDeleteMode.collectAsStateWithLifecycle()
    val forceDeleteEvent by viewModel.forceDeleteEvent.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val videosInFolder by viewModel.videosInFolder.collectAsStateWithLifecycle()
    val rootFolders by viewModel.rootFolders.collectAsStateWithLifecycle()
    val subFolders by viewModel.subFolders.collectAsStateWithLifecycle()
    var showFolderTree by remember { mutableStateOf(false) }
    var showFolderPickerDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(forceDeleteEvent) {
        forceDeleteEvent?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
            }
            viewModel.consumeForceDeleteEvent()
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val path = FileHelper.copyUriToPrivateDir(context, uri, com.retimebox.lite.data.local.entity.MediaType.VIDEO)
                if (path != null) {
                    viewModel.addVideo(path, currentFolderId)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                modifier = Modifier.height(20.dp),
                title = { },
                navigationIcon = {
                    if (currentFolderId != null) {
                        IconButton(onClick = { viewModel.goToParentFolder() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    if (batchMode) {
                        TextButton(onClick = { viewModel.clearSelection() }) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(onClick = { viewModel.exitBatchMode() }) {
                            Text(stringResource(R.string.exit_batch_mode))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!batchMode && currentFolderId != null) {
                FloatingActionButton(onClick = {
                    videoPickerLauncher.launch("video/*")
                }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_video))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 批量操作栏
            if (batchMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.selected_count, selectedIds.size),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row {
                        TextButton(
                            onClick = { viewModel.clearSelection() },
                            modifier = Modifier.defaultMinSize(minWidth = 1.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(
                            onClick = { showFolderPickerDialog = true },
                            modifier = Modifier.defaultMinSize(minWidth = 1.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(stringResource(R.string.batch_move_folder))
                        }
                        TextButton(
                            onClick = { viewModel.toggleForceDeleteMode() },
                            modifier = Modifier.defaultMinSize(minWidth = 1.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "强删",
                                color = if (forceDeleteMode) Color.Red else Color.DarkGray
                            )
                        }
                        TextButton(
                            onClick = { viewModel.batchDelete() },
                            modifier = Modifier.defaultMinSize(minWidth = 1.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.batch_delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        TextButton(
                            onClick = { viewModel.exitBatchMode() },
                            modifier = Modifier.defaultMinSize(minWidth = 1.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(stringResource(R.string.exit_batch_mode))
                        }
                    }
                }
            }

            // 文件夹导航栏
            if (!batchMode && (rootFolders.isNotEmpty() || subFolders.isNotEmpty() || currentFolderId != null)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFolderTree = !showFolderTree }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "视频目录",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { showFolderTree = !showFolderTree }) {
                                Icon(
                                    if (showFolderTree) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        }

                        if (showFolderTree) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.goToParentFolder()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (currentFolderId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (currentFolderId == null) "根目录" else "上层文件夹",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (currentFolderId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            subFolders.forEach { folder ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.navigateToFolder(folder.id)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Filled.Folder,
                                        contentDescription = null,
                                        tint = if (currentFolderId == folder.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = folder.folderName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (currentFolderId == folder.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // "全部" 标签（仅根目录）
            if (!batchMode && currentFolderId == null && videosInFolder.isNotEmpty()) {
                Text(
                    text = "全部",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (videosInFolder.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_videos),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = rememberLazyGridState(),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(videosInFolder) { video ->
                        VideoGridItem(
                            item = video,
                            isSelected = selectedIds.contains(video.id),
                            batchMode = batchMode,
                            forceDeleteMode = forceDeleteMode,
                            onClick = {
                                if (batchMode) {
                                    viewModel.toggleSelection(video.id, video.sourceType)
                                } else {
                                    onOpenVideo(video.id)
                                }
                            },
                            onLongClick = {
                                if (!batchMode) {
                                    viewModel.enterBatchMode()
                                    viewModel.toggleSelection(video.id, video.sourceType)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // 批量移动文件夹对话框
    if (showFolderPickerDialog) {
        FolderPicker(
            selectedFolderIds = emptyList(),
            primaryFolderId = null,
            onDismiss = { showFolderPickerDialog = false },
            onFolderSelected = { folderId ->
                viewModel.batchMoveToFolder(folderId)
                showFolderPickerDialog = false
            },
            onPrimaryFolderSet = {},
            singleSelect = true
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoGridItem(
    item: MediaItem,
    isSelected: Boolean,
    batchMode: Boolean,
    forceDeleteMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val isIndexItem = item.sourceType == SourceType.FROM_RECORD_INDEX
    val indexSelectable = isIndexItem && forceDeleteMode
    val context = LocalContext.current
    val file = FileHelper.getFileFromRelativePath(context, item.fileRelativePath)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = !batchMode || !isIndexItem || indexSelectable,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (file.exists()) {
                VideoThumbnail(
                    videoFile = file,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            // 播放按钮
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(
                        color = Color(0x66000000),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(4.dp)
            )

            if (isIndexItem) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(
                            color = Color(0x99BDBDBD),
                            shape = RoundedCornerShape(bottomEnd = 8.dp)
                        )
                        .padding(4.dp)
                ) {
                    Text(
                        text = "索引",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            if (isSelected && (batchMode || forceDeleteMode)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(
                            color = if (forceDeleteMode) Color.Red else MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50)
                        )
                        .padding(2.dp)
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .width(16.dp)
                            .height(16.dp)
                    )
                }
            }

            if (isIndexItem && batchMode && !forceDeleteMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Color(0x66FFFFFF))
                )
            }
        }
    }
}

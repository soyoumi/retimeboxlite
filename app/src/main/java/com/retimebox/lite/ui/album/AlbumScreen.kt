package com.retimebox.lite.ui.album

import androidx.compose.foundation.ExperimentalFoundationApi

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
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
import com.retimebox.lite.viewmodel.AlbumViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    onOpenImage: (Long) -> Unit,
    onOpenRecordEditor: (Long?, Long?) -> Unit,
    viewModel: AlbumViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentFolderId by viewModel.currentFolderId.collectAsStateWithLifecycle()
    val batchMode by viewModel.batchMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val imagesInFolder by viewModel.imagesInFolder.collectAsStateWithLifecycle()
    val rootFolders by viewModel.rootFolders.collectAsStateWithLifecycle()
    val subFolders by viewModel.subFolders.collectAsStateWithLifecycle()

    var showFolderPickerDialog by remember { mutableStateOf(false) }
    var showFolderTree by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val path = FileHelper.copyUriToPrivateDir(context, uri, com.retimebox.lite.data.local.entity.MediaType.IMAGE)
                if (path != null) {
                    viewModel.addImage(path, currentFolderId)
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
                    imagePickerLauncher.launch("image/*")
                }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_image))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                                text = "相册目录",
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
                        TextButton(onClick = { viewModel.clearSelection() }) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(onClick = { showFolderPickerDialog = true }) {
                            Text(stringResource(R.string.batch_move_folder))
                        }
                        TextButton(onClick = { viewModel.batchDelete() }) {
                            Text(
                                text = stringResource(R.string.batch_delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        TextButton(onClick = { viewModel.exitBatchMode() }) {
                            Text(stringResource(R.string.exit_batch_mode))
                        }
                    }
                }
            }

            // "全部" 标签（仅根目录）
            if (!batchMode && currentFolderId == null && imagesInFolder.isNotEmpty()) {
                Text(
                    text = "全部",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // 图片网格
            if (imagesInFolder.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_images),
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
                    items(imagesInFolder) { image ->
                        ImageGridItem(
                            item = image,
                            isSelected = selectedIds.contains(image.id),
                            batchMode = batchMode,
                            onClick = {
                                if (batchMode) {
                                    viewModel.toggleSelection(image.id, image.sourceType)
                                } else {
                                    onOpenImage(image.id)
                                }
                            },
                            onLongClick = {
                                if (!batchMode) {
                                    viewModel.enterBatchMode()
                                    viewModel.toggleSelection(image.id, image.sourceType)
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
private fun ImageGridItem(
    item: MediaItem,
    isSelected: Boolean,
    batchMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val isIndexItem = item.sourceType == SourceType.FROM_RECORD_INDEX

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = !batchMode || !isIndexItem,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            val context = LocalContext.current
            val file = FileHelper.getFileFromRelativePath(context, item.fileRelativePath)

            if (file.exists()) {
                AsyncImage(
                    model = file,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            // 索引条目标记
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

            // 选中标记
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
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

            // 索引条目禁用遮罩
            if (isIndexItem && batchMode) {
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

package com.retimebox.lite.ui.space

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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
import com.retimebox.lite.data.local.entity.SourceType
import com.retimebox.lite.data.local.entity.SpaceLinkItem
import com.retimebox.lite.data.local.entity.SpaceType
import com.retimebox.lite.ui.folder.FolderPicker
import com.retimebox.lite.util.FileHelper
import com.retimebox.lite.viewmodel.SpaceViewModel
import com.retimebox.lite.data.local.entity.SpaceFileItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceScreen(
    onOpenSpaceLink: (Long) -> Unit,
    onOpenSpaceFile: (Long) -> Unit = {},
    viewModel: SpaceViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentFolderId by viewModel.currentFolderId.collectAsStateWithLifecycle()
    val batchMode by viewModel.batchMode.collectAsStateWithLifecycle()
    val selectedEntries by viewModel.selectedEntries.collectAsStateWithLifecycle()
    val selectedCount by viewModel.selectedCount.collectAsStateWithLifecycle()
    val itemsInFolder by viewModel.itemsInFolder.collectAsStateWithLifecycle()
    val rootFolders by viewModel.rootFolders.collectAsStateWithLifecycle()
    val subFolders by viewModel.subFolders.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showFolderTree by remember { mutableStateOf(false) }
    var showFolderPickerDialog by remember { mutableStateOf(false) }
    var showEditFileDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<SpaceEntry?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

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
                    showAddDialog = true
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "插入空间文件")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (batchMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.selected_count, selectedCount),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row {
                        TextButton(onClick = { viewModel.clearSelection() }) {
                            Text(stringResource(R.string.cancel))
                        }
                        if (selectedCount == 1) {
                            val selected = selectedEntries.first()
                            if (selected.itemType == SpaceEntryType.FILE) {
                                TextButton(onClick = {
                                    editingEntry = selected
                                    showEditFileDialog = true
                                }) {
                                    Text("编辑")
                                }
                            } else if (selected.itemType == SpaceEntryType.LINK) {
                                TextButton(onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("所选择的不是空间文件，无法进行编辑")
                                    }
                                }) {
                                    Text("编辑")
                                }
                            }
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
                                text =  "空间目录",
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
            if (!batchMode && currentFolderId == null && itemsInFolder.isNotEmpty()) {
                Text(
                    text = "全部",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (itemsInFolder.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无空间条目",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = rememberLazyGridState(),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(itemsInFolder) { entry ->
                        SpaceGridItem(
                            entry = entry,
                            isSelected = selectedEntries.any { it.id == entry.id && it.itemType == entry.itemType },
                            batchMode = batchMode,
                            onClick = {
                                if (batchMode) {
                                    viewModel.toggleSelection(entry)
                                } else {
                                    if (entry.itemType == SpaceEntryType.LINK) {
                                        onOpenSpaceLink(entry.id)
                                    } else {
                                        onOpenSpaceFile(entry.id)
                                    }
                                }
                            },
                            onLongClick = {
                                if (!batchMode) {
                                    viewModel.enterBatchMode()
                                    viewModel.toggleSelection(entry)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // 插入空间文件对话框
    if (showAddDialog) {
        SpaceFileDialog(
            context = context,
            folderId = currentFolderId,
            onDismiss = { showAddDialog = false },
            onConfirm = { spaceType, filePath, name, thumbnailUrl ->
                viewModel.addSpaceFile(spaceType, filePath, name, thumbnailUrl, currentFolderId)
                showAddDialog = false
            }
        )
    }

    // 编辑空间文件对话框
    if (showEditFileDialog && editingEntry != null) {
        val editEntry = editingEntry!!
        SpaceFileDialog(
            context = context,
            folderId = editEntry.folderId,
            editingId = editEntry.id,
            editingSpaceType = editEntry.spaceType,
            editingName = editEntry.name,
            editingThumbnailUrl = editEntry.thumbnailUrl,
            editingFilePath = editEntry.filePath,
            onDismiss = {
                showEditFileDialog = false
                editingEntry = null
            },
            onConfirm = { spaceType, filePath, name, thumbnailUrl ->
                viewModel.updateSpaceFile(editEntry.id, spaceType, filePath, name, thumbnailUrl)
                showEditFileDialog = false
                editingEntry = null
            }
        )
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
private fun SpaceGridItem(
    entry: SpaceEntry,
    isSelected: Boolean,
    batchMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isIndexItem = entry.sourceType == SourceType.FROM_RECORD_INDEX
    val typeLabel = when (entry.spaceType) {
        SpaceType.PANORAMA_IMAGE -> stringResource(R.string.space_link_type_panorama_image)
        SpaceType.PANORAMA_VIDEO -> stringResource(R.string.space_link_type_panorama_video)
        SpaceType.GSPLAT -> stringResource(R.string.space_link_type_gsplat)
    }

    val bgColor = when (entry.spaceType) {
        SpaceType.PANORAMA_IMAGE -> Color(0xFF6897BB)
        SpaceType.PANORAMA_VIDEO -> Color(0xFF808080)
        SpaceType.GSPLAT -> Color(0xFF6A8759)
    }

    val displayName = entry.name.ifEmpty { entry.webUrl ?: entry.filePath ?: "" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = !batchMode || !isIndexItem,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                if (entry.thumbnailUrl != null && entry.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = FileHelper.getFileFromRelativePath(context, entry.thumbnailUrl),
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
                        Text(
                            text = "3D",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (entry.sourceType == SourceType.FROM_RECORD_INDEX) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xFF757575),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "索引",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFF1565C0),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun AddSpaceLinkDialog(
    onDismiss: () -> Unit,
    onConfirm: (SpaceType, String, String, String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedType by remember { mutableStateOf(SpaceType.PANORAMA_IMAGE) }
    var url by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var thumbnailPath by remember { mutableStateOf<String?>(null) }

    val thumbnailPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val path = FileHelper.copyThumbnailToDir(context, uri)
                if (path != null) {
                    thumbnailPath = path
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            FileHelper.deleteThumbnailFile(context, thumbnailPath)
            onDismiss()
        },
        title = { Text("新增空间链接") },
        text = {
            Column {
                Text("类型", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpaceType.PANORAMA_IMAGE.let {
                        FilterChip(
                            selected = selectedType == it,
                            onClick = { selectedType = it },
                            label = { Text(stringResource(R.string.space_link_type_panorama_image)) }
                        )
                    }
                    SpaceType.PANORAMA_VIDEO.let {
                        FilterChip(
                            selected = selectedType == it,
                            onClick = { selectedType = it },
                            label = { Text(stringResource(R.string.space_link_type_panorama_video)) }
                        )
                    }
                    SpaceType.GSPLAT.let {
                        FilterChip(
                            selected = selectedType == it,
                            onClick = { selectedType = it },
                            label = { Text(stringResource(R.string.space_link_type_gsplat)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.space_link_web_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.space_link_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("缩略图", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))

                if (thumbnailPath != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = FileHelper.getFileFromRelativePath(context, thumbnailPath!!),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = {
                                FileHelper.deleteThumbnailFile(context, thumbnailPath)
                                thumbnailPath = null
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(
                                    color = Color(0x88000000),
                                    shape = RoundedCornerShape(50)
                                )
                                .width(28.dp)
                                .height(28.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "移除缩略图",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    TextButton(
                        onClick = { thumbnailPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("上传缩略图")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (url.isNotBlank()) {
                    onConfirm(selectedType, url, name, thumbnailPath)
                }
            }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                FileHelper.deleteThumbnailFile(context, thumbnailPath)
                onDismiss()
            }) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun EditSpaceLinkDialog(
    link: SpaceLinkItem,
    onDismiss: () -> Unit,
    onConfirm: (SpaceType, String, String, String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedType by remember { mutableStateOf(link.spaceType) }
    var url by remember { mutableStateOf(link.webUrl) }
    var name by remember { mutableStateOf(link.name) }
    var thumbnailPath by remember { mutableStateOf<String?>(link.thumbnailUrl?.ifBlank { null }) }

    val thumbnailPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val path = FileHelper.copyThumbnailToDir(context, uri)
                if (path != null) {
                    FileHelper.deleteThumbnailFile(context, thumbnailPath)
                    thumbnailPath = path
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            onDismiss()
        },
        title = { Text("编辑空间链接") },
        text = {
            Column {
                Text("类型", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpaceType.PANORAMA_IMAGE.let {
                        FilterChip(
                            selected = selectedType == it,
                            onClick = { selectedType = it },
                            label = { Text(stringResource(R.string.space_link_type_panorama_image)) }
                        )
                    }
                    SpaceType.PANORAMA_VIDEO.let {
                        FilterChip(
                            selected = selectedType == it,
                            onClick = { selectedType = it },
                            label = { Text(stringResource(R.string.space_link_type_panorama_video)) }
                        )
                    }
                    SpaceType.GSPLAT.let {
                        FilterChip(
                            selected = selectedType == it,
                            onClick = { selectedType = it },
                            label = { Text(stringResource(R.string.space_link_type_gsplat)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.space_link_web_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.space_link_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("缩略图", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))

                if (thumbnailPath != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = FileHelper.getFileFromRelativePath(context, thumbnailPath!!),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = {
                                FileHelper.deleteThumbnailFile(context, thumbnailPath)
                                thumbnailPath = null
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(
                                    color = Color(0x88000000),
                                    shape = RoundedCornerShape(50)
                                )
                                .width(28.dp)
                                .height(28.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "移除缩略图",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    TextButton(
                        onClick = { thumbnailPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("上传缩略图")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (url.isNotBlank()) {
                    onConfirm(selectedType, url, name, thumbnailPath)
                }
            }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
            }) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

package com.retimebox.lite.ui.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.retimebox.lite.R
import com.retimebox.lite.data.local.entity.Folder
import com.retimebox.lite.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPicker(
    selectedFolderIds: List<Long>,
    primaryFolderId: Long?,
    onDismiss: () -> Unit,
    onFolderSelected: (Long) -> Unit,
    onPrimaryFolderSet: (Long?) -> Unit,
    singleSelect: Boolean = false,
    viewModel: SettingsViewModel = viewModel()
) {
    val allFolders by viewModel.allFolders.collectAsStateWithLifecycle()

    var expandedFolders by remember { mutableStateOf(setOf<Long>()) }
    var tempSelectedIds by remember { mutableStateOf(selectedFolderIds.toSet()) }
    var tempPrimaryId by remember { mutableStateOf(primaryFolderId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_folder)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (singleSelect) "点击文件夹选择目标位置" else "点击文件夹名称可多选，点击 ★ 设为主文件夹",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                FolderTreeItem(
                    folder = null,
                    allFolders = allFolders,
                    expandedFolders = expandedFolders,
                    selectedIds = tempSelectedIds,
                    primaryId = tempPrimaryId,
                    singleSelect = singleSelect,
                    onToggleExpand = { id ->
                        expandedFolders = if (expandedFolders.contains(id)) {
                            expandedFolders - id
                        } else {
                            expandedFolders + id
                        }
                    },
                    onToggleSelect = { id ->
                        if (singleSelect) {
                            onFolderSelected(id)
                        } else {
                            tempSelectedIds = if (tempSelectedIds.contains(id)) {
                                tempSelectedIds - id
                            } else {
                                tempSelectedIds + id
                            }
                            onFolderSelected(id)
                        }
                    },
                    onSetPrimary = { id ->
                        if (!singleSelect) {
                            tempPrimaryId = if (tempPrimaryId == id) null else id
                            onPrimaryFolderSet(tempPrimaryId)
                        }
                    },
                    depth = 0
                )
            }
        },
        confirmButton = {
            if (!singleSelect) {
                TextButton(onClick = {
                    onDismiss()
                }) {
                    Text(stringResource(R.string.ok))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun FolderTreeItem(
    folder: Folder?,
    allFolders: List<Folder>,
    expandedFolders: Set<Long>,
    selectedIds: Set<Long>,
    primaryId: Long?,
    singleSelect: Boolean,
    onToggleExpand: (Long) -> Unit,
    onToggleSelect: (Long) -> Unit,
    onSetPrimary: (Long) -> Unit,
    depth: Int
) {
    val children = if (folder == null) {
        allFolders.filter { it.parentFolderId == null }
    } else {
        allFolders.filter { it.parentFolderId == folder.id }
    }

    if (folder != null) {
        val isExpanded = expandedFolders.contains(folder.id)
        val isSelected = selectedIds.contains(folder.id)
        val isPrimary = primaryId == folder.id

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 16).dp)
                .clickable { onToggleSelect(folder.id) }
                .background(
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (children.isNotEmpty()) {
                Text(
                    text = if (isExpanded) "▼" else "▶",
                    modifier = Modifier
                        .clickable { onToggleExpand(folder.id) }
                        .padding(end = 4.dp)
                )
            } else {
                Spacer(modifier = Modifier.padding(end = 4.dp))
            }

            Icon(
                Icons.Filled.Folder,
                contentDescription = null,
                tint = parseColorHex(folder.colorHex),
                modifier = Modifier.padding(end = 8.dp)
            )

            Text(
                text = folder.folderName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            if (!singleSelect && isPrimary) {
                Text(
                    text = "★",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (isSelected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (!singleSelect) {
                Text(
                    text = "主",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .clickable { onSetPrimary(folder.id) }
                        .padding(start = 8.dp)
                )
            }
        }

        if (isExpanded) {
            children.forEach { child ->
                FolderTreeItem(
                    folder = child,
                    allFolders = allFolders,
                    expandedFolders = expandedFolders,
                    selectedIds = selectedIds,
                    primaryId = primaryId,
                    singleSelect = singleSelect,
                    onToggleExpand = onToggleExpand,
                    onToggleSelect = onToggleSelect,
                    onSetPrimary = onSetPrimary,
                    depth = depth + 1
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "根目录",
                style = MaterialTheme.typography.titleSmall
            )
        }
        children.forEach { child ->
            FolderTreeItem(
                folder = child,
                allFolders = allFolders,
                expandedFolders = expandedFolders,
                selectedIds = selectedIds,
                primaryId = primaryId,
                singleSelect = singleSelect,
                onToggleExpand = onToggleExpand,
                onToggleSelect = onToggleSelect,
                onSetPrimary = onSetPrimary,
                depth = 1
            )
        }
    }
}

private fun parseColorHex(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF1565C0.toInt())
    }
}
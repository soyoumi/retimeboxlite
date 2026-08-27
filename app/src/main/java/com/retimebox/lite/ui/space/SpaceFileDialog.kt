package com.retimebox.lite.ui.space

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.retimebox.lite.R
import com.retimebox.lite.data.local.entity.SpaceType
import com.retimebox.lite.util.FileHelper
import kotlinx.coroutines.launch

@Composable
fun SpaceFileDialog(
    context: Context,
    folderId: Long?,
    editingId: Long? = null,
    editingSpaceType: SpaceType? = null,
    editingName: String? = null,
    editingThumbnailUrl: String? = null,
    editingFilePath: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (SpaceType, String, String, String?) -> Unit
) {
    val scope = rememberCoroutineScope()

    var spaceType by remember { mutableStateOf(editingSpaceType ?: SpaceType.PANORAMA_IMAGE) }
    var name by remember { mutableStateOf(editingName ?: "") }
    var thumbnail by remember { mutableStateOf(editingThumbnailUrl) }
    var filePath by remember { mutableStateOf(editingFilePath) }

    val thumbnailPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val oldThumb = thumbnail
                val path = FileHelper.copyThumbnailToDir(context, uri)
                if (path != null) {
                    if (editingThumbnailUrl == null) {
                        FileHelper.deleteThumbnailFile(context, oldThumb)
                    }
                    thumbnail = path
                }
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val path = FileHelper.copySpaceFileToDir(context, uri, spaceType)
                if (path != null) {
                    filePath = path
                    if (name.isBlank()) {
                        name = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "空间文件"
                    }
                }
            }
        }
    }

    val title = if (editingId != null) "编辑空间文件" else stringResource(R.string.insert_space_file)

    AlertDialog(
        onDismissRequest = {
            if (editingId == null) {
                FileHelper.deleteThumbnailFile(context, thumbnail)
                filePath?.let { path ->
                    val file = FileHelper.getFileFromRelativePath(context, path)
                    if (file.exists()) file.delete()
                }
            }
            onDismiss()
        },
        title = { Text(title) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.space_link_type),
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = spaceType == SpaceType.PANORAMA_IMAGE,
                        onClick = { spaceType = SpaceType.PANORAMA_IMAGE }
                    )
                    Text(
                        text = stringResource(R.string.space_link_type_panorama_image),
                        modifier = Modifier.clickable { spaceType = SpaceType.PANORAMA_IMAGE }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = spaceType == SpaceType.PANORAMA_VIDEO,
                        onClick = { spaceType = SpaceType.PANORAMA_VIDEO }
                    )
                    Text(
                        text = stringResource(R.string.space_link_type_panorama_video),
                        modifier = Modifier.clickable { spaceType = SpaceType.PANORAMA_VIDEO }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = spaceType == SpaceType.GSPLAT,
                        onClick = { spaceType = SpaceType.GSPLAT }
                    )
                    Text(
                        text = stringResource(R.string.space_link_type_gsplat),
                        modifier = Modifier.clickable { spaceType = SpaceType.GSPLAT }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.space_link_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text("上传文件", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))

                if (filePath != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = filePath!!.substringAfterLast('/'),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                val file = FileHelper.getFileFromRelativePath(context, filePath!!)
                                if (file.exists()) file.delete()
                                filePath = null
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "移除文件")
                            }
                        }
                    }
                } else {
                    TextButton(
                        onClick = {
                            val mimeTypes = when (spaceType) {
                                SpaceType.PANORAMA_IMAGE -> "image/*"
                                SpaceType.PANORAMA_VIDEO -> "video/*"
                                SpaceType.GSPLAT -> "*/*"
                            }
                            filePickerLauncher.launch(mimeTypes)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("选择文件")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("缩略图", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))

                if (thumbnail != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = FileHelper.getFileFromRelativePath(context, thumbnail!!),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = {
                                if (editingThumbnailUrl == null) {
                                    FileHelper.deleteThumbnailFile(context, thumbnail)
                                }
                                thumbnail = null
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
                val currentPath = filePath ?: return@TextButton
                val fileName = name.ifBlank { currentPath.substringAfterLast('/') }
                onConfirm(spaceType, currentPath, fileName, thumbnail)
            }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (editingId == null) {
                    FileHelper.deleteThumbnailFile(context, thumbnail)
                    filePath?.let { path ->
                        val file = FileHelper.getFileFromRelativePath(context, path)
                        if (file.exists()) file.delete()
                    }
                }
                onDismiss()
            }) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

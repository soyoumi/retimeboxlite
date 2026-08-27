package com.retimebox.lite.ui.record

import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.retimebox.lite.R
import com.retimebox.lite.data.local.entity.MediaItem
import com.retimebox.lite.data.local.entity.MediaType
import com.retimebox.lite.data.local.entity.RefType
import com.retimebox.lite.data.local.entity.SpaceFileItem
import com.retimebox.lite.data.local.entity.SpaceLinkItem
import com.retimebox.lite.data.local.entity.SpaceType
import com.retimebox.lite.ui.folder.FolderPicker
import com.retimebox.lite.ui.components.VideoThumbnail
import com.retimebox.lite.util.FileHelper
import com.retimebox.lite.util.RichEditorHelper
import com.retimebox.lite.viewmodel.RecordEditorViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordEditorScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    recordId: Long? = null,
    folderId: Long? = null,
    onOpenSpaceFile: (Long) -> Unit = {},
    viewModel: RecordEditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val relatedFolderIds by viewModel.relatedFolderIds.collectAsStateWithLifecycle()
    val primaryFolderId by viewModel.primaryFolderId.collectAsStateWithLifecycle()
    val recordDate by viewModel.recordDate.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val referencedMedia by viewModel.referencedMedia.collectAsStateWithLifecycle()
    val referencedSpaceLinks by viewModel.referencedSpaceLinks.collectAsStateWithLifecycle()
    val referencedSpaceFiles by viewModel.referencedSpaceFiles.collectAsStateWithLifecycle()
    val allFolders by viewModel.allFolders.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var textFieldValue by remember { mutableStateOf(TextFieldValue()) }

    var showFolderPicker by remember { mutableStateOf(false) }
    var showSpaceLinkDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showRecordDialog by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0L) }
    var recordingStartTime by remember { mutableStateOf(0L) }
    var recordingTempPath by remember { mutableStateOf<String?>(null) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingPermissionGranted by remember { mutableStateOf(false) }
    var spaceLinkType by remember { mutableStateOf(SpaceType.PANORAMA_IMAGE) }
    var spaceLinkUrl by remember { mutableStateOf("") }
    var spaceLinkName by remember { mutableStateOf("") }
    var spaceLinkThumbnail by remember { mutableStateOf<String?>(null) }
    var editingSpaceLinkId by remember { mutableStateOf<Long?>(null) }

    var showSpaceFileDialog by remember { mutableStateOf(false) }
    var editingSpaceFileId by remember { mutableStateOf<Long?>(null) }
    var spaceFileType by remember { mutableStateOf(SpaceType.PANORAMA_IMAGE) }
    var spaceFileName by remember { mutableStateOf("") }
    var spaceFileThumbnail by remember { mutableStateOf<String?>(null) }
    var spaceFilePath by remember { mutableStateOf<String?>(null) }

    val spaceLinkThumbnailPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val path = FileHelper.copyThumbnailToDir(context, uri)
                if (path != null) {
                    FileHelper.deleteThumbnailFile(context, spaceLinkThumbnail)
                    spaceLinkThumbnail = path
                }
            }
        }
    }

    val spaceFileThumbnailPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val path = FileHelper.copyThumbnailToDir(context, uri)
                if (path != null) {
                    FileHelper.deleteThumbnailFile(context, spaceFileThumbnail)
                    spaceFileThumbnail = path
                }
            }
        }
    }

    val spaceFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                if (primaryFolderId == null && relatedFolderIds.isEmpty()) {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.folder_required)
                    )
                }
                val path = FileHelper.copySpaceFileToDir(context, uri, spaceFileType)
                if (path != null) {
                    spaceFilePath = path
                    if (spaceFileName.isBlank()) {
                        spaceFileName = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "空间文件"
                    }
                }
            }
        }
    }

    // 初始化
    LaunchedEffect(recordId, folderId) {
        if (recordId != null && recordId > 0) {
            viewModel.loadRecord(recordId)
        } else {
            viewModel.initNewRecord(folderId)
        }
    }

    // 加载已有笔记内容到编辑器
    val markdown by viewModel.contentMarkdown.collectAsStateWithLifecycle()
    LaunchedEffect(markdown) {
        if (markdown.isNotEmpty() && textFieldValue.text.isEmpty()) {
            textFieldValue = TextFieldValue(markdown)
        }
    }

    // 保存后返回
    LaunchedEffect(saved) {
        if (saved) onSaved()
    }

    // 错误提示
    LaunchedEffect(error) {
        if (error != null) {
            snackbarHostState.showSnackbar(error!!)
        }
    }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (primaryFolderId == null && relatedFolderIds.isEmpty()) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.folder_required)
                    )
                }
            }
            scope.launch {
                try {
                    val path = FileHelper.copyUriToPrivateDir(context, uri, MediaType.IMAGE)
                    if (path != null) {
                        val folder = primaryFolderId
                        val mediaItemId = viewModel.addImageReference(path, folder)
                        val shortcode = RichEditorHelper.createImageShortcode(mediaItemId, path)
                        textFieldValue = RichEditorHelper.insertAtCursor(textFieldValue, shortcode)
                        viewModel.updateContentMarkdown(textFieldValue.text)
                    }
                } catch (e: Exception) {
                    viewModel.setError(e.message ?: "添加图片失败")
                }
            }
        }
    }

    // 视频选择器
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (primaryFolderId == null && relatedFolderIds.isEmpty()) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.folder_required)
                    )
                }
            }
            scope.launch {
                try {
                    val path = FileHelper.copyUriToPrivateDir(context, uri, MediaType.VIDEO)
                    if (path != null) {
                        val folder = primaryFolderId
                        val mediaItemId = viewModel.addVideoReference(path, folder)
                        val shortcode = RichEditorHelper.createVideoShortcode(mediaItemId, path)
                        textFieldValue = RichEditorHelper.insertAtCursor(textFieldValue, shortcode)
                        viewModel.updateContentMarkdown(textFieldValue.text)
                    }
                } catch (e: Exception) {
                    viewModel.setError(e.message ?: "添加视频失败")
                }
            }
        }
    }

    // 语音选择器
    val voicePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (primaryFolderId == null && relatedFolderIds.isEmpty()) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.folder_required)
                    )
                }
            }
            scope.launch {
                try {
                    val path = FileHelper.copyUriToPrivateDir(context, uri, MediaType.VOICE)
                    if (path != null) {
                        val folder = primaryFolderId
                        val mediaItemId = viewModel.addVoiceReference(path, folder)
                        val shortcode = RichEditorHelper.createVoiceShortcode(mediaItemId, path)
                        textFieldValue = RichEditorHelper.insertAtCursor(textFieldValue, shortcode)
                        viewModel.updateContentMarkdown(textFieldValue.text)
                    }
                } catch (e: Exception) {
                    viewModel.setError(e.message ?: "添加语音失败")
                }
            }
        }
    }

    fun cleanupRecordingResources() {
        try {
            mediaRecorder?.apply {
                try { stop() } catch (_: Exception) {}
                try { reset() } catch (_: Exception) {}
                release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null
        isRecording = false
        recordingDuration = 0L
        recordingTempPath = null
    }

    fun performStartRecording() {
        try {
            val dir = FileHelper.getSubDir(context, MediaType.VOICE)
            val fileName = "RETimeBox_${System.currentTimeMillis()}.m4a"
            val tempFile = File(dir, fileName)

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context).apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128000)
                    setAudioSamplingRate(44100)
                    setOutputFile(tempFile.absolutePath)
                    prepare()
                    start()
                }
            } else {
                MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128000)
                    setAudioSamplingRate(44100)
                    setOutputFile(tempFile.absolutePath)
                    prepare()
                    start()
                }
            }

            val tempPath = FileHelper.getRelativePath(MediaType.VOICE, fileName)
            recordingTempPath = tempPath
            mediaRecorder = recorder
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            recordingDuration = 0L

            scope.launch {
                while (isRecording) {
                    val elapsed = System.currentTimeMillis() - recordingStartTime
                    recordingDuration = elapsed
                    kotlinx.coroutines.delay(50)
                }
            }
        } catch (e: Exception) {
            viewModel.setError("录音启动失败: ${e.message}")
            cleanupRecordingResources()
        }
    }

    fun performStopRecording() {
        val recorder = mediaRecorder
        val tempPath = recordingTempPath
        val elapsed = System.currentTimeMillis() - recordingStartTime

        isRecording = false

        try {
            recorder?.apply {
                try { stop() } catch (_: Exception) {}
                reset()
                release()
            }
        } catch (_: Exception) {}

        mediaRecorder = null

        if (elapsed > 500 && tempPath != null) {
            val fileName = tempPath.substringAfterLast('/')
            val dir = FileHelper.getSubDir(context, MediaType.VOICE)
            val actualFile = File(dir, fileName)
            if (actualFile.exists() && actualFile.length() > 0) {
                scope.launch {
                    val mediaItemId = viewModel.addVoiceReference(tempPath, primaryFolderId)
                    val shortcode = RichEditorHelper.createVoiceShortcode(mediaItemId, tempPath)
                    textFieldValue = RichEditorHelper.insertAtCursor(textFieldValue, shortcode)
                    viewModel.updateContentMarkdown(textFieldValue.text)
                }
            } else {
                actualFile.delete()
            }
            showRecordDialog = false
        } else {
            if (tempPath != null) {
                val fileName = tempPath.substringAfterLast('/')
                val dir = FileHelper.getSubDir(context, MediaType.VOICE)
                File(dir, fileName).delete()
            }
            showRecordDialog = false
        }
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        recordingPermissionGranted = granted
        if (granted) {
            performStartRecording()
        } else {
            viewModel.setError("需要录音权限才能录制语音")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (recordId != null) stringResource(R.string.record_edit) else stringResource(R.string.new_record_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (relatedFolderIds.isEmpty()) {
                            viewModel.setError(context.getString(R.string.folder_required))
                        } else {
                            viewModel.updateContentMarkdown(textFieldValue.text)
                            viewModel.save()
                        }
                    }) {
                        Text(stringResource(R.string.save))
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
            // 标题
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text(stringResource(R.string.title_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 日期选择
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dateFormat.format(Date(recordDate)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .clickable { showDatePicker = true }
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 关联文件夹
            Text(
                text = stringResource(R.string.select_folder),
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                relatedFolderIds.forEach { fid ->
                    val folderName = allFolders.find { it.id == fid }?.folderName ?: fid.toString()
                    AssistChip(
                        onClick = { viewModel.toggleRelatedFolder(fid) },
                        label = { Text(folderName) },
                        leadingIcon = {
                            if (fid == primaryFolderId) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        }
                    )
                }
                AssistChip(
                    onClick = {
                        if (allFolders.isEmpty()) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.no_folders_for_selection)
                                )
                            }
                        } else {
                            showFolderPicker = true
                        }
                    },
                    label = { Text("+ ${stringResource(R.string.select_folder)}") }
                )
            }
            if (relatedFolderIds.isEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.folder_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 编辑器工具栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Icon(Icons.Filled.Image, contentDescription = stringResource(R.string.insert_image))
                }
                IconButton(onClick = { videoPickerLauncher.launch("video/*") }) {
                    Icon(Icons.Filled.Movie, contentDescription = stringResource(R.string.insert_video))
                }
                IconButton(onClick = { voicePickerLauncher.launch("audio/*") }) {
                    Icon(Icons.Filled.AudioFile, contentDescription = stringResource(R.string.insert_voice))
                }
                IconButton(onClick = { showRecordDialog = true }) {
                    Icon(Icons.Filled.Mic, contentDescription = "录音")
                }
                // 屏蔽插入空间链接图标，保留原代码不删除
                // IconButton(onClick = {
                //     editingSpaceLinkId = null
                //     spaceLinkType = SpaceType.PANORAMA_IMAGE
                //     spaceLinkUrl = ""
                //     spaceLinkName = ""
                //     spaceLinkThumbnail = null
                //     showSpaceLinkDialog = true
                // }) {
                //     Icon(Icons.Filled.Link, contentDescription = stringResource(R.string.insert_space_link))
                // }
                IconButton(onClick = {
                    editingSpaceFileId = null
                    spaceFileType = SpaceType.PANORAMA_IMAGE
                    spaceFileName = ""
                    spaceFileThumbnail = null
                    spaceFilePath = null
                    showSpaceFileDialog = true
                }) {
                    Icon(Icons.Filled.Public, contentDescription = stringResource(R.string.insert_space_file))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 文本编辑器
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                placeholder = { Text("输入笔记内容...") }
            )

            LaunchedEffect(textFieldValue.text) {
                viewModel.updateContentMarkdown(textFieldValue.text)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 移除媒体引用
            fun removeMediaReference(id: Long, mediaType: MediaType) {
                val refType = refTypeFromMediaType(mediaType)
                val currentMd = textFieldValue.text
                val newMd = RichEditorHelper.removeReference(currentMd, refType, id)
                textFieldValue = TextFieldValue(newMd)
                viewModel.updateContentMarkdown(newMd)
                viewModel.removeReference(id, refType)
            }

            // 移除空间链接引用
            fun removeSpaceLinkReference(id: Long) {
                val currentMd = textFieldValue.text
                val newMd = RichEditorHelper.removeReference(currentMd, RefType.SPACE_LINK, id)
                textFieldValue = TextFieldValue(newMd)
                viewModel.updateContentMarkdown(newMd)
                viewModel.removeReference(id, RefType.SPACE_LINK)
            }

            // 移除空间文件引用
            fun removeSpaceFileReference(id: Long) {
                val currentMd = textFieldValue.text
                val newMd = RichEditorHelper.removeReference(currentMd, RefType.SPACE_FILE, id)
                textFieldValue = TextFieldValue(newMd)
                viewModel.updateContentMarkdown(newMd)
                viewModel.removeReference(id, RefType.SPACE_FILE)
            }

            // 已添加的引用预览
            if (referencedMedia.isNotEmpty() || referencedSpaceLinks.isNotEmpty() || referencedSpaceFiles.isNotEmpty()) {
                Text(
                    text = "已添加的内容",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(4.dp))

                referencedMedia.forEach { mediaItem ->
                    MediaPreviewCard(
                        mediaItem = mediaItem,
                        context = context,
                        onRemove = {
                            removeMediaReference(mediaItem.id, mediaItem.mediaType)
                        }
                    )
                }

                referencedSpaceLinks.forEach { link ->
                    SpaceLinkPreviewCard(
                        link = link,
                        onClick = {
                            editingSpaceLinkId = link.id
                            spaceLinkType = link.spaceType
                            spaceLinkUrl = link.webUrl
                            spaceLinkName = link.name
                            spaceLinkThumbnail = link.thumbnailUrl?.ifBlank { null }
                            showSpaceLinkDialog = true
                        },
                        onRemove = {
                            removeSpaceLinkReference(link.id)
                        }
                    )
                }

                referencedSpaceFiles.forEach { fileItem ->
                    SpaceFilePreviewCard(
                        fileItem = fileItem,
                        onClick = { onOpenSpaceFile(fileItem.id) },
                        onEdit = {
                            editingSpaceFileId = fileItem.id
                            spaceFileType = fileItem.spaceType
                            spaceFileName = fileItem.name
                            spaceFileThumbnail = fileItem.thumbnailUrl?.ifBlank { null }
                            spaceFilePath = fileItem.filePath
                            showSpaceFileDialog = true
                        },
                        onRemove = {
                            removeSpaceFileReference(fileItem.id)
                        }
                    )
                }
            }
        }
    }

    // 文件夹选择对话框
    if (showDatePicker) {
        val fullFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
        var dateText by remember { mutableStateOf(fullFormat.format(Date(recordDate))) }
        var dateParseError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = { Text("编辑日期时间") },
            text = {
                Column {
                    Text(
                        text = "格式：yyyy-MM-dd HH:mm:ss",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { v ->
                            dateText = v
                            dateParseError = null
                        },
                        label = { Text("日期时间") },
                        isError = dateParseError != null,
                        supportingText = { if (dateParseError != null) Text(dateParseError!!) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        fullFormat.isLenient = false
                        val parsed = fullFormat.parse(dateText)
                        if (parsed == null) {
                            dateParseError = "无法解析日期时间"
                        } else {
                            viewModel.updateRecordDate(parsed.time)
                            showDatePicker = false
                        }
                    } catch (e: Exception) {
                        dateParseError = "格式不正确（示例：2026-08-27 14:30:00）"
                    }
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showFolderPicker) {
        FolderPicker(
            selectedFolderIds = relatedFolderIds,
            primaryFolderId = primaryFolderId,
            onDismiss = { showFolderPicker = false },
            onFolderSelected = { folderId ->
                viewModel.toggleRelatedFolder(folderId)
            },
            onPrimaryFolderSet = { folderId ->
                viewModel.setPrimaryFolder(folderId)
            }
        )
    }

    // 空间链接对话框
    if (showSpaceLinkDialog) {
        val isEditing = editingSpaceLinkId != null
        AlertDialog(
            onDismissRequest = {
                FileHelper.deleteThumbnailFile(context, spaceLinkThumbnail)
                showSpaceLinkDialog = false
                editingSpaceLinkId = null
            },
            title = { Text(if (isEditing) "编辑空间链接" else stringResource(R.string.insert_space_link)) },
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
                            selected = spaceLinkType == SpaceType.PANORAMA_IMAGE,
                            onClick = { spaceLinkType = SpaceType.PANORAMA_IMAGE }
                        )
                        Text(
                            text = stringResource(R.string.space_link_type_panorama_image),
                            modifier = Modifier.clickable { spaceLinkType = SpaceType.PANORAMA_IMAGE }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = spaceLinkType == SpaceType.PANORAMA_VIDEO,
                            onClick = { spaceLinkType = SpaceType.PANORAMA_VIDEO }
                        )
                        Text(
                            text = stringResource(R.string.space_link_type_panorama_video),
                            modifier = Modifier.clickable { spaceLinkType = SpaceType.PANORAMA_VIDEO }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = spaceLinkType == SpaceType.GSPLAT,
                            onClick = { spaceLinkType = SpaceType.GSPLAT }
                        )
                        Text(
                            text = stringResource(R.string.space_link_type_gsplat),
                            modifier = Modifier.clickable { spaceLinkType = SpaceType.GSPLAT }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = spaceLinkUrl,
                        onValueChange = { spaceLinkUrl = it },
                        label = { Text(stringResource(R.string.space_link_web_url)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = spaceLinkName,
                        onValueChange = { spaceLinkName = it },
                        label = { Text(stringResource(R.string.space_link_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("缩略图", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))

                    if (spaceLinkThumbnail != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = FileHelper.getFileFromRelativePath(context, spaceLinkThumbnail!!),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = {
                                    FileHelper.deleteThumbnailFile(context, spaceLinkThumbnail)
                                    spaceLinkThumbnail = null
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
                            onClick = { spaceLinkThumbnailPickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("上传缩略图")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (spaceLinkUrl.isNotBlank()) {
                        if (!isEditing && primaryFolderId == null && relatedFolderIds.isEmpty()) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.folder_required)
                                )
                            }
                        }
                        scope.launch {
                            if (isEditing) {
                                viewModel.updateSpaceLinkReference(
                                    id = editingSpaceLinkId!!,
                                    spaceType = spaceLinkType,
                                    webUrl = spaceLinkUrl,
                                    name = spaceLinkName,
                                    thumbnailUrl = spaceLinkThumbnail
                                )
                                val currentMd = textFieldValue.text
                                val newMd = RichEditorHelper.updateSpaceLinkShortcode(
                                    currentMd, editingSpaceLinkId!!,
                                    spaceLinkType, spaceLinkUrl,
                                    spaceLinkName, spaceLinkThumbnail
                                )
                                textFieldValue = TextFieldValue(newMd)
                                viewModel.updateContentMarkdown(newMd)
                            } else {
                                val spaceLinkId = viewModel.addSpaceLinkReference(
                                    spaceType = spaceLinkType,
                                    webUrl = spaceLinkUrl,
                                    name = spaceLinkName,
                                    thumbnailUrl = spaceLinkThumbnail,
                                    folderId = primaryFolderId
                                )
                                val shortcode = RichEditorHelper.createSpaceLinkShortcode(
                                    spaceLinkId, spaceLinkType,
                                    spaceLinkUrl, spaceLinkName,
                                    spaceLinkThumbnail
                                )
                                textFieldValue = RichEditorHelper.insertAtCursor(textFieldValue, shortcode)
                                viewModel.updateContentMarkdown(textFieldValue.text)
                            }
                        }
                        showSpaceLinkDialog = false
                        editingSpaceLinkId = null
                    }
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    FileHelper.deleteThumbnailFile(context, spaceLinkThumbnail)
                    showSpaceLinkDialog = false
                    editingSpaceLinkId = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 空间文件对话框
    if (showSpaceFileDialog) {
        AlertDialog(
            onDismissRequest = {
                FileHelper.deleteThumbnailFile(context, spaceFileThumbnail)
                spaceFilePath?.let { path ->
                    val file = FileHelper.getFileFromRelativePath(context, path)
                    if (file.exists()) file.delete()
                }
                showSpaceFileDialog = false
            },
            title = {
                Text(if (editingSpaceFileId != null) "编辑空间文件" else stringResource(R.string.insert_space_file))
            },
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
                            selected = spaceFileType == SpaceType.PANORAMA_IMAGE,
                            onClick = { spaceFileType = SpaceType.PANORAMA_IMAGE }
                        )
                        Text(
                            text = stringResource(R.string.space_link_type_panorama_image),
                            modifier = Modifier.clickable { spaceFileType = SpaceType.PANORAMA_IMAGE }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = spaceFileType == SpaceType.PANORAMA_VIDEO,
                            onClick = { spaceFileType = SpaceType.PANORAMA_VIDEO }
                        )
                        Text(
                            text = stringResource(R.string.space_link_type_panorama_video),
                            modifier = Modifier.clickable { spaceFileType = SpaceType.PANORAMA_VIDEO }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = spaceFileType == SpaceType.GSPLAT,
                            onClick = { spaceFileType = SpaceType.GSPLAT }
                        )
                        Text(
                            text = stringResource(R.string.space_link_type_gsplat),
                            modifier = Modifier.clickable { spaceFileType = SpaceType.GSPLAT }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = spaceFileName,
                        onValueChange = { spaceFileName = it },
                        label = { Text(stringResource(R.string.space_link_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("上传文件", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))

                    if (spaceFilePath != null) {
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
                                    text = spaceFilePath!!.substringAfterLast('/'),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    val file = FileHelper.getFileFromRelativePath(context, spaceFilePath!!)
                                    if (file.exists()) file.delete()
                                    spaceFilePath = null
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = "移除文件")
                                }
                            }
                        }
                    } else {
                        TextButton(
                            onClick = {
                                val mimeTypes = when (spaceFileType) {
                                    SpaceType.PANORAMA_IMAGE -> "image/*"
                                    SpaceType.PANORAMA_VIDEO -> "video/*"
                                    SpaceType.GSPLAT -> "*/*"
                                }
                                spaceFilePickerLauncher.launch(mimeTypes)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("选择文件")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("缩略图", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))

                    if (spaceFileThumbnail != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = FileHelper.getFileFromRelativePath(context, spaceFileThumbnail!!),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = {
                                    FileHelper.deleteThumbnailFile(context, spaceFileThumbnail)
                                    spaceFileThumbnail = null
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
                            onClick = { spaceFileThumbnailPickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("上传缩略图")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (spaceFilePath != null) {
                        if (editingSpaceFileId == null && primaryFolderId == null && relatedFolderIds.isEmpty()) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.folder_required)
                                )
                            }
                            return@TextButton
                        }
                        val capturedFilePath = spaceFilePath!!
                        val capturedFileType = spaceFileType
                        val capturedFileName = spaceFileName.ifBlank {
                            capturedFilePath.substringAfterLast('/')
                        }
                        val capturedThumbnail = spaceFileThumbnail
                        val capturedPrimaryFolderId = primaryFolderId
                        val capturedEditId = editingSpaceFileId
                        scope.launch {
                            try {
                                if (capturedEditId != null) {
                                    // 编辑模式：先更新 DB + 引用（使卡片显示新名称），再替换短代码
                                    viewModel.updateSpaceFileReference(
                                        id = capturedEditId,
                                        spaceType = capturedFileType,
                                        filePath = capturedFilePath,
                                        name = capturedFileName,
                                        thumbnailUrl = capturedThumbnail
                                    )
                                    val newShortcode = RichEditorHelper.createSpaceFileShortcode(
                                        capturedEditId, capturedFileType,
                                        capturedFileName, capturedThumbnail
                                    )
                                    val newText = replaceSpaceFileShortcodeInMarkdown(
                                        textFieldValue.text, capturedEditId, newShortcode
                                    )
                                    textFieldValue = TextFieldValue(
                                        annotatedString = androidx.compose.ui.text.AnnotatedString(newText),
                                        selection = textFieldValue.selection,
                                        composition = textFieldValue.composition
                                    )
                                    viewModel.updateContentMarkdown(newText)
                                    showSpaceFileDialog = false
                                    editingSpaceFileId = null
                                    spaceFileType = SpaceType.PANORAMA_IMAGE
                                    spaceFileName = ""
                                    spaceFileThumbnail = null
                                    spaceFilePath = null
                                } else {
                                    val spaceFileId = viewModel.addSpaceFileReference(
                                        spaceType = capturedFileType,
                                        filePath = capturedFilePath,
                                        name = capturedFileName,
                                        thumbnailUrl = capturedThumbnail,
                                        folderId = capturedPrimaryFolderId
                                    )
                                    val shortcode = RichEditorHelper.createSpaceFileShortcode(
                                        spaceFileId, capturedFileType,
                                        capturedFileName, capturedThumbnail
                                    )
                                    textFieldValue = RichEditorHelper.insertAtCursor(textFieldValue, shortcode)
                                    viewModel.updateContentMarkdown(textFieldValue.text)
                                    showSpaceFileDialog = false
                                    editingSpaceFileId = null
                                    spaceFileType = SpaceType.PANORAMA_IMAGE
                                    spaceFileName = ""
                                    spaceFileThumbnail = null
                                    spaceFilePath = null
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("RecordEditor", "Failed space file operation", e)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (capturedEditId != null) "编辑空间文件失败: ${e.message}"
                                        else "添加空间文件失败: ${e.message}"
                                    )
                                }
                            }
                        }
                    }
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    FileHelper.deleteThumbnailFile(context, spaceFileThumbnail)
                    spaceFilePath?.let { path ->
                        val file = FileHelper.getFileFromRelativePath(context, path)
                        if (file.exists()) file.delete()
                    }
                    showSpaceFileDialog = false
                    spaceFileType = SpaceType.PANORAMA_IMAGE
                    spaceFileName = ""
                    spaceFileThumbnail = null
                    spaceFilePath = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 录音对话框
    if (showRecordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isRecording) {
                    cleanupRecordingResources()
                    showRecordDialog = false
                }
            },
            title = { Text("录制语音") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isRecording) {
                            val seconds = (recordingDuration / 1000).toString().padStart(2, '0')
                            val millis = (recordingDuration % 1000 / 10).toString().padStart(2, '0')
                            "$seconds.$millis"
                        } else {
                            "准备录音"
                        },
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!isRecording) {
                            androidx.compose.material3.FloatingActionButton(
                                onClick = {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPermission) {
                                        performStartRecording()
                                    } else {
                                        recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Mic, contentDescription = null)
                            }
                        } else {
                            androidx.compose.material3.FloatingActionButton(
                                onClick = {
                                    performStopRecording()
                                },
                                containerColor = MaterialTheme.colorScheme.error
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = null)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!isRecording) {
                            cleanupRecordingResources()
                            showRecordDialog = false
                        }
                    },
                    enabled = !isRecording
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            dismissButton = {}
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            cleanupRecordingResources()
        }
    }
}

private fun refTypeFromMediaType(mediaType: MediaType): RefType {
    return when (mediaType) {
        MediaType.IMAGE -> RefType.IMAGE
        MediaType.VIDEO -> RefType.VIDEO
        MediaType.VOICE -> RefType.VOICE
    }
}

@Composable
private fun MediaPreviewCard(
    mediaItem: MediaItem,
    context: android.content.Context,
    onRemove: () -> Unit
) {
    val typeLabel = when (mediaItem.mediaType) {
        MediaType.IMAGE -> "图片"
        MediaType.VIDEO -> "视频"
        MediaType.VOICE -> "语音"
    }
    val file = FileHelper.getFileFromRelativePath(context, mediaItem.fileRelativePath)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (mediaItem.mediaType) {
                MediaType.IMAGE -> {
                    if (file.exists()) {
                        AsyncImage(
                            model = Uri.fromFile(file),
                            contentDescription = null,
                            modifier = Modifier
                                .width(60.dp)
                                .height(60.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(60.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null)
                        }
                    }
                }
                MediaType.VIDEO -> {
                    if (file.exists()) {
                        VideoThumbnail(
                            videoFile = file,
                            modifier = Modifier
                                .width(60.dp)
                                .height(60.dp)
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .background(
                                        color = Color(0x88000000),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .padding(4.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(60.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Movie, contentDescription = null)
                        }
                    }
                }
                MediaType.VOICE -> {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(60.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.AudioFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "[$typeLabel]",
                    style = MaterialTheme.typography.titleMedium
                )
                if (mediaItem.mediaType == MediaType.VOICE && file.exists()) {
                    Text(
                        text = "${file.length() / 1024} KB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = null)
            }
        }
    }
}

@Composable
private fun SpaceLinkPreviewCard(
    link: SpaceLinkItem,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val typeLabel = when (link.spaceType) {
        SpaceType.PANORAMA_IMAGE -> "全景图片"
        SpaceType.PANORAMA_VIDEO -> "全景视频"
        SpaceType.GSPLAT -> "高斯泼溅"
    }
    val bgColor = when (link.spaceType) {
        SpaceType.PANORAMA_IMAGE -> Color(0xFFBBDEFB)
        SpaceType.PANORAMA_VIDEO -> Color(0xFF90CAF9)
        SpaceType.GSPLAT -> Color(0xFFB3E5FC)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
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
                        Text(
                            text = "3D",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF0D47A1)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(
                            color = Color(0xFF1565C0),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = typeLabel.take(1),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = link.name.ifEmpty { link.webUrl },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1565C0)
                )
            }

            IconButton(onClick = onClick) {
                Icon(Icons.Filled.Edit, contentDescription = null)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = null)
            }
        }
    }
}

@Composable
private fun SpaceFilePreviewCard(
    fileItem: SpaceFileItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val typeLabel = when (fileItem.spaceType) {
        SpaceType.PANORAMA_IMAGE -> "全景图片"
        SpaceType.PANORAMA_VIDEO -> "全景视频"
        SpaceType.GSPLAT -> "高斯泼溅"
    }
    val bgColor = when (fileItem.spaceType) {
        SpaceType.PANORAMA_IMAGE -> Color(0xFF6897BB)
        SpaceType.PANORAMA_VIDEO -> Color(0xFF808080)
        SpaceType.GSPLAT -> Color(0xFF6A8759)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (fileItem.thumbnailUrl != null && fileItem.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = FileHelper.getFileFromRelativePath(context, fileItem.thumbnailUrl),
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
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(
                            color = Color(0xFF2E7D32),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = typeLabel.take(1),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileItem.name.ifEmpty { fileItem.filePath.substringAfterLast('/') },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2E7D32)
                )
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "编辑")
            }
            IconButton(onClick = onClick) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = null)
            }
        }
    }
}

private fun replaceSpaceFileShortcodeInMarkdown(markdown: String, id: Long, newShortcode: String): String {
    val pattern = Regex("""\[spacefile]\[$id]\[[^\]]*]\[[^\]]*]\[[^\]]*]""")
    return pattern.replace(markdown, newShortcode)
}

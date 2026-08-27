package com.retimebox.lite.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Process
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.retimebox.lite.R
import com.retimebox.lite.util.BackupRestoreManager
import com.retimebox.lite.util.BackupRestoreState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenFolderManager: () -> Unit
) {
    val appName = stringResource(R.string.app_name)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var showAboutDialog by remember { mutableStateOf(!prefs.getBoolean(KEY_HAS_SEEN_ABOUT, false)) }
    var showBackupConfirm by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var progressPercentage by remember { mutableFloatStateOf(0f) }
    var isBackupOperation by remember { mutableStateOf(true) }

    val backupState by BackupRestoreManager.state.collectAsStateWithLifecycle()

    LaunchedEffect(backupState) {
        when (val state = backupState) {
            is BackupRestoreState.Idle -> { }
            is BackupRestoreState.InProgress -> {
                showProgressDialog = true
                progressPercentage = state.percentage
                isBackupOperation = state.isBackup
            }
            is BackupRestoreState.Success -> {
                showProgressDialog = false
                progressPercentage = 0f
                snackbarHostState.showSnackbar(state.message)
                if (state.isRestore) {
                    restartApp(context)
                }
                BackupRestoreManager.resetState()
            }
            is BackupRestoreState.Error -> {
                showProgressDialog = false
                progressPercentage = 0f
                snackbarHostState.showSnackbar(state.message)
                BackupRestoreManager.resetState()
            }
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    BackupRestoreManager.backup(
                        context = context,
                        destUri = uri,
                        onProgress = { progressPercentage = it }
                    )
                } catch (e: Exception) {
                    BackupRestoreManager.resetState()
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.backup_no_permission)
                    )
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    BackupRestoreManager.restore(
                        context = context,
                        sourceUri = uri,
                        onProgress = { progressPercentage = it }
                    )
                } catch (e: Exception) {
                    BackupRestoreManager.resetState()
                    snackbarHostState.showSnackbar(
                        e.message ?: context.getString(R.string.restore_failed)
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.general_settings),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = {
                            Text(text = stringResource(R.string.folder_management))
                        },
                        leadingContent = {
                            Icon(
                                Icons.Filled.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Icon(Icons.Filled.ChevronRight, contentDescription = null)
                        },
                        modifier = Modifier.clickable { onOpenFolderManager() }
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.backup)) },
                        leadingContent = {
                            Icon(
                                Icons.Filled.Save,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.clickable {
                            if (!BackupRestoreManager.isProcessing()) {
                                showBackupConfirm = true
                            }
                        }
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.restore)) },
                        leadingContent = {
                            Icon(
                                Icons.Filled.Restore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.clickable {
                            if (!BackupRestoreManager.isProcessing()) {
                                showRestoreConfirm = true
                            }
                        }
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text(text = "关于 $appName") },
                        modifier = Modifier.clickable { showAboutDialog = true }
                    )
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(text = "关于 $appName") },
            text = {
                val welcomeText = "欢迎使用 $appName 版。"
                val aboutText = """
                    ${welcomeText}

                    本应用是一款记录人生记忆的应用，支持文字、语音、图片、视频、全景图片、全景视频、高斯泼溅等方式记录记忆内容。

                    该版本只是一个简化版本，由于本人并非是专门搞编程的人，只是用AI写了下APP，不能写太复杂，那样不太适合，会太过于麻烦而且很难做出来。
                    
                    首次使用应用需要在设置页面中新建目录等相关操作，否则无法添加笔记内容，也无法直接添加图片等文件或其它内容。
                    
                    本应用已经调整为全部本地化使用，因在手机上存储数据文件，考虑到使用手机空间需要节约使用，因此本应用适合轻量化使用，影像内容其实可以看个大概，从而不在意清晰度，对于需要在意的清晰度的往往是人像影像，您可以将这类影像媒体处理成清晰一些的，但建议尽量小体积，不仅是指普通图片、普通视频，也指全景图片、全景视频、高斯泼溅，高斯泼溅建议几十MB大小更好，如果你有高斯泼溅文件体积不是几十MB大小的，可以用某些工具转换压缩，这个您自行解决，本应用不再建议使用自带的备份或还原功能，因为随着记录增多，全部存储的数据文件总体积会变大很多，使用应用自带的备份和恢复会比较花费时间，过程中不确保不会出现错误问题，建议您结合应用“MT管理器”应用手动备份还原文件，操作时应退出本应用，将此路径目录 /storage/emulated/0/Android/data/com.retimebox.lite/retimeboxlitefiles/  下的所有文件备份，还原时解压文件还原到该目录下。
                    
                    还原数据操作如果无法还原成功，请关闭本应用后，结合使用应用“MT管理器”，将备份后的压缩文件解压缩文件到 /Android/data/com.retimebox.lite/retimeboxlitefiles/ 目录下，进行此操作之前务必查看一下备份的压缩文件里是否正常看到文件，再进行手动还原操作，如果还原路径下有文件最好是删除处理后再进行手动还原操作。

                    如果有人感兴趣愿意去做或者有公司愿意去做，可以做出来一个完整的版本。
                """.trimIndent()
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(text = aboutText)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putBoolean(KEY_HAS_SEEN_ABOUT, true).apply()
                    showAboutDialog = false
                }) {
                    Text(text = stringResource(R.string.ok))
                }
            }
        )
    }

    if (showBackupConfirm) {
        AlertDialog(
            onDismissRequest = { showBackupConfirm = false },
            title = { Text(text = stringResource(R.string.backup_confirm_title)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(text = stringResource(R.string.backup_confirm_message))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showBackupConfirm = false
                    backupLauncher.launch(BackupRestoreManager.generateBackupFileName())
                }) {
                    Text(text = stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupConfirm = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text(text = stringResource(R.string.confirm_restore_title)) },
            text = {
                Text(text = stringResource(R.string.confirm_restore_message))
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    restoreLauncher.launch(arrayOf("application/zip", "*/*"))
                }) {
                    Text(text = stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showProgressDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = if (isBackupOperation)
                        stringResource(R.string.backup_in_progress)
                    else
                        stringResource(R.string.restore_in_progress)
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator()
                    LinearProgressIndicator(
                        progress = { progressPercentage },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${(progressPercentage * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = { }
        )
    }
}

private fun restartApp(context: Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
    if (intent != null) {
        intent.addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_CLEAR_TASK or
            Intent.FLAG_ACTIVITY_NEW_TASK
        )
        context.startActivity(intent)
        Process.killProcess(Process.myPid())
    }
}

@Composable
fun AboutDialog(
    appName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "关于 $appName") },
        text = {
            val welcomeText = "欢迎使用 $appName 版。"
            val aboutText = """
                ${welcomeText}

                本应用是一款记录人生记忆的应用，支持文字、语音、图片、视频、全景图片、全景视频、高斯泼溅等方式记录记忆内容。

                该版本只是一个简化版本，由于本人并非是专门搞编程的人，只是用AI写了下APP，不能写太复杂，那样不太适合，会太过于麻烦而且很难做出来。
                
                首次使用应用需要在设置页面中新建目录等相关操作，否则无法添加笔记内容，也无法直接添加图片等文件或其它内容。
                
                本应用已经调整为全部本地化使用，因在手机上存储数据文件，考虑到使用手机空间需要节约使用，因此本应用适合轻量化使用，影像内容其实可以看个大概，从而不在意清晰度，对于需要在意的清晰度的往往是人像影像，您可以将这类影像媒体处理成清晰一些的，但建议尽量小体积，不仅是指普通图片、普通视频，也指全景图片、全景视频、高斯泼溅，高斯泼溅建议几十MB大小更好，如果你有高斯泼溅文件体积不是几十MB大小的，可以用某些工具转换压缩，这个您自行解决，本应用不再建议使用自带的备份或还原功能，因为随着记录增多，全部存储的数据文件总体积会变大很多，使用应用自带的备份和恢复会比较花费时间，过程中不确保不会出现错误问题，建议您结合应用“MT管理器”应用手动备份还原文件，操作时应退出本应用，将此路径目录 /storage/emulated/0/Android/data/com.retimebox.lite/retimeboxlitefiles/  下的所有文件备份，还原时解压文件还原到该目录下。
                
                还原数据操作如果无法还原成功，请关闭本应用后，结合使用应用"MT管理器"，将备份后的压缩文件解压缩文件到 /Android/data/com.retimebox.lite/retimeboxlitefiles/ 目录下，进行此操作之前务必查看一下备份的压缩文件里是否正常看到文件，再进行手动还原操作，如果还原路径下有文件最好是删除处理后再进行手动还原操作。

                如果有人感兴趣愿意去做或者有公司愿意去做，可以做出来一个完整的版本。
            """.trimIndent()
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(text = aboutText)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.ok))
            }
        }
    )
}

private const val PREFS_NAME = "app_prefs"
private const val KEY_HAS_SEEN_ABOUT = "has_seen_about"
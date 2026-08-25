package com.retimebox.lite.util

import android.content.Context
import android.net.Uri
import com.retimebox.lite.R
import com.retimebox.lite.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

sealed class BackupRestoreState {
    data object Idle : BackupRestoreState()
    data class InProgress(val percentage: Float, val isBackup: Boolean) : BackupRestoreState()
    data class Success(val message: String, val isRestore: Boolean) : BackupRestoreState()
    data class Error(val message: String) : BackupRestoreState()
}

object BackupRestoreManager {

    private val _state = MutableStateFlow<BackupRestoreState>(BackupRestoreState.Idle)
    val state: StateFlow<BackupRestoreState> = _state.asStateFlow()

    private const val BUFFER_SIZE = 8192

    suspend fun backup(
        context: Context,
        destUri: Uri,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        _state.value = BackupRestoreState.InProgress(0f, true)
        try {
            val sourceDir = FileHelper.getExternalRootDir(context)
            if (!sourceDir.exists()) {
                throw IllegalStateException("源目录不存在")
            }

            val totalSize = calculateTotalSize(sourceDir)
            var copiedBytes = 0L

            context.contentResolver.openOutputStream(destUri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream, BUFFER_SIZE)).use { zipOut ->
                    sourceDir.walkTopDown().forEach { file ->
                        val relativePath = file.relativeTo(sourceDir).path

                        if (relativePath.isEmpty()) return@forEach

                        if (file.isDirectory) {
                            val dirEntry = ZipEntry(relativePath + "/")
                            dirEntry.time = file.lastModified()
                            zipOut.putNextEntry(dirEntry)
                            zipOut.closeEntry()
                        } else {
                            val entry = ZipEntry(relativePath)
                            entry.time = file.lastModified()
                            zipOut.putNextEntry(entry)

                            FileInputStream(file).use { fis ->
                                val buffer = ByteArray(BUFFER_SIZE)
                                var read: Int
                                while (fis.read(buffer).also { read = it } != -1) {
                                    zipOut.write(buffer, 0, read)
                                }
                            }
                            copiedBytes += file.length()
                            val progress = if (totalSize > 0) copiedBytes.toFloat() / totalSize else 1f
                            onProgress(progress.coerceIn(0f, 1f))
                            _state.value = BackupRestoreState.InProgress(progress, true)

                            zipOut.closeEntry()
                        }
                    }
                }
            } ?: throw IllegalStateException("无法打开输出流")

            _state.value = BackupRestoreState.Success(
                context.getString(R.string.backup_success),
                false
            )
        } catch (e: Exception) {
            android.util.Log.e("BackupRestoreManager", "Backup failed", e)
            _state.value = BackupRestoreState.Error(
                e.message ?: context.getString(R.string.backup_failed)
            )
        }
    }

    suspend fun restore(
        context: Context,
        sourceUri: Uri,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        _state.value = BackupRestoreState.InProgress(0f, false)
        var tempDir: File? = null
        try {
            val destDir = FileHelper.getExternalRootDir(context)

            // 断开数据库连接，避免文件占用
            AppDatabase.closeAndClearInstance()

            tempDir = File(context.cacheDir, "restore_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            // 第一遍：统计条目数量
            var totalEntries = 0
            context.contentResolver.openInputStream(sourceUri)?.use { countStream ->
                ZipInputStream(BufferedInputStream(countStream, BUFFER_SIZE)).use { zipCount ->
                    while (zipCount.nextEntry != null) totalEntries++
                }
            } ?: throw IllegalStateException("无法打开输入流")

            // 第二遍：实际解压
            var processedEntries = 0
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream, BUFFER_SIZE)).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val outFile = File(tempDir!!, entry.name)
                        val parentDir = outFile.parentFile
                        if (parentDir != null && !parentDir.exists()) {
                            parentDir.mkdirs()
                        }

                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            FileOutputStream(outFile).use { fos ->
                                val buffer = ByteArray(BUFFER_SIZE)
                                var read: Int
                                while (zipIn.read(buffer).also { read = it } != -1) {
                                    fos.write(buffer, 0, read)
                                }
                            }
                        }

                        zipIn.closeEntry()
                        processedEntries++
                        val progress = processedEntries.toFloat() / totalEntries.coerceAtLeast(1)
                        onProgress(progress)
                        _state.value = BackupRestoreState.InProgress(progress, false)
                        entry = zipIn.nextEntry
                    }
                }
            } ?: throw IllegalStateException("无法打开输入流")

            // 复制临时目录到目标目录（跨文件系统安全）
            if (destDir.exists()) {
                destDir.deleteRecursively()
            }
            tempDir!!.copyRecursively(destDir, overwrite = true)
            tempDir = null

            // 还原数据库连接
            AppDatabase.getInstance(context)

            _state.value = BackupRestoreState.Success(
                context.getString(R.string.restore_success),
                true
            )
        } catch (e: Exception) {
            android.util.Log.e("BackupRestoreManager", "Restore failed", e)
            tempDir?.deleteRecursively()

            // 失败后也恢复数据库连接
            AppDatabase.getInstance(context)

            _state.value = BackupRestoreState.Error(
                e.message ?: context.getString(R.string.restore_failed)
            )
        }
    }

    fun resetState() {
        _state.value = BackupRestoreState.Idle
    }

    fun generateBackupFileName(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "RETimeBox_backup_${dateFormat.format(Date())}.zip"
    }

    private fun calculateTotalSize(dir: File): Long {
        var size = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) size += file.length()
        }
        return size
    }

    fun isProcessing(): Boolean {
        return _state.value is BackupRestoreState.InProgress
    }
}
package com.retimebox.lite.util

import android.content.Context
import android.net.Uri
import com.retimebox.lite.data.local.entity.MediaType
import com.retimebox.lite.data.local.entity.SpaceFileItem
import com.retimebox.lite.data.local.entity.SpaceType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FileHelper {

    private const val VOICE_DIR = "voice"
    private const val IMAGE_DIR = "image"
    private const val VIDEO_DIR = "video"
    private const val SPURL_DIR = "spurl"
    private const val MD_DIR = "md"
    private const val DB_DIR = "db"
    private const val THUMBNAIL_DIR = "thumbnails"
    private const val EXTERNAL_ROOT = "retimeboxlitefiles"
    private const val PANOIMG_DIR = "panoimg"
    private const val PANOVIDEO_DIR = "panovideo"
    private const val GSPLAT_DIR = "gsplat"

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault())

    private fun currentYear(): String =
        Calendar.getInstance().get(Calendar.YEAR).toString()

    private fun yearFromDate(dateMillis: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dateMillis
        return cal.get(Calendar.YEAR).toString()
    }

    /**
     * 外部存储根目录：/storage/emulated/0/Android/data/<pkg>/retimeboxlitefiles/
     */
    fun getExternalRootDir(context: Context): File {
        val extDir = context.getExternalFilesDir(null)
            ?: context.filesDir
        val root = File(extDir.parentFile ?: extDir, EXTERNAL_ROOT)
        if (!root.exists()) root.mkdirs()
        return root
    }

    /**
     * 获取 DB 目录（无年份）：retimeboxlitefiles/db/
     */
    fun getDbDir(context: Context): File {
        val dir = File(getExternalRootDir(context), DB_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 获取应用私有 files 根目录（兼容旧调用）
     * 现在指向外部存储的 retimeboxlitefiles/ 目录
     */
    fun getFilesDir(context: Context): File = getExternalRootDir(context)

    /**
     * 根据媒体类型获取带年份的子目录
     */
    fun getSubDir(context: Context, mediaType: MediaType): File {
        val subDir = when (mediaType) {
            MediaType.VOICE -> VOICE_DIR
            MediaType.IMAGE -> IMAGE_DIR
            MediaType.VIDEO -> VIDEO_DIR
        }
        val dir = File(getExternalRootDir(context), "$subDir/${currentYear()}")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 获取 spurl 子目录（带年份）：retimeboxlitefiles/spurl/2026/
     */
    fun getSpurlDir(context: Context, year: String = currentYear()): File {
        val dir = File(getExternalRootDir(context), "$SPURL_DIR/$year")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 获取 thumbnails 子目录（带年份）：retimeboxlitefiles/image/2026/thumbnails/
     */
    fun getThumbnailsDir(context: Context, year: String = currentYear()): File {
        val dir = File(getExternalRootDir(context), "$IMAGE_DIR/$year/$THUMBNAIL_DIR")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 复制缩略图到 thumbnails 目录，返回相对路径
     */
    suspend fun copyThumbnailToDir(context: Context, uri: Uri): String? {
        return try {
            val dir = getThumbnailsDir(context)
            val fileName = "thumb_${System.currentTimeMillis()}.jpg"
            val destFile = File(dir, fileName)

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open input stream for URI: $uri")

            inputStream.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            }

            if (!destFile.exists() || destFile.length() == 0L) {
                destFile.delete()
                throw IllegalStateException("Failed to copy thumbnail or file is empty")
            }

            "$IMAGE_DIR/${currentYear()}/$THUMBNAIL_DIR/$fileName"
        } catch (e: Exception) {
            android.util.Log.e("FileHelper", "copyThumbnailToDir failed", e)
            null
        }
    }

    /**
     * 删除缩略图文件
     */
    fun deleteThumbnailFile(context: Context, relativePath: String?) {
        if (relativePath.isNullOrBlank()) return
        try {
            val file = getFileFromRelativePath(context, relativePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            android.util.Log.e("FileHelper", "deleteThumbnailFile failed", e)
        }
    }

    /**
     * 获取 md 子目录（带年份）：retimeboxlitefiles/md/2026/
     */
    fun getMdDir(context: Context, year: String = currentYear()): File {
        val dir = File(getExternalRootDir(context), "$MD_DIR/$year")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 生成唯一文件名
     */
    fun generateFileName(extension: String): String {
        val timestamp = dateFormat.format(Date())
        return "RETimeBox_${timestamp}.$extension"
    }

    /**
     * 从 Uri 拷贝文件到私有目录，返回相对路径
     */
    suspend fun copyUriToPrivateDir(
        context: Context,
        uri: Uri,
        mediaType: MediaType
    ): String? {
        return try {
            val dir = getSubDir(context, mediaType)
            val extension = getDefaultExtension(mediaType)
            val fileName = generateFileName(extension)
            val destFile = File(dir, fileName)

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open input stream for URI: $uri")

            inputStream.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            }

            if (!destFile.exists() || destFile.length() == 0L) {
                destFile.delete()
                throw IllegalStateException("Failed to copy file or file is empty")
            }

            getRelativePath(mediaType, fileName)
        } catch (e: Exception) {
            android.util.Log.e("FileHelper", "copyUriToPrivateDir failed", e)
            null
        }
    }

    /**
     * 从相对路径获取绝对路径 File 对象
     */
    fun getFileFromRelativePath(context: Context, relativePath: String): File {
        return File(getExternalRootDir(context), relativePath)
    }

    /**
     * 删除私有目录下的文件
     */
    suspend fun deleteRelativePath(context: Context, relativePath: String) {
        val file = getFileFromRelativePath(context, relativePath)
        if (file.exists()) {
            file.delete()
        }
    }

    /**
     * 根据媒体类型获取默认扩展名
     */
    fun getDefaultExtension(mediaType: MediaType): String {
        return when (mediaType) {
            MediaType.VOICE -> "m4a"
            MediaType.IMAGE -> "jpg"
            MediaType.VIDEO -> "mp4"
        }
    }

    /**
     * 构建相对路径（含年份，供数据库存储）
     */
    fun getRelativePath(mediaType: MediaType, fileName: String): String {
        val subDir = when (mediaType) {
            MediaType.VOICE -> VOICE_DIR
            MediaType.IMAGE -> IMAGE_DIR
            MediaType.VIDEO -> VIDEO_DIR
        }
        return "$subDir/${currentYear()}/$fileName"
    }

    /**
     * 获取媒体类型对应的子目录名
     */
    fun getSubDirName(mediaType: MediaType): String = when (mediaType) {
        MediaType.VOICE -> VOICE_DIR
        MediaType.IMAGE -> IMAGE_DIR
        MediaType.VIDEO -> VIDEO_DIR
    }

    /**
     * 创建模拟语音文件（临时方案，用于测试）
     */
    fun createMockVoiceFile(context: Context, durationMs: Long): String? {
        return try {
            val dir = getSubDir(context, MediaType.VOICE)
            val fileName = "RETimeBox_${System.currentTimeMillis()}.m4a"
            val destFile = File(dir, fileName)

            destFile.outputStream().use { output ->
                val durationSeconds = (durationMs / 1000L).coerceAtLeast(1L).toInt()
                val placeholderSize = minOf(durationSeconds * 1000L, 10000L).toInt()
                val placeholder = ByteArray(placeholderSize) { 0 }
                output.write(placeholder)
            }

            if (destFile.exists() && destFile.length() > 0) {
                "$VOICE_DIR/${currentYear()}/$fileName"
            } else {
                destFile.delete()
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("FileHelper", "createMockVoiceFile failed", e)
            null
        }
    }

    /**
     * 测试用方法（占位）
     */
    fun getRelativePathForTest(): String? = null

    /**
     * 保存笔记 MD 文件到外部 md/年份/ 目录
     */
    fun saveRecordMarkdown(
        context: Context,
        recordId: Long,
        title: String,
        contentMarkdown: String,
        recordDate: Long = System.currentTimeMillis()
    ) {
        try {
            val year = yearFromDate(recordDate)
            val mdDir = getMdDir(context, year)

            val safeTitle = title.ifBlank { "untitled_$recordId" }
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .take(100)
            val fileName = "${safeTitle}_$recordId.md"
            val mdFile = File(mdDir, fileName)
            mdFile.writeText(contentMarkdown)
        } catch (e: Exception) {
            android.util.Log.e("FileHelper", "saveRecordMarkdown failed", e)
        }
    }

    /**
     * 删除笔记 MD 文件
     */
    fun deleteRecordMarkdown(
        context: Context,
        recordId: Long,
        title: String,
        recordDate: Long = System.currentTimeMillis()
    ) {
        try {
            val year = yearFromDate(recordDate)
            val mdDir = getMdDir(context, year)
            if (!mdDir.exists()) return

            val safeTitle = title.ifBlank { "untitled_$recordId" }
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .take(100)
            val fileName = "${safeTitle}_$recordId.md"
            val mdFile = File(mdDir, fileName)
            if (mdFile.exists()) {
                mdFile.delete()
            }
        } catch (e: Exception) {
            android.util.Log.e("FileHelper", "deleteRecordMarkdown failed", e)
        }
    }

    /**
     * 更新笔记 MD 文件（先删除旧年份的文件，再创建新年份的文件）
     */
    fun updateRecordMarkdown(
        context: Context,
        recordId: Long,
        oldTitle: String,
        newTitle: String,
        contentMarkdown: String,
        oldRecordDate: Long,
        newRecordDate: Long
    ) {
        val oldYear = yearFromDate(oldRecordDate)
        val newYear = yearFromDate(newRecordDate)
        if (oldTitle != newTitle || oldYear != newYear) {
            deleteRecordMarkdown(context, recordId, oldTitle, oldRecordDate)
        }
        saveRecordMarkdown(context, recordId, newTitle, contentMarkdown, newRecordDate)
    }

    /**
     * 保存空间链接 txt 文件到 spurl/年份/ 目录
     */
    fun saveSpaceLinkTxt(
        context: Context,
        itemId: Long,
        webUrl: String,
        name: String,
        spaceType: SpaceType,
        thumbnailUrl: String?,
        year: String = currentYear()
    ) {
        try {
            val dir = getSpurlDir(context, year)
            val fileName = "spurl_${itemId}.txt"
            val txtFile = File(dir, fileName)

            val typeLabel = when (spaceType) {
                SpaceType.PANORAMA_IMAGE -> "全景图片"
                SpaceType.PANORAMA_VIDEO -> "全景视频"
                SpaceType.GSPLAT -> "高斯泼溅"
            }

            val content = buildString {
                appendLine("URL: $webUrl")
                appendLine("名称: $name")
                appendLine("类型: $typeLabel")
                if (!thumbnailUrl.isNullOrBlank()) {
                    appendLine("缩略图: $thumbnailUrl")
                }
            }
            txtFile.writeText(content)
        } catch (e: Exception) {
            android.util.Log.e("FileHelper", "saveSpaceLinkTxt failed", e)
        }
    }

    /**
     * 删除空间链接 txt 文件
     */
    fun deleteSpaceLinkTxt(
        context: Context,
        itemId: Long,
        year: String = currentYear()
    ) {
        try {
            val dir = getSpurlDir(context, year)
            val fileName = "spurl_${itemId}.txt"
            val txtFile = File(dir, fileName)
            if (txtFile.exists()) {
                txtFile.delete()
            }
        } catch (e: Exception) {
            android.util.Log.e("FileHelper", "deleteSpaceLinkTxt failed", e)
        }
    }

    fun getPanoimgDir(context: Context): File {
        val dir = File(getExternalRootDir(context), PANOIMG_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getPanovideoDir(context: Context): File {
        val dir = File(getExternalRootDir(context), PANOVIDEO_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getGsplatDir(context: Context): File {
        val dir = File(getExternalRootDir(context), GSPLAT_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getSpaceFileDir(context: Context, spaceType: SpaceType): File {
        return when (spaceType) {
            SpaceType.PANORAMA_IMAGE -> getPanoimgDir(context)
            SpaceType.PANORAMA_VIDEO -> getPanovideoDir(context)
            SpaceType.GSPLAT -> getGsplatDir(context)
        }
    }

    suspend fun copySpaceFileToDir(context: Context, uri: Uri, spaceType: SpaceType): String? {
        return try {
            val dir = getSpaceFileDir(context, spaceType)
            val originalExt = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase()
            val extension = when (spaceType) {
                SpaceType.PANORAMA_IMAGE -> if (originalExt == "png" || originalExt == "jpg" || originalExt == "jpeg") originalExt else "jpg"
                SpaceType.PANORAMA_VIDEO -> if (originalExt == "mp4") originalExt else "mp4"
                SpaceType.GSPLAT -> if (originalExt == "ply" || originalExt == "sog") originalExt else "ply"
            }
            val fileName = generateFileName(extension)
            val destFile = File(dir, fileName)

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open input stream for URI: $uri")

            inputStream.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            }

            if (!destFile.exists() || destFile.length() == 0L) {
                destFile.delete()
                throw IllegalStateException("Failed to copy space file or file is empty")
            }

            "${getSpaceFileRelativeDir(spaceType)}/$fileName"
        } catch (e: Exception) {
            android.util.Log.e("FileHelper", "copySpaceFileToDir failed", e)
            null
        }
    }

    private fun getSpaceFileRelativeDir(spaceType: SpaceType): String {
        return when (spaceType) {
            SpaceType.PANORAMA_IMAGE -> PANOIMG_DIR
            SpaceType.PANORAMA_VIDEO -> PANOVIDEO_DIR
            SpaceType.GSPLAT -> GSPLAT_DIR
        }
    }

    fun getSpaceFileAbsolutePath(context: Context, relativePath: String): String {
        val file = getFileFromRelativePath(context, relativePath)
        return file.absolutePath
    }

    /**
     * 保存空间文件 MD 信息到 spfile/{year}/{id}.md
     * 文件内容每行：类型标签、名称、文件路径、缩略图路径
     */
    fun saveSpaceFileMd(context: Context, item: SpaceFileItem) {
        try {
            val year = yearFromDate(item.createTime)
            val dir = File(getExternalRootDir(context), "spfile/$year")
            if (!dir.exists()) dir.mkdirs()
            val mdFile = File(dir, "${item.id}.md")

            val typeLabel = when (item.spaceType) {
                SpaceType.PANORAMA_IMAGE -> "全景图片"
                SpaceType.PANORAMA_VIDEO -> "全景视频"
                SpaceType.GSPLAT -> "高斯泼溅"
            }
            mdFile.writeText("$typeLabel\n${item.name}\n${item.filePath}\n${item.thumbnailUrl ?: ""}")
        } catch (e: Exception) {
            android.util.Log.e("FileHelper", "saveSpaceFileMd failed", e)
        }
    }

    /**
     * 删除空间文件 MD 信息文件 spfile/{year}/{id}.md
     */
    fun deleteSpaceFileMd(context: Context, id: Long, createTime: Long) {
        try {
            val year = yearFromDate(createTime)
            val mdFile = File(getExternalRootDir(context), "spfile/$year/$id.md")
            if (mdFile.exists()) {
                mdFile.delete()
            }
        } catch (e: Exception) {
            android.util.Log.e("FileHelper", "deleteSpaceFileMd failed", e)
        }
    }
}

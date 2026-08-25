package com.retimebox.lite.data.repository

import com.retimebox.lite.data.local.dao.MediaItemDao
import com.retimebox.lite.data.local.dao.RecordDao
import com.retimebox.lite.data.local.entity.MediaItem
import com.retimebox.lite.data.local.entity.MediaType
import com.retimebox.lite.data.local.entity.SourceType
import kotlinx.coroutines.flow.Flow

class MediaRepository(
    private val mediaItemDao: MediaItemDao,
    private val recordDao: RecordDao
) {

    fun observeByFolderAndType(folderId: Long, mediaType: MediaType): Flow<List<MediaItem>> =
        mediaItemDao.observeByFolderAndType(folderId, mediaType)

    fun observeByType(mediaType: MediaType): Flow<List<MediaItem>> =
        mediaItemDao.observeByType(mediaType)

    fun observeDirectAddByFolderAndType(folderId: Long, mediaType: MediaType): Flow<List<MediaItem>> =
        mediaItemDao.observeDirectAddByFolderAndType(folderId, mediaType)

    fun observeIndexByType(mediaType: MediaType): Flow<List<MediaItem>> =
        mediaItemDao.observeIndexByType(mediaType)

    fun observeDirectAddByType(mediaType: MediaType): Flow<List<MediaItem>> =
        mediaItemDao.observeDirectAddByType(mediaType)

    fun observeIndexItemsByFolderAndType(folderId: Long, mediaType: MediaType): Flow<List<MediaItem>> =
        mediaItemDao.observeIndexItemsByFolderAndType(folderId, mediaType)

    suspend fun findById(id: Long): MediaItem? = mediaItemDao.findById(id)

    /**
     * 新增 DIRECT_ADD 媒体条目
     */
    suspend fun addDirectAdd(
        mediaType: MediaType,
        fileRelativePath: String,
        folderId: Long
    ): Long {
        val item = MediaItem(
            mediaType = mediaType,
            fileRelativePath = fileRelativePath,
            sourceType = SourceType.DIRECT_ADD,
            folderId = folderId
        )
        return mediaItemDao.insert(item)
    }

    /**
     * 新增 FROM_RECORD_INDEX 索引条目
     */
    suspend fun addIndexItem(
        mediaType: MediaType,
        fileRelativePath: String,
        bindRecordId: Long,
        folderId: Long
    ): Long {
        val item = MediaItem(
            mediaType = mediaType,
            fileRelativePath = fileRelativePath,
            sourceType = SourceType.FROM_RECORD_INDEX,
            bindRecordId = bindRecordId,
            folderId = folderId
        )
        return mediaItemDao.insert(item)
    }

    /**
     * 直接新增媒体（供编辑器使用，返回 id 供引用）
     */
    suspend fun insertForEditor(
        mediaType: MediaType,
        fileRelativePath: String,
        folderId: Long
    ): MediaItem {
        val item = MediaItem(
            mediaType = mediaType,
            fileRelativePath = fileRelativePath,
            sourceType = SourceType.DIRECT_ADD,
            folderId = folderId
        )
        val id = mediaItemDao.insert(item)
        return item.copy(id = id)
    }

    /**
     * 批量删除 DIRECT_ADD 条目（数据层二次校验）
     */
    suspend fun batchDeleteDirectAdd(
        ids: List<Long>,
        onDeleteFile: suspend (String) -> Unit = {}
    ) {
        // 数据层二次校验：只允许删除 DIRECT_ADD，防止绕过 UI 操作索引条目
        val items = mediaItemDao.getDirectAddByIds(ids)
        val filePaths = items.map { it.fileRelativePath }.distinct()

        for (item in items) {
            mediaItemDao.deleteById(item.id)
        }

        // 引用计数：计数为0才删磁盘文件
        for (path in filePaths) {
            val count = mediaItemDao.countByFilePath(path)
            if (count == 0) {
                onDeleteFile(path)
            }
        }
    }

    /**
     * 批量修改文件夹归属（仅 DIRECT_ADD 条目）
     */
    suspend fun batchMoveFolder(ids: List<Long>, targetFolderId: Long) {
        // 数据层二次校验：只操作 DIRECT_ADD
        val items = mediaItemDao.getDirectAddByIds(ids)
        val validIds = items.map { it.id }
        if (validIds.isNotEmpty()) {
            mediaItemDao.moveFolder(validIds, targetFolderId)
        }
    }

    /**
     * 单条删除（引用计数检查）
     */
    suspend fun deleteById(id: Long, onDeleteFile: suspend (String) -> Unit = {}) {
        val item = mediaItemDao.findById(id) ?: return
        mediaItemDao.deleteById(id)
        val count = mediaItemDao.countByFilePath(item.fileRelativePath)
        if (count == 0) {
            onDeleteFile(item.fileRelativePath)
        }
    }
}

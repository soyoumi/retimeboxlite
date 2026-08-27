package com.retimebox.lite.data.repository

import com.retimebox.lite.data.local.dao.SpaceFileItemDao
import com.retimebox.lite.data.local.entity.SpaceFileItem
import com.retimebox.lite.data.local.entity.SpaceType
import com.retimebox.lite.data.local.entity.SourceType
import com.retimebox.lite.util.FileHelper
import kotlinx.coroutines.flow.Flow

class SpaceFileRepository(
    private val spaceFileItemDao: SpaceFileItemDao
) {

    fun observeByFolder(folderId: Long): Flow<List<SpaceFileItem>> =
        spaceFileItemDao.observeByFolder(folderId)

    fun observeAll(): Flow<List<SpaceFileItem>> = spaceFileItemDao.observeAll()

    suspend fun findById(id: Long): SpaceFileItem? = spaceFileItemDao.findById(id)

    suspend fun findByIds(ids: List<Long>): List<SpaceFileItem> = spaceFileItemDao.getByIds(ids)

    suspend fun findDirectAddByPath(filePath: String): SpaceFileItem? =
        spaceFileItemDao.findDirectAddByPath(filePath)

    suspend fun deleteIndexItem(id: Long) {
        spaceFileItemDao.deleteById(id)
    }

    suspend fun update(item: SpaceFileItem) {
        spaceFileItemDao.update(item)
    }

    suspend fun insert(item: SpaceFileItem): Long = spaceFileItemDao.insert(item)

    suspend fun insertForEditor(
        spaceType: SpaceType,
        filePath: String,
        name: String,
        thumbnailUrl: String?,
        folderId: Long,
        bindRecordId: Long? = null
    ): SpaceFileItem {
        val item = SpaceFileItem(
            spaceType = spaceType,
            filePath = filePath,
            name = name,
            thumbnailUrl = thumbnailUrl,
            sourceType = SourceType.DIRECT_ADD,
            folderId = folderId,
            bindRecordId = bindRecordId
        )
        val id = spaceFileItemDao.insert(item)
        return item.copy(id = id)
    }

    suspend fun deleteById(id: Long) {
        val item = spaceFileItemDao.findById(id)
        if (item != null) {
            spaceFileItemDao.deleteById(id)
        }
    }

    suspend fun deleteByIdWithFile(context: android.content.Context, id: Long) {
        val item = spaceFileItemDao.findById(id)
        if (item != null) {
            spaceFileItemDao.deleteById(id)
            val file = FileHelper.getFileFromRelativePath(context, item.filePath)
            if (file.exists()) {
                file.delete()
            }
            if (!item.thumbnailUrl.isNullOrBlank()) {
                FileHelper.deleteThumbnailFile(context, item.thumbnailUrl)
            }
        }
    }

    suspend fun batchDelete(ids: List<Long>, context: android.content.Context) {
        val items = spaceFileItemDao.getByIds(ids)
        for (item in items) {
            val file = FileHelper.getFileFromRelativePath(context, item.filePath)
            if (file.exists()) {
                file.delete()
            }
            if (!item.thumbnailUrl.isNullOrBlank()) {
                FileHelper.deleteThumbnailFile(context, item.thumbnailUrl)
            }
        }
        spaceFileItemDao.deleteByIds(ids)
    }

    suspend fun batchMoveFolder(ids: List<Long>, targetFolderId: Long) {
        spaceFileItemDao.moveFolder(ids, targetFolderId)
    }

    suspend fun updateByFilePath(filePath: String, spaceType: SpaceType, name: String, thumbnailUrl: String?) {
        spaceFileItemDao.updateByFilePath(filePath, spaceType.name, name, thumbnailUrl)
    }
}

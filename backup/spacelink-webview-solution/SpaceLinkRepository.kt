package com.retimebox.lite.data.repository

import com.retimebox.lite.data.local.dao.RecordDao
import com.retimebox.lite.data.local.dao.SpaceLinkItemDao
import com.retimebox.lite.data.local.entity.SourceType
import com.retimebox.lite.data.local.entity.SpaceLinkItem
import com.retimebox.lite.data.local.entity.SpaceType
import kotlinx.coroutines.flow.Flow

class SpaceLinkRepository(
    private val spaceLinkItemDao: SpaceLinkItemDao,
    private val recordDao: RecordDao
) {

    fun observeByFolder(folderId: Long): Flow<List<SpaceLinkItem>> =
        spaceLinkItemDao.observeByFolder(folderId)

    fun observeDirectAddByFolder(folderId: Long): Flow<List<SpaceLinkItem>> =
        spaceLinkItemDao.observeDirectAddByFolder(folderId)

    fun observeIndexByFolder(folderId: Long): Flow<List<SpaceLinkItem>> =
        spaceLinkItemDao.observeIndexByFolder(folderId)

    fun observeAll(): Flow<List<SpaceLinkItem>> = spaceLinkItemDao.observeAll()

    suspend fun findById(id: Long): SpaceLinkItem? = spaceLinkItemDao.findById(id)

    suspend fun update(item: SpaceLinkItem) {
        spaceLinkItemDao.update(item)
    }

    suspend fun addDirectAdd(
        spaceType: SpaceType,
        webUrl: String,
        name: String,
        thumbnailUrl: String?,
        folderId: Long
    ): Long {
        val item = SpaceLinkItem(
            spaceType = spaceType,
            webUrl = webUrl,
            name = name,
            thumbnailUrl = thumbnailUrl,
            sourceType = SourceType.DIRECT_ADD,
            folderId = folderId
        )
        return spaceLinkItemDao.insert(item)
    }

    suspend fun addIndexItem(
        spaceType: SpaceType,
        webUrl: String,
        name: String,
        thumbnailUrl: String?,
        bindRecordId: Long,
        folderId: Long
    ): Long {
        val item = SpaceLinkItem(
            spaceType = spaceType,
            webUrl = webUrl,
            name = name,
            thumbnailUrl = thumbnailUrl,
            sourceType = SourceType.FROM_RECORD_INDEX,
            bindRecordId = bindRecordId,
            folderId = folderId
        )
        return spaceLinkItemDao.insert(item)
    }

    suspend fun insertForEditor(
        spaceType: SpaceType,
        webUrl: String,
        name: String,
        thumbnailUrl: String?,
        folderId: Long
    ): SpaceLinkItem {
        val item = SpaceLinkItem(
            spaceType = spaceType,
            webUrl = webUrl,
            name = name,
            thumbnailUrl = thumbnailUrl,
            sourceType = SourceType.DIRECT_ADD,
            folderId = folderId
        )
        val id = spaceLinkItemDao.insert(item)
        return item.copy(id = id)
    }

    suspend fun batchDeleteDirectAdd(ids: List<Long>) {
        spaceLinkItemDao.deleteDirectAddByIds(ids)
    }

    suspend fun batchMoveFolder(ids: List<Long>, targetFolderId: Long) {
        val items = spaceLinkItemDao.getDirectAddByIds(ids)
        val validIds = items.map { it.id }
        if (validIds.isNotEmpty()) {
            spaceLinkItemDao.moveFolder(validIds, targetFolderId)
        }
    }

    suspend fun deleteById(id: Long) {
        spaceLinkItemDao.deleteById(id)
    }
}

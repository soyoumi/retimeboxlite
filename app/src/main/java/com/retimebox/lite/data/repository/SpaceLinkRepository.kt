package com.retimebox.lite.data.repository

import android.content.Context
import com.retimebox.lite.data.local.dao.RecordDao
import com.retimebox.lite.data.local.dao.SpaceLinkItemDao
import com.retimebox.lite.data.local.entity.SourceType
import com.retimebox.lite.data.local.entity.SpaceLinkItem
import com.retimebox.lite.data.local.entity.SpaceType
import com.retimebox.lite.util.FileHelper
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

    /**
     * 新增 DIRECT_ADD 空间链接（同时保存 spurl txt 文件）
     */
    suspend fun addDirectAdd(
        context: Context,
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
        val id = spaceLinkItemDao.insert(item)
        FileHelper.saveSpaceLinkTxt(
            context = context,
            itemId = id,
            webUrl = webUrl,
            name = name,
            spaceType = spaceType,
            thumbnailUrl = thumbnailUrl
        )
        return id
    }

    /**
     * 新增 FROM_RECORD_INDEX 索引条目
     */
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

    /**
     * 插入供编辑器使用（同时保存 spurl txt 文件）
     */
    suspend fun insertForEditor(
        context: Context,
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
        FileHelper.saveSpaceLinkTxt(
            context = context,
            itemId = id,
            webUrl = webUrl,
            name = name,
            spaceType = spaceType,
            thumbnailUrl = thumbnailUrl
        )
        return item.copy(id = id)
    }

    /**
     * 批量删除 DIRECT_ADD 条目（数据层二次校验 + 删除 spurl txt）
     */
    suspend fun batchDeleteDirectAdd(ids: List<Long>, context: Context? = null) {
        val items = spaceLinkItemDao.getDirectAddByIds(ids)
        for (item in items) {
            spaceLinkItemDao.deleteById(item.id)
            if (context != null) {
                FileHelper.deleteSpaceLinkTxt(context, item.id)
            }
        }
    }

    /**
     * 批量修改文件夹归属（仅 DIRECT_ADD）
     */
    suspend fun batchMoveFolder(ids: List<Long>, targetFolderId: Long) {
        val items = spaceLinkItemDao.getDirectAddByIds(ids)
        val validIds = items.map { it.id }
        if (validIds.isNotEmpty()) {
            spaceLinkItemDao.moveFolder(validIds, targetFolderId)
        }
    }

    suspend fun deleteById(id: Long, context: Context? = null) {
        spaceLinkItemDao.deleteById(id)
        if (context != null) {
            FileHelper.deleteSpaceLinkTxt(context, id)
        }
    }
}

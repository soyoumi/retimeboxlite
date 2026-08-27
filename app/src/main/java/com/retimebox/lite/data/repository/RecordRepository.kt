package com.retimebox.lite.data.repository

import android.content.Context
import com.retimebox.lite.data.local.dao.MediaItemDao
import com.retimebox.lite.data.local.dao.RecordDao
import com.retimebox.lite.data.local.dao.SpaceFileItemDao
import com.retimebox.lite.data.local.dao.SpaceLinkItemDao
import com.retimebox.lite.data.local.entity.ContentReference
import com.retimebox.lite.data.local.entity.MediaItem
import com.retimebox.lite.data.local.entity.Record
import com.retimebox.lite.data.local.entity.RefType
import com.retimebox.lite.data.local.entity.SourceType
import com.retimebox.lite.data.local.entity.SpaceLinkItem
import com.retimebox.lite.util.FileHelper
import com.retimebox.lite.util.RichEditorHelper
import kotlinx.coroutines.flow.Flow

class RecordRepository(
    private val recordDao: RecordDao,
    private val mediaItemDao: MediaItemDao,
    private val spaceLinkItemDao: SpaceLinkItemDao,
    private val spaceFileItemDao: SpaceFileItemDao
) {

    fun observeRecord(id: Long): Flow<Record?> = recordDao.observeById(id)

    fun observeAllRecords(): Flow<List<Record>> = recordDao.observeAll()

    fun observeRecordsByDateRange(start: Long, end: Long): Flow<List<Record>> =
        recordDao.observeByDateRange(start, end)

    suspend fun getRecord(id: Long): Record? = recordDao.findById(id)

    suspend fun getRecordDates(): List<Long> = recordDao.getAllRecordDates()

    /**
     * 新建笔记（无媒体关联，纯文字）
     */
    suspend fun createRecord(
        recordDate: Long,
        title: String,
        contentMarkdown: String,
        relatedFolderIds: List<Long>,
        primaryFolderId: Long?,
        contentReferences: List<ContentReference>
    ): Long {
        val now = System.currentTimeMillis()
        val record = Record(
            recordDate = recordDate,
            title = title,
            contentMarkdown = contentMarkdown,
            contentReferenceIds = contentReferences,
            relatedFolderIds = relatedFolderIds,
            primaryFolderId = primaryFolderId,
            createTime = now,
            updateTime = now
        )
        val recordId = recordDao.insert(record)

        // 为媒体引用创建 FROM_RECORD_INDEX 索引条目
        createIndexItems(recordId, contentReferences, primaryFolderId)

        return recordId
    }

    /**
     * 更新笔记：先清理旧索引条目，再生成新索引
     */
    suspend fun updateRecord(
        id: Long,
        recordDate: Long,
        title: String,
        contentMarkdown: String,
        relatedFolderIds: List<Long>,
        primaryFolderId: Long?,
        contentReferences: List<ContentReference>
    ) {
        val oldRecord = recordDao.findById(id) ?: return
        val now = System.currentTimeMillis()

        // 清理旧索引条目（FROM_RECORD_INDEX）
        mediaItemDao.deleteIndexItemsByRecord(id)
        spaceLinkItemDao.deleteIndexItemsByRecord(id)
        spaceFileItemDao.deleteIndexItemsByRecord(id)

        // 更新笔记
        val updated = oldRecord.copy(
            recordDate = recordDate,
            title = title,
            contentMarkdown = contentMarkdown,
            contentReferenceIds = contentReferences,
            relatedFolderIds = relatedFolderIds,
            primaryFolderId = primaryFolderId,
            updateTime = now
        )
        recordDao.update(updated)

        // 创建新索引条目
        createIndexItems(id, contentReferences, primaryFolderId)
    }

    /**
     * 删除笔记：同步删除其 FROM_RECORD_INDEX 索引条目
     * 引用计数为0才删磁盘文件
     */
    suspend fun deleteRecord(
        id: Long,
        context: Context? = null,
        recordDate: Long = System.currentTimeMillis(),
        onDeleteFile: suspend (String) -> Unit = {}
    ) {
        val oldRecord = recordDao.findById(id)

        // 删除 MD 文件
        if (context != null && oldRecord != null) {
            FileHelper.deleteRecordMarkdown(
                context = context,
                recordId = id,
                title = oldRecord.title,
                recordDate = oldRecord.recordDate
            )
        }

        // 收集旧索引条目的文件路径
        val oldMediaItems = mediaItemDao.getIndexItemsByRecord(id)
        val oldFilePaths = oldMediaItems.map { it.fileRelativePath }.distinct()

        // 收集旧空间链接
        val oldSpaceLinks = spaceLinkItemDao.getIndexItemsByRecord(id)

        // 删除笔记（CASCADE 删除索引条目）
        recordDao.deleteById(id)

        // 引用计数检查
        for (path in oldFilePaths) {
            val count = mediaItemDao.countByFilePath(path)
            if (count == 0) {
                onDeleteFile(path)
            }
        }
    }

    /**
     * 为笔记的内容引用创建索引条目
     * 关键修复：避免为同一文件+笔记创建重复条目
     * - 如果已有该笔记对此文件的 FROM_RECORD_INDEX 条目，跳过（避免重复）
     * - 否则创建新的 FROM_RECORD_INDEX 条目
     * 注意：DIRECT_ADD 条目保持独立，不做提升，避免笔记删除时丢失物理数据
     */
    private suspend fun createIndexItems(
        recordId: Long,
        references: List<ContentReference>,
        primaryFolderId: Long?
    ) {
        for (ref in references) {
            when (ref.refType) {
                RefType.IMAGE, RefType.VIDEO, RefType.VOICE -> {
                    val existing = mediaItemDao.findById(ref.targetId)
                    if (existing != null) {
                        val effectiveFolderId = primaryFolderId ?: existing.folderId
                        val mediaType = existing.mediaType

                        // 检查该笔记对该文件是否已有 FROM_RECORD_INDEX 条目
                        val existingIndex = mediaItemDao.getIndexItemByPathAndRecord(
                            effectiveFolderId, mediaType, existing.fileRelativePath, recordId
                        )
                        if (existingIndex != null) {
                            // 已有索引条目，跳过（避免重复）
                            continue
                        }

                        // 创建新的 FROM_RECORD_INDEX 条目
                        val newItem = existing.copy(
                            id = 0,
                            sourceType = SourceType.FROM_RECORD_INDEX,
                            bindRecordId = recordId,
                            folderId = effectiveFolderId,
                            createTime = System.currentTimeMillis()
                        )
                        mediaItemDao.insert(newItem)
                    }
                }
                RefType.SPACE_LINK -> {
                    val existing = spaceLinkItemDao.findById(ref.targetId)
                    if (existing != null) {
                        val effectiveFolderId = primaryFolderId ?: existing.folderId

                        // 检查该笔记对该 URL 是否已有 FROM_RECORD_INDEX 条目
                        val existingIndex = spaceLinkItemDao.getIndexItemByUrlAndRecord(
                            effectiveFolderId, existing.webUrl, recordId
                        )
                        if (existingIndex != null) {
                            // 已有索引条目，跳过（避免重复）
                            continue
                        }

                        // 创建新的 FROM_RECORD_INDEX 条目
                        val newItem = existing.copy(
                            id = 0,
                            sourceType = SourceType.FROM_RECORD_INDEX,
                            bindRecordId = recordId,
                            folderId = effectiveFolderId,
                            createTime = System.currentTimeMillis()
                        )
                        spaceLinkItemDao.insert(newItem)
                    }
                }
                RefType.SPACE_FILE -> {
                    val existing = spaceFileItemDao.findById(ref.targetId)
                    if (existing != null) {
                        val effectiveFolderId = primaryFolderId ?: existing.folderId

                        val existingIndex = spaceFileItemDao.getIndexItemByPathAndRecord(
                            effectiveFolderId, existing.filePath, recordId
                        )
                        if (existingIndex != null) {
                            continue
                        }

                        val newItem = existing.copy(
                            id = 0,
                            sourceType = SourceType.FROM_RECORD_INDEX,
                            bindRecordId = recordId,
                            folderId = effectiveFolderId,
                            createTime = System.currentTimeMillis()
                        )
                        spaceFileItemDao.insert(newItem)
                    }
                }
            }
        }
    }

    /**
     * 更新笔记的文件夹关联
     */
    suspend fun updateFolders(
        recordId: Long,
        relatedFolderIds: List<Long>,
        primaryFolderId: Long?
    ) {
        val record = recordDao.findById(recordId) ?: return
        recordDao.updateRelatedFolderIds(recordId, relatedFolderIds)
        recordDao.updatePrimaryFolderId(recordId, primaryFolderId)

        // 同步更新该笔记的索引条目 folderId
        val indexMediaItems = mediaItemDao.getIndexItemsByRecord(recordId)
        for (item in indexMediaItems) {
            val newFolderId = primaryFolderId ?: item.folderId
            if (newFolderId != item.folderId) {
                mediaItemDao.update(item.copy(folderId = newFolderId))
            }
        }

        val indexSpaces = spaceLinkItemDao.getIndexItemsByRecord(recordId)
        for (item in indexSpaces) {
            val newFolderId = primaryFolderId ?: item.folderId
            if (newFolderId != item.folderId) {
                spaceLinkItemDao.update(item.copy(folderId = newFolderId))
            }
        }

        val indexSpaceFiles = spaceFileItemDao.getIndexItemsByRecord(recordId)
        for (item in indexSpaceFiles) {
            val newFolderId = primaryFolderId ?: item.folderId
            if (newFolderId != item.folderId) {
                spaceFileItemDao.update(item.copy(folderId = newFolderId))
            }
        }
    }

    /**
     * 搜索笔记（按标题或内容）
     */
    fun search(query: String): Flow<List<Record>> {
        return recordDao.searchByTitleOrContent(query)
    }

    /**
     * 强删场景：从笔记中移除指定引用并同步 MD 文件
     * 内部走 updateRecord 路径，会清理旧索引条目并重建
     */
    suspend fun removeReferenceFromRecord(
        recordId: Long,
        refType: RefType,
        targetId: Long,
        context: Context
    ) {
        val record = recordDao.findById(recordId) ?: return
        val newRefs = record.contentReferenceIds.filterNot {
            it.refType == refType && it.targetId == targetId
        }
        val newMd = RichEditorHelper.removeReference(record.contentMarkdown, refType, targetId)
        if (newRefs == record.contentReferenceIds && newMd == record.contentMarkdown) return

        updateRecord(
            id = recordId,
            recordDate = record.recordDate,
            title = record.title,
            contentMarkdown = newMd,
            relatedFolderIds = record.relatedFolderIds,
            primaryFolderId = record.primaryFolderId,
            contentReferences = newRefs
        )

        FileHelper.saveRecordMarkdown(
            context = context,
            recordId = recordId,
            title = record.title,
            contentMarkdown = newMd,
            recordDate = record.recordDate
        )
    }
}

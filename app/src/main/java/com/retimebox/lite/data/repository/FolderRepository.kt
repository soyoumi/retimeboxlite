package com.retimebox.lite.data.repository

import com.retimebox.lite.data.local.dao.FolderDao
import com.retimebox.lite.data.local.dao.MediaItemDao
import com.retimebox.lite.data.local.dao.RecordDao
import com.retimebox.lite.data.local.dao.SpaceLinkItemDao
import com.retimebox.lite.data.local.entity.Folder
import com.retimebox.lite.data.local.entity.MediaType
import com.retimebox.lite.data.local.entity.SourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FolderRepository(
    private val folderDao: FolderDao,
    private val mediaItemDao: MediaItemDao,
    private val spaceLinkItemDao: SpaceLinkItemDao,
    private val recordDao: RecordDao
) {

    private val mutex = Mutex()

    fun observeRootFolders(): Flow<List<Folder>> = folderDao.observeRootFolders()

    fun observeChildrenOf(parentId: Long): Flow<List<Folder>> = folderDao.observeChildrenOf(parentId)

    fun observeAll(): Flow<List<Folder>> = folderDao.observeAll()

    fun observeRecordsByFolder(folderId: Long): Flow<List<com.retimebox.lite.data.local.entity.Record>> {
        // 注意：relatedFolderIds 是 JSON 存储，LIKE 可能产生子串误匹配，此处加载全量过滤
        return recordDao.observeAll().map { records ->
            records.filter { it.relatedFolderIds.contains(folderId) }
        }
    }

    suspend fun getFolder(id: Long): Folder? = folderDao.findById(id)

    suspend fun getFolderColorHex(id: Long): String {
        val folder = folderDao.findById(id)
        return folder?.colorHex ?: "#6750A4"
    }

    suspend fun createFolder(name: String, parentId: Long?, colorHex: String): Long {
        val now = System.currentTimeMillis()
        val folder = Folder(
            folderName = name,
            parentFolderId = parentId,
            colorHex = colorHex,
            createTime = now,
            updateTime = now
        )
        return folderDao.insert(folder)
    }

    suspend fun renameFolder(id: Long, newName: String, newColorHex: String? = null) {
        val folder = folderDao.findById(id) ?: return
        val updated = folder.copy(
            folderName = newName,
            colorHex = newColorHex ?: folder.colorHex,
            updateTime = System.currentTimeMillis()
        )
        folderDao.update(updated)
    }

    suspend fun moveFolder(folderId: Long, newParentId: Long?): Boolean {
        if (hasCycle(folderId, newParentId)) return false
        val folder = folderDao.findById(folderId) ?: return false
        val updated = folder.copy(
            parentFolderId = newParentId,
            updateTime = System.currentTimeMillis()
        )
        folderDao.update(updated)
        return true
    }

    /**
     * 循环引用检测：检查 newParentId 是否是 folderId 的子孙节点
     * 防止子文件夹指向自身或其任何祖先
     */
    private suspend fun hasCycle(folderId: Long, newParentId: Long?): Boolean {
        if (newParentId == null) return false
        if (newParentId == folderId) return true
        var current = folderDao.findById(newParentId) ?: return false
        while (true) {
            if (current.id == folderId) return true
            val parentId = current.parentFolderId ?: return false
            current = folderDao.findById(parentId) ?: return false
        }
    }

    /**
     * 递归删除文件夹：
     * 1. 收集目标文件夹及所有子孙文件夹ID
     * 2. 对每个文件夹：
     *    - 将 FROM_RECORD_INDEX 索引条目 folderId 迁移到 0（跟随笔记生命周期，不随文件夹删除）
     *    - 删除 DIRECT_ADD 媒体/空间条目（按引用计数清磁盘）
     *    - 从笔记 relatedFolderIds 移除该 folderId、笔记 primaryFolderId 置空
     * 3. 最后删除文件夹实体
     */
    suspend fun deleteFolder(folderId: Long, onDeleteFile: suspend (String) -> Unit = {}) {
        mutex.withLock {
            val folderIds = collectDescendantIds(folderId) + folderId

            for (fid in folderIds) {
                // 迁移 FROM_RECORD_INDEX 索引条目 folderId → 0，防止级联删除
                mediaItemDao.reindexIndexItemsFolder(fid, 0L)
                spaceLinkItemDao.reindexIndexItemsFolder(fid, 0L)

                // 删除 DIRECT_ADD 媒体条目
                val directAddImages = mediaItemDao.getDirectAddByFolderAndType(fid, MediaType.IMAGE)
                val directAddVideos = mediaItemDao.getDirectAddByFolderAndType(fid, MediaType.VIDEO)
                val allDirectAddMedia = directAddImages + directAddVideos
                val filePaths = allDirectAddMedia.map { it.fileRelativePath }.distinct()

                for (item in allDirectAddMedia) {
                    mediaItemDao.deleteById(item.id)
                }

                // 删除 DIRECT_ADD 空间链接条目
                val directAddSpaces = spaceLinkItemDao.getDirectAddByFolder(fid)
                for (item in directAddSpaces) {
                    spaceLinkItemDao.deleteById(item.id)
                }

                // 引用计数检查：计数为0才删磁盘文件
                for (path in filePaths) {
                    val count = mediaItemDao.countByFilePath(path)
                    if (count == 0) {
                        onDeleteFile(path)
                    }
                }

                // 从笔记 relatedFolderIds 移除该 folderId
                removeFolderFromRelatedFolderIds(fid)

                // 清理笔记 primaryFolderId
                recordDao.clearPrimaryFolderId(fid)
            }

            // 删除文件夹实体
            folderDao.deleteById(folderId)
        }
    }

    private suspend fun collectDescendantIds(parentId: Long): List<Long> {
        val result = mutableListOf<Long>()
        val children = folderDao.getChildIdsOf(parentId)
        result.addAll(children)
        for (childId in children) {
            result.addAll(collectDescendantIds(childId))
        }
        return result
    }

    private suspend fun removeFolderFromRelatedFolderIds(folderId: Long) {
        val allRecords = recordDao.getAllRecords()
        for (record in allRecords) {
            if (record.relatedFolderIds.contains(folderId)) {
                val newIds = record.relatedFolderIds.filter { it != folderId }
                recordDao.updateRelatedFolderIds(record.id, newIds)
            }
        }
    }

    /**
     * 获取文件夹的层级路径（从根到当前）
     * 例如："工作/项目A/子任务"
     */
    suspend fun getFolderPath(folderId: Long?): String {
        if (folderId == null) return ""
        val segments = mutableListOf<String>()
        var currentId: Long? = folderId
        while (currentId != null) {
            val folder = folderDao.findById(currentId) ?: break
            segments.add(0, folder.folderName)
            currentId = folder.parentFolderId
        }
        return segments.joinToString("/")
    }
}

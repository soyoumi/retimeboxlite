package com.retimebox.lite.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.retimebox.lite.data.local.entity.MediaItem
import com.retimebox.lite.data.local.entity.MediaType
import com.retimebox.lite.data.local.entity.SourceType
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MediaItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaItem>)

    @Update
    suspend fun update(item: MediaItem)

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun findById(id: Long): MediaItem?

    @Query("SELECT * FROM media_items WHERE folderId = :folderId AND mediaType = :mediaType ORDER BY createTime DESC")
    fun observeByFolderAndType(folderId: Long, mediaType: MediaType): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE mediaType = :mediaType ORDER BY createTime DESC")
    fun observeByType(mediaType: MediaType): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE bindRecordId = :recordId AND mediaType = :mediaType")
    suspend fun getByRecordAndType(recordId: Long, mediaType: MediaType): List<MediaItem>

    @Query("SELECT * FROM media_items WHERE bindRecordId = :recordId")
    suspend fun getByRecord(recordId: Long): List<MediaItem>

    @Query("SELECT * FROM media_items WHERE bindRecordId = :recordId AND sourceType = 'FROM_RECORD_INDEX'")
    suspend fun getIndexItemsByRecord(recordId: Long): List<MediaItem>

    @Query("SELECT * FROM media_items WHERE sourceType = 'FROM_RECORD_INDEX' AND mediaType = :mediaType AND folderId = :folderId ORDER BY createTime DESC")
    suspend fun getIndexItemsByFolderAndType(folderId: Long, mediaType: MediaType): List<MediaItem>

    @Query("SELECT * FROM media_items WHERE sourceType = 'DIRECT_ADD' AND mediaType = :mediaType AND folderId = :folderId ORDER BY createTime DESC")
    fun observeDirectAddByFolderAndType(folderId: Long, mediaType: MediaType): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE folderId = :folderId AND mediaType = :mediaType AND fileRelativePath = :fileRelativePath AND sourceType = 'FROM_RECORD_INDEX' AND bindRecordId = :bindRecordId LIMIT 1")
    suspend fun getIndexItemByPathAndRecord(folderId: Long, mediaType: MediaType, fileRelativePath: String, bindRecordId: Long): MediaItem?

    @Query("SELECT * FROM media_items WHERE id IN (:ids) AND sourceType != 'FROM_RECORD_INDEX'")
    suspend fun getDirectAddByIds(ids: List<Long>): List<MediaItem>

    @Query("SELECT * FROM media_items WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<MediaItem>

    @Query("SELECT * FROM media_items WHERE fileRelativePath = :filePath AND mediaType = :mediaType AND sourceType = 'DIRECT_ADD' LIMIT 1")
    suspend fun findDirectAddByPath(filePath: String, mediaType: MediaType): MediaItem?

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM media_items WHERE id IN (:ids) AND sourceType != 'FROM_RECORD_INDEX'")
    suspend fun deleteDirectAddByIds(ids: List<Long>)

    @Query("DELETE FROM media_items WHERE bindRecordId = :recordId AND sourceType = 'FROM_RECORD_INDEX'")
    suspend fun deleteIndexItemsByRecord(recordId: Long)

    @Query("DELETE FROM media_items WHERE folderId = :folderId AND sourceType = 'DIRECT_ADD'")
    suspend fun deleteDirectAddByFolder(folderId: Long)

    @Query("UPDATE media_items SET folderId = :targetFolderId WHERE id IN (:ids) AND sourceType != 'FROM_RECORD_INDEX'")
    suspend fun moveFolder(ids: List<Long>, targetFolderId: Long)

    @Query("SELECT COUNT(*) FROM media_items WHERE fileRelativePath = :path")
    suspend fun countByFilePath(path: String): Int

    @Query("SELECT DISTINCT fileRelativePath FROM media_items WHERE id IN (:ids)")
    suspend fun getDistinctFilePaths(ids: List<Long>): List<String>

    @Query("SELECT * FROM media_items WHERE mediaType = :mediaType AND sourceType = 'FROM_RECORD_INDEX' AND folderId = :folderId ORDER BY createTime DESC")
    fun observeIndexItemsByFolderAndType(folderId: Long, mediaType: MediaType): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE mediaType = :mediaType AND sourceType != 'FROM_RECORD_INDEX' ORDER BY createTime DESC")
    fun observeDirectAddByType(mediaType: MediaType): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE mediaType = :mediaType AND sourceType = 'FROM_RECORD_INDEX' ORDER BY createTime DESC")
    fun observeIndexByType(mediaType: MediaType): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE folderId = :folderId AND mediaType = :mediaType AND sourceType = 'DIRECT_ADD'")
    suspend fun getDirectAddByFolderAndType(folderId: Long, mediaType: MediaType): List<MediaItem>

    @Query("UPDATE media_items SET folderId = :safeFolderId WHERE folderId = :folderId AND sourceType = 'FROM_RECORD_INDEX'")
    suspend fun reindexIndexItemsFolder(folderId: Long, safeFolderId: Long)
}

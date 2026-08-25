package com.retimebox.lite.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.retimebox.lite.data.local.entity.SourceType
import com.retimebox.lite.data.local.entity.SpaceLinkItem
import com.retimebox.lite.data.local.entity.SpaceType
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceLinkItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SpaceLinkItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SpaceLinkItem>)

    @Update
    suspend fun update(item: SpaceLinkItem)

    @Query("SELECT * FROM space_link_items WHERE id = :id")
    suspend fun findById(id: Long): SpaceLinkItem?

    @Query("SELECT * FROM space_link_items WHERE folderId = :folderId ORDER BY createTime DESC")
    fun observeByFolder(folderId: Long): Flow<List<SpaceLinkItem>>

    @Query("SELECT * FROM space_link_items ORDER BY createTime DESC")
    fun observeAll(): Flow<List<SpaceLinkItem>>

    @Query("SELECT * FROM space_link_items WHERE bindRecordId = :recordId")
    suspend fun getByRecord(recordId: Long): List<SpaceLinkItem>

    @Query("SELECT * FROM space_link_items WHERE bindRecordId = :recordId AND sourceType = 'FROM_RECORD_INDEX'")
    suspend fun getIndexItemsByRecord(recordId: Long): List<SpaceLinkItem>

    @Query("SELECT * FROM space_link_items WHERE sourceType = 'DIRECT_ADD' AND folderId = :folderId ORDER BY createTime DESC")
    fun observeDirectAddByFolder(folderId: Long): Flow<List<SpaceLinkItem>>

    @Query("SELECT * FROM space_link_items WHERE sourceType = 'FROM_RECORD_INDEX' AND folderId = :folderId ORDER BY createTime DESC")
    fun observeIndexByFolder(folderId: Long): Flow<List<SpaceLinkItem>>

    @Query("SELECT * FROM space_link_items WHERE folderId = :folderId AND webUrl = :webUrl AND sourceType = 'FROM_RECORD_INDEX' AND bindRecordId = :bindRecordId LIMIT 1")
    suspend fun getIndexItemByUrlAndRecord(folderId: Long, webUrl: String, bindRecordId: Long): SpaceLinkItem?

    @Query("SELECT * FROM space_link_items WHERE id IN (:ids) AND sourceType != 'FROM_RECORD_INDEX'")
    suspend fun getDirectAddByIds(ids: List<Long>): List<SpaceLinkItem>

    @Query("DELETE FROM space_link_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM space_link_items WHERE id IN (:ids) AND sourceType != 'FROM_RECORD_INDEX'")
    suspend fun deleteDirectAddByIds(ids: List<Long>)

    @Query("DELETE FROM space_link_items WHERE bindRecordId = :recordId AND sourceType = 'FROM_RECORD_INDEX'")
    suspend fun deleteIndexItemsByRecord(recordId: Long)

    @Query("DELETE FROM space_link_items WHERE folderId = :folderId AND sourceType = 'DIRECT_ADD'")
    suspend fun deleteDirectAddByFolder(folderId: Long)

    @Query("UPDATE space_link_items SET folderId = :targetFolderId WHERE id IN (:ids) AND sourceType != 'FROM_RECORD_INDEX'")
    suspend fun moveFolder(ids: List<Long>, targetFolderId: Long)

    @Query("SELECT * FROM space_link_items WHERE folderId = :folderId AND sourceType = 'DIRECT_ADD'")
    suspend fun getDirectAddByFolder(folderId: Long): List<SpaceLinkItem>

    @Query("UPDATE space_link_items SET folderId = :safeFolderId WHERE folderId = :folderId AND sourceType = 'FROM_RECORD_INDEX'")
    suspend fun reindexIndexItemsFolder(folderId: Long, safeFolderId: Long)
}

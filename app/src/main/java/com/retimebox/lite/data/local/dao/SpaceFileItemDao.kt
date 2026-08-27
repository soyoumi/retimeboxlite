package com.retimebox.lite.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.retimebox.lite.data.local.entity.SpaceFileItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceFileItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SpaceFileItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SpaceFileItem>)

    @Update
    suspend fun update(item: SpaceFileItem)

    @Query("SELECT * FROM space_file_items WHERE id = :id")
    suspend fun findById(id: Long): SpaceFileItem?

    @Query("SELECT * FROM space_file_items WHERE folderId = :folderId ORDER BY createTime DESC")
    fun observeByFolder(folderId: Long): Flow<List<SpaceFileItem>>

    @Query("SELECT * FROM space_file_items ORDER BY createTime DESC")
    fun observeAll(): Flow<List<SpaceFileItem>>

    @Query("SELECT * FROM space_file_items WHERE bindRecordId = :recordId")
    suspend fun getByRecord(recordId: Long): List<SpaceFileItem>

    @Query("SELECT * FROM space_file_items WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<SpaceFileItem>

    @Query("SELECT * FROM space_file_items WHERE filePath = :filePath AND sourceType = 'DIRECT_ADD' LIMIT 1")
    suspend fun findDirectAddByPath(filePath: String): SpaceFileItem?

    @Query("SELECT * FROM space_file_items WHERE folderId = :folderId AND filePath = :filePath AND bindRecordId = :recordId AND sourceType = 'FROM_RECORD_INDEX' LIMIT 1")
    suspend fun getIndexItemByPathAndRecord(folderId: Long, filePath: String, recordId: Long): SpaceFileItem?

    @Query("SELECT * FROM space_file_items WHERE bindRecordId = :recordId AND sourceType = 'FROM_RECORD_INDEX'")
    suspend fun getIndexItemsByRecord(recordId: Long): List<SpaceFileItem>

    @Query("DELETE FROM space_file_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM space_file_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM space_file_items WHERE bindRecordId = :recordId AND sourceType = 'FROM_RECORD_INDEX'")
    suspend fun deleteIndexItemsByRecord(recordId: Long)

    @Query("DELETE FROM space_file_items WHERE bindRecordId = :recordId")
    suspend fun deleteByRecord(recordId: Long)

    @Query("UPDATE space_file_items SET folderId = :targetFolderId WHERE id IN (:ids)")
    suspend fun moveFolder(ids: List<Long>, targetFolderId: Long)

    @Query("UPDATE space_file_items SET spaceType = :spaceType, name = :name, thumbnailUrl = :thumbnailUrl WHERE filePath = :filePath")
    suspend fun updateByFilePath(filePath: String, spaceType: String, name: String, thumbnailUrl: String?)
}

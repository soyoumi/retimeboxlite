package com.retimebox.lite.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.retimebox.lite.data.local.entity.Record
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: Record): Long

    @Update
    suspend fun update(record: Record)

    @Query("SELECT * FROM records WHERE id = :id")
    suspend fun findById(id: Long): Record?

    @Query("SELECT * FROM records WHERE id = :id")
    fun observeById(id: Long): Flow<Record?>

    @Query("SELECT * FROM records ORDER BY recordDate DESC, createTime DESC")
    fun observeAll(): Flow<List<Record>>

    @Query("SELECT * FROM records WHERE recordDate BETWEEN :start AND :end ORDER BY createTime DESC")
    fun observeByDateRange(start: Long, end: Long): Flow<List<Record>>

    @Query("SELECT * FROM records WHERE recordDate BETWEEN :start AND :end ORDER BY createTime DESC")
    suspend fun getByDateRange(start: Long, end: Long): List<Record>

    @Query("SELECT * FROM records WHERE primaryFolderId = :folderId ORDER BY recordDate DESC, createTime DESC")
    fun observeByPrimaryFolder(folderId: Long): Flow<List<Record>>

    @Query("UPDATE records SET relatedFolderIds = :newIds, updateTime = :updateTime WHERE id = :id")
    suspend fun updateRelatedFolderIds(id: Long, newIds: List<Long>, updateTime: Long = System.currentTimeMillis())

    @Query("UPDATE records SET primaryFolderId = :primaryFolderId, updateTime = :updateTime WHERE id = :id")
    suspend fun updatePrimaryFolderId(id: Long, primaryFolderId: Long?, updateTime: Long = System.currentTimeMillis())

    @Query("UPDATE records SET primaryFolderId = NULL, updateTime = :updateTime WHERE primaryFolderId = :folderId")
    suspend fun clearPrimaryFolderId(folderId: Long, updateTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT recordDate FROM records ORDER BY recordDate DESC")
    suspend fun getAllRecordDates(): List<Long>

    @Query("SELECT * FROM records")
    suspend fun getAllRecords(): List<Record>

    @Query("SELECT * FROM records WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<Record>

    @Query("SELECT * FROM records WHERE title LIKE '%' || :query || '%' OR contentMarkdown LIKE '%' || :query || '%' ORDER BY recordDate DESC, createTime DESC")
    fun searchByTitleOrContent(query: String): Flow<List<Record>>

    @Query("SELECT * FROM records WHERE title LIKE '%' || :query || '%' OR contentMarkdown LIKE '%' || :query || '%' ORDER BY recordDate DESC, createTime DESC")
    suspend fun searchByTitleOrContentSync(query: String): List<Record>
}

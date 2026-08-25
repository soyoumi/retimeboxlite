package com.retimebox.lite.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.retimebox.lite.data.local.entity.Folder
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: Folder): Long

    @Update
    suspend fun update(folder: Folder)

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun findById(id: Long): Folder?

    @Query("SELECT * FROM folders WHERE parentFolderId IS NULL ORDER BY folderName ASC")
    fun observeRootFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE parentFolderId = :parentId ORDER BY folderName ASC")
    fun observeChildrenOf(parentId: Long): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE parentFolderId = :parentId ORDER BY folderName ASC")
    suspend fun getChildrenOf(parentId: Long): List<Folder>

    @Query("SELECT * FROM folders WHERE id = :parentId")
    suspend fun getParent(parentId: Long): Folder?

    @Query("SELECT * FROM folders ORDER BY folderName ASC")
    suspend fun getAll(): List<Folder>

    @Query("SELECT * FROM folders ORDER BY folderName ASC")
    fun observeAll(): Flow<List<Folder>>

    @Query("SELECT id FROM folders WHERE parentFolderId = :parentId")
    suspend fun getChildIdsOf(parentId: Long): List<Long>

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM folders WHERE parentFolderId = :parentId")
    suspend fun deleteChildrenOf(parentId: Long)

    @Query("SELECT COUNT(*) FROM folders WHERE parentFolderId = :parentId")
    suspend fun countChildrenOf(parentId: Long): Int
}

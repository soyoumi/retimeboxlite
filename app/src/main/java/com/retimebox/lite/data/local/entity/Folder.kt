package com.retimebox.lite.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["parentFolderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["parentFolderId"]),
        Index(value = ["parentFolderId", "folderName"], unique = true)
    ]
)
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val parentFolderId: Long? = null,
    val folderName: String,
    val colorHex: String = "#6750A4",
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis()
)

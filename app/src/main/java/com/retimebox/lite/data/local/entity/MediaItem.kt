package com.retimebox.lite.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_items",
    foreignKeys = [
        ForeignKey(
            entity = Record::class,
            parentColumns = ["id"],
            childColumns = ["bindRecordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["bindRecordId"]),
        Index(value = ["sourceType"]),
        Index(value = ["fileRelativePath"]),
        Index(value = ["mediaType"]),
        Index(value = ["folderId", "mediaType"])
    ]
)
data class MediaItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaType: MediaType,
    val fileRelativePath: String,
    val sourceType: SourceType,
    val bindRecordId: Long? = null,
    val folderId: Long,
    val createTime: Long = System.currentTimeMillis()
)

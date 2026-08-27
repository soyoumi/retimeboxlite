package com.retimebox.lite.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "space_file_items",
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
        Index(value = ["folderId", "spaceType"])
    ]
)
data class SpaceFileItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val spaceType: SpaceType,
    val filePath: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val sourceType: SourceType = SourceType.DIRECT_ADD,
    val bindRecordId: Long? = null,
    val folderId: Long,
    val createTime: Long = System.currentTimeMillis()
)

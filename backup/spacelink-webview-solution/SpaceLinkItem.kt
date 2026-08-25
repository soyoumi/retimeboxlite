package com.retimebox.lite.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "space_link_items",
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
data class SpaceLinkItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val spaceType: SpaceType,
    val webUrl: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val sourceType: SourceType,
    val bindRecordId: Long? = null,
    val folderId: Long,
    val createTime: Long = System.currentTimeMillis()
)

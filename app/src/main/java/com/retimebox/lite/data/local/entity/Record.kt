package com.retimebox.lite.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

data class ContentReference(
    val refType: RefType,
    val targetId: Long
)

@Entity(tableName = "records")
data class Record(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordDate: Long,
    val title: String,
    val contentMarkdown: String = "",
    val contentReferenceIds: List<ContentReference> = emptyList(),
    val relatedFolderIds: List<Long> = emptyList(),
    val primaryFolderId: Long? = null,
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis()
)

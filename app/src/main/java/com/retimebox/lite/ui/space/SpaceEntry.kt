package com.retimebox.lite.ui.space

import com.retimebox.lite.data.local.entity.SourceType
import com.retimebox.lite.data.local.entity.SpaceFileItem
import com.retimebox.lite.data.local.entity.SpaceLinkItem
import com.retimebox.lite.data.local.entity.SpaceType

enum class SpaceEntryType { LINK, FILE }

data class SpaceEntry(
    val id: Long,
    val spaceType: SpaceType,
    val name: String,
    val thumbnailUrl: String?,
    val sourceType: SourceType,
    val bindRecordId: Long?,
    val folderId: Long?,
    val createTime: Long,
    val itemType: SpaceEntryType,
    val webUrl: String? = null,
    val filePath: String? = null
) {
    companion object {
        fun fromLink(item: SpaceLinkItem): SpaceEntry = SpaceEntry(
            id = item.id,
            spaceType = item.spaceType,
            name = item.name,
            thumbnailUrl = item.thumbnailUrl,
            sourceType = item.sourceType,
            bindRecordId = item.bindRecordId,
            folderId = item.folderId,
            createTime = item.createTime,
            itemType = SpaceEntryType.LINK,
            webUrl = item.webUrl
        )

        fun fromFile(item: SpaceFileItem): SpaceEntry = SpaceEntry(
            id = item.id,
            spaceType = item.spaceType,
            name = item.name,
            thumbnailUrl = item.thumbnailUrl,
            sourceType = item.sourceType,
            bindRecordId = item.bindRecordId,
            folderId = item.folderId,
            createTime = item.createTime,
            itemType = SpaceEntryType.FILE,
            filePath = item.filePath
        )
    }
}

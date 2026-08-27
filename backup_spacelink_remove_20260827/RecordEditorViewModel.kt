package com.retimebox.lite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.retimebox.lite.RetimeboxApplication
import com.retimebox.lite.R
import com.retimebox.lite.data.local.entity.ContentReference
import com.retimebox.lite.data.local.entity.Folder
import com.retimebox.lite.data.local.entity.MediaItem
import com.retimebox.lite.data.local.entity.MediaType
import com.retimebox.lite.data.local.entity.Record
import com.retimebox.lite.data.local.entity.RefType
import com.retimebox.lite.data.local.entity.SpaceFileItem
import com.retimebox.lite.data.local.entity.SpaceLinkItem
import com.retimebox.lite.data.local.entity.SpaceType
import com.retimebox.lite.data.local.entity.SourceType
import com.retimebox.lite.data.repository.FolderRepository
import com.retimebox.lite.data.repository.MediaRepository
import com.retimebox.lite.data.repository.RecordRepository
import com.retimebox.lite.data.repository.SpaceFileRepository
import com.retimebox.lite.data.repository.SpaceLinkRepository
import com.retimebox.lite.util.FileHelper
import com.retimebox.lite.util.RichEditorHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecordEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RetimeboxApplication
    private val recordRepository: RecordRepository = app.recordRepository
    private val folderRepository: FolderRepository = app.folderRepository
    private val mediaRepository: MediaRepository = app.mediaRepository
    private val spaceLinkRepository: SpaceLinkRepository = app.spaceLinkRepository
    private val spaceFileRepository: SpaceFileRepository = app.spaceFileRepository

    private val _editingRecordId = MutableStateFlow<Long?>(null)
    val editingRecordId: StateFlow<Long?> = _editingRecordId.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _contentMarkdown = MutableStateFlow("")
    val contentMarkdown: StateFlow<String> = _contentMarkdown.asStateFlow()

    private val _recordDate = MutableStateFlow(System.currentTimeMillis())
    val recordDate: StateFlow<Long> = _recordDate.asStateFlow()

    private val _relatedFolderIds = MutableStateFlow<List<Long>>(emptyList())
    val relatedFolderIds: StateFlow<List<Long>> = _relatedFolderIds.asStateFlow()

    private val _primaryFolderId = MutableStateFlow<Long?>(null)
    val primaryFolderId: StateFlow<Long?> = _primaryFolderId.asStateFlow()

    private val _contentReferences = MutableStateFlow<List<ContentReference>>(emptyList())
    val contentReferences: StateFlow<List<ContentReference>> = _contentReferences.asStateFlow()

    private val _referencedMedia = MutableStateFlow<List<MediaItem>>(emptyList())
    val referencedMedia: StateFlow<List<MediaItem>> = _referencedMedia.asStateFlow()

    private val _referencedSpaceLinks = MutableStateFlow<List<SpaceLinkItem>>(emptyList())
    val referencedSpaceLinks: StateFlow<List<SpaceLinkItem>> = _referencedSpaceLinks.asStateFlow()

    private val _referencedSpaceFiles = MutableStateFlow<List<SpaceFileItem>>(emptyList())
    val referencedSpaceFiles: StateFlow<List<SpaceFileItem>> = _referencedSpaceFiles.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setError(message: String?) {
        _error.value = message
    }

    val allFolders: StateFlow<List<Folder>> = folderRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadRecord(id: Long) {
        _editingRecordId.value = id
        viewModelScope.launch {
            val record = recordRepository.getRecord(id)
            if (record != null) {
                _title.value = record.title
                _contentMarkdown.value = record.contentMarkdown
                _recordDate.value = record.recordDate
                _relatedFolderIds.value = record.relatedFolderIds
                _primaryFolderId.value = record.primaryFolderId
                _contentReferences.value = record.contentReferenceIds

                val mediaRefs = record.contentReferenceIds.filter {
                    it.refType == RefType.IMAGE || it.refType == RefType.VIDEO || it.refType == RefType.VOICE
                }
                val spaceRefs = record.contentReferenceIds.filter {
                    it.refType == RefType.SPACE_LINK
                }
                val spaceFileRefs = record.contentReferenceIds.filter {
                    it.refType == RefType.SPACE_FILE
                }

                val mediaItems = mutableListOf<MediaItem>()
                for (ref in mediaRefs) {
                    mediaRepository.findById(ref.targetId)?.let { mediaItems.add(it) }
                }
                _referencedMedia.value = mediaItems

                val spaceItems = mutableListOf<SpaceLinkItem>()
                for (ref in spaceRefs) {
                    spaceLinkRepository.findById(ref.targetId)?.let { spaceItems.add(it) }
                }
                _referencedSpaceLinks.value = spaceItems

                val spaceFileItems = mutableListOf<SpaceFileItem>()
                for (ref in spaceFileRefs) {
                    spaceFileRepository.findById(ref.targetId)?.let { spaceFileItems.add(it) }
                }
                _referencedSpaceFiles.value = spaceFileItems
            }
        }
    }

    fun initNewRecord(defaultFolderId: Long? = null) {
        _editingRecordId.value = null
        _title.value = ""
        _contentMarkdown.value = ""
        _recordDate.value = System.currentTimeMillis()
        defaultFolderId?.let {
            _relatedFolderIds.value = listOf(it)
            _primaryFolderId.value = it
        }
        _contentReferences.value = emptyList()
        _referencedMedia.value = emptyList()
        _referencedSpaceLinks.value = emptyList()
        _referencedSpaceFiles.value = emptyList()
        _saved.value = false
        _error.value = null
    }

    fun updateTitle(title: String) {
        _title.value = title
    }

    fun updateContentMarkdown(markdown: String) {
        _contentMarkdown.value = markdown
    }

    fun updateRecordDate(date: Long) {
        _recordDate.value = date
    }

    fun toggleRelatedFolder(folderId: Long) {
        val current = _relatedFolderIds.value
        _relatedFolderIds.value = if (current.contains(folderId)) {
            current.filter { it != folderId }
        } else {
            current + folderId
        }
        if (_primaryFolderId.value == folderId && !_relatedFolderIds.value.contains(folderId)) {
            _primaryFolderId.value = null
        }
    }

    fun setPrimaryFolder(folderId: Long?) {
        _primaryFolderId.value = folderId
        if (folderId != null && !_relatedFolderIds.value.contains(folderId)) {
            _relatedFolderIds.value = _relatedFolderIds.value + folderId
        }
    }

    suspend fun addImageReference(fileRelativePath: String, folderId: Long?): Long {
        val targetFolderId = folderId ?: _primaryFolderId.value ?: 0L
        val mediaItem = mediaRepository.insertForEditor(
            mediaType = MediaType.IMAGE,
            fileRelativePath = fileRelativePath,
            folderId = targetFolderId
        )
        val ref = ContentReference(refType = RefType.IMAGE, targetId = mediaItem.id)
        _contentReferences.value = _contentReferences.value + ref
        _referencedMedia.value = _referencedMedia.value + mediaItem
        return mediaItem.id
    }

    suspend fun addVideoReference(fileRelativePath: String, folderId: Long?): Long {
        val targetFolderId = folderId ?: _primaryFolderId.value ?: 0L
        val mediaItem = mediaRepository.insertForEditor(
            mediaType = MediaType.VIDEO,
            fileRelativePath = fileRelativePath,
            folderId = targetFolderId
        )
        val ref = ContentReference(refType = RefType.VIDEO, targetId = mediaItem.id)
        _contentReferences.value = _contentReferences.value + ref
        _referencedMedia.value = _referencedMedia.value + mediaItem
        return mediaItem.id
    }

    suspend fun addVoiceReference(fileRelativePath: String, folderId: Long?): Long {
        val targetFolderId = folderId ?: _primaryFolderId.value ?: 0L
        val mediaItem = mediaRepository.insertForEditor(
            mediaType = MediaType.VOICE,
            fileRelativePath = fileRelativePath,
            folderId = targetFolderId
        )
        val ref = ContentReference(refType = RefType.VOICE, targetId = mediaItem.id)
        _contentReferences.value = _contentReferences.value + ref
        _referencedMedia.value = _referencedMedia.value + mediaItem
        return mediaItem.id
    }

    suspend fun addSpaceLinkReference(
        spaceType: SpaceType,
        webUrl: String,
        name: String,
        thumbnailUrl: String?,
        folderId: Long?
    ): Long {
        val targetFolderId = folderId ?: _primaryFolderId.value ?: 0L
        val spaceItem = spaceLinkRepository.insertForEditor(
            context = getApplication(),
            spaceType = spaceType,
            webUrl = webUrl,
            name = name,
            thumbnailUrl = thumbnailUrl,
            folderId = targetFolderId
        )
        val ref = ContentReference(refType = RefType.SPACE_LINK, targetId = spaceItem.id)
        _contentReferences.value = _contentReferences.value + ref
        _referencedSpaceLinks.value = _referencedSpaceLinks.value + spaceItem
        return spaceItem.id
    }

    suspend fun addSpaceFileReference(
        spaceType: SpaceType,
        filePath: String,
        name: String,
        thumbnailUrl: String?,
        folderId: Long?
    ): Long {
        val targetFolderId = folderId ?: _primaryFolderId.value ?: 0L
        val spaceFileItem = spaceFileRepository.insertForEditor(
            spaceType = spaceType,
            filePath = filePath,
            name = name,
            thumbnailUrl = thumbnailUrl,
            folderId = targetFolderId
        )
        val ref = ContentReference(refType = RefType.SPACE_FILE, targetId = spaceFileItem.id)
        _contentReferences.value = _contentReferences.value + ref
        _referencedSpaceFiles.value = _referencedSpaceFiles.value + spaceFileItem
        return spaceFileItem.id
    }

    suspend fun updateSpaceLinkReference(
        id: Long,
        spaceType: SpaceType,
        webUrl: String,
        name: String,
        thumbnailUrl: String?
    ) {
        val existing = spaceLinkRepository.findById(id) ?: return
        val updated = existing.copy(
            spaceType = spaceType,
            webUrl = webUrl,
            name = name,
            thumbnailUrl = thumbnailUrl
        )
        spaceLinkRepository.update(updated)
        _referencedSpaceLinks.value = _referencedSpaceLinks.value.map {
            if (it.id == id) updated else it
        }
        val newMd = RichEditorHelper.updateSpaceLinkShortcode(
            _contentMarkdown.value, id, spaceType, webUrl, name, thumbnailUrl
        )
        _contentMarkdown.value = newMd
    }

    suspend fun updateSpaceFileReference(
        id: Long,
        spaceType: SpaceType,
        filePath: String,
        name: String,
        thumbnailUrl: String?
    ) {
        val existing = spaceFileRepository.findById(id) ?: return
        val updated = existing.copy(
            spaceType = spaceType,
            filePath = filePath,
            name = name,
            thumbnailUrl = thumbnailUrl
        )
        spaceFileRepository.update(updated)
        _referencedSpaceFiles.value = _referencedSpaceFiles.value.map {
            if (it.id == id) updated else it
        }
    }

    fun removeReference(targetId: Long, refType: RefType) {
        _contentReferences.value = _contentReferences.value.filter {
            !(it.refType == refType && it.targetId == targetId)
        }
        when (refType) {
            RefType.IMAGE, RefType.VIDEO, RefType.VOICE -> {
                _referencedMedia.value = _referencedMedia.value.filter { it.id != targetId }
                viewModelScope.launch {
                    mediaRepository.deleteById(targetId)
                }
            }
            RefType.SPACE_LINK -> {
                _referencedSpaceLinks.value = _referencedSpaceLinks.value.filter { it.id != targetId }
                viewModelScope.launch {
                    spaceLinkRepository.deleteById(targetId, getApplication())
                }
            }
            RefType.SPACE_FILE -> {
                _referencedSpaceFiles.value = _referencedSpaceFiles.value.filter { it.id != targetId }
                viewModelScope.launch {
                    val item = spaceFileRepository.findById(targetId)
                    if (item != null) {
                        if (item.sourceType == SourceType.DIRECT_ADD) {
                            spaceFileRepository.deleteByIdWithFile(getApplication(), targetId)
                        } else {
                            spaceFileRepository.deleteById(targetId)
                        }
                    }
                }
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            try {
                val relatedIds = _relatedFolderIds.value
                val primaryId = _primaryFolderId.value
                    ?: if (relatedIds.isNotEmpty()) relatedIds.first() else null

                if (relatedIds.isEmpty()) {
                    _error.value = getApplication<Application>().getString(R.string.folder_required)
                    return@launch
                }

                val context = getApplication<Application>()

                syncSpaceFileNamesFromShortcodes(context)

                if (_editingRecordId.value != null) {
                    val existing = recordRepository.getRecord(_editingRecordId.value!!)
                    val oldTitle = existing?.title ?: ""
                    val oldRecordDate = existing?.recordDate ?: _recordDate.value

                    recordRepository.updateRecord(
                        id = _editingRecordId.value!!,
                        recordDate = _recordDate.value,
                        title = _title.value,
                        contentMarkdown = _contentMarkdown.value,
                        relatedFolderIds = relatedIds,
                        primaryFolderId = primaryId,
                        contentReferences = _contentReferences.value
                    )

                    FileHelper.updateRecordMarkdown(
                        context = context,
                        recordId = _editingRecordId.value!!,
                        oldTitle = oldTitle,
                        newTitle = _title.value,
                        contentMarkdown = _contentMarkdown.value,
                        oldRecordDate = oldRecordDate,
                        newRecordDate = _recordDate.value
                    )
                } else {
                    val recordId = recordRepository.createRecord(
                        recordDate = _recordDate.value,
                        title = _title.value,
                        contentMarkdown = _contentMarkdown.value,
                        relatedFolderIds = relatedIds,
                        primaryFolderId = primaryId,
                        contentReferences = _contentReferences.value
                    )

                    FileHelper.saveRecordMarkdown(
                        context = context,
                        recordId = recordId,
                        title = _title.value,
                        contentMarkdown = _contentMarkdown.value,
                        recordDate = _recordDate.value
                    )
                }
                _saved.value = true
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    private suspend fun syncSpaceFileNamesFromShortcodes(context: android.content.Context) {
        val md = _contentMarkdown.value
        val regex = Regex("""\[spacefile\]\[(\d+)\]\[[^\]]*\]\[([^\]]*)\]\[([^\]]*)\]""")
        for (match in regex.findAll(md)) {
            val id = match.groupValues[1].toLongOrNull() ?: continue
            val name = match.groupValues[2]
            val thumbnail = match.groupValues[3].ifBlank { null }
            val item = spaceFileRepository.findById(id) ?: continue
            if (item.name != name || item.thumbnailUrl != thumbnail) {
                spaceFileRepository.updateNameAndThumbnailByFilePath(item.filePath, name, thumbnail)
                if (item.sourceType == SourceType.DIRECT_ADD) {
                    val year = java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault())
                        .format(java.util.Date(item.createTime))
                    val mdRelativePath = "spfile/$year/${item.id}.md"
                    val mdFile = FileHelper.getFileFromRelativePath(context, mdRelativePath)
                    if (mdFile.exists()) {
                        val typeStr = when (item.spaceType) {
                            SpaceType.PANORAMA_IMAGE -> "全景图片"
                            SpaceType.PANORAMA_VIDEO -> "全景视频"
                            SpaceType.GSPLAT -> "高斯泼溅"
                        }
                        mdFile.writeText("$typeStr\n$name\n${item.filePath}\n${thumbnail ?: ""}")
                    }
                }
            }
        }
    }
}

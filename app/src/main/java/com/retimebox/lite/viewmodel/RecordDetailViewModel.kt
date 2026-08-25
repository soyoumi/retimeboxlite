package com.retimebox.lite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.retimebox.lite.RetimeboxApplication
import com.retimebox.lite.data.local.entity.Folder
import com.retimebox.lite.data.local.entity.MediaItem
import com.retimebox.lite.data.local.entity.Record
import com.retimebox.lite.data.local.entity.RefType
import com.retimebox.lite.data.local.entity.SpaceLinkItem
import com.retimebox.lite.data.repository.FolderRepository
import com.retimebox.lite.data.repository.MediaRepository
import com.retimebox.lite.data.repository.RecordRepository
import com.retimebox.lite.data.repository.SpaceLinkRepository
import com.retimebox.lite.util.FileHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecordDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RetimeboxApplication
    private val recordRepository: RecordRepository = app.recordRepository
    private val mediaRepository: MediaRepository = app.mediaRepository
    private val spaceLinkRepository: SpaceLinkRepository = app.spaceLinkRepository
    private val folderRepository: FolderRepository = app.folderRepository

    private val _recordId = MutableStateFlow<Long>(0)
    val recordId: StateFlow<Long> = _recordId.asStateFlow()

    val record: StateFlow<Record?> = recordId.flatMapLatest { id ->
        recordRepository.observeRecord(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()

    private val _spaceLinks = MutableStateFlow<List<SpaceLinkItem>>(emptyList())
    val spaceLinks: StateFlow<List<SpaceLinkItem>> = _spaceLinks.asStateFlow()

    private val _relatedFolders = MutableStateFlow<List<Folder>>(emptyList())
    val relatedFolders: StateFlow<List<Folder>> = _relatedFolders.asStateFlow()

    private val _primaryFolder = MutableStateFlow<Folder?>(null)
    val primaryFolder: StateFlow<Folder?> = _primaryFolder.asStateFlow()

    private val _recordDate = MutableStateFlow(System.currentTimeMillis())
    val recordDate: StateFlow<Long> = _recordDate.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    fun loadRecord(id: Long) {
        _recordId.value = id
        viewModelScope.launch {
            val record = recordRepository.getRecord(id)
            if (record != null) {
                loadReferences(record)
                loadFolderInfo(record)
            }
        }
    }

    private suspend fun loadReferences(record: Record) {
        val mediaRefs = record.contentReferenceIds.filter {
            it.refType == RefType.IMAGE ||
            it.refType == RefType.VIDEO ||
            it.refType == RefType.VOICE
        }
        val spaceRefs = record.contentReferenceIds.filter {
            it.refType == RefType.SPACE_LINK
        }

        val mediaItems = mutableListOf<MediaItem>()
        for (ref in mediaRefs) {
            mediaRepository.findById(ref.targetId)?.let { mediaItems.add(it) }
        }
        _mediaItems.value = mediaItems

        val spaceLinks = mutableListOf<SpaceLinkItem>()
        for (ref in spaceRefs) {
            spaceLinkRepository.findById(ref.targetId)?.let { spaceLinks.add(it) }
        }
        _spaceLinks.value = spaceLinks
    }

    private suspend fun loadFolderInfo(record: Record) {
        val folders = mutableListOf<Folder>()
        for (fid in record.relatedFolderIds) {
            folderRepository.getFolder(fid)?.let { folders.add(it) }
        }
        _relatedFolders.value = folders

        record.primaryFolderId?.let { pid ->
            _primaryFolder.value = folderRepository.getFolder(pid)
        }

        _recordDate.value = record.recordDate
    }

    fun deleteRecord() {
        viewModelScope.launch {
            val record = recordRepository.getRecord(recordId.value)
            recordRepository.deleteRecord(
                id = recordId.value,
                context = getApplication(),
                recordDate = record?.recordDate ?: _recordDate.value
            ) { path ->
                FileHelper.deleteRelativePath(getApplication(), path)
            }
            _deleted.value = true
        }
    }
}

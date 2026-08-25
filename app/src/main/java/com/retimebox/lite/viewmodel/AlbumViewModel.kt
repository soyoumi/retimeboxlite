package com.retimebox.lite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.retimebox.lite.RetimeboxApplication
import com.retimebox.lite.data.local.entity.MediaItem
import com.retimebox.lite.data.local.entity.MediaType
import com.retimebox.lite.data.local.entity.SourceType
import com.retimebox.lite.data.repository.FolderRepository
import com.retimebox.lite.data.repository.MediaRepository
import com.retimebox.lite.util.FileHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlbumViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RetimeboxApplication
    private val mediaRepository: MediaRepository = app.mediaRepository
    private val folderRepository: FolderRepository = app.folderRepository

    private val _currentFolderId = MutableStateFlow<Long?>(null)
    val currentFolderId: StateFlow<Long?> = _currentFolderId.asStateFlow()

    private val _batchMode = MutableStateFlow(false)
    val batchMode: StateFlow<Boolean> = _batchMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    val imagesInFolder: StateFlow<List<MediaItem>> = currentFolderId.flatMapLatest { folderId ->
        if (folderId == null) {
            mediaRepository.observeByType(MediaType.IMAGE)
        } else {
            mediaRepository.observeByFolderAndType(folderId, MediaType.IMAGE)
        }
    }.map { items ->
        // 去重：同一文件只保留一个条目，优先保留 FROM_RECORD_INDEX
        val seen = mutableMapOf<String, MediaItem>()
        for (item in items) {
            val key = item.fileRelativePath
            val existing = seen[key]
            if (existing == null || (item.sourceType == SourceType.FROM_RECORD_INDEX && existing.sourceType != SourceType.FROM_RECORD_INDEX)) {
                seen[key] = item
            }
        }
        // 去重后按创建时间倒序排列
        seen.values.toList().sortedByDescending { it.createTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rootFolders = folderRepository.observeRootFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subFolders = currentFolderId.flatMapLatest { folderId ->
        if (folderId == null) {
            folderRepository.observeRootFolders()
        } else {
            folderRepository.observeChildrenOf(folderId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun navigateToFolder(folderId: Long?) {
        _currentFolderId.value = folderId
    }

    fun goToParentFolder() {
        val id = currentFolderId.value ?: return
        viewModelScope.launch {
            val folder = folderRepository.getFolder(id)
            _currentFolderId.value = folder?.parentFolderId
        }
    }

    fun enterBatchMode() {
        _batchMode.value = true
    }

    fun exitBatchMode() {
        _batchMode.value = false
        _selectedIds.value = emptySet()
    }

    fun toggleSelection(id: Long, sourceType: SourceType) {
        if (sourceType == SourceType.FROM_RECORD_INDEX) return

        val current = _selectedIds.value
        _selectedIds.value = if (current.contains(id)) {
            current - id
        } else {
            current + id
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun batchDelete() {
        viewModelScope.launch {
            val ids = _selectedIds.value.toList()
            mediaRepository.batchDeleteDirectAdd(ids) { path ->
                FileHelper.deleteRelativePath(getApplication(), path)
            }
            _selectedIds.value = emptySet()
            _batchMode.value = false
        }
    }

    fun batchMoveToFolder(targetFolderId: Long) {
        viewModelScope.launch {
            mediaRepository.batchMoveFolder(_selectedIds.value.toList(), targetFolderId)
            _selectedIds.value = emptySet()
            _batchMode.value = false
        }
    }

    fun addImage(fileRelativePath: String, folderId: Long?) {
        viewModelScope.launch {
            val targetFolderId = folderId ?: 0L
            mediaRepository.addDirectAdd(
                mediaType = MediaType.IMAGE,
                fileRelativePath = fileRelativePath,
                folderId = targetFolderId
            )
        }
    }
}

package com.retimebox.lite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.retimebox.lite.RetimeboxApplication
import com.retimebox.lite.data.local.entity.MediaItem
import com.retimebox.lite.data.local.entity.MediaType
import com.retimebox.lite.data.local.entity.RefType
import com.retimebox.lite.data.local.entity.SourceType
import com.retimebox.lite.data.repository.FolderRepository
import com.retimebox.lite.data.repository.MediaRepository
import com.retimebox.lite.data.repository.RecordRepository
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
    private val recordRepository: RecordRepository = app.recordRepository

    private val _currentFolderId = MutableStateFlow<Long?>(null)
    val currentFolderId: StateFlow<Long?> = _currentFolderId.asStateFlow()

    private val _batchMode = MutableStateFlow(false)
    val batchMode: StateFlow<Boolean> = _batchMode.asStateFlow()

    private val _forceDeleteMode = MutableStateFlow(false)
    val forceDeleteMode: StateFlow<Boolean> = _forceDeleteMode.asStateFlow()

    private val _forceDeleteEvent = MutableStateFlow<String?>(null)
    val forceDeleteEvent: StateFlow<String?> = _forceDeleteEvent.asStateFlow()

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
        _forceDeleteMode.value = false
        _selectedIds.value = emptySet()
    }

    fun toggleSelection(id: Long, sourceType: SourceType) {
        if (sourceType == SourceType.FROM_RECORD_INDEX && !_forceDeleteMode.value) return

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

    fun toggleForceDeleteMode() {
        if (!_forceDeleteMode.value) {
            // 进入强删模式
            _forceDeleteMode.value = true
            _selectedIds.value = emptySet()
            _forceDeleteEvent.value = "已进入强删模式，可勾选索引条目"
        } else {
            // 已在强删模式
            val current = _selectedIds.value
            if (current.isEmpty()) {
                _forceDeleteMode.value = false
                _forceDeleteEvent.value = "已退出强删模式"
            } else {
                viewModelScope.launch {
                    batchForceDeleteInternal(current.toList())
                    _forceDeleteMode.value = false
                    _batchMode.value = false
                }
            }
        }
    }

    fun consumeForceDeleteEvent() {
        _forceDeleteEvent.value = null
    }

    private suspend fun batchForceDeleteInternal(ids: List<Long>) {
        val items = mediaRepository.findByIds(ids)
        val context = getApplication<Application>()
        for (item in items) {
            if (item.sourceType == SourceType.DIRECT_ADD) {
                // 非索引条目且非空间文件：删物理文件 + DB
                FileHelper.deleteRelativePath(context, item.fileRelativePath)
                mediaRepository.deleteById(item.id) { /* 已删文件 */ }
            } else {
                // 索引条目：删笔记引用（同步 MD），再删对应 DIRECT_ADD + 物理文件（引用计数=0 时删）
                item.bindRecordId?.let { recordId ->
                    val directAdd = mediaRepository.findDirectAddByPath(item.fileRelativePath, item.mediaType)
                    if (directAdd != null) {
                        val refType = when (item.mediaType) {
                            MediaType.IMAGE -> RefType.IMAGE
                            MediaType.VIDEO -> RefType.VIDEO
                            MediaType.VOICE -> RefType.VOICE
                        }
                        recordRepository.removeReferenceFromRecord(recordId, refType, directAdd.id, context)
                        mediaRepository.deleteById(directAdd.id) { path ->
                            FileHelper.deleteRelativePath(context, path)
                        }
                    }
                }
                mediaRepository.deleteIndexItem(item.id)
            }
        }
        _selectedIds.value = emptySet()
    }

    fun batchDelete() {
        if (_forceDeleteMode.value) {
            _forceDeleteEvent.value = "强删模式下禁止普通删除操作"
            return
        }
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

package com.retimebox.lite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.retimebox.lite.RetimeboxApplication
import com.retimebox.lite.data.local.entity.SourceType
import com.retimebox.lite.data.local.entity.SpaceFileItem
import com.retimebox.lite.data.local.entity.SpaceLinkItem
import com.retimebox.lite.data.local.entity.SpaceType
import com.retimebox.lite.data.repository.FolderRepository
import com.retimebox.lite.data.repository.SpaceFileRepository
import com.retimebox.lite.data.repository.SpaceLinkRepository
import com.retimebox.lite.ui.space.SpaceEntry
import com.retimebox.lite.ui.space.SpaceEntryType
import com.retimebox.lite.util.FileHelper
import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SpaceViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RetimeboxApplication
    private val spaceLinkRepository: SpaceLinkRepository = app.spaceLinkRepository
    private val spaceFileRepository: SpaceFileRepository = app.spaceFileRepository
    private val folderRepository: FolderRepository = app.folderRepository

    private val _currentFolderId = MutableStateFlow<Long?>(null)
    val currentFolderId: StateFlow<Long?> = _currentFolderId.asStateFlow()

    private val _batchMode = MutableStateFlow(false)
    val batchMode: StateFlow<Boolean> = _batchMode.asStateFlow()

    private val _selectedEntries = MutableStateFlow<List<SpaceEntry>>(emptyList())
    val selectedEntries: StateFlow<List<SpaceEntry>> = _selectedEntries.asStateFlow()

    val selectedCount: StateFlow<Int> = _selectedEntries.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val itemsInFolder: StateFlow<List<SpaceEntry>> = currentFolderId.flatMapLatest { folderId ->
        val linkFlow = if (folderId == null) {
            spaceLinkRepository.observeAll()
        } else {
            spaceLinkRepository.observeByFolder(folderId)
        }
        val fileFlow = if (folderId == null) {
            spaceFileRepository.observeAll()
        } else {
            spaceFileRepository.observeByFolder(folderId)
        }
        combine(linkFlow, fileFlow) { links, files ->
            val seenUrls = mutableMapOf<String, SpaceLinkItem>()
            for (item in links) {
                val key = item.webUrl
                val existing = seenUrls[key]
                if (existing == null || (item.sourceType == SourceType.FROM_RECORD_INDEX && existing.sourceType != SourceType.FROM_RECORD_INDEX)) {
                    seenUrls[key] = item
                }
            }
            val linkEntries = seenUrls.values.map { SpaceEntry.fromLink(it) }
            val seenPaths = mutableMapOf<String, SpaceFileItem>()
            for (item in files) {
                val key = item.filePath
                val existing = seenPaths[key]
                if (existing == null || (item.sourceType == SourceType.FROM_RECORD_INDEX && existing.sourceType != SourceType.FROM_RECORD_INDEX)) {
                    seenPaths[key] = item
                }
            }
            val fileEntries = seenPaths.values.map { SpaceEntry.fromFile(it) }
            (linkEntries + fileEntries).sortedByDescending { it.createTime }
        }
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
        _selectedEntries.value = emptyList()
    }

    fun clearSelection() {
        _selectedEntries.value = emptyList()
    }

    fun toggleSelection(entry: SpaceEntry) {
        if (entry.sourceType == SourceType.FROM_RECORD_INDEX) return

        val current = _selectedEntries.value
        _selectedEntries.value = if (current.any { it.id == entry.id && it.itemType == entry.itemType }) {
            current.filterNot { it.id == entry.id && it.itemType == entry.itemType }
        } else {
            current + entry
        }
    }

    fun isEntrySelected(entry: SpaceEntry): Boolean =
        _selectedEntries.value.any { it.id == entry.id && it.itemType == entry.itemType }

    fun batchDelete() {
        viewModelScope.launch {
            val entries = _selectedEntries.value
            val linkIds = entries.filter { it.itemType == SpaceEntryType.LINK }.map { it.id }
            val fileIds = entries.filter { it.itemType == SpaceEntryType.FILE }.map { it.id }
            if (linkIds.isNotEmpty()) {
                spaceLinkRepository.batchDeleteDirectAdd(
                    ids = linkIds,
                    context = getApplication()
                )
            }
            if (fileIds.isNotEmpty()) {
                val directAddFileEntries = entries.filter {
                    it.itemType == SpaceEntryType.FILE && it.sourceType == SourceType.DIRECT_ADD
                }
                directAddFileEntries.forEach { entry ->
                    deleteSpaceFileMd(getApplication(), entry.id, entry.createTime)
                }
                spaceFileRepository.batchDelete(fileIds, getApplication())
            }
            _selectedEntries.value = emptyList()
            _batchMode.value = false
        }
    }

    fun batchMoveToFolder(targetFolderId: Long) {
        viewModelScope.launch {
            val entries = _selectedEntries.value
            val linkIds = entries.filter { it.itemType == SpaceEntryType.LINK }.map { it.id }
            val fileIds = entries.filter { it.itemType == SpaceEntryType.FILE }.map { it.id }
            if (linkIds.isNotEmpty()) {
                spaceLinkRepository.batchMoveFolder(linkIds, targetFolderId)
            }
            if (fileIds.isNotEmpty()) {
                spaceFileRepository.batchMoveFolder(fileIds, targetFolderId)
            }
            _selectedEntries.value = emptyList()
            _batchMode.value = false
        }
    }

    fun addSpaceFile(
        spaceType: SpaceType,
        filePath: String,
        name: String,
        thumbnailUrl: String?,
        folderId: Long?
    ) {
        viewModelScope.launch {
            val targetFolderId = folderId ?: 0L
            val item = spaceFileRepository.insertForEditor(
                spaceType = spaceType,
                filePath = filePath,
                name = name,
                thumbnailUrl = thumbnailUrl,
                folderId = targetFolderId
            )
            createSpaceFileMd(getApplication(), item)
        }
    }

    private fun createSpaceFileMd(context: Context, item: SpaceFileItem) {
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(item.createTime))
        val typeStr = when (item.spaceType) {
            SpaceType.PANORAMA_IMAGE -> "全景图片"
            SpaceType.PANORAMA_VIDEO -> "全景视频"
            SpaceType.GSPLAT -> "高斯泼溅"
        }
        val mdRelativePath = "spfile/$year/${item.id}.md"
        val mdFile = FileHelper.getFileFromRelativePath(context, mdRelativePath)
        mdFile.parentFile?.mkdirs()
        mdFile.writeText("$typeStr\n${item.name}\n${item.filePath}\n${item.thumbnailUrl ?: ""}")
    }

    private fun deleteSpaceFileMd(context: Context, id: Long, createTime: Long) {
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(createTime))
        val mdRelativePath = "spfile/$year/$id.md"
        val mdFile = FileHelper.getFileFromRelativePath(context, mdRelativePath)
        if (mdFile.exists()) {
            mdFile.delete()
        }
    }

    fun updateSpaceFile(
        id: Long,
        spaceType: SpaceType,
        filePath: String,
        name: String,
        thumbnailUrl: String?
    ) {
        viewModelScope.launch {
            val item = spaceFileRepository.findById(id) ?: return@launch
            val updated = item.copy(
                spaceType = spaceType,
                filePath = filePath,
                name = name,
                thumbnailUrl = thumbnailUrl
            )
            spaceFileRepository.update(updated)
            if (updated.sourceType == SourceType.DIRECT_ADD) {
                createSpaceFileMd(getApplication(), updated)
            }
        }
    }

    fun getSpaceFileById(id: Long, onResult: (SpaceFileItem?) -> Unit) {
        viewModelScope.launch {
            onResult(spaceFileRepository.findById(id))
        }
    }

    fun addSpaceLink(
        spaceType: SpaceType,
        webUrl: String,
        name: String,
        thumbnailUrl: String?,
        folderId: Long?
    ) {
        viewModelScope.launch {
            val targetFolderId = folderId ?: 0L
            spaceLinkRepository.addDirectAdd(
                context = getApplication(),
                spaceType = spaceType,
                webUrl = webUrl,
                name = name,
                thumbnailUrl = thumbnailUrl,
                folderId = targetFolderId
            )
        }
    }

    fun updateSpaceLink(
        id: Long,
        spaceType: SpaceType,
        webUrl: String,
        name: String,
        thumbnailUrl: String?
    ) {
        viewModelScope.launch {
            val item = spaceLinkRepository.findById(id) ?: return@launch
            val updated = item.copy(
                spaceType = spaceType,
                webUrl = webUrl,
                name = name,
                thumbnailUrl = thumbnailUrl
            )
            spaceLinkRepository.update(updated)
        }
    }
}

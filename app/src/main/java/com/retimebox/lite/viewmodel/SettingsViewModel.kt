package com.retimebox.lite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.retimebox.lite.RetimeboxApplication
import com.retimebox.lite.data.local.entity.Folder
import com.retimebox.lite.data.repository.FolderRepository
import com.retimebox.lite.util.FileHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RetimeboxApplication
    private val folderRepository: FolderRepository = app.folderRepository

    val rootFolders: StateFlow<List<Folder>> = folderRepository.observeRootFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFolders: StateFlow<List<Folder>> = folderRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getSubFolders(parentId: Long) = folderRepository.observeChildrenOf(parentId)

    fun createFolder(name: String, parentId: Long?, colorHex: String = "#6750A4") {
        viewModelScope.launch {
            folderRepository.createFolder(name, parentId, colorHex)
        }
    }

    fun renameFolder(id: Long, newName: String, newColorHex: String? = null) {
        viewModelScope.launch {
            folderRepository.renameFolder(id, newName, newColorHex)
        }
    }

    fun deleteFolder(id: Long) {
        viewModelScope.launch {
            folderRepository.deleteFolder(id) { path ->
                FileHelper.deleteRelativePath(getApplication(), path)
            }
        }
    }

    suspend fun getFolder(id: Long): Folder? = folderRepository.getFolder(id)
}

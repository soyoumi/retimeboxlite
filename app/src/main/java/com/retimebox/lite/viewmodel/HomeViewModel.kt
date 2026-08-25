package com.retimebox.lite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.retimebox.lite.RetimeboxApplication
import com.retimebox.lite.data.local.entity.Record
import com.retimebox.lite.data.repository.FolderRepository
import com.retimebox.lite.data.repository.RecordRepository
import com.retimebox.lite.util.FileHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RetimeboxApplication
    private val recordRepository: RecordRepository = app.recordRepository
    private val folderRepository: FolderRepository = app.folderRepository

    private val _currentView = MutableStateFlow(0)
    val currentView: StateFlow<Int> = _currentView.asStateFlow()

    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    private val _currentFolderId = MutableStateFlow<Long?>(null)
    val currentFolderId: StateFlow<Long?> = _currentFolderId.asStateFlow()

    val recordsForDate: StateFlow<List<Record>> = selectedDate.flatMapLatest { date ->
        val cal = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = cal.timeInMillis

        recordRepository.observeRecordsByDateRange(startOfDay, endOfDay)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recordsInFolder: StateFlow<List<Record>> = currentFolderId.flatMapLatest { folderId ->
        if (folderId == null) {
            recordRepository.observeAllRecords()
        } else {
            folderRepository.observeRecordsByFolder(folderId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecords: StateFlow<List<Record>> = recordRepository.observeAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rootFolders = folderRepository.observeRootFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subFolders = currentFolderId.flatMapLatest { folderId ->
        if (folderId == null) {
            folderRepository.observeRootFolders()
        } else {
            folderRepository.observeChildrenOf(folderId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val isSearchActive: StateFlow<Boolean> = _searchQuery.map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val searchResults: StateFlow<List<Record>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            flowOf(emptyList())
        } else {
            recordRepository.search(query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun switchView(viewIndex: Int) {
        _currentView.value = viewIndex
    }

    fun prevDay() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate.value }
        cal.add(Calendar.DAY_OF_YEAR, -1)
        _selectedDate.value = cal.timeInMillis
    }

    fun nextDay() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate.value }
        cal.add(Calendar.DAY_OF_YEAR, 1)
        _selectedDate.value = cal.timeInMillis
    }

    fun goToday() {
        _selectedDate.value = System.currentTimeMillis()
    }

    fun selectDate(date: Long) {
        _selectedDate.value = date
    }

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

    fun deleteRecord(record: Record) {
        viewModelScope.launch {
            recordRepository.deleteRecord(
                id = record.id,
                context = getApplication(),
                recordDate = record.recordDate
            ) { path ->
                FileHelper.deleteRelativePath(getApplication(), path)
            }
        }
    }
}

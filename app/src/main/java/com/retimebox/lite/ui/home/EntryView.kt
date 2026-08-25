package com.retimebox.lite.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.retimebox.lite.R
import com.retimebox.lite.data.local.entity.Record
import com.retimebox.lite.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryView(
    onOpenRecord: (Long) -> Unit,
    onOpenRecordEditor: (Long?, Long?) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val currentView by viewModel.currentView.collectAsStateWithLifecycle()
    val currentFolderId by viewModel.currentFolderId.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                modifier = Modifier.height(20.dp),
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                val tabs = listOf(
                    stringResource(R.string.view_entry),
                    stringResource(R.string.view_calendar),
                    stringResource(R.string.view_directory)
                )
                ScrollableTabRow(selectedTabIndex = currentView) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = currentView == index,
                            onClick = { viewModel.switchView(index) },
                            text = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (currentView) {
                    0 -> EntryListView(
                        viewModel = viewModel,
                        onOpenRecord = onOpenRecord
                    )
                    1 -> CalendarView(
                        viewModel = viewModel,
                        onOpenRecord = onOpenRecord
                    )
                    2 -> DirectoryView(
                        viewModel = viewModel,
                        onOpenRecord = onOpenRecord,
                        onOpenRecordEditor = onOpenRecordEditor
                    )
                }
            }

            // 新建笔记 FAB（仅在条目视图和目录视图显示）
            if (currentView == 0 || (currentView == 2 && currentFolderId != null)) {
                FloatingActionButton(
                    onClick = {
                        val folderId = if (currentView == 2) currentFolderId else null
                        onOpenRecordEditor(null, folderId)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_record))
                }
            }
        }
    }
}

@Composable
private fun EntryListView(
    viewModel: HomeViewModel,
    onOpenRecord: (Long) -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val records by viewModel.recordsForDate.collectAsStateWithLifecycle()

    val dateFormat = remember { SimpleDateFormat("yyyy年M月d日 EEEE", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 日期导航
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.prevDay() }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.prev_day))
            }
            Text(
                text = dateFormat.format(Date(selectedDate)),
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = { viewModel.nextDay() }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.next_day))
            }
        }

        if (records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_records),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(records) { record ->
                    RecordItem(
                        record = record,
                        onClick = { onOpenRecord(record.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun RecordItem(
    record: Record,
    onClick: () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (record.title.isNotEmpty()) {
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = record.contentMarkdown
                    .replace(Regex("<br\\s*/?>"), " ")
                    .take(100)
                    .ifEmpty { "（空笔记）" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(record.createTime)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CalendarView(
    viewModel: HomeViewModel,
    onOpenRecord: (Long) -> Unit
) {
    val records by viewModel.allRecords.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // 简易日历
        MinimalCalendar(
            selectedDate = selectedDate,
            recordDates = records.map { it.recordDate },
            onDateSelected = { viewModel.selectDate(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 选中日期的笔记列表
        val recordsForDay by viewModel.recordsForDate.collectAsStateWithLifecycle()
        if (recordsForDay.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_record_on_day),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recordsForDay) { record ->
                    RecordItem(record = record, onClick = { onOpenRecord(record.id) })
                }
            }
        }
    }
}

@Composable
private fun MinimalCalendar(
    selectedDate: Long,
    recordDates: List<Long>,
    onDateSelected: (Long) -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = selectedDate
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)

    val firstDayOfMonth = Calendar.getInstance().apply {
        set(year, month, 1)
    }
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    // 获取当月有笔记的日期（day-of-month）
    val recordDays = recordDates.map { ts ->
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month) {
            cal.get(Calendar.DAY_OF_MONTH)
        } else -1
    }.filter { it > 0 }.toSet()

    val dayOfWeekOfFirst = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "${year}年 ${month + 1}月",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 星期标题行
        Row(modifier = Modifier.fillMaxWidth()) {
            val weekDays = listOf("日", "一", "二", "三", "四", "五", "六")
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 日期网格
        val totalCells = dayOfWeekOfFirst + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - dayOfWeekOfFirst + 1
                    val isValidDay = dayNumber in 1..daysInMonth

                    if (isValidDay) {
                        val dayCal = Calendar.getInstance().apply {
                            set(year, month, dayNumber, 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val isSelected = dayCal.timeInMillis == Calendar.getInstance().apply {
                            timeInMillis = selectedDate
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val hasRecord = dayNumber in recordDays

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                                .clickable { onDateSelected(dayCal.timeInMillis) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayNumber.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                if (hasRecord) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .then(
                                                if (isSelected)
                                                    Modifier.padding(horizontal = 4.dp)
                                                else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                        modifier = Modifier
                                            .padding(1.dp)
                                            .background(
                                                color = if (isSelected)
                                                    MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.primaryContainer,
                                                shape = MaterialTheme.shapes.small
                                            )
                                            .then(Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    ) { }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

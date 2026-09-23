package com.lengcs.fkwakeup.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lengcs.fkwakeup.core.database.repository.CourseRepository
import com.lengcs.fkwakeup.core.database.repository.CurrentTermProvider
import com.lengcs.fkwakeup.core.datastore.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderManagementViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val currentTermProvider: CurrentTermProvider,
    private val courseRepository: CourseRepository,
) : ViewModel() {
    private val _courses = MutableStateFlow<List<Pair<Long, String>>>(emptyList())
    val courses: StateFlow<List<Pair<Long, String>>> = _courses
    private val _defaultMinutes = MutableStateFlow(10)
    val defaultMinutes: StateFlow<Int> = _defaultMinutes

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { _defaultMinutes.value = it.reminderDefaultMinutesBefore }
        }
        viewModelScope.launch {
            currentTermProvider.observeCurrentTerm().flatMapLatest { term ->
                term?.let { courseRepository.observeCourses(it.id) } ?: flowOf(emptyList())
            }.collect { entries ->
                _courses.value = entries.map { it.course.id to it.course.name }
            }
        }
    }

    fun setDefaultMinutes(value: Int) = viewModelScope.launch {
        settingsRepository.setReminderDefaultMinutes(value)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderManagementScreen(
    onBack: () -> Unit,
    onCourseClick: (Long) -> Unit,
    viewModel: ReminderManagementViewModel = hiltViewModel(),
) {
    val defaultMinutes by viewModel.defaultMinutes.collectAsState()
    val courses by viewModel.courses.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text("提醒管理") }, navigationIcon = { TextButton(onClick = onBack) { Text("返回") } }) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("全局默认提前时间", style = MaterialTheme.typography.titleMedium)
            Text("新课程的提醒默认关闭。开启时会使用这里的提前时间，之后可单独修改。", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 5, 10, 15).forEach { minutes ->
                    FilterChip(
                        selected = defaultMinutes == minutes,
                        onClick = { viewModel.setDefaultMinutes(minutes) },
                        label = { Text(if (minutes == 0) "准时" else "${minutes}分钟") },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 60).forEach { minutes ->
                    FilterChip(defaultMinutes == minutes, { viewModel.setDefaultMinutes(minutes) }, { Text("${minutes}分钟") })
                }
            }
            Text("本学期课程", style = MaterialTheme.typography.titleMedium)
            if (courses.isEmpty()) Text("当前学期还没有课程")
            courses.forEach { (id, name) ->
                Card(Modifier.fillMaxWidth().clickable { onCourseClick(id) }) {
                    Text(name, Modifier.padding(16.dp))
                }
            }
        }
    }
}

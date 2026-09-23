package com.lengcs.fkwakeup.feature.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lengcs.fkwakeup.core.designsystem.picker.LabeledWheel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseReminderScreen(
    onBack: () -> Unit,
    viewModel: CourseReminderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    fun finishSave() {
        viewModel.save()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == false
        ) {
            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
        }
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) finishSave() else viewModel.save()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.courseName.isBlank()) "课程提醒" else "${state.courseName} · 提醒") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
            )
        },
    ) { padding ->
        if (state.loading) {
            Column(Modifier.padding(padding).padding(24.dp)) { Text("正在读取课程…") }
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PermissionCard(
                onNotificationPermission = {
                    if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                onExactAlarmPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                    )
                },
            )
            Text("提醒范围", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderScope.entries.forEach { scope ->
                    FilterChip(
                        selected = state.scope == scope,
                        onClick = { viewModel.setScope(scope) },
                        label = { Text(scope.label()) },
                    )
                }
            }
            Text("提前时间", style = MaterialTheme.typography.titleMedium)
            OffsetChoices(state.primaryMinutes, { it?.let(viewModel::setPrimary) })
            Text("第二次提醒（可选）", style = MaterialTheme.typography.titleSmall)
            OffsetChoices(state.secondaryMinutes, viewModel::setSecondary, includeOff = true)

            if (state.scope != ReminderScope.OFF) {
                Text(if (state.scope == ReminderScope.CUSTOM) "选择要提醒的日期" else "本学期例外", style = MaterialTheme.typography.titleMedium)
                if (state.choices.isEmpty()) Text("当前学期没有未来的固定课程")
                state.choices.forEach { choice ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(choice.label)
                                Text(choice.date.toString(), style = MaterialTheme.typography.bodySmall)
                                if (choice.key in state.selectedKeys) {
                                    OccurrenceTimeButton(
                                        offset = state.occurrenceOffsets[choice.key],
                                        defaultPrimary = state.primaryMinutes,
                                        defaultSecondary = state.secondaryMinutes,
                                        onSave = { first, second -> viewModel.setOccurrenceOffsets(choice.key, first, second) },
                                        onReset = { viewModel.clearOccurrenceOffsets(choice.key) },
                                    )
                                }
                            }
                            Switch(checked = choice.key in state.selectedKeys, onCheckedChange = { viewModel.toggle(choice.key) })
                        }
                    }
                }
            }

            Text("异步网课", style = MaterialTheme.typography.titleMedium)
            ReminderSwitch("开放日提醒", "开放当天 ${formatTime(state.asyncOpenMinutes)}", state.asyncOpen, viewModel::setAsyncOpen)
            if (state.asyncOpen) TimeButton(state.asyncOpenMinutes, viewModel::setAsyncOpenMinutes)
            ReminderSwitch("截止提醒", "截止前 ${state.asyncDeadlineDays} 天 ${formatTime(state.asyncDeadlineMinutes)}", state.asyncDeadline, viewModel::setAsyncDeadline)
            if (state.asyncDeadline) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..3).forEach { days ->
                        FilterChip(state.asyncDeadlineDays == days, { viewModel.setAsyncDeadlineDays(days) }, { Text(if (days == 0) "截止当天" else "提前${days}天") })
                    }
                }
                TimeButton(state.asyncDeadlineMinutes, viewModel::setAsyncDeadlineMinutes)
            }
            Button(
                onClick = {
                    val enabling = state.scope != ReminderScope.OFF || state.asyncOpen || state.asyncDeadline
                    if (enabling && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else if (enabling) finishSave()
                    else viewModel.save()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("保存提醒") }
            if (state.saved) Text("提醒已保存", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PermissionCard(onNotificationPermission: () -> Unit, onExactAlarmPermission: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("系统权限", style = MaterialTheme.typography.titleSmall)
            Text("允许通知后才能收到提醒；允许精确闹钟可让提醒更准时。未授权精确闹钟时会自动使用后台任务，可能略有延迟。", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (Build.VERSION.SDK_INT >= 33) TextButton(onClick = onNotificationPermission) { Text("允许通知") }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) TextButton(onClick = onExactAlarmPermission) { Text("精确提醒") }
            }
        }
    }
}

@Composable
private fun OffsetChoices(selected: Int?, onSelect: (Int?) -> Unit, includeOff: Boolean = false) {
    var customOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (includeOff) FilterChip(selected == null, { onSelect(null) }, { Text("关闭") })
        listOf(0, 5, 10, 15, 30, 60).forEach { minutes ->
            FilterChip(selected == minutes, { onSelect(minutes) }, { Text(if (minutes == 0) "准时" else "${minutes}分钟") })
        }
        FilterChip(selected !in listOf(null, 0, 5, 10, 15, 30, 60), { customOpen = true }, { Text("自定义") })
    }
    if (customOpen) MinutesDialog(selected ?: 10, { customOpen = false }, { onSelect(it); customOpen = false })
}

@Composable
private fun MinutesDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var value by remember { mutableStateOf(initial.coerceIn(0, 180)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义提前时间") },
        text = {
            LabeledWheel(
                label = "分钟",
                items = (0..180).map(Int::toString),
                selectedIndex = value,
                onSelectedChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun TimeButton(minutes: Int, onConfirm: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    TextButton(onClick = { open = true }) { Text("调整时间 · ${formatTime(minutes)}") }
    if (open) TimeWheelDialog(minutes, { open = false }, { onConfirm(it); open = false })
}

@Composable
private fun OccurrenceTimeButton(
    offset: Pair<Int, Int?>?,
    defaultPrimary: Int,
    defaultSecondary: Int?,
    onSave: (Int, Int?) -> Unit,
    onReset: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    TextButton(onClick = { open = true }) {
        Text(offset?.let { "本次：提前${it.first}分钟" } ?: "使用课程默认时间")
    }
    if (open) {
        var first by remember { mutableStateOf(offset?.first ?: defaultPrimary) }
        var second by remember { mutableStateOf(offset?.second ?: defaultSecondary) }
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("本次课程的提醒时间") },
            text = {
                Column {
                    Text("第一次：提前 $first 分钟")
                    LabeledWheel("分钟", (0..180).map(Int::toString), first.coerceIn(0, 180), { first = it }, Modifier.fillMaxWidth())
                    Text("第二次：${second?.let { "提前 $it 分钟" } ?: "关闭"}")
                    Row {
                        TextButton(onClick = { second = null }) { Text("关闭第二次") }
                        TextButton(onClick = { second = second ?: 5 }) { Text("设置第二次") }
                    }
                    second?.let { minutes ->
                        LabeledWheel("分钟", (0..180).map(Int::toString), minutes.coerceIn(0, 180), { second = it }, Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = { TextButton(onClick = { onSave(first, second); open = false }) { Text("保存") } },
            dismissButton = {
                TextButton(onClick = { onReset(); open = false }) { Text("恢复默认") }
            },
        )
    }
}

@Composable
private fun TimeWheelDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var hour by remember { mutableStateOf(initial / 60) }
    var minute by remember { mutableStateOf(initial % 60) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择提醒时间") },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledWheel("时", (0..23).map { "%02d".format(it) }, hour, { hour = it }, Modifier.weight(1f))
                LabeledWheel("分", (0..59).map { "%02d".format(it) }, minute, { minute = it }, Modifier.weight(1f))
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(hour * 60 + minute) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ReminderSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title); Text(subtitle, style = MaterialTheme.typography.bodySmall) }
        Switch(checked, onChange)
    }
}

private fun ReminderScope.label() = when (this) {
    ReminderScope.OFF -> "关闭"
    ReminderScope.ALL_FUTURE -> "以后每次"
    ReminderScope.CUSTOM -> "选择日期"
}

private fun formatTime(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)

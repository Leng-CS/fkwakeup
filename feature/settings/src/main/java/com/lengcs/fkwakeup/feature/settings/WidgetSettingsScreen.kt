package com.lengcs.fkwakeup.feature.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lengcs.fkwakeup.core.common.WidgetLessonBarMode
import com.lengcs.fkwakeup.core.common.WidgetLessonRange
import com.lengcs.fkwakeup.widget.glance.WidgetBackgroundType
import com.lengcs.fkwakeup.widget.glance.WidgetConfig
import com.lengcs.fkwakeup.widget.glance.WidgetImagePreset
import com.lengcs.fkwakeup.widget.glance.WidgetInstance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettingsScreen(
    onBack: () -> Unit,
    viewModel: WidgetSettingsViewModel = hiltViewModel(),
) {
    val instances by viewModel.instances.collectAsState()
    var selectedId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(instances) {
        if (selectedId != null && instances.none { it.appWidgetId == selectedId }) selectedId = null
    }
    val selected = instances.firstOrNull { it.appWidgetId == selectedId }
    if (selected == null) {
        WidgetInstanceList(instances, onBack) { selectedId = it }
    } else {
        WidgetConfigEditor(
            instance = selected,
            displayNumber = instances.indexOfFirst { it.appWidgetId == selected.appWidgetId } + 1,
            onBack = { selectedId = null },
            onSave = { viewModel.save(selected.appWidgetId, it) },
        )
    }
}

@Composable
private fun WidgetInstanceList(
    instances: List<WidgetInstance>,
    onBack: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    Scaffold(topBar = { SettingsTopBar("小组件设置", onBack) }) { padding ->
        if (instances.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("请先在桌面添加课表小组件")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                itemsIndexed(instances, key = { _, item -> item.appWidgetId }) { index, instance ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(12.dp, 6.dp)
                            .clickable { onSelect(instance.appWidgetId) },
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.height(42.dp).fillMaxWidth(0.2f)
                                    .background(Color(instance.config.backgroundStartArgb)),
                            )
                            Column(Modifier.padding(start = 12.dp)) {
                                Text("课表小组件 ${index + 1} · ${instance.sizeMode.label}")
                                Text(instance.config.backgroundType.displayName(), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetConfigEditor(
    instance: WidgetInstance,
    displayNumber: Int,
    onBack: () -> Unit,
    onSave: (WidgetConfig) -> Unit,
) {
    var draft by remember(instance.appWidgetId, instance.config) { mutableStateOf(instance.config) }
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        draft = draft.copy(backgroundType = WidgetBackgroundType.PHOTO, imageUri = uri.toString())
    }
    Scaffold(
        topBar = { SettingsTopBar("课表小组件 $displayNumber · ${instance.sizeMode.label}", onBack) },
        bottomBar = { Button(onClick = { onSave(draft); onBack() }, modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("保存") } },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            item { SettingTitle("课程范围") }
            item { ChoiceRow(WidgetLessonRange.entries, draft.range, WidgetLessonRange::label) { draft = draft.copy(range = it) } }
            item { SettingTitle("左侧竖条") }
            item { ChoiceRow(WidgetLessonBarMode.entries, draft.barMode, WidgetLessonBarMode::label) { draft = draft.copy(barMode = it) } }
            item { SettingTitle("背景") }
            item { ChoiceRow(WidgetBackgroundType.entries, draft.backgroundType, WidgetBackgroundType::displayName) { draft = draft.copy(backgroundType = it) } }
            if (draft.backgroundType == WidgetBackgroundType.PRESET_IMAGE) {
                item { ChoiceRow(WidgetImagePreset.entries, draft.imagePreset, WidgetImagePreset::displayName) { draft = draft.copy(imagePreset = it) } }
            }
            if (draft.backgroundType == WidgetBackgroundType.PHOTO) {
                item { TextButton(onClick = { photoPicker.launch(arrayOf("image/*")) }) { Text("从相册选择图片") } }
                item { Text(if (draft.imageUri == null) "尚未选择图片" else "已选择相册图片", style = MaterialTheme.typography.bodySmall) }
            }
            if (draft.backgroundType == WidgetBackgroundType.SOLID || draft.backgroundType == WidgetBackgroundType.GRADIENT) {
                item { ColorControls("主颜色", Color(draft.backgroundStartArgb)) { draft = draft.copy(backgroundStartArgb = it.toArgb()) } }
            }
            if (draft.backgroundType == WidgetBackgroundType.GRADIENT) {
                item { ColorControls("渐变终止颜色", Color(draft.backgroundEndArgb)) { draft = draft.copy(backgroundEndArgb = it.toArgb()) } }
            }
            item { OpacityControl("背景与课程卡片透明度", draft.backgroundAlpha) { draft = draft.copy(backgroundAlpha = it) } }
            item { OpacityControl("文字透明度", draft.textAlpha) { draft = draft.copy(textAlpha = it) } }
            item {
                FilterChip(
                    selected = draft.darkPreset,
                    onClick = { draft = draft.copy(darkPreset = !draft.darkPreset) },
                    label = { Text("深色模式预设") },
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(title = { Text(title) }, navigationIcon = { TextButton(onClick = onBack) { Text("返回") } })
}

@Composable
private fun SettingTitle(text: String) = Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))

@Composable
private fun <T> ChoiceRow(values: Iterable<T>, selected: T, label: (T) -> String, onChange: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        values.forEach { value -> FilterChip(selected = value == selected, onClick = { onChange(value) }, label = { Text(label(value)) }) }
    }
}

@Composable
private fun OpacityControl(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(Modifier.padding(top = 12.dp)) {
        Text("$label ${(value * 100).toInt()}%")
        Slider(value = value, onValueChange = onChange)
    }
}

@Composable
private fun ColorControls(label: String, color: Color, onChange: (Color) -> Unit) {
    Column(Modifier.padding(top = 12.dp)) {
        Text(label)
        ColorChannel("红", color.red) { onChange(color.copy(red = it)) }
        ColorChannel("绿", color.green) { onChange(color.copy(green = it)) }
        ColorChannel("蓝", color.blue) { onChange(color.copy(blue = it)) }
    }
}

@Composable
private fun ColorChannel(name: String, value: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(name, modifier = Modifier.padding(end = 8.dp))
        Slider(value = value, onValueChange = onChange, modifier = Modifier.weight(1f))
    }
}

private fun Color.toArgb(): Int = (alpha * 255).toInt().coerceIn(0, 255).shl(24) or
    (red * 255).toInt().coerceIn(0, 255).shl(16) or
    (green * 255).toInt().coerceIn(0, 255).shl(8) or (blue * 255).toInt().coerceIn(0, 255)

private fun WidgetLessonRange.label() = when (this) {
    WidgetLessonRange.TODAY_REMAINING -> "今日未结束"
    WidgetLessonRange.TODAY_ALL -> "今日全部"
    WidgetLessonRange.TODAY_AND_TOMORROW -> "今日＋明天"
    WidgetLessonRange.THIS_WEEK -> "本周"
}
private fun WidgetLessonBarMode.label() = when (this) {
    WidgetLessonBarMode.COURSE_COLOR -> "课程颜色"
    WidgetLessonBarMode.SECTION -> "节次"
    WidgetLessonBarMode.COLOR_AND_SECTION -> "两者"
}
private fun WidgetBackgroundType.displayName() = when (this) {
    WidgetBackgroundType.TRANSPARENT -> "完全透明"
    WidgetBackgroundType.SOLID -> "纯色"
    WidgetBackgroundType.GRADIENT -> "渐变"
    WidgetBackgroundType.PRESET_IMAGE -> "预设图"
    WidgetBackgroundType.PHOTO -> "相册图片"
}
private fun WidgetImagePreset.displayName() = when (this) {
    WidgetImagePreset.SKY -> "天空"
    WidgetImagePreset.SUNSET -> "日落"
    WidgetImagePreset.PAPER -> "纸张"
}

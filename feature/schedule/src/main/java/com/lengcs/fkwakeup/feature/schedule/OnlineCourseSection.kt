package com.lengcs.fkwakeup.feature.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lengcs.fkwakeup.core.common.OnlineCourseItem
import com.lengcs.fkwakeup.core.common.OnlineCoursePhase

@Composable
internal fun OnlineCourseSection(
    items: List<OnlineCourseItem>,
    onCourseClick: (Long) -> Unit,
    onAlarmClick: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }
    var endedExpanded by remember { mutableStateOf(false) }
    val current = items.filter { it.phase != OnlineCoursePhase.ENDED }
    val ended = items.filter { it.phase == OnlineCoursePhase.ENDED }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
    ) {
        Column(
            modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.online_section_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "⌃" else "⌄")
                }
            }
            if (expanded) {
                current.forEach { item -> OnlineCourseCard(item, onCourseClick, onAlarmClick) }
                if (ended.isNotEmpty()) {
                    TextButton(onClick = { endedExpanded = !endedExpanded }) {
                        Text(stringResource(R.string.online_ended_title, ended.size))
                    }
                    if (endedExpanded) {
                        ended.forEach { item -> OnlineCourseCard(item, onCourseClick, onAlarmClick) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineCourseCard(
    item: OnlineCourseItem,
    onCourseClick: (Long) -> Unit,
    onAlarmClick: (Long) -> Unit,
) {
    val ended = item.phase == OnlineCoursePhase.ENDED
    val alarmDescription = stringResource(R.string.online_alarm_cd)
    val alpha = if (ended) 0.5f else 1f
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onCourseClick(item.course.id) },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = alpha),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = item.course.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(phaseText(item.phase), style = MaterialTheme.typography.labelSmall)
                }
                Text(
                    text = stringResource(
                        R.string.online_date_range,
                        item.window.startDate.toString(),
                        item.window.endDate.toString(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                listOfNotNull(item.window.platform, item.course.teacher)
                    .takeIf { it.isNotEmpty() }
                    ?.let { Text(it.joinToString(" · "), style = MaterialTheme.typography.labelSmall) }
            }
            IconButton(
                onClick = { onAlarmClick(item.course.id) },
                modifier = Modifier.semantics { contentDescription = alarmDescription },
            ) { Text("⏰") }
        }
    }
}

@Composable
private fun phaseText(phase: OnlineCoursePhase): String = stringResource(
    when (phase) {
        OnlineCoursePhase.ACTIVE -> R.string.online_phase_active
        OnlineCoursePhase.UPCOMING -> R.string.online_phase_upcoming
        OnlineCoursePhase.ENDED -> R.string.online_phase_ended
    },
)

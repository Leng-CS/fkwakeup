package com.lengcs.fkwakeup.core.designsystem.picker

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * 滚轮选择器，交互类似设定闹钟时间的那种滚轮。
 *
 * 上下滑动，松手自动吸附；中间高亮项即选中值。
 *
 * 用 `LazyColumn` + `rememberSnapFlingBehavior` 实现，**不引入第三方滚轮库**。
 */
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleCount: Int = 3,
    itemHeight: Dp = 40.dp,
) {
    require(visibleCount % 2 == 1) { "visibleCount 必须是奇数，才能有明确的中心项" }

    val state: LazyListState = rememberLazyListState()
    val viewportHeight = itemHeight * visibleCount
    val verticalPadding = itemHeight * (visibleCount / 2)

    // 外部选中值变化时滚动对齐（滚动中不动，避免打断手势）
    LaunchedEffect(selectedIndex) {
        if (!state.isScrollInProgress) {
            state.animateScrollToItem(selectedIndex.coerceIn(0, items.lastIndex))
        }
    }

    // 滚动停止后，取离中心最近的一项作为选中值
    LaunchedEffect(state.isScrollInProgress) {
        if (!state.isScrollInProgress) {
            val info = state.layoutInfo
            if (info.visibleItemsInfo.isNotEmpty()) {
                val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
                val nearest = info.visibleItemsInfo.minByOrNull { item ->
                    abs((item.offset + item.size / 2) - center)
                }
                nearest?.index
                    ?.coerceIn(0, items.lastIndex)
                    ?.takeIf { it != selectedIndex }
                    ?.let { onSelectedChange(it) }
            }
        }
    }

    Box(
        modifier = modifier
            .height(viewportHeight)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        // 中间高亮条
        Surface(
            modifier = Modifier
                .height(itemHeight)
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {}

        LazyColumn(
            state = state,
            modifier = Modifier.height(viewportHeight),
            contentPadding = PaddingValues(vertical = verticalPadding),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
        ) {
            items(items.size) { index ->
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val isSelected = index == selectedIndex
                    Text(
                        text = items[index],
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        },
                    )
                }
            }
        }
    }
}

/** 一行文字标签 + 滚轮，便于在表单里复用 */
@Composable
fun LabeledWheel(
    label: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 40.dp,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        WheelPicker(
            items = items,
            selectedIndex = selectedIndex,
            onSelectedChange = onSelectedChange,
            itemHeight = itemHeight,
        )
    }
}

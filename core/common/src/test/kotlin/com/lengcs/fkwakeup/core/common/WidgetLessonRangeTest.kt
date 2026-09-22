package com.lengcs.fkwakeup.core.common

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class WidgetLessonRangeTest {

    private val wednesday = LocalDate.of(2026, 9, 23)

    @Test
    fun `今日加明天只返回两个连续日期`() {
        assertThat(WidgetLessonRangePicker.dates(WidgetLessonRange.TODAY_AND_TOMORROW, wednesday))
            .containsExactly(wednesday, wednesday.plusDays(1))
            .inOrder()
    }

    @Test
    fun `本周从周一到周日且包含过去日期`() {
        assertThat(WidgetLessonRangePicker.dates(WidgetLessonRange.THIS_WEEK, wednesday))
            .containsExactly(
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 22),
                wednesday,
                LocalDate.of(2026, 9, 24),
                LocalDate.of(2026, 9, 25),
                LocalDate.of(2026, 9, 26),
                LocalDate.of(2026, 9, 27),
            )
            .inOrder()
    }

    @Test
    fun `今日未结束过滤正好结束的课程`() {
        assertThat(
            WidgetLessonRangePicker.shouldKeep(
                WidgetLessonRange.TODAY_REMAINING,
                wednesday,
                endMinutes = 10 * 60,
                today = wednesday,
                nowMinutes = 10 * 60,
            ),
        ).isFalse()
    }

    @Test
    fun `今日未结束保留正在上的课程`() {
        assertThat(
            WidgetLessonRangePicker.shouldKeep(
                WidgetLessonRange.TODAY_REMAINING,
                wednesday,
                endMinutes = 10 * 60 + 45,
                today = wednesday,
                nowMinutes = 10 * 60,
            ),
        ).isTrue()
    }

    @Test
    fun `今日全部保留今天已经结束的课程`() {
        assertThat(
            WidgetLessonRangePicker.shouldKeep(
                WidgetLessonRange.TODAY_ALL,
                wednesday,
                endMinutes = 8 * 60,
                today = wednesday,
                nowMinutes = 10 * 60,
            ),
        ).isTrue()
    }

    @Test
    fun `范围以外的日期不会进入列表`() {
        assertThat(
            WidgetLessonRangePicker.shouldKeep(
                WidgetLessonRange.TODAY_AND_TOMORROW,
                wednesday.plusDays(2),
                endMinutes = 8 * 60,
                today = wednesday,
                nowMinutes = 0,
            ),
        ).isFalse()
    }
}

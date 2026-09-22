package com.lengcs.fkwakeup.widget.glance

import com.google.common.truth.Truth.assertThat
import com.lengcs.fkwakeup.core.model.CourseSession
import com.lengcs.fkwakeup.core.model.SessionDeliveryMode
import org.junit.Test

class WidgetConfigurationTest {
    @Test
    fun `height maps to one two and four row modes`() {
        assertThat(WidgetSizeMode.fromHeightDp(56)).isEqualTo(WidgetSizeMode.FOUR_BY_ONE)
        assertThat(WidgetSizeMode.fromHeightDp(112)).isEqualTo(WidgetSizeMode.FOUR_BY_TWO)
        assertThat(WidgetSizeMode.fromHeightDp(224)).isEqualTo(WidgetSizeMode.FOUR_BY_FOUR)
    }

    @Test
    fun `new instance uses remaining lessons and course color bar`() {
        val config = WidgetConfig()
        assertThat(config.range.name).isEqualTo("TODAY_REMAINING")
        assertThat(config.barMode.name).isEqualTo("COURSE_COLOR")
    }

    @Test
    fun `widget excludes live online sessions`() {
        val onsite = CourseSession(
            courseId = 1L,
            dayOfWeek = 1,
            startSection = 1,
            endSection = 2,
            weekSpec = "1-18",
        )
        val live = onsite.copy(deliveryMode = SessionDeliveryMode.LIVE_ONLINE)

        assertThat(onsite.isVisibleInWidget()).isTrue()
        assertThat(live.isVisibleInWidget()).isFalse()
    }
}

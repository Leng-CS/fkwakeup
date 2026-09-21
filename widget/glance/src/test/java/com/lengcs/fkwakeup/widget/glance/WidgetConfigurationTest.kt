package com.lengcs.fkwakeup.widget.glance

import com.google.common.truth.Truth.assertThat
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
}

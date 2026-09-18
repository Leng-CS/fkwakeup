package com.lengcs.fkwakeup.core.importer

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FieldNormalizerTest {

    @Test
    fun `星期数字`() {
        assertThat(FieldNormalizer.normalizeDayOfWeek("1")).isEqualTo(1)
        assertThat(FieldNormalizer.normalizeDayOfWeek("7")).isEqualTo(7)
        assertThat(FieldNormalizer.normalizeDayOfWeek("0")).isEqualTo(7) // 0 视为周日
        assertThat(FieldNormalizer.normalizeDayOfWeek("9")).isNull()
    }

    @Test
    fun `星期中文`() {
        assertThat(FieldNormalizer.normalizeDayOfWeek("周一")).isEqualTo(1)
        assertThat(FieldNormalizer.normalizeDayOfWeek("星期一")).isEqualTo(1)
        assertThat(FieldNormalizer.normalizeDayOfWeek("星期三")).isEqualTo(3)
        assertThat(FieldNormalizer.normalizeDayOfWeek("周日")).isEqualTo(7)
        assertThat(FieldNormalizer.normalizeDayOfWeek("星期天")).isEqualTo(7)
    }

    @Test
    fun `星期英文大小写不敏感`() {
        assertThat(FieldNormalizer.normalizeDayOfWeek("Mon")).isEqualTo(1)
        assertThat(FieldNormalizer.normalizeDayOfWeek("MONDAY")).isEqualTo(1)
        assertThat(FieldNormalizer.normalizeDayOfWeek("Sunday")).isEqualTo(7)
        assertThat(FieldNormalizer.normalizeDayOfWeek("wed")).isEqualTo(3)
    }

    @Test
    fun `星期兜底抠数字`() {
        assertThat(FieldNormalizer.normalizeDayOfWeek("星期3")).isEqualTo(3)
        assertThat(FieldNormalizer.normalizeDayOfWeek("第5天")).isEqualTo(5)
    }

    @Test
    fun `星期无法识别返回 null`() {
        assertThat(FieldNormalizer.normalizeDayOfWeek("abc")).isNull()
        assertThat(FieldNormalizer.normalizeDayOfWeek(null)).isNull()
        assertThat(FieldNormalizer.normalizeDayOfWeek("  ")).isNull()
    }

    @Test
    fun `节次区间各种写法`() {
        assertThat(FieldNormalizer.normalizeSectionRange("1-2")).isEqualTo(1 to 2)
        assertThat(FieldNormalizer.normalizeSectionRange("第1-2节")).isEqualTo(1 to 2)
        assertThat(FieldNormalizer.normalizeSectionRange("1-2节")).isEqualTo(1 to 2)
        assertThat(FieldNormalizer.normalizeSectionRange("1,2")).isEqualTo(1 to 2)
        assertThat(FieldNormalizer.normalizeSectionRange("3")).isEqualTo(3 to 3)
        assertThat(FieldNormalizer.normalizeSectionRange("第3节")).isEqualTo(3 to 3)
    }

    @Test
    fun `节次区间非法返回 null`() {
        assertThat(FieldNormalizer.normalizeSectionRange("abc")).isNull()
        assertThat(FieldNormalizer.normalizeSectionRange(null)).isNull()
        assertThat(FieldNormalizer.normalizeSectionRange("0-2")).isNull()
    }

    @Test
    fun `占位文本归一为 null`() {
        assertThat(FieldNormalizer.normalizeOptionalText("未知")).isNull()
        assertThat(FieldNormalizer.normalizeOptionalText("无")).isNull()
        assertThat(FieldNormalizer.normalizeOptionalText("-")).isNull()
        assertThat(FieldNormalizer.normalizeOptionalText("待定")).isNull()
        assertThat(FieldNormalizer.normalizeOptionalText(null)).isNull()
    }

    @Test
    fun `有效文本去首尾空白`() {
        assertThat(FieldNormalizer.normalizeOptionalText("  教三-301  ")).isEqualTo("教三-301")
    }

    @Test
    fun `周次表达式只去空白`() {
        assertThat(FieldNormalizer.normalizeWeekSpec("  1-16  ")).isEqualTo("1-16")
        assertThat(FieldNormalizer.normalizeWeekSpec("")).isNull()
    }
}

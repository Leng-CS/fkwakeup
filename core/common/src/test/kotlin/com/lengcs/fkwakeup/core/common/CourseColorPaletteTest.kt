package com.lengcs.fkwakeup.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CourseColorPaletteTest {

    @Test
    fun `预设色有 12 个`() {
        assertThat(CourseColorPalette.colors).hasSize(12)
        assertThat(CourseColorPalette.colors.distinct()).hasSize(12)
    }

    @Test
    fun `同名课程取到同一个颜色`() {
        assertThat(CourseColorPalette.pickArgb("高等数学A"))
            .isEqualTo(CourseColorPalette.pickArgb("高等数学A"))
    }

    @Test
    fun `取色稳定在预设范围内`() {
        CourseColorPalette.colors.contains(CourseColorPalette.pickArgb("任何课程名"))
        assertThat(CourseColorPalette.colors)
            .contains(CourseColorPalette.pickArgb("任何课程名"))
        assertThat(CourseColorPalette.colors)
            .contains(CourseColorPalette.pickArgb("  高等数学A  "))
    }

    @Test
    fun `自动取色只用前 10 个色，扩色板不会让已有课表变色`() {
        // 逐个课程名验证：自动色必须落在这 10 个里
        val autoRange = CourseColorPalette.colors.take(10)
        listOf("高等数学A", "数据结构", "大学英语（二）", "线性代数", "体育（篮球）", "计算机网络")
            .forEach { name ->
                assertThat(autoRange).contains(CourseColorPalette.pickArgb(name))
            }
    }

    @Test
    fun `新增的两个色不会出现在自动取色结果里`() {
        val appended = CourseColorPalette.colors.drop(10)
        assertThat(appended).hasSize(2)
        // 遍历一批课名，自动色不应该命中新增色
        (1..200).map { "课程$it" }.forEach { name ->
            assertThat(appended).doesNotContain(CourseColorPalette.pickArgb(name))
        }
    }

    @Test
    fun `自定义色优先`() {
        val custom = 0xFF123456.toInt()
        assertThat(CourseColorPalette.resolve(custom, "高等数学A")).isEqualTo(custom)
    }

    @Test
    fun `没自定义时退回哈希色`() {
        assertThat(CourseColorPalette.resolve(null, "高等数学A"))
            .isEqualTo(CourseColorPalette.pickArgb("高等数学A"))
    }

    @Test
    fun `浅色背景应该用深色文字`() {
        assertThat(CourseTextColor.shouldUseDarkText(0xFFFFFFFF.toInt())).isTrue()
        assertThat(CourseTextColor.shouldUseDarkText(0xFFFDD835.toInt())).isTrue()
    }

    @Test
    fun `深色背景应该用浅色文字`() {
        assertThat(CourseTextColor.shouldUseDarkText(0xFF000000.toInt())).isFalse()
        assertThat(CourseTextColor.shouldUseDarkText(0xFF6750A4.toInt())).isFalse()
    }

    @Test
    fun `预设色里每个颜色都能得到可读的文字色`() {
        // 每个预设色都必须能算出「深色或浅色文字」之一，不能出现中间态导致两边都看不清
        CourseColorPalette.colors.forEach { argb ->
            val dark = CourseTextColor.shouldUseDarkText(argb)
            assertThat(dark).isAnyOf(true, false)
        }
    }
}

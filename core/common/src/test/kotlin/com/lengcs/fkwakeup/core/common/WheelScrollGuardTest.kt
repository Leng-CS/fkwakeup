package com.lengcs.fkwakeup.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `WheelScrollGuard` 的单测（对应 Issue #17）。
 *
 * 重点守住的是**首帧不回写**：Compose 的 isScrollInProgress 初始就是 false，
 * 若直接据此判定「滚动已结束」，首次组合就会把外部选中值重置成第一项。
 */
class WheelScrollGuardTest {

    @Test
    fun `从未滚动过时即使最近项是 0 也不回写`() {
        // 这就是 issue #17 的核心场景：
        // 抽屉刚打开，selectedIndex=2（周三），但视口中心落在第 0 项上。
        // 修复前会回调 onSelectedChange(0)，把用户的值重置成「周一」。
        val guard = WheelScrollGuard()
        assertNull(guard.onScrollStateChanged(isScrollInProgress = false, nearestIndex = 0, selectedIndex = 2))
    }

    @Test
    fun `连续多次首帧式调用都不会回写`() {
        // 重组可能让 effect 重跑，守卫必须持续保持沉默
        val guard = WheelScrollGuard()
        repeat(5) {
            assertNull(
                guard.onScrollStateChanged(
                    isScrollInProgress = false,
                    nearestIndex = 0,
                    selectedIndex = 2,
                ),
            )
        }
    }

    @Test
    fun `滚动中不回写`() {
        val guard = WheelScrollGuard()
        assertNull(guard.onScrollStateChanged(isScrollInProgress = true, nearestIndex = 3, selectedIndex = 2))
    }

    @Test
    fun `滚动结束后回写最近项`() {
        val guard = WheelScrollGuard()
        guard.onScrollStateChanged(isScrollInProgress = true, nearestIndex = 2, selectedIndex = 2)
        assertEquals(
            5,
            guard.onScrollStateChanged(isScrollInProgress = false, nearestIndex = 5, selectedIndex = 2),
        )
    }

    @Test
    fun `落点没变时不回写，避免多余的 UI 状态更新`() {
        val guard = WheelScrollGuard()
        guard.onScrollStateChanged(isScrollInProgress = true, nearestIndex = 2, selectedIndex = 2)
        assertNull(guard.onScrollStateChanged(isScrollInProgress = false, nearestIndex = 2, selectedIndex = 2))
    }

    @Test
    fun `拿不到布局信息时不回写`() {
        val guard = WheelScrollGuard()
        guard.onScrollStateChanged(isScrollInProgress = true, nearestIndex = null, selectedIndex = 2)
        assertNull(guard.onScrollStateChanged(isScrollInProgress = false, nearestIndex = null, selectedIndex = 2))
    }

    @Test
    fun `回写后守卫复位，下一次滚动仍能正常回写`() {
        val guard = WheelScrollGuard()
        // 第一次滚动
        guard.onScrollStateChanged(isScrollInProgress = true, nearestIndex = 1, selectedIndex = 0)
        assertEquals(1, guard.onScrollStateChanged(isScrollInProgress = false, nearestIndex = 1, selectedIndex = 0))
        // 第二次滚动：若守卫没复位，这里会静默失效
        guard.onScrollStateChanged(isScrollInProgress = true, nearestIndex = 1, selectedIndex = 1)
        assertEquals(4, guard.onScrollStateChanged(isScrollInProgress = false, nearestIndex = 4, selectedIndex = 1))
    }

    @Test
    fun `回写过一次之后，再出现未滚动的调用仍不回写`() {
        // 回归保护：复位逻辑不能退化成「一旦滚过就永久放行」
        val guard = WheelScrollGuard()
        guard.onScrollStateChanged(isScrollInProgress = true, nearestIndex = 0, selectedIndex = 0)
        assertEquals(3, guard.onScrollStateChanged(isScrollInProgress = false, nearestIndex = 3, selectedIndex = 0))
        assertNull(guard.onScrollStateChanged(isScrollInProgress = false, nearestIndex = 0, selectedIndex = 3))
    }

    @Test
    fun `初始时的自动滚动对齐之后落在原值上不会触发回调`() {
        // WheelPicker 首次组合会 animateScrollToItem(selectedIndex)，
        // 这次滚动即便被算作一次真实滚动，落点也必然等于 selectedIndex，
        // 因此不应产生任何回写。
        val guard = WheelScrollGuard()
        guard.onScrollStateChanged(isScrollInProgress = true, nearestIndex = 4, selectedIndex = 4)
        assertNull(guard.onScrollStateChanged(isScrollInProgress = false, nearestIndex = 4, selectedIndex = 4))
    }
}

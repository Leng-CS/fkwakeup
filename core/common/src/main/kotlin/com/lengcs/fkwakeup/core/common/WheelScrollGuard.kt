package com.lengcs.fkwakeup.core.common

/**
 * 滚轮「滚动结束后把最近项回写为选中值」的判定守卫。
 *
 * **为什么需要它**：Compose 的 `LazyListState.isScrollInProgress` 初始值就是 `false`。
 * 如果直接用它判断「滚动已结束」，首次组合就会被误触发一次 —— 而那一刻视口中心
 * 通常恰好落在第 0 项上，于是会回调 `onSelectedChange(0)`，把外部传进来的选中值
 * **静默重置成第一项**（周一 / 第 1 节）。
 *
 * 这个守卫把「是否真的滚动过」记下来，只有「滚动过、且现在停了」才允许回写。
 * 纯逻辑、不依赖 Android，便于单测；由 UI 层把状态喂进来。
 *
 * 用法：
 * ```
 * val guard = remember { WheelScrollGuard() }
 * LaunchedEffect(state.isScrollInProgress) {
 *     guard.onScrollStateChanged(state.isScrollInProgress, nearestIndex, selectedIndex)
 *         ?.let(onSelectedChange)
 * }
 * ```
 */
class WheelScrollGuard {

    private var hasScrolled = false

    /**
     * @param isScrollInProgress 当前是否正在滚动
     * @param nearestIndex 离视口中心最近的一项下标；拿不到布局信息时传 null
     * @param selectedIndex 外部已知的选中下标
     * @return 需要回写的新下标；返回 null 表示**不要回写**
     */
    fun onScrollStateChanged(
        isScrollInProgress: Boolean,
        nearestIndex: Int?,
        selectedIndex: Int,
    ): Int? {
        if (isScrollInProgress) {
            hasScrolled = true
            return null
        }

        // 从未滚动过（典型场景：首次组合）→ 不回写。
        // 这一步就是本类存在的意义。
        if (!hasScrolled) return null

        hasScrolled = false

        // 没有布局信息、或落点没变，都不必回调，避免多余的 UI 状态更新
        if (nearestIndex == null || nearestIndex == selectedIndex) return null

        return nearestIndex
    }
}

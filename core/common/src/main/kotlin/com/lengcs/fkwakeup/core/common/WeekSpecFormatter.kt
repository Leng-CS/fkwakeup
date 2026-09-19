package com.lengcs.fkwakeup.core.common

/**
 * 周次集合 → 周次表达式字符串。
 *
 * 存储格式仍然是字符串（`weekSpec` 是导入格式 v1.0 的契约字段，不能改结构），
 * 但 UI 改成点选后需要把用户勾选的周反算成表达式。
 *
 * 与 [WeekSpecParser] 互为逆运算，有往返测试守着。
 */
object WeekSpecFormatter {

    /**
     * @param weeks 用户勾选的周（1 起）
     * @param totalWeeks 学期总周数，用于丢弃越界值
     * @return 表达式；空集合返回空串（调用方需提示用户至少选一周）
     */
    fun format(weeks: Set<Int>, totalWeeks: Int): String {
        val sorted = weeks.filter { it in 1..totalWeeks }.distinct().sorted()
        if (sorted.isEmpty()) return ""

        // 先判断是不是「等距步长 2」——也就是纯单周或纯双周
        if (sorted.size >= 3 && isStepTwo(sorted)) {
            val from = sorted.first()
            val to = sorted.last()
            // 注意用 ${} 包住，否则 `to单` 会被当成一个标识符（Kotlin 允许中文标识符）
            return if (from % 2 == 1) "$from-${to}单" else "$from-${to}双"
        }

        // 否则按连续性分组：1-9,11-18
        return sorted.groupConsecutive().joinToString(",") { (from, to) ->
            if (from == to) "$from" else "$from-$to"
        }
    }

    /**
     * 连续区间数量，用于 UI 上判断是否需要提示「周次不连续」。
     */
    fun segmentCount(weeks: Set<Int>, totalWeeks: Int): Int {
        val sorted = weeks.filter { it in 1..totalWeeks }.distinct().sorted()
        if (sorted.isEmpty()) return 0
        if (sorted.size >= 3 && isStepTwo(sorted)) return 1
        return sorted.groupConsecutive().size
    }

    private fun isStepTwo(sorted: List<Int>): Boolean {
        for (i in 1 until sorted.size) {
            if (sorted[i] - sorted[i - 1] != 2) return false
        }
        return true
    }

    /** [1,2,3,5,6] -> [(1,3), (5,6)] */
    private fun List<Int>.groupConsecutive(): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        var from = first()
        var prev = first()
        for (i in 1 until size) {
            val v = this[i]
            if (v == prev + 1) {
                prev = v
            } else {
                result += from to prev
                from = v
                prev = v
            }
        }
        result += from to prev
        return result
    }
}

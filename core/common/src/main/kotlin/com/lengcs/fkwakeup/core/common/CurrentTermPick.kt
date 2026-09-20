package com.lengcs.fkwakeup.core.common

/**
 * 「当前学期」的选择规则。
 *
 * 这是全应用**唯一**一处定义「该用哪个学期」的地方，抽成纯逻辑是为了能单测 ——
 * 真正的数据源（DataStore 里的 `currentTermId`、Room 里的学期列表）由
 * `core:database` 的 `CurrentTermProvider` 提供。
 *
 * 规则：**优先用用户配置的学期**（`configuredTermId > 0`）；
 * 只有当它无效时（首次启动、或那个学期已经被删掉）才兜底。
 *
 * ## 兜底为什么是「列表第一个」
 *
 * `fallbackIds` 由 `TermDao.observeActive()` 提供，它的排序是
 * `ORDER BY start_monday_epoch_day DESC` —— 即**开学日期由晚到早**。
 * 所以第一个就是「最近的一个学期」，作为兜底是合理的启发式。
 *
 * ## ⚠️ 不要拿它当「当前学期」用
 *
 * `pick(0, ids)` 的结果是「开学日期最晚的学期」，**不等于**「用户当前正在看的学期」。
 * 想拿当前学期，请调 `CurrentTermProvider.observeCurrentTerm()` /
 * `resolveCurrentTermId()`。混用会导致课程被写进 / 搬到别的学期（见 CHANGELOG #26）。
 */
object CurrentTermPick {

    /**
     * @param configuredTermId 用户配置的学期 ID；`<= 0` 表示没有配置或无效
     * @param fallbackIds 兜底候选（按开学日期由晚到早）；通常来自 `observeTerms()`
     * @return 该用哪个学期；一个学期都没有时返回 null
     */
    fun pick(configuredTermId: Long, fallbackIds: List<Long>): Long? =
        configuredTermId.takeIf { it > 0 } ?: fallbackIds.firstOrNull()
}

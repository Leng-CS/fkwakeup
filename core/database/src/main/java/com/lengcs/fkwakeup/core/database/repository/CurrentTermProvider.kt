package com.lengcs.fkwakeup.core.database.repository

import com.lengcs.fkwakeup.core.common.CurrentTermPick
import com.lengcs.fkwakeup.core.datastore.SettingsRepository
import com.lengcs.fkwakeup.core.model.Term
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 「当前学期」的响应式来源 —— **全应用唯一的权威定义**。
 *
 * 之前各页面只在 init 里读一次 `settings.currentTermId`，然后订阅那个学期的数据流 ——
 * 于是在学期管理页切换学期后，周视图和课程管理页不会跟着变，必须重启应用才生效。
 * 这里把「当前学期」本身做成 Flow：只要 DataStore 里的 currentTermId 变了，下游就会重新订阅。
 *
 * ## ⚠️ 想拿当前学期，只能用这里的 API
 *
 * **不要写 `termRepository.observeTerms().first()`** —— 那取到的是
 * 「开学日期最晚的学期」（DAO 按 `start_monday_epoch_day DESC` 排序），
 * 它只在「没有任何配置」时才等于当前学期。混用会导致课程被写进别的学期、
 * 甚至把已有课程的 `term_id` 改掉使其从当前学期消失（见 `docs/CHANGELOG.md` 的 #26）。
 *
 * 选择规则本身抽在 `core:common` 的 [CurrentTermPick]（纯逻辑、可单测）。
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Singleton
class CurrentTermProvider @Inject constructor(
    private val termRepository: TermRepository,
    private val settingsRepository: SettingsRepository,
) {

    /** 当前学期；没有任何学期时为 null。切换学期后会自动重新发射。 */
    fun observeCurrentTerm(): Flow<Term?> =
        settingsRepository.settings
            .map { it.currentTermId }
            .distinctUntilChanged()
            .mapLatest { id -> resolve(id) }
            .distinctUntilChanged()

    /** 当前学期 ID（DataStore 里的原始值，未经兜底）；没有时为 -1 */
    fun observeCurrentTermId(): Flow<Long> =
        settingsRepository.settings
            .map { it.currentTermId }
            .distinctUntilChanged()

    /**
     * 一次性取「当前学期」。
     * 适合在 `init` 里读一次、不需要跟随切换的场景。
     */
    suspend fun currentTerm(): Term? =
        resolveCurrentTermId()?.let { termRepository.getTerm(it) }

    /**
     * 一次性取「当前学期 ID」。
     *
     * 优先用配置值；配置无效（首次启动、或那个学期已被删掉）时退回
     * 「开学日期最晚的未归档学期」；一个学期都没有时返回 null。
     */
    suspend fun resolveCurrentTermId(): Long? =
        CurrentTermPick.pick(
            configuredTermId = settingsRepository.settings.first().currentTermId,
            fallbackIds = activeTermIds(),
        )

    private suspend fun resolve(id: Long): Term? =
        CurrentTermPick.pick(configuredTermId = id, fallbackIds = activeTermIds())
            ?.let { termRepository.getTerm(it) }

    /** 未归档学期的 ID，按「开学日期由晚到早」——这正是兜底要的顺序 */
    private suspend fun activeTermIds(): List<Long> =
        termRepository.observeTerms().first().map { it.id }
}

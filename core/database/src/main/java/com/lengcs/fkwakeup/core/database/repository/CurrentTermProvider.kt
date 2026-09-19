package com.lengcs.fkwakeup.core.database.repository

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
 * 「当前学期」的响应式来源。
 *
 * 之前各页面只在 init 里读一次 `settings.currentTermId`，然后订阅那个学期的数据流 ——
 * 于是在学期管理页切换学期后，周视图和课程管理页不会跟着变，必须重启应用才生效。
 * 这里把「当前学期」本身做成 Flow：只要 DataStore 里的 currentTermId 变了，下游就会重新订阅。
 */
@Singleton
class CurrentTermProvider @Inject constructor(
    private val termRepository: TermRepository,
    private val settingsRepository: SettingsRepository,
) {

    /** 当前学期；没有任何学期时为 null */
    fun observeCurrentTerm(): Flow<Term?> =
        settingsRepository.settings
            .map { it.currentTermId }
            .distinctUntilChanged()
            .mapLatest { id -> resolve(id) }
            .distinctUntilChanged()

    /** 当前学期 ID；没有时为 -1 */
    fun observeCurrentTermId(): Flow<Long> =
        settingsRepository.settings
            .map { it.currentTermId }
            .distinctUntilChanged()

    private suspend fun resolve(id: Long): Term? {
        // 没有指定（首次启动、或当前学期被删掉）时退回最新的未归档学期
        val resolved = if (id > 0) {
            id
        } else {
            termRepository.observeTerms().first().firstOrNull()?.id
        }
        return resolved?.let { termRepository.getTerm(it) }
    }
}

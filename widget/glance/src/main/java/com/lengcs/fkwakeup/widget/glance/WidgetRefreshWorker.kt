package com.lengcs.fkwakeup.widget.glance

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * 兜底刷新。
 *
 * 触发路径有三条（见 [WidgetRefreshScheduler]）：15 分钟周期兜底、
 * 课程边界的 OneTime 定时（#31）、系统广播。
 * 每次刷新后都按最新数据重排下一个边界任务 —— 任务链自动续上。
 *
 * 故意不使用 @HiltWorker：本 Worker 没有任何需要注入的依赖
 * （数据是通过 Hilt EntryPoint 从 Application 取的），
 * 省掉 hilt-work 依赖与 Application 的 WorkerFactory 配置。
 */
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result =
        try {
            WidgetRefreshScheduler.refreshNow(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
}

package com.lengcs.fkwakeup.widget.glance

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * 兜底刷新。WorkManager 的最小周期就是 15 分钟，
 * 所以「改课立刻更新」必须靠写库后主动调用 [FkwakeupWidget.refresh]，
 * 这里只负责在进程被杀、广播漏掉的情况下把内容拉回正确。
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
            FkwakeupWidget.refresh(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
}

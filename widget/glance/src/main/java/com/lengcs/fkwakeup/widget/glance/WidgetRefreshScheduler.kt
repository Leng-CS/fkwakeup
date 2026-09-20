package com.lengcs.fkwakeup.widget.glance

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * 小组件刷新调度。
 *
 * 单一机制都不够，所以要组合（主开发文档 7.3）：
 * 1. 写库 / 改课时主动 [refreshNow] —— 保证即时性
 * 2. **课程边界精确刷新（#31）** —— 「最近课程」列表会随时间变化：某节课上完要移出、
 *    下一节开始要标「正在上」。数据加载时算出下一个边界时刻（未开始课程的开始时刻 /
 *    进行中课程的结束时刻，取最早者），用 OneTimeWork 定时到那一刻刷新
 * 3. WorkManager 15 分钟周期 —— 进程被杀后的兜底
 * 4. 日期 / 时间 / 时区 / 开机 / 升级广播 —— 跨天、跨时区、重启后正确
 *
 * 注意：`ACTION_TIME_TICK` 无法静态注册，做不了分钟级刷新，不要依赖它。
 */
object WidgetRefreshScheduler {

    private const val UNIQUE_WORK_NAME = "fkwakeup_widget_refresh"
    private const val UNIQUE_BOUNDARY_WORK_NAME = "fkwakeup_widget_boundary_refresh"

    /**
     * 立即刷新一次，并按最新数据重排课程边界任务。
     * 写库 / 改课 / 跨天广播都走这里，调用方不需要关心边界调度。
     */
    suspend fun refreshNow(context: Context) {
        refreshAllWidgets(context)
        scheduleBoundary(context, WidgetLoader.load(context)?.nextBoundary)
    }

    /**
     * 定时到 [boundary]（今天的某个时刻）刷新一次。
     *
     * - 时刻已过或为 null → 取消已有任务（今天没有剩余边界，交给周期任务与跨天广播）
     * - 每次刷新后 Worker 会再次计算下一个边界，任务链自动续上
     * - 用 [ExistingWorkPolicy.REPLACE]：数据变了边界可能变，旧任务作废
     */
    suspend fun scheduleBoundary(context: Context, boundary: LocalDateTime?) {
        val workManager = WorkManager.getInstance(context)
        if (boundary == null) {
            workManager.cancelUniqueWork(UNIQUE_BOUNDARY_WORK_NAME)
            return
        }
        val delay = Duration.between(LocalDateTime.now(), boundary)
        if (delay.isNegative || delay.isZero) return

        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(
            UNIQUE_BOUNDARY_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    /** 登记 15 分钟周期任务（已存在则保留） */
    fun enqueue(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}

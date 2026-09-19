package com.lengcs.fkwakeup.widget.glance

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 小组件刷新调度。
 *
 * 单一机制都不够，所以要组合（主开发文档 7.3）：
 * 1. 写库 / 改课时主动 [refreshNow] —— 保证即时性
 * 2. WorkManager 15 分钟周期 —— 进程被杀后的兜底
 * 3. 日期 / 时间 / 时区 / 开机 / 升级广播 —— 跨天、跨时区、重启后正确
 *
 * 注意：`ACTION_TIME_TICK` 无法静态注册，做不了分钟级刷新，不要依赖它。
 */
object WidgetRefreshScheduler {

    private const val UNIQUE_WORK_NAME = "fkwakeup_widget_refresh"

    /** 立即刷新一次 */
    suspend fun refreshNow(context: Context) {
        FkwakeupWidget.refresh(context)
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

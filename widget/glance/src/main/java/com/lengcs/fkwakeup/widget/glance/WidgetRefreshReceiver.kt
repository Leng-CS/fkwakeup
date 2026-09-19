package com.lengcs.fkwakeup.widget.glance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 系统事件触发的刷新。
 *
 * 覆盖：跨天（DATE_CHANGED）、系统时间改动（TIME_CHANGED）、
 * 跨时区（TIMEZONE_CHANGED）、重启后（BOOT_COMPLETED）、
 * 应用升级后（MY_PACKAGE_REPLACED，否则小组件会一直停在旧布局）。
 */
class WidgetRefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val appContext = context.applicationContext
                WidgetRefreshScheduler.refreshNow(appContext)
                WidgetRefreshScheduler.enqueue(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

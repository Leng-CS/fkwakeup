package com.lengcs.fkwakeup

import android.app.Application
import com.lengcs.fkwakeup.widget.glance.WidgetRefreshScheduler
import com.lengcs.fkwakeup.core.reminder.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class FkwakeupApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 登记小组件的 15 分钟兜底刷新。
        //
        // **必须放到后台线程**：WorkManager 的初始化比较重，同步在 onCreate 里做
        // 会把冷启动拖到超时（实测在模拟器上直接 ANR: "failed to complete startup"）。
        // 兜底刷新晚几百毫秒注册完全无所谓。
        CoroutineScope(Dispatchers.Default).launch {
            WidgetRefreshScheduler.enqueue(this@FkwakeupApplication)
            ReminderScheduler.requestRebuild(this@FkwakeupApplication)
        }
    }
}

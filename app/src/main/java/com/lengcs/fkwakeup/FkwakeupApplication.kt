package com.lengcs.fkwakeup

import android.app.Application
import com.lengcs.fkwakeup.widget.glance.WidgetRefreshScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FkwakeupApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 登记小组件的 15 分钟兜底刷新（已存在则保留）
        WidgetRefreshScheduler.enqueue(this)
    }
}

package com.lengcs.fkwakeup.core.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = ReminderNotification.show(context, intent.toReminderData())
}

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = ReminderScheduler.requestRebuild(context)
}

class ReminderDeliveryWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        ReminderNotification.show(applicationContext, inputData)
        return Result.success()
    }
}

class ReminderRebuildWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        EntryPointAccessors.fromApplication(applicationContext, ReminderSchedulerEntryPoint::class.java).scheduler().rebuild()
    }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ReminderSchedulerEntryPoint {
    fun scheduler(): ReminderScheduler
}

private fun Intent.toReminderData(): Data = Data.Builder().also { builder ->
    getStringExtra("alarmKey")?.let { builder.putString("alarmKey", it) }
    builder.putLong("courseId", getLongExtra("courseId", -1L))
    getStringExtra("courseName")?.let { builder.putString("courseName", it) }
    getStringExtra("kind")?.let { builder.putString("kind", it) }
    getStringExtra("occurrenceDate")?.let { builder.putString("occurrenceDate", it) }
    getStringExtra("startAt")?.let { builder.putString("startAt", it) }
    getStringExtra("place")?.let { builder.putString("place", it) }
}.build()

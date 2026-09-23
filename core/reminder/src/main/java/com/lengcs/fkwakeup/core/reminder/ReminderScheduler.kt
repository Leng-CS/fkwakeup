package com.lengcs.fkwakeup.core.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.getSystemService
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.lengcs.fkwakeup.core.common.PlannedReminder
import com.lengcs.fkwakeup.core.common.ReminderPlanner
import com.lengcs.fkwakeup.core.database.repository.CourseRepository
import com.lengcs.fkwakeup.core.database.repository.CurrentTermProvider
import com.lengcs.fkwakeup.core.database.repository.ReminderRepository
import com.lengcs.fkwakeup.core.database.repository.TermRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val currentTermProvider: CurrentTermProvider,
    private val courseRepository: CourseRepository,
    private val termRepository: TermRepository,
    private val reminderRepository: ReminderRepository,
) {
    suspend fun rebuild(now: LocalDateTime = LocalDateTime.now()) {
        cancelKnown()
        val term = currentTermProvider.currentTerm() ?: return
        val planned = ReminderPlanner.plan(
            term = term,
            courses = courseRepository.observeCourses(term.id).first(),
            sections = termRepository.getSections(term.id),
            rules = reminderRepository.getAllRules(),
            overrides = reminderRepository.getAllOverrides(),
        ).filter { it.triggerAt >= now.minusMinutes(MAX_LATE_MINUTES) }
        planned.forEach { schedule(it, now) }
        prefs().edit().putStringSet(KEY_ALARMS, planned.map { it.alarmKey }.toSet()).apply()
    }

    fun canScheduleExactly(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService<AlarmManager>()?.canScheduleExactAlarms() == true
    }

    private fun schedule(item: PlannedReminder, now: LocalDateTime) {
        val delay = Duration.between(now, item.triggerAt).toMillis().coerceAtLeast(0L)
        val data = item.toData()
        if (canScheduleExactly()) {
            val triggerMillis = System.currentTimeMillis() + delay
            context.getSystemService<AlarmManager>()?.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                alarmIntent(item.alarmKey, data),
            )
        } else {
            val request = OneTimeWorkRequestBuilder<ReminderDeliveryWorker>()
                .setInputData(data)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(workName(item.alarmKey), androidx.work.ExistingWorkPolicy.REPLACE, request)
        }
    }

    private fun cancelKnown() {
        prefs().getStringSet(KEY_ALARMS, emptySet()).orEmpty().forEach { key ->
            context.getSystemService<AlarmManager>()?.cancel(alarmIntent(key, Data.EMPTY))
            WorkManager.getInstance(context).cancelUniqueWork(workName(key))
        }
        prefs().edit().remove(KEY_ALARMS).apply()
    }

    private fun alarmIntent(key: String, data: Data): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            this.data = Uri.parse("fkwakeup://reminder/alarm/${Uri.encode(key)}")
            putExtra("alarmKey", data.getString("alarmKey"))
            putExtra("courseId", data.getLong("courseId", -1L))
            putExtra("courseName", data.getString("courseName"))
            putExtra("kind", data.getString("kind"))
            putExtra("occurrenceDate", data.getString("occurrenceDate"))
            putExtra("startAt", data.getString("startAt"))
            putExtra("place", data.getString("place"))
        }
        return PendingIntent.getBroadcast(context, key.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun prefs() = context.getSharedPreferences("reminder_scheduler", Context.MODE_PRIVATE)

    companion object {
        const val MAX_LATE_MINUTES = 15L
        private const val KEY_ALARMS = "alarm_keys"
        internal fun workName(key: String) = "course-reminder:$key"
        fun requestRebuild(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "course-reminder-rebuild",
                androidx.work.ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ReminderRebuildWorker>().build(),
            )
        }
    }
}

internal fun PlannedReminder.toData(): Data = Data.Builder()
    .putString("alarmKey", alarmKey)
    .putLong("courseId", courseId)
    .putString("courseName", courseName)
    .putString("kind", kind.name)
    .putString("occurrenceDate", occurrenceDate.toString())
    .putString("startAt", startAt?.toString())
    .putString("place", place)
    .build()

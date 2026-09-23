package com.lengcs.fkwakeup.core.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import com.lengcs.fkwakeup.core.model.ReminderOccurrenceKind
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal object ReminderNotification {
    private const val CHANNEL_ID = "course_reminders"

    fun show(context: Context, data: Data) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        createChannel(context)
        val payload = ReminderPayload.from(data) ?: return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(payload.title())
            .setContentText(payload.body())
            .setContentIntent(payload.contentIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(payload.alarmKey.hashCode(), notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "课程提醒", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "上课、网课开放和截止时间提醒"
            },
        )
    }
}

private data class ReminderPayload(
    val alarmKey: String,
    val courseId: Long,
    val courseName: String,
    val kind: ReminderOccurrenceKind,
    val occurrenceDate: LocalDate,
    val startAt: LocalDateTime?,
    val place: String?,
) {
    fun title(): String = when (kind) {
        ReminderOccurrenceKind.SESSION -> "课程即将开始 · $courseName"
        ReminderOccurrenceKind.ASYNC_OPEN -> "网课今日开放 · $courseName"
        ReminderOccurrenceKind.ASYNC_DEADLINE -> "网课即将截止 · $courseName"
    }

    fun body(): String = when (kind) {
        ReminderOccurrenceKind.SESSION -> listOfNotNull(startAt?.format(TIME_FORMAT), place).joinToString(" · ")
        ReminderOccurrenceKind.ASYNC_OPEN -> "点击查看课程详情"
        ReminderOccurrenceKind.ASYNC_DEADLINE -> "请及时完成课程内容"
    }

    fun contentIntent(context: Context): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
        launch.data = Uri.Builder().scheme("fkwakeup").authority("reminder")
            .appendPath(if (kind == ReminderOccurrenceKind.SESSION) "schedule" else "course")
            .appendQueryParameter("courseId", courseId.toString())
            .appendQueryParameter("date", occurrenceDate.toString()).build()
        launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(context, alarmKey.hashCode(), launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
        fun from(data: Data): ReminderPayload? = runCatching {
            ReminderPayload(
                alarmKey = requireNotNull(data.getString("alarmKey")),
                courseId = data.getLong("courseId", -1L).also { require(it >= 0L) },
                courseName = requireNotNull(data.getString("courseName")),
                kind = ReminderOccurrenceKind.valueOf(requireNotNull(data.getString("kind"))),
                occurrenceDate = LocalDate.parse(requireNotNull(data.getString("occurrenceDate"))),
                startAt = data.getString("startAt")?.let(LocalDateTime::parse),
                place = data.getString("place"),
            )
        }.getOrNull()
    }
}

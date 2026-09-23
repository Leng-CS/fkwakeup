package com.lengcs.fkwakeup.core.database.mapper

import com.lengcs.fkwakeup.core.database.dao.CourseWithSessions as DbCourseWithSessions
import com.lengcs.fkwakeup.core.database.entity.CourseEntity
import com.lengcs.fkwakeup.core.database.entity.CourseSessionEntity
import com.lengcs.fkwakeup.core.database.entity.OnlineCourseWindowEntity
import com.lengcs.fkwakeup.core.database.entity.SectionTemplateEntity
import com.lengcs.fkwakeup.core.database.entity.TermEntity
import com.lengcs.fkwakeup.core.model.Course
import com.lengcs.fkwakeup.core.model.CourseSession
import com.lengcs.fkwakeup.core.model.CourseWithSessions
import com.lengcs.fkwakeup.core.model.OnlineCourseWindow
import com.lengcs.fkwakeup.core.model.CourseReminderRule
import com.lengcs.fkwakeup.core.model.RecurringReminderMode
import com.lengcs.fkwakeup.core.model.ReminderOccurrenceKind
import com.lengcs.fkwakeup.core.model.ReminderOccurrenceOverride
import com.lengcs.fkwakeup.core.database.entity.CourseReminderRuleEntity
import com.lengcs.fkwakeup.core.database.entity.ReminderOccurrenceOverrideEntity
import com.lengcs.fkwakeup.core.model.SectionTemplate
import com.lengcs.fkwakeup.core.model.Term
import com.lengcs.fkwakeup.core.model.SessionDeliveryMode
import java.time.Instant
import java.time.LocalDate

fun TermEntity.toDomain(): Term =
    Term(
        id = id,
        name = name,
        startMonday = LocalDate.ofEpochDay(startMondayEpochDay),
        totalWeeks = totalWeeks,
        isArchived = isArchived,
        createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    )

fun Term.toEntity(): TermEntity =
    TermEntity(
        id = id,
        name = name,
        startMondayEpochDay = startMonday.toEpochDay(),
        totalWeeks = totalWeeks,
        isArchived = isArchived,
        createdAtEpochMillis = createdAt.toEpochMilli(),
    )

fun SectionTemplateEntity.toDomain(): SectionTemplate =
    SectionTemplate(
        termId = termId,
        index = index,
        startMinutes = startMinutes,
        endMinutes = endMinutes,
    )

fun SectionTemplate.toEntity(): SectionTemplateEntity =
    SectionTemplateEntity(
        termId = termId,
        index = index,
        startMinutes = startMinutes,
        endMinutes = endMinutes,
    )

fun CourseEntity.toDomain(): Course =
    Course(
        id = id,
        termId = termId,
        name = name,
        teacher = teacher,
        colorArgb = colorArgb,
        note = note,
    )

fun Course.toEntity(): CourseEntity =
    CourseEntity(
        id = id,
        termId = termId,
        name = name,
        teacher = teacher,
        colorArgb = colorArgb,
        note = note,
        createdAtEpochMillis = System.currentTimeMillis(),
    )

fun CourseSessionEntity.toDomain(): CourseSession =
    CourseSession(
        id = id,
        courseId = courseId,
        dayOfWeek = dayOfWeek,
        startSection = startSection,
        endSection = endSection,
        weekSpec = weekSpec,
        location = location,
        note = note,
        deliveryMode = runCatching { SessionDeliveryMode.valueOf(deliveryMode) }
            .getOrDefault(SessionDeliveryMode.ONSITE),
        onlinePlatform = onlinePlatform,
        onlineUrl = onlineUrl,
    )

fun CourseSession.toEntity(): CourseSessionEntity =
    CourseSessionEntity(
        id = id,
        courseId = courseId,
        dayOfWeek = dayOfWeek,
        startSection = startSection,
        endSection = endSection,
        weekSpec = weekSpec,
        location = location,
        note = note,
        deliveryMode = deliveryMode.name,
        onlinePlatform = onlinePlatform,
        onlineUrl = onlineUrl,
    )

fun OnlineCourseWindowEntity.toDomain(): OnlineCourseWindow =
    OnlineCourseWindow(
        id = id,
        courseId = courseId,
        startDate = LocalDate.ofEpochDay(startDateEpochDay),
        endDate = LocalDate.ofEpochDay(endDateEpochDay),
        platform = platform,
        url = url,
        note = note,
    )

fun OnlineCourseWindow.toEntity(): OnlineCourseWindowEntity =
    OnlineCourseWindowEntity(
        id = id,
        courseId = courseId,
        startDateEpochDay = startDate.toEpochDay(),
        endDateEpochDay = endDate.toEpochDay(),
        platform = platform,
        url = url,
        note = note,
    )

fun CourseReminderRuleEntity.toDomain() = CourseReminderRule(
    courseId = courseId,
    recurringMode = runCatching { RecurringReminderMode.valueOf(recurringMode) }.getOrDefault(RecurringReminderMode.NONE),
    primaryMinutesBefore = primaryMinutesBefore,
    secondaryMinutesBefore = secondaryMinutesBefore,
    asyncOpenEnabled = asyncOpenEnabled,
    asyncOpenMinutesOfDay = asyncOpenMinutesOfDay,
    asyncDeadlineEnabled = asyncDeadlineEnabled,
    asyncDeadlineDaysBefore = asyncDeadlineDaysBefore,
    asyncDeadlineMinutesOfDay = asyncDeadlineMinutesOfDay,
)

fun CourseReminderRule.toEntity() = CourseReminderRuleEntity(
    courseId, recurringMode.name, primaryMinutesBefore, secondaryMinutesBefore,
    asyncOpenEnabled, asyncOpenMinutesOfDay, asyncDeadlineEnabled,
    asyncDeadlineDaysBefore, asyncDeadlineMinutesOfDay,
)

fun ReminderOccurrenceOverrideEntity.toDomain() = ReminderOccurrenceOverride(
    occurrenceKey, courseId,
    runCatching { ReminderOccurrenceKind.valueOf(kind) }.getOrDefault(ReminderOccurrenceKind.SESSION),
    sourceId, weekNumber, enabled, primaryMinutesBefore, secondaryMinutesBefore,
)

fun ReminderOccurrenceOverride.toEntity() = ReminderOccurrenceOverrideEntity(
    occurrenceKey, courseId, kind.name, sourceId, weekNumber, enabled,
    primaryMinutesBefore, secondaryMinutesBefore,
)

fun DbCourseWithSessions.toDomain(): CourseWithSessions =
    CourseWithSessions(
        course = course.toDomain(),
        sessions = sessions.map { it.toDomain() },
        onlineWindows = onlineWindows.map { it.toDomain() },
    )

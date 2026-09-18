package com.lengcs.fkwakeup.core.database.mapper

import com.lengcs.fkwakeup.core.database.dao.CourseWithSessions as DbCourseWithSessions
import com.lengcs.fkwakeup.core.database.entity.CourseEntity
import com.lengcs.fkwakeup.core.database.entity.CourseSessionEntity
import com.lengcs.fkwakeup.core.database.entity.SectionTemplateEntity
import com.lengcs.fkwakeup.core.database.entity.TermEntity
import com.lengcs.fkwakeup.core.model.Course
import com.lengcs.fkwakeup.core.model.CourseSession
import com.lengcs.fkwakeup.core.model.CourseWithSessions
import com.lengcs.fkwakeup.core.model.SectionTemplate
import com.lengcs.fkwakeup.core.model.Term
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
    )

fun DbCourseWithSessions.toDomain(): CourseWithSessions =
    CourseWithSessions(
        course = course.toDomain(),
        sessions = sessions.map { it.toDomain() },
    )

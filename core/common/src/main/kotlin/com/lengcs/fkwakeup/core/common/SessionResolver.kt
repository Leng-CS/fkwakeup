package com.lengcs.fkwakeup.core.common

import com.lengcs.fkwakeup.core.model.CourseSession
import com.lengcs.fkwakeup.core.model.CourseSessionResolved
import com.lengcs.fkwakeup.core.model.CourseWithSessions

/**
 * 把周次表达式解析成具体的周集合，供课表渲染时过滤。
 */
fun CourseSession.resolveWeeks(totalWeeks: Int): CourseSessionResolved =
    CourseSessionResolved(
        session = this,
        weeks = WeekSpecParser.parse(weekSpec, totalWeeks),
    )

fun CourseSession.resolveWeeksOrEmpty(totalWeeks: Int): CourseSessionResolved =
    CourseSessionResolved(
        session = this,
        weeks = WeekSpecParser.parseOrNull(weekSpec, totalWeeks) ?: emptySet(),
    )

/**
 * 只保留在 [week] 上课的时间段。
 */
fun CourseWithSessions.sessionsInWeek(week: Int, totalWeeks: Int): List<CourseSession> =
    sessions.filter { session ->
        val weeks = WeekSpecParser.parseOrNull(session.weekSpec, totalWeeks) ?: emptySet()
        week in weeks
    }

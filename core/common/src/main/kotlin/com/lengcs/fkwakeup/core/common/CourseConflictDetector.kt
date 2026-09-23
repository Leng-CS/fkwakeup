package com.lengcs.fkwakeup.core.common

import com.lengcs.fkwakeup.core.model.CourseSession

data class ConflictCandidate(
    val courseName: String,
    val session: CourseSession,
)

data class CourseConflict(
    val first: ConflictCandidate,
    val second: ConflictCandidate,
    val overlappingWeeks: Set<Int>,
) {
    fun message(): String = buildString {
        append(first.courseName).append(" 与 ").append(second.courseName)
        append("在").append(WEEKDAYS[first.session.dayOfWeek - 1])
        append("第").append(maxOf(first.session.startSection, second.session.startSection))
        append("–").append(minOf(first.session.endSection, second.session.endSection)).append("节冲突")
        append("（第").append(WeekSpecFormatter.format(overlappingWeeks, overlappingWeeks.maxOrNull() ?: 1)).append("周）")
    }
}

object CourseConflictDetector {
    fun detect(candidates: List<ConflictCandidate>, totalWeeks: Int): List<CourseConflict> {
        val weeksByIndex = candidates.map { WeekSpecParser.parseOrNull(it.session.weekSpec, totalWeeks).orEmpty() }
        return buildList {
            candidates.indices.forEach { leftIndex ->
                ((leftIndex + 1) until candidates.size).forEach { rightIndex ->
                    val left = candidates[leftIndex]
                    val right = candidates[rightIndex]
                    if (samePersistedSession(left.session, right.session)) return@forEach
                    if (left.session.dayOfWeek != right.session.dayOfWeek) return@forEach
                    if (!sectionsOverlap(left.session, right.session)) return@forEach
                    val overlappingWeeks = weeksByIndex[leftIndex] intersect weeksByIndex[rightIndex]
                    if (overlappingWeeks.isNotEmpty()) add(CourseConflict(left, right, overlappingWeeks))
                }
            }
        }
    }

    private fun samePersistedSession(left: CourseSession, right: CourseSession): Boolean =
        left.id > 0L && left.id == right.id

    private fun sectionsOverlap(left: CourseSession, right: CourseSession): Boolean =
        left.startSection <= right.endSection && right.startSection <= left.endSection
}

private val WEEKDAYS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

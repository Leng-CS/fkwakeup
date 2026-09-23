package com.lengcs.fkwakeup.core.common

import com.google.common.truth.Truth.assertThat
import com.lengcs.fkwakeup.core.model.CourseSession
import com.lengcs.fkwakeup.core.model.SessionDeliveryMode
import org.junit.Test

class CourseConflictDetectorTest {
    @Test
    fun detectsSameDaySectionAndWeekIntersection() {
        val conflicts = CourseConflictDetector.detect(
            listOf(candidate("高数", 1, 1, 2, "1-8"), candidate("英语", 1, 2, 3, "2-10")),
            18,
        )

        assertThat(conflicts).hasSize(1)
        assertThat(conflicts.single().overlappingWeeks).containsExactlyElementsIn(2..8)
    }

    @Test
    fun adjacentSectionsDoNotConflict() {
        val conflicts = CourseConflictDetector.detect(
            listOf(candidate("高数", 1, 1, 2, "1-8"), candidate("英语", 1, 3, 4, "1-8")),
            18,
        )

        assertThat(conflicts).isEmpty()
    }

    @Test
    fun disjointWeeksDoNotConflict() {
        val conflicts = CourseConflictDetector.detect(
            listOf(candidate("高数", 1, 1, 2, "1-4"), candidate("英语", 1, 1, 2, "5-8")),
            18,
        )

        assertThat(conflicts).isEmpty()
    }

    @Test
    fun liveOnlineAndOnsiteAreCompared() {
        val onsite = candidate("高数", 3, 3, 4, "1-18")
        val live = candidate("直播课", 3, 4, 5, "2-18双").let {
            it.copy(session = it.session.copy(deliveryMode = SessionDeliveryMode.LIVE_ONLINE, onlinePlatform = "课堂"))
        }

        assertThat(CourseConflictDetector.detect(listOf(onsite, live), 18)).hasSize(1)
    }

    @Test
    fun samePersistedSessionIsIgnored() {
        val session = candidate("高数", 1, 1, 2, "1-18").session.copy(id = 7)
        assertThat(CourseConflictDetector.detect(listOf(ConflictCandidate("旧", session), ConflictCandidate("新", session)), 18)).isEmpty()
    }

    private fun candidate(name: String, day: Int, start: Int, end: Int, weeks: String) = ConflictCandidate(
        name,
        CourseSession(courseId = name.hashCode().toLong(), dayOfWeek = day, startSection = start, endSection = end, weekSpec = weeks),
    )
}

package com.lengcs.fkwakeup.core.database.mapper

import com.lengcs.fkwakeup.core.database.entity.CourseSessionEntity
import com.lengcs.fkwakeup.core.model.OnlineCourseWindow
import com.lengcs.fkwakeup.core.model.SessionDeliveryMode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class OnlineCourseMappersTest {
    @Test
    fun `直播时间段往返保留平台和链接`() {
        val entity = CourseSessionEntity(
            id = 2L,
            courseId = 1L,
            dayOfWeek = 3,
            startSection = 1,
            endSection = 2,
            weekSpec = "1-16",
            location = null,
            note = null,
            deliveryMode = "LIVE_ONLINE",
            onlinePlatform = "腾讯会议",
            onlineUrl = "https://example.edu/live",
        )

        val domain = entity.toDomain()

        assertEquals(SessionDeliveryMode.LIVE_ONLINE, domain.deliveryMode)
        assertEquals("腾讯会议", domain.onlinePlatform)
        assertEquals("https://example.edu/live", domain.onlineUrl)
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun `未知授课方式安全回退为线下`() {
        val domain = CourseSessionEntity(
            id = 2L,
            courseId = 1L,
            dayOfWeek = 3,
            startSection = 1,
            endSection = 2,
            weekSpec = "1-16",
            location = null,
            note = null,
            deliveryMode = "UNKNOWN",
            onlinePlatform = null,
            onlineUrl = null,
        ).toDomain()

        assertEquals(SessionDeliveryMode.ONSITE, domain.deliveryMode)
    }

    @Test
    fun `异步开放期往返保留日期`() {
        val domain = OnlineCourseWindow(
            id = 3L,
            courseId = 1L,
            startDate = LocalDate.parse("2026-09-01"),
            endDate = LocalDate.parse("2026-12-31"),
            platform = "学习通",
            url = "https://example.edu/course",
        )

        assertEquals(domain, domain.toEntity().toDomain())
    }
}

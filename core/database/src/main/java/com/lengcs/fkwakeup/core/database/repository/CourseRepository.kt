package com.lengcs.fkwakeup.core.database.repository

import com.lengcs.fkwakeup.core.database.dao.CourseDao
import com.lengcs.fkwakeup.core.database.dao.CourseSessionDao
import com.lengcs.fkwakeup.core.database.dao.OnlineCourseWindowDao
import com.lengcs.fkwakeup.core.database.mapper.toDomain
import com.lengcs.fkwakeup.core.database.mapper.toEntity
import com.lengcs.fkwakeup.core.model.Course
import com.lengcs.fkwakeup.core.model.CourseSession
import com.lengcs.fkwakeup.core.model.CourseWithSessions
import com.lengcs.fkwakeup.core.model.OnlineCourseWindow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepository @Inject constructor(
    private val courseDao: CourseDao,
    private val courseSessionDao: CourseSessionDao,
    private val onlineCourseWindowDao: OnlineCourseWindowDao,
) {

    fun observeCourses(termId: Long): Flow<List<CourseWithSessions>> =
        courseDao.observeCoursesWithSessions(termId).map { list -> list.map { it.toDomain() } }

    suspend fun getCourse(id: Long): Course? = courseDao.getById(id)?.toDomain()

    suspend fun getCourseWithSessions(courseId: Long): CourseWithSessions? {
        val course = courseDao.getById(courseId) ?: return null
        val sessions = courseSessionDao.getByCourse(courseId)
        val onlineWindows = onlineCourseWindowDao.getByCourse(courseId)
        return CourseWithSessions(
            course.toDomain(),
            sessions.map { it.toDomain() },
            onlineWindows.map { it.toDomain() },
        )
    }

    suspend fun addCourse(course: Course): Long = courseDao.insert(course.toEntity())

    suspend fun updateCourse(course: Course) {
        courseDao.update(course.toEntity())
    }

    suspend fun deleteCourse(course: Course) {
        courseDao.delete(course.toEntity())
    }

    suspend fun addSession(session: CourseSession): Long =
        courseSessionDao.insert(session.toEntity())

    suspend fun addSessions(sessions: List<CourseSession>): List<Long> =
        courseSessionDao.insertAll(sessions.map { it.toEntity() })

    suspend fun updateSession(session: CourseSession) {
        courseSessionDao.update(session.toEntity())
    }

    suspend fun deleteSession(session: CourseSession) {
        courseSessionDao.delete(session.toEntity())
    }

    suspend fun replaceSessions(courseId: Long, sessions: List<CourseSession>) {
        courseSessionDao.deleteByCourse(courseId)
        courseSessionDao.insertAll(sessions.map { it.toEntity() })
    }

    suspend fun addOnlineWindow(window: OnlineCourseWindow): Long =
        onlineCourseWindowDao.insert(window.toEntity())

    suspend fun updateOnlineWindow(window: OnlineCourseWindow) {
        onlineCourseWindowDao.update(window.toEntity())
    }

    suspend fun deleteOnlineWindow(window: OnlineCourseWindow) {
        onlineCourseWindowDao.delete(window.toEntity())
    }

    suspend fun replaceOnlineWindows(courseId: Long, windows: List<OnlineCourseWindow>) {
        onlineCourseWindowDao.deleteByCourse(courseId)
        onlineCourseWindowDao.insertAll(windows.map { it.toEntity() })
    }
}

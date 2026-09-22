package com.lengcs.fkwakeup.core.importer

import com.lengcs.fkwakeup.core.common.WeekSpecParser
import com.lengcs.fkwakeup.core.importer.model.ErrorCodes
import com.lengcs.fkwakeup.core.importer.model.ImportDraft
import com.lengcs.fkwakeup.core.importer.model.ImportError
import com.lengcs.fkwakeup.core.importer.model.SessionDraft
import com.lengcs.fkwakeup.core.importer.model.OnlineWindowDraft
import com.lengcs.fkwakeup.core.common.OnlineCourseTimeline
import com.lengcs.fkwakeup.core.model.OnlineCourseWindow
import com.lengcs.fkwakeup.core.model.Term
import java.time.Instant

/**
 * L2 校验：把草稿里的每一条问题变成**可定位**的错误（第 N 条）。
 */
object TimetableValidator {

    /**
     * @param draft 归一化后的草稿
     * @param maxSectionIndex 节次时间表里定义的最大节次，用于 E_SECTION_OOB
     */
    fun validate(draft: ImportDraft, maxSectionIndex: Int? = null): List<ImportError> {
        val errors = mutableListOf<ImportError>()

        // 文件级错误直接透传
        errors += draft.errors.filter { it.recordIndex == null }

        val totalWeeks = draft.term?.totalWeeks ?: 18
        val limit = maxSectionIndex ?: draft.sectionTemplates?.maxOfOrNull { it.index }

        for (session in draft.sessions) {
            errors += validateSession(session, totalWeeks, limit)
        }
        for (window in draft.onlineWindows) {
            errors += validateOnlineWindow(window, draft)
        }
        return errors
    }

    private fun validateSession(
        session: SessionDraft,
        totalWeeks: Int,
        maxSectionIndex: Int?,
    ): List<ImportError> {
        val errors = mutableListOf<ImportError>()
        val i = session.index

        if (session.name.isNullOrBlank()) {
            errors += ImportError(ErrorCodes.FORMAT, "第 $i 条记录缺少课程名", i)
        }

        val dow = session.dayOfWeek
        if (dow == null || dow !in 1..7) {
            errors += ImportError(
                ErrorCodes.DOW,
                "第 $i 条记录：星期值应为 1-7，实际为 ${session.dayOfWeek ?: "缺失"}",
                i,
            )
        }

        val start = session.startSection
        val end = session.endSection
        when {
            start == null || end == null -> {
                errors += ImportError(
                    ErrorCodes.SECTION_RANGE,
                    "第 $i 条记录：缺少节次信息",
                    i,
                )
            }
            end < start -> {
                errors += ImportError(
                    ErrorCodes.SECTION_RANGE,
                    "第 $i 条记录：结束节次($end) 小于开始节次($start)",
                    i,
                )
            }
            maxSectionIndex != null && end > maxSectionIndex -> {
                errors += ImportError(
                    ErrorCodes.SECTION_OOB,
                    "第 $i 条记录：第 $end 节未在节次时间表中定义（最大 $maxSectionIndex）",
                    i,
                )
            }
        }

        val weeks = session.weeks
        if (weeks == null) {
            errors += ImportError(ErrorCodes.WEEKSPEC, "第 $i 条记录：缺少周次", i)
        } else if (WeekSpecParser.parseOrNull(weeks, totalWeeks) == null) {
            errors += ImportError(
                ErrorCodes.WEEKSPEC,
                "第 $i 条记录：周次\"$weeks\"无法识别",
                i,
            )
        }

        if (session.deliveryMode == null) {
            errors += ImportError(
                ErrorCodes.DELIVERY_MODE,
                "第 $i 条记录：授课方式应为线下或直播",
                i,
            )
        }

        return errors
    }

    private fun validateOnlineWindow(window: OnlineWindowDraft, draft: ImportDraft): List<ImportError> {
        val errors = mutableListOf<ImportError>()
        val i = window.index
        if (window.name.isNullOrBlank()) {
            errors += ImportError(ErrorCodes.FORMAT, "第 $i 条网课记录缺少课程名", i)
        }
        val start = window.startDate
        val end = window.endDate
        if (start == null || end == null || end.isBefore(start)) {
            errors += ImportError(ErrorCodes.ONLINE_DATE, "第 $i 条网课：开放日期格式错误或结束日期早于开始日期", i)
            return errors
        }
        val termDraft = draft.term ?: return errors
        val term = Term(
            name = termDraft.name,
            startMonday = termDraft.startMonday,
            totalWeeks = termDraft.totalWeeks,
            createdAt = Instant.EPOCH,
        )
        val domainWindow = OnlineCourseWindow(
            courseId = 0L,
            startDate = start,
            endDate = end,
        )
        if (!OnlineCourseTimeline.overlapsTerm(domainWindow, term)) {
            errors += ImportError(ErrorCodes.ONLINE_DATE, "第 $i 条网课：开放期与学期没有重叠", i)
        }
        return errors
    }
}

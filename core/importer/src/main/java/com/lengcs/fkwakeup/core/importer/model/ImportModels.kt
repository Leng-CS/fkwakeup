package com.lengcs.fkwakeup.core.importer.model

import com.lengcs.fkwakeup.core.model.SectionTemplate
import com.lengcs.fkwakeup.core.model.SessionDeliveryMode
import java.time.LocalDate

/** 错误码（主开发文档 5.5） */
object ErrorCodes {
    const val NO_JSON = "E_NO_JSON"
    const val FORMAT = "E_FORMAT"
    const val VERSION = "E_VERSION"
    const val TERM_DATE = "E_TERM_DATE"
    const val DOW = "E_DOW"
    const val SECTION_RANGE = "E_SECTION_RANGE"
    const val SECTION_OOB = "E_SECTION_OOB"
    const val WEEKSPEC = "E_WEEKSPEC"
    const val TIME = "E_TIME"
    const val DELIVERY_MODE = "E_DELIVERY_MODE"
    const val ONLINE_DATE = "E_ONLINE_DATE"
}

/**
 * 导入错误。[recordIndex] 为 null 表示文件级错误，否则指向第 N 条记录（1 起）。
 */
data class ImportError(
    val code: String,
    val message: String,
    val recordIndex: Int? = null,
) {
    fun withIndex(index: Int): ImportError = copy(recordIndex = index)
}

/** 学期草稿 */
data class TermDraft(
    val name: String,
    val startMonday: LocalDate,
    val totalWeeks: Int,
)

/**
 * 单条上课时间段草稿。字段可为 null —— 归一化阶段不抛异常，
 * 由校验阶段统一产出可定位的错误，这样预览页能一次列出所有问题。
 */
data class SessionDraft(
    val index: Int,
    val name: String?,
    val teacher: String?,
    val location: String?,
    val dayOfWeek: Int?,
    val startSection: Int?,
    val endSection: Int?,
    val weeks: String?,
    val note: String?,
    val deliveryMode: SessionDeliveryMode? = SessionDeliveryMode.ONSITE,
    val onlinePlatform: String? = null,
    val onlineUrl: String? = null,
)

data class OnlineWindowDraft(
    val index: Int,
    val name: String?,
    val teacher: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val platform: String?,
    val url: String?,
    val note: String?,
)

/** 归一化后的完整导入草稿 */
data class ImportDraft(
    val term: TermDraft?,
    val sectionTemplates: List<SectionTemplate>?,
    val sessions: List<SessionDraft>,
    val errors: List<ImportError>,
    val onlineWindows: List<OnlineWindowDraft> = emptyList(),
)

/**
 * 归并后的一门课（按 名称+教师 归并键）。
 *
 * [colorArgb] 是用户在**导入预览页**选的自定义课块颜色，null 表示「自动」（按课名哈希取色）。
 * 它只在预览页存在，JSON 导入契约里没有这个字段 —— 没有改动第 5 章的格式规范。
 */
data class MergedCourse(
    val name: String,
    val teacher: String?,
    val colorArgb: Int? = null,
    val sessions: List<SessionDraft>,
    val onlineWindows: List<OnlineWindowDraft> = emptyList(),
)

/** 导入结果 */
data class ImportResult(
    val term: TermDraft?,
    val sectionTemplates: List<SectionTemplate>?,
    val courses: List<MergedCourse>,
    val errors: List<ImportError>,
    val sessionCount: Int,
    val onlineWindowCount: Int = 0,
) {
    /** 无文件级错误且至少识别出一条记录 */
    val isSuccess: Boolean
        get() = errors.none { it.recordIndex == null } && sessionCount + onlineWindowCount > 0
}

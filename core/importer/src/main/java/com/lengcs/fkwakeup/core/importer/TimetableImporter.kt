package com.lengcs.fkwakeup.core.importer

import com.lengcs.fkwakeup.core.importer.model.ErrorCodes
import com.lengcs.fkwakeup.core.importer.model.ImportError
import com.lengcs.fkwakeup.core.importer.model.ImportResult
import com.lengcs.fkwakeup.core.model.DefaultSections

/**
 * 导入门面：串起 L1 提取 → L2 归一/校验 → 归并。
 *
 * 典型用法：
 * ```kotlin
 * val result = TimetableImporter.import(aiReplyText)
 * if (result.isSuccess) { /* 展示预览页 */ } else { /* 展示 result.errors */ }
 * ```
 */
object TimetableImporter {

    fun import(rawText: String): ImportResult {
        // ---- L1 结构化提取 ----
        val json = JsonExtractor.extract(rawText)
            ?: return ImportResult(
                term = null,
                sectionTemplates = null,
                courses = emptyList(),
                errors = listOf(JsonExtractor.noJsonError()),
                sessionCount = 0,
            )

        // ---- L2 归一化 + L3 安全修复 ----
        val draft = TimetableNormalizer.normalize(json)

        // 节次时间表缺失时用默认值补齐
        val sectionTemplates = draft.sectionTemplates
            ?: DefaultSections.forTerm(0L)

        // ---- 校验 ----
        val errors = TimetableValidator.validate(draft)

        // ---- 归并 ----
        val courses = CourseMerger.merge(draft.sessions, draft.onlineWindows)

        return ImportResult(
            term = draft.term,
            sectionTemplates = sectionTemplates,
            courses = courses,
            errors = errors,
            sessionCount = draft.sessions.size,
            onlineWindowCount = draft.onlineWindows.size,
        )
    }

    /**
     * 生成给 AI 看的错误清单（每行一条），用于「复制错误报告，让 AI 重新生成」。
     */
    fun formatErrorReport(errors: List<ImportError>): String =
        if (errors.isEmpty()) {
            "（无错误）"
        } else {
            errors.joinToString(separator = "\n") { error ->
                val target = error.recordIndex?.let { "第 $it 条记录：" }.orEmpty()
                "$target${error.message}（${error.code}）"
            }
        }

    /** 文件级错误（无法定位到某条记录） */
    fun isFileLevelError(code: String): Boolean =
        code == ErrorCodes.NO_JSON || code == ErrorCodes.FORMAT || code == ErrorCodes.VERSION
}

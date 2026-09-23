package com.lengcs.fkwakeup.core.importer

import com.lengcs.fkwakeup.core.common.CurrentWeekCalculator
import com.lengcs.fkwakeup.core.importer.model.ErrorCodes
import com.lengcs.fkwakeup.core.importer.model.ImportDraft
import com.lengcs.fkwakeup.core.importer.model.ImportError
import com.lengcs.fkwakeup.core.importer.model.SessionDraft
import com.lengcs.fkwakeup.core.importer.model.OnlineWindowDraft
import com.lengcs.fkwakeup.core.importer.model.TermDraft
import com.lengcs.fkwakeup.core.model.DefaultSections
import com.lengcs.fkwakeup.core.model.SectionTemplate
import com.lengcs.fkwakeup.core.model.SessionDeliveryMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * L2 归一化 + L3 安全自动修复。
 *
 * 这里**不抛异常**：所有问题都收集成 [ImportError]，让预览页一次列全。
 */
@kotlinx.serialization.ExperimentalSerializationApi
object TimetableNormalizer {

    private val lenientJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
    }

    fun normalize(jsonText: String): ImportDraft {
        val errors = mutableListOf<ImportError>()

        val root = runCatching { lenientJson.parseToJsonElement(jsonText).jsonObject }
            .getOrNull()
        if (root == null) {
            errors += ImportError(ErrorCodes.NO_JSON, "JSON 无法解析")
            return ImportDraft(null, null, emptyList(), errors)
        }

        // ---- 文件级：format / version ----
        val format = root.stringOrNull("format")
        if (format != "campus-timetable") {
            errors += ImportError(
                ErrorCodes.FORMAT,
                "format 应为 campus-timetable，实际为 ${format ?: "缺失"}",
            )
        }
        val version = root.stringOrNull("version")
        if (version !in setOf("1.0", "1.1")) {
            errors += ImportError(
                ErrorCodes.VERSION,
                "version 应为 1.0 或 1.1，实际为 ${version ?: "缺失"}",
            )
        }

        // ---- 学期 ----
        val term = normalizeTerm(root["term"], errors)

        // ---- 节次时间表 ----
        val sectionTemplates = normalizeSections(root["sectionTemplates"], errors)

        // ---- 时间段列表 ----
        val totalWeeks = term?.totalWeeks ?: 18
        val sessions = normalizeSessions(root["sessions"], totalWeeks, errors)
        val onlineWindows = normalizeOnlineWindows(root["onlineWindows"], term, errors)

        return ImportDraft(
            term = term,
            sectionTemplates = sectionTemplates,
            sessions = sessions,
            errors = errors,
            onlineWindows = onlineWindows,
        )
    }

    private fun normalizeTerm(element: JsonElement?, errors: MutableList<ImportError>): TermDraft? {
        val obj = element as? JsonObject ?: run {
            errors += ImportError(ErrorCodes.TERM_DATE, "缺少 term 字段")
            return null
        }

        val rawDate = obj.stringOrNull("startMonday")
            ?: obj.stringOrNull("startDate")
            ?: obj.stringOrNull("start")

        val parsedDate = rawDate?.let {
            runCatching { LocalDate.parse(it.trim()) }.getOrElse { null }
        }

        if (rawDate == null || parsedDate == null) {
            errors += ImportError(
                ErrorCodes.TERM_DATE,
                "学期起始日无法识别：${rawDate ?: "缺失"}，应为 YYYY-MM-DD",
            )
        }

        val totalWeeks = obj.intOrNull("totalWeeks") ?: obj.intOrNull("weeks") ?: 18
        val name = obj.stringOrNull("name")?.takeIf { it.isNotBlank() }
            ?: parsedDate?.let { deriveTermName(it) }
            ?: "未命名学期"

        // 非周一时向前对齐到周一（主开发文档 7.2）
        val startMonday = parsedDate?.let { CurrentWeekCalculator.alignToMonday(it) } ?: LocalDate.now()

        return TermDraft(
            name = name,
            startMonday = startMonday,
            totalWeeks = totalWeeks.coerceIn(1, 30),
        )
    }

    private fun deriveTermName(date: LocalDate): String {
        val year = date.year
        val autumn = date.monthValue >= 8
        return if (autumn) "$year-${year + 1} 秋季学期" else "$year 春季学期"
    }

    private fun normalizeSections(
        element: JsonElement?,
        errors: MutableList<ImportError>,
    ): List<SectionTemplate>? {
        val array = element as? kotlinx.serialization.json.JsonArray ?: return null
        val result = mutableListOf<SectionTemplate>()

        array.forEachIndexed { i, item ->
            val obj = item as? JsonObject ?: return@forEachIndexed
            val index = obj.intOrNull("index") ?: (i + 1)
            val start = obj.stringOrNull("start")
            val end = obj.stringOrNull("end")
            val startMinutes = start?.let { runCatching { DefaultSections.toMinutes(it) }.getOrNull() }
            val endMinutes = end?.let { runCatching { DefaultSections.toMinutes(it) }.getOrNull() }

            if (startMinutes == null || endMinutes == null || endMinutes <= startMinutes) {
                errors += ImportError(
                    ErrorCodes.TIME,
                    "第 ${i + 1} 节时间设置有误：${start ?: "缺失"}-${end ?: "缺失"}",
                )
                return@forEachIndexed
            }
            result += SectionTemplate(
                termId = 0L, // 落库前由调用方替换为真实 termId
                index = index,
                startMinutes = startMinutes,
                endMinutes = endMinutes,
            )
        }

        return result.takeIf { it.isNotEmpty() }
    }

    private fun normalizeSessions(
        element: JsonElement?,
        totalWeeks: Int,
        errors: MutableList<ImportError>,
    ): List<SessionDraft> {
        val array = element as? kotlinx.serialization.json.JsonArray
        if (array == null) {
            errors += ImportError(ErrorCodes.FORMAT, "缺少 sessions 数组")
            return emptyList()
        }

        return array.mapIndexed { i, item ->
            val obj = item as? JsonObject
            if (obj == null) {
                errors += ImportError(ErrorCodes.FORMAT, "第 ${i + 1} 条记录不是对象", i + 1)
                return@mapIndexed SessionDraft(i + 1, null, null, null, null, null, null, null, null)
            }

            val map = obj.toStringMap()

            val name = FieldNormalizer.nameKey(map)?.trim()?.takeIf { it.isNotBlank() }
            val teacher = FieldNormalizer.normalizeOptionalText(FieldNormalizer.teacherKey(map))
            val location = FieldNormalizer.normalizeOptionalText(FieldNormalizer.locationKey(map))
            val dayOfWeek = FieldNormalizer.normalizeDayOfWeek(FieldNormalizer.dowKey(map))

            val startRaw = FieldNormalizer.startKey(map)
            val endRaw = FieldNormalizer.endKey(map)
            val range = resolveSectionRange(startRaw, endRaw)

            // L3 安全修复：缺周次时按整学期
            val weeks = FieldNormalizer.normalizeWeekSpec(FieldNormalizer.weeksKey(map))
                ?: "1-$totalWeeks"

            val note = obj.stringOrNull("note")?.trim()?.takeIf { it.isNotBlank() }
            val onlinePlatform = FieldNormalizer.normalizeOptionalText(
                obj.stringOrNull("onlinePlatform") ?: obj.stringOrNull("platform"),
            )
            val onlineUrl = FieldNormalizer.normalizeOptionalText(
                obj.stringOrNull("onlineUrl") ?: obj.stringOrNull("url") ?: obj.stringOrNull("link"),
            )
            val deliveryMode = normalizeDeliveryMode(
                obj.stringOrNull("deliveryMode") ?: obj.stringOrNull("mode") ?: obj.stringOrNull("授课方式"),
                hasOnlineDetails = onlinePlatform != null || onlineUrl != null,
            )

            SessionDraft(
                index = i + 1,
                name = name,
                teacher = teacher,
                location = location,
                dayOfWeek = dayOfWeek,
                startSection = range?.first,
                endSection = range?.second,
                weeks = weeks,
                note = note,
                deliveryMode = deliveryMode,
                onlinePlatform = onlinePlatform,
                onlineUrl = onlineUrl,
            )
        }
    }

    private fun normalizeOnlineWindows(
        element: JsonElement?,
        term: TermDraft?,
        errors: MutableList<ImportError>,
    ): List<OnlineWindowDraft> {
        val array = element as? kotlinx.serialization.json.JsonArray ?: return emptyList()
        return array.mapIndexed { i, item ->
            val obj = item as? JsonObject
            if (obj == null) {
                errors += ImportError(ErrorCodes.FORMAT, "第 ${i + 1} 条网课记录不是对象", i + 1)
                return@mapIndexed OnlineWindowDraft(i + 1, null, null, null, null, null, null, null)
            }
            val map = obj.toStringMap()
            val startWeek = obj.intOrNull("startWeek")
            val endWeek = obj.intOrNull("endWeek")
            val explicitStart = parseDate(obj.stringOrNull("startDate") ?: obj.stringOrNull("start"))
            val explicitEnd = parseDate(obj.stringOrNull("endDate") ?: obj.stringOrNull("end"))
            val inferred = explicitStart == null && explicitEnd == null && term != null &&
                startWeek != null && endWeek != null &&
                startWeek in 1..term.totalWeeks && endWeek in startWeek..term.totalWeeks
            val inferredStart = if (inferred) term?.startMonday?.plusWeeks(((startWeek ?: 1) - 1).toLong()) else null
            val inferredEnd = if (inferred) term?.startMonday?.plusWeeks((endWeek ?: 1).toLong())?.minusDays(1) else null
            OnlineWindowDraft(
                index = i + 1,
                name = FieldNormalizer.nameKey(map)?.trim()?.takeIf { it.isNotBlank() },
                teacher = FieldNormalizer.normalizeOptionalText(FieldNormalizer.teacherKey(map)),
                startDate = explicitStart ?: inferredStart,
                endDate = explicitEnd ?: inferredEnd,
                platform = FieldNormalizer.normalizeOptionalText(obj.stringOrNull("platform")),
                url = FieldNormalizer.normalizeOptionalText(
                    obj.stringOrNull("url") ?: obj.stringOrNull("link") ?: obj.stringOrNull("courseUrl"),
                ),
                note = listOfNotNull(
                    FieldNormalizer.normalizeOptionalText(obj.stringOrNull("note")),
                    "根据学期周次推算日期".takeIf { inferred },
                ).joinToString("；").ifBlank { null },
            )
        }
    }

    private fun parseDate(raw: String?): LocalDate? = raw?.trim()?.let { value ->
        try {
            LocalDate.parse(value)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun normalizeDeliveryMode(raw: String?, hasOnlineDetails: Boolean): SessionDeliveryMode? {
        val value = raw?.trim()?.lowercase()
        if (value.isNullOrEmpty()) {
            return if (hasOnlineDetails) SessionDeliveryMode.LIVE_ONLINE else SessionDeliveryMode.ONSITE
        }
        return when (value.replace("_", "").replace("-", "")) {
            "onsite", "offline", "线下" -> SessionDeliveryMode.ONSITE
            "liveonline", "online", "live", "直播", "直播网课" -> SessionDeliveryMode.LIVE_ONLINE
            else -> null
        }
    }

    private fun resolveSectionRange(startRaw: String?, endRaw: String?): Pair<Int, Int>? {
        val startInt = FieldNormalizer.normalizeSectionValue(startRaw)
        val endInt = FieldNormalizer.normalizeSectionValue(endRaw)

        return when {
            startInt != null && endInt != null -> startInt to endInt
            startInt != null && endInt == null ->
                FieldNormalizer.normalizeSectionRange(startRaw) ?: (startInt to startInt)
            else -> FieldNormalizer.normalizeSectionRange(startRaw)
                ?: FieldNormalizer.normalizeSectionRange(endRaw)
        }
    }

    // ---- JsonElement 辅助 ----

    private fun JsonObject.stringOrNull(key: String): String? {
        val el = entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
            ?: return null
        return (el as? JsonPrimitive)?.contentOrNull
    }

    private fun JsonObject.intOrNull(key: String): Int? {
        val el = entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
            ?: return null
        val primitive = el as? JsonPrimitive ?: return null
        return primitive.intOrNull ?: primitive.contentOrNull?.trim()?.toIntOrNull()
    }

    /** 只保留基本类型，便于用别名表取值 */
    private fun JsonObject.toStringMap(): Map<String, String> =
        entries.mapNotNull { (key, value) ->
            val primitive = value as? JsonPrimitive ?: return@mapNotNull null
            if (primitive.isString || primitive.booleanOrNull != null || primitive.intOrNull != null) {
                key to primitive.contentOrNull.orEmpty()
            } else {
                null
            }
        }.toMap()
}

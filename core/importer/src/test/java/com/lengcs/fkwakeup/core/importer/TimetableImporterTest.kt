package com.lengcs.fkwakeup.core.importer

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import com.lengcs.fkwakeup.core.model.SessionDeliveryMode

/**
 * 端到端：L1 提取 → L2 归一/校验 → 归并
 */
class TimetableImporterTest {

    private val sampleJson = """
        {
          "format": "campus-timetable",
          "version": "1.0",
          "term": { "name": "2026-2027 秋季学期", "startMonday": "2026-09-07", "totalWeeks": 18 },
          "sessions": [
            {"name":"高等数学A","teacher":"张伟","location":"教三-301","dayOfWeek":1,"startSection":1,"endSection":2,"weeks":"1-16"},
            {"name":"高等数学A","teacher":"张伟","location":"教三-301","dayOfWeek":3,"startSection":3,"endSection":4,"weeks":"1-16"},
            {"name":"大学英语（二）","teacher":"李娜","location":"外语楼-202","dayOfWeek":2,"startSection":3,"endSection":4,"weeks":"1-16单"},
            {"name":"数据结构","teacher":"王强","location":"信息楼-A405","dayOfWeek":2,"startSection":5,"endSection":7,"weeks":"1-9,11-18"},
            {"name":"线性代数","teacher":null,"location":"教二-108","dayOfWeek":4,"startSection":5,"endSection":6,"weeks":"1-16"}
          ]
        }
    """.trimIndent()

    @Test
    fun `示例文件导入成功并正确归并`() {
        val result = TimetableImporter.import(sampleJson)

        assertThat(result.errors).isEmpty()
        assertThat(result.sessionCount).isEqualTo(5)
        assertThat(result.courses).hasSize(4)
        assertThat(result.isSuccess).isTrue()

        val math = result.courses.first { it.name == "高等数学A" }
        assertThat(math.sessions).hasSize(2)
        assertThat(math.teacher).isEqualTo("张伟")
    }

    @Test
    fun `带围栏和前言后语也能导入`() {
        val text = """
            好的，我已识别完成：

            ```json
            $sampleJson
            ```

            如需调整请告诉我。
        """.trimIndent()

        val result = TimetableImporter.import(text)
        assertThat(result.errors).isEmpty()
        assertThat(result.courses).hasSize(4)
    }

    @Test
    fun `学期信息解析正确`() {
        val result = TimetableImporter.import(sampleJson)
        assertThat(result.term).isNotNull()
        assertThat(result.term!!.totalWeeks).isEqualTo(18)
        assertThat(result.term!!.startMonday.toString()).isEqualTo("2026-09-07")
    }

    @Test
    fun `字段名用别名也能识别`() {
        val json = """
            {
              "format":"campus-timetable","version":"1.0",
              "term":{"name":"T","startMonday":"2026-09-07","totalWeeks":18},
              "sessions":[
                {"courseName":"体育","教师":"刘洋","classroom":"东体育场","星期":3,"startPeriod":7,"endPeriod":8,"周次":"1-16"}
              ]
            }
        """.trimIndent()

        val result = TimetableImporter.import(json)
        assertThat(result.errors).isEmpty()
        val course = result.courses.single()
        assertThat(course.name).isEqualTo("体育")
        assertThat(course.teacher).isEqualTo("刘洋")
        assertThat(course.sessions.single().dayOfWeek).isEqualTo(3)
        assertThat(course.sessions.single().location).isEqualTo("东体育场")
    }

    @Test
    fun `非法的 dayOfWeek 能定位到第 N 条`() {
        val json = """
            {
              "format":"campus-timetable","version":"1.0",
              "term":{"name":"T","startMonday":"2026-09-07","totalWeeks":18},
              "sessions":[
                {"name":"A","dayOfWeek":1,"startSection":1,"endSection":2,"weeks":"1-16"},
                {"name":"B","dayOfWeek":9,"startSection":1,"endSection":2,"weeks":"1-16"}
              ]
            }
        """.trimIndent()

        val result = TimetableImporter.import(json)
        val dowError = result.errors.first { it.code == "E_DOW" }
        assertThat(dowError.recordIndex).isEqualTo(2)
        assertThat(dowError.message).contains("第 2 条")
    }

    @Test
    fun `中文周次写法产出 E_WEEKSPEC 且可定位`() {
        val json = """
            {
              "format":"campus-timetable","version":"1.0",
              "term":{"name":"T","startMonday":"2026-09-07","totalWeeks":18},
              "sessions":[{"name":"A","dayOfWeek":1,"startSection":1,"endSection":2,"weeks":"1-16周（单）"}]
            }
        """.trimIndent()

        val result = TimetableImporter.import(json)
        val e = result.errors.first { it.code == "E_WEEKSPEC" }
        assertThat(e.recordIndex).isEqualTo(1)
    }

    @Test
    fun `缺少 JSON 时返回 E_NO_JSON`() {
        val result = TimetableImporter.import("抱歉，图片太模糊了。")
        assertThat(result.errors.map { it.code }).containsExactly("E_NO_JSON")
        assertThat(result.isSuccess).isFalse()
    }

    @Test
    fun `format 不对返回 E_FORMAT`() {
        val json = """{"format":"something","version":"1.0","sessions":[]}"""
        val result = TimetableImporter.import(json)
        assertThat(result.errors.map { it.code }).contains("E_FORMAT")
    }

    @Test
    fun `错误报告格式可贴回 AI`() {
        val result = TimetableImporter.import("看不清")
        val report = TimetableImporter.formatErrorReport(result.errors)
        assertThat(report).contains("E_NO_JSON")
    }

    @Test
    fun `节次时间表缺失时用默认值补齐`() {
        val result = TimetableImporter.import(sampleJson)
        assertThat(result.sectionTemplates).isNotNull()
        assertThat(result.sectionTemplates!!.size).isEqualTo(12)
        assertThat(result.sectionTemplates!!.first().index).isEqualTo(1)
    }

    @Test
    fun `v1_1 同时导入直播与异步网课`() {
        val json = """
            {
              "format":"campus-timetable","version":"1.1",
              "term":{"name":"T","startMonday":"2026-09-07","totalWeeks":18},
              "sessions":[{
                "name":"大学英语","teacher":"李娜","dayOfWeek":3,
                "startSection":1,"endSection":2,"weeks":"1-16",
                "deliveryMode":"liveOnline","onlinePlatform":"腾讯会议",
                "onlineUrl":"https://example.edu/live"
              }],
              "onlineWindows":[{
                "name":"大学英语","teacher":"李娜","startDate":"2026-09-01",
                "endDate":"2026-12-31","platform":"学习通","url":"https://example.edu/course"
              }]
            }
        """.trimIndent()

        val result = TimetableImporter.import(json)

        assertThat(result.errors).isEmpty()
        assertThat(result.sessionCount).isEqualTo(1)
        assertThat(result.onlineWindowCount).isEqualTo(1)
        assertThat(result.courses).hasSize(1)
        val course = result.courses.single()
        assertThat(course.sessions.single().deliveryMode).isEqualTo(SessionDeliveryMode.LIVE_ONLINE)
        assertThat(course.sessions.single().onlinePlatform).isEqualTo("腾讯会议")
        assertThat(course.onlineWindows.single().platform).isEqualTo("学习通")
    }

    @Test
    fun `异步网课结束日期早于开始日期会报错`() {
        val json = """
            {
              "format":"campus-timetable","version":"1.1",
              "term":{"name":"T","startMonday":"2026-09-07","totalWeeks":18},
              "sessions":[],
              "onlineWindows":[{"name":"网课","startDate":"2026-10-10","endDate":"2026-09-01"}]
            }
        """.trimIndent()

        val result = TimetableImporter.import(json)

        assertThat(result.errors.map { it.code }).contains("E_ONLINE_DATE")
    }

    @Test
    fun `只含异步网课的 v1_1 文件也导入成功`() {
        val json = """
            {
              "format":"campus-timetable","version":"1.1",
              "term":{"name":"T","startMonday":"2026-09-07","totalWeeks":18},
              "sessions":[],
              "onlineWindows":[{"name":"网课","startDate":"2026-09-07","endDate":"2026-09-08"}]
            }
        """.trimIndent()

        val result = TimetableImporter.import(json)

        assertThat(result.errors).isEmpty()
        assertThat(result.isSuccess).isTrue()
        assertThat(result.courses.single().onlineWindows).hasSize(1)
    }
}

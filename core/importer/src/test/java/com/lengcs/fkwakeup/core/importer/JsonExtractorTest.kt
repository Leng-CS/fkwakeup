package com.lengcs.fkwakeup.core.importer

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class JsonExtractorTest {

    @Test
    fun `纯 JSON 文本原样取出`() {
        val text = """{"format":"campus-timetable","version":"1.0"}"""
        assertThat(JsonExtractor.extract(text)).isEqualTo(text)
    }

    @Test
    fun `剥离 json 代码围栏`() {
        val text = """
            好的，这是识别结果：

            ```json
            {"format":"campus-timetable","version":"1.0","sessions":[]}
            ```

            如有疑问请告诉我。
        """.trimIndent()

        val extracted = JsonExtractor.extract(text)
        assertThat(extracted).isNotNull()
        assertThat(extracted).doesNotContain("```")
        assertThat(extracted).doesNotContain("好的")
        assertThat(extracted).contains("\"sessions\":[]")
    }

    @Test
    fun `剥离无语言标记的代码围栏`() {
        val text = "```\n{\"format\":\"campus-timetable\"}\n```"
        assertThat(JsonExtractor.extract(text)).isEqualTo("""{"format":"campus-timetable"}""")
    }

    @Test
    fun `带前后正文且无围栏时仍能取出对象`() {
        val text = "识别完成，结果如下：{\"format\":\"campus-timetable\",\"version\":\"1.0\"} 以上。"
        val extracted = JsonExtractor.extract(text)
        assertThat(extracted).isEqualTo("""{"format":"campus-timetable","version":"1.0"}""")
    }

    @Test
    fun `只取第一个配平的对象，丢弃尾部多余内容`() {
        val text = """{"a":1} {"b":2} 这是多余说明"""
        assertThat(JsonExtractor.extract(text)).isEqualTo("""{"a":1}""")
    }

    @Test
    fun `修复尾随逗号`() {
        val text = """{"a":1,"b":2,}"""
        assertThat(JsonExtractor.extract(text)).isEqualTo("""{"a":1,"b":2}""")
    }

    @Test
    fun `修复单引号`() {
        val text = """{'format':'campus-timetable'}"""
        assertThat(JsonExtractor.extract(text)).isEqualTo("""{"format":"campus-timetable"}""")
    }

    @Test
    fun `字符串内部的花括号不影响配平`() {
        val text = """{"name":"高等数学A(上){}","ok":true}"""
        assertThat(JsonExtractor.extract(text)).isEqualTo(text)
    }

    @Test
    fun `没有 JSON 时返回 null`() {
        assertThat(JsonExtractor.extract("抱歉，我没看懂这张图。")).isNull()
        assertThat(JsonExtractor.extract("")).isNull()
        assertThat(JsonExtractor.extract("   ")).isNull()
    }
}

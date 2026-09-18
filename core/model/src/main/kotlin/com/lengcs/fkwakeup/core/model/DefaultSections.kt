package com.lengcs.fkwakeup.core.model

/**
 * 默认节次时间表（主开发文档 4.4）。
 *
 * 各校节次时间不同，这里只是新建学期时的初始值，用户可随时修改。
 * 模块内不依赖 core:common，避免循环依赖。
 */
object DefaultSections {

    private val specs = listOf(
        1 to ("08:00" to "08:45"),
        2 to ("08:55" to "09:40"),
        3 to ("10:00" to "10:45"),
        4 to ("10:55" to "11:40"),
        5 to ("14:00" to "14:45"),
        6 to ("14:55" to "15:40"),
        7 to ("16:00" to "16:45"),
        8 to ("16:55" to "17:40"),
        9 to ("19:00" to "19:45"),
        10 to ("19:55" to "20:40"),
        11 to ("20:50" to "21:35"),
        12 to ("21:45" to "22:30"),
    )

    fun forTerm(termId: Long): List<SectionTemplate> =
        specs.map { (index, range) ->
            SectionTemplate(
                termId = termId,
                index = index,
                startMinutes = toMinutes(range.first),
                endMinutes = toMinutes(range.second),
            )
        }

    /** "HH:mm" -> 自 00:00 起的分钟数 */
    fun toMinutes(clock: String): Int {
        val parts = clock.split(":")
        require(parts.size == 2) { "时间格式应为 HH:mm，实际为 $clock" }
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        require(hour in 0..23 && minute in 0..59) { "时间越界：$clock" }
        return hour * 60 + minute
    }
}

package com.lengcs.fkwakeup.core.importer

import com.lengcs.fkwakeup.core.importer.model.ErrorCodes
import com.lengcs.fkwakeup.core.importer.model.ImportError

/**
 * L1 结构化提取：把 AI 的任意回复文本变成一段干净的 JSON。
 *
 * AI 常见的包裹形式：
 * - ```json ... ``` 代码块围栏
 * - 「好的，以下是识别结果：」前言 + 「如有疑问请告诉我」后语
 * - 尾随逗号、单引号等轻微不合法写法
 */
object JsonExtractor {

    private val JSON_FENCE_REGEX = Regex("```(?:json|jsonc|JSON)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)

    /**
     * 提取 JSON 子串。
     *
     * @return 提取结果；找不到 JSON 时返回 null，由调用方转成 E_NO_JSON
     */
    fun extract(rawText: String): String? {
        if (rawText.isBlank()) return null

        val fromFence = extractFromFence(rawText)
        val candidate = fromFence ?: rawText

        return extractBalancedObject(candidate)?.let { repair(it) }
            ?: repair(candidate).takeIf { it.trim().startsWith("{") }
    }

    /** 优先取 ```json 围栏内容，其次任意围栏 */
    private fun extractFromFence(text: String): String? {
        val match = JSON_FENCE_REGEX.find(text) ?: return null
        val content = match.groupValues[1]
        return content.takeIf { it.contains("{") }
    }

    /**
     * 括号配平扫描：从第一个 '{' 开始，跳过字符串字面量与转义，
     * 返回第一个完整闭合的 JSON 对象，避免把尾部多余文本带进来。
     */
    private fun extractBalancedObject(text: String): String? {
        val start = text.indexOf('{')
        if (start < 0) return null

        var depth = 0
        var inString = false
        var escaped = false

        for (i in start until text.length) {
            val ch = text[i]
            if (escaped) {
                escaped = false
                continue
            }
            when {
                ch == '\\' && inString -> escaped = true
                ch == '"' -> inString = !inString
                inString -> Unit
                ch == '{' -> depth++
                ch == '}' -> {
                    depth--
                    if (depth == 0) return text.substring(start, i + 1)
                }
            }
        }
        return null
    }

    /**
     * 仅做两项安全修复：去除尾随逗号、单引号换双引号。
     * 不做更多改写，避免把本来正确的内容改坏。
     */
    private fun repair(text: String): String {
        var result = text

        // 单引号字符串 -> 双引号字符串（仅当内容里没有双引号时整体替换更安全）
        if (!result.contains('"') && result.contains('\'')) {
            result = result.replace('\'', '"')
        }

        // 尾随逗号：, 后面紧跟 } 或 ]
        result = result.replace(Regex(""",(\s*[}\]])"""), "$1")
        return result
    }

    fun noJsonError(): ImportError =
        ImportError(ErrorCodes.NO_JSON, "未找到课表数据：文本中没有合法的 JSON 对象")
}

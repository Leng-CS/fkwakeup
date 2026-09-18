package com.lengcs.fkwakeup.feature.importexport

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.lengcs.fkwakeup.core.importer.TimetableImporter
import com.lengcs.fkwakeup.core.importer.model.ImportError

/**
 * 提示词文本一律从 res/raw 读取，**禁止硬编码进 Kotlin**（主开发文档 FR-01A）。
 * 文案的唯一来源是 docs/recognition-prompt.md。
 */
object ImportTexts {

    fun prompt(context: Context): String = readRaw(context, R.raw.prompt_import)

    fun repairTemplate(context: Context): String = readRaw(context, R.raw.prompt_repair)

    /**
     * 拼装「修复版提示词 + 错误列表 + 原始文本」，供失败页复制给 AI。
     * 原始文本超过 8 KB 时截断，避免剪贴板过大。
     */
    fun buildRepairReport(
        context: Context,
        errors: List<ImportError>,
        original: String,
    ): String {
        val errorList = TimetableImporter.formatErrorReport(errors)
        val truncated = if (original.length > MAX_ORIGINAL_LENGTH) {
            original.take(MAX_ORIGINAL_LENGTH) + "\n...（内容过长已截断）"
        } else {
            original
        }
        return repairTemplate(context)
            .replace("{ERRORS}", errorList)
            .replace("{ORIGINAL}", truncated)
    }

    private const val MAX_ORIGINAL_LENGTH = 8 * 1024

    private fun readRaw(context: Context, resId: Int): String =
        context.resources.openRawResource(resId).bufferedReader().use { it.readText() }
}

/**
 * 复制必须是纯文本（ClipData.newPlainText），否则粘贴进部分 AI 输入框会带样式或被截断。
 */
fun copyPlainText(context: Context, label: String, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText(label, text))
}

fun readClipboardText(context: Context): String? {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return manager.primaryClip
        ?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.text
        ?.toString()
}

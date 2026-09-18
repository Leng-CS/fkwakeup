package com.lengcs.fkwakeup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

private const val MAX_SHARED_LENGTH = 1024 * 1024

/**
 * 分享入口：用户在 AI App 里点「分享」，选择本 App 即可直达导入页。
 *
 * 只做一件事 —— 取出文本并转发给 MainActivity，然后立即结束自己。
 */
@AndroidEntryPoint
class ShareImportActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = intent?.takeIf { it.action == Intent.ACTION_SEND }
            ?.getStringExtra(Intent.EXTRA_TEXT)

        when {
            text.isNullOrBlank() -> {
                Toast.makeText(this, "没有收到课表内容", Toast.LENGTH_SHORT).show()
                goHome()
            }
            text.length > MAX_SHARED_LENGTH -> {
                Toast.makeText(this, "内容过长，请使用文件导入", Toast.LENGTH_LONG).show()
                goHome()
            }
            else -> {
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .putExtra(Intent.EXTRA_TEXT, text)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                )
                finish()
            }
        }
    }

    private fun goHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

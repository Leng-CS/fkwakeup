package com.lengcs.fkwakeup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lengcs.fkwakeup.core.designsystem.theme.FkwakeupTheme
import com.lengcs.fkwakeup.feature.importexport.ImportFlow
import com.lengcs.fkwakeup.feature.schedule.ScheduleScreen
import dagger.hilt.android.AndroidEntryPoint

object Routes {
    const val HOME = "home"
    const val IMPORT = "import"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * 分享进来的文本。放在 Activity 层，因为它有两个入口：
     * 冷启动走 onCreate，App 已在运行时走 onNewIntent。
     */
    private val incomingText = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        incomingText.value = intent?.getStringExtra(Intent.EXTRA_TEXT)

        setContent {
            FkwakeupTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    FkwakeupApp(sharedText = incomingText.value)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingText.value = intent.getStringExtra(Intent.EXTRA_TEXT)
    }
}

@Composable
private fun FkwakeupApp(sharedText: String?) {
    val navController = rememberNavController()

    // 冷启动自带文本、或运行中被分享唤起，两种情况都要落到导入页
    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) {
            navController.navigate(Routes.IMPORT) { launchSingleTop = true }
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            // enableEdgeToEdge 之后必须让 Scaffold 处理系统栏内边距，
            // 否则顶栏会被状态栏压住、按钮点不到
            Scaffold { padding ->
                ScheduleScreen(
                    onImportClick = { navController.navigate(Routes.IMPORT) },
                    modifier = Modifier.padding(padding),
                )
            }
        }
        composable(Routes.IMPORT) {
            Scaffold { padding ->
                ImportFlow(
                    initialText = sharedText,
                    onImported = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}


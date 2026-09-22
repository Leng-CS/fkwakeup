package com.lengcs.fkwakeup

import android.content.Intent
import android.net.Uri
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
import com.lengcs.fkwakeup.feature.course.CourseEditScreen
import com.lengcs.fkwakeup.feature.course.CourseManageScreen
import com.lengcs.fkwakeup.feature.course.SectionTemplateScreen
import com.lengcs.fkwakeup.feature.course.TermManageScreen
import com.lengcs.fkwakeup.feature.importexport.ImportFlow
import com.lengcs.fkwakeup.feature.schedule.ScheduleScreen
import com.lengcs.fkwakeup.feature.settings.WidgetSettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate

object Routes {
    const val HOME = "home"
    const val IMPORT = "import"
    const val MANAGE = "manage"
    const val WIDGET_SETTINGS = "widget-settings"
    const val ALARM_PLACEHOLDER = "alarm-placeholder"
    const val COURSE_EDIT = "course/{courseId}"
    const val TERM = "term"
    const val SECTIONS = "sections"
    fun courseEdit(courseId: Long = 0L) = "course/$courseId"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val incomingText = mutableStateOf<String?>(null)
    private val widgetLink = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        acceptIntent(intent)
        setContent {
            FkwakeupTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    FkwakeupApp(sharedText = incomingText.value, widgetLink = widgetLink.value)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptIntent(intent)
    }

    private fun acceptIntent(intent: Intent?) {
        incomingText.value = intent?.getStringExtra(Intent.EXTRA_TEXT)
        widgetLink.value = intent?.data?.takeIf { it.scheme == "fkwakeup" && it.host == "widget" }
    }
}

@Composable
private fun FkwakeupApp(sharedText: String?, widgetLink: Uri?) {
    val navController = rememberNavController()
    val targetDate = widgetLink?.takeIf { it.lastPathSegment == "schedule" }
        ?.getQueryParameter("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) navController.navigate(Routes.IMPORT) { launchSingleTop = true }
    }
    LaunchedEffect(widgetLink) {
        when (widgetLink?.lastPathSegment) {
            "schedule" -> navController.navigate(Routes.HOME) { launchSingleTop = true }
            "alarm" -> navController.navigate(Routes.ALARM_PLACEHOLDER) { launchSingleTop = true }
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            Scaffold { padding ->
                ScheduleScreen(
                    onImportClick = { navController.navigate(Routes.IMPORT) },
                    onManageClick = { navController.navigate(Routes.MANAGE) },
                    onOnlineCourseClick = { navController.navigate(Routes.courseEdit(it)) },
                    onOnlineAlarmClick = { navController.navigate(Routes.ALARM_PLACEHOLDER) },
                    targetDate = targetDate,
                    modifier = Modifier.padding(padding),
                )
            }
        }
        composable(Routes.MANAGE) {
            CourseManageScreen(
                onBack = { navController.popBackStack() },
                onAddCourse = { navController.navigate(Routes.courseEdit(0L)) },
                onEditCourse = { id -> navController.navigate(Routes.courseEdit(id)) },
                onManageTerms = { navController.navigate(Routes.TERM) },
                onManageSections = { navController.navigate(Routes.SECTIONS) },
                onManageWidgets = { navController.navigate(Routes.WIDGET_SETTINGS) },
            )
        }
        composable(Routes.WIDGET_SETTINGS) { WidgetSettingsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.ALARM_PLACEHOLDER) { AlarmPlaceholder(onBack = { navController.popBackStack() }) }
        composable(Routes.COURSE_EDIT) { CourseEditScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.TERM) { TermManageScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SECTIONS) { SectionTemplateScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.IMPORT) {
            Scaffold { padding ->
                ImportFlow(
                    onBack = { navController.popBackStack() },
                    initialText = sharedText,
                    onImported = {
                        navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                    },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun AlarmPlaceholder(onBack: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("课程提醒将在 M8 提供", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("返回") }
        }
    }
}

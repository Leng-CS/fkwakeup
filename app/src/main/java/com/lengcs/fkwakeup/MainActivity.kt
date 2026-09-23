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
import com.lengcs.fkwakeup.feature.settings.CourseReminderScreen
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate

object Routes {
    const val HOME = "home"
    const val IMPORT = "import"
    const val MANAGE = "manage"
    const val WIDGET_SETTINGS = "widget-settings"
    const val REMINDER = "reminder/{courseId}?occurrenceKey={occurrenceKey}"
    const val COURSE_EDIT = "course/{courseId}"
    const val TERM = "term"
    const val SECTIONS = "sections"
    fun courseEdit(courseId: Long = 0L) = "course/$courseId"
    fun reminder(courseId: Long, occurrenceKey: String? = null) = buildString {
        append("reminder/$courseId")
        occurrenceKey?.let { append("?occurrenceKey=").append(Uri.encode(it)) }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val incomingText = mutableStateOf<String?>(null)
    private val appLink = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        acceptIntent(intent)
        setContent {
            FkwakeupTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    FkwakeupApp(sharedText = incomingText.value, appLink = appLink.value)
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
        appLink.value = intent?.data?.takeIf { it.scheme == "fkwakeup" }
    }
}

@Composable
private fun FkwakeupApp(sharedText: String?, appLink: Uri?) {
    val navController = rememberNavController()
    val targetDate = appLink?.takeIf { it.lastPathSegment == "schedule" }
        ?.getQueryParameter("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) navController.navigate(Routes.IMPORT) { launchSingleTop = true }
    }
    LaunchedEffect(appLink) {
        when (appLink?.lastPathSegment) {
            "schedule" -> navController.navigate(Routes.HOME) { launchSingleTop = true }
            "reminder" -> appLink.getQueryParameter("courseId")?.toLongOrNull()?.let { courseId ->
                navController.navigate(Routes.reminder(courseId, appLink.getQueryParameter("occurrenceKey"))) { launchSingleTop = true }
            }
            "course" -> appLink.getQueryParameter("courseId")?.toLongOrNull()?.let { courseId ->
                navController.navigate(Routes.courseEdit(courseId)) { launchSingleTop = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            Scaffold { padding ->
                ScheduleScreen(
                    onImportClick = { navController.navigate(Routes.IMPORT) },
                    onManageClick = { navController.navigate(Routes.MANAGE) },
                    onOnlineCourseClick = { navController.navigate(Routes.courseEdit(it)) },
                    onOnlineAlarmClick = { navController.navigate(Routes.reminder(it)) },
                    onReminderClick = { courseId, key -> navController.navigate(Routes.reminder(courseId, key)) },
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
        composable(Routes.REMINDER) { CourseReminderScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.COURSE_EDIT) { entry ->
            val courseId = entry.arguments?.getString("courseId")?.toLongOrNull() ?: 0L
            CourseEditScreen(
                onBack = { navController.popBackStack() },
                onReminder = { if (courseId > 0L) navController.navigate(Routes.reminder(courseId)) },
            )
        }
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

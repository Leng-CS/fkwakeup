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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lengcs.fkwakeup.core.designsystem.theme.FkwakeupTheme
import com.lengcs.fkwakeup.feature.importexport.ImportFlow
import dagger.hilt.android.AndroidEntryPoint

object Routes {
    const val HOME = "home"
    const val IMPORT = "import"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val startDestination = if (sharedText.isNullOrBlank()) Routes.HOME else Routes.IMPORT

        setContent {
            FkwakeupTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    FkwakeupApp(
                        startDestination = startDestination,
                        sharedText = sharedText,
                    )
                }
            }
        }
    }
}

@Composable
private fun FkwakeupApp(
    startDestination: String,
    sharedText: String?,
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.HOME) {
            HomeScreen(onImportClick = { navController.navigate(Routes.IMPORT) })
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

@Composable
private fun HomeScreen(onImportClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "fkwakeup", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "还没有课表，导入一份试试",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(
            onClick = onImportClick,
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(),
        ) {
            Text("导入课表")
        }
    }
}

package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.MainViewModel
import com.example.ui.TranscriptionUiState
import com.example.ui.components.TranscriptionProcessingDialog
import com.example.ui.screens.ArchivedNotesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NoteDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VocabularyScreen
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val transcriptionState by viewModel.transcriptionState.collectAsState()

    // Global Toast listener
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(VoiceTranscriberApp.instance, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Modal Processing Dialog during speech transcription
    TranscriptionProcessingDialog(
        state = transcriptionState,
        onDismiss = {
            val state = transcriptionState
            if (state is TranscriptionUiState.Success) {
                viewModel.dismissTranscriptionDialog()
                navController.navigate("note_detail/${state.noteId}")
            } else {
                viewModel.dismissTranscriptionDialog()
            }
        },
        onOpenSettings = {
            viewModel.dismissTranscriptionDialog()
            navController.navigate("settings")
        }
    )

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToNoteDetail = { id -> navController.navigate("note_detail/$id") },
                onNavigateToArchived = { navController.navigate("archived") },
                onNavigateToVocabulary = { navController.navigate("vocabulary") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }

        composable(
            route = "note_detail/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
            NoteDetailScreen(
                noteId = noteId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Backward compatibility route
        composable(
            route = "result/{recordId}",
            arguments = listOf(navArgument("recordId") { type = NavType.LongType })
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getLong("recordId") ?: 0L
            NoteDetailScreen(
                noteId = recordId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("archived") {
            ArchivedNotesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNoteDetail = { id -> navController.navigate("note_detail/$id") }
            )
        }

        composable("vocabulary") {
            VocabularyScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

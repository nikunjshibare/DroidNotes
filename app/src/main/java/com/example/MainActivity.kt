package com.example

import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.NoteEntity
import com.example.sync.ICloudNotesSyncBridge
import com.example.ui.AuthWebViewScreen
import com.example.ui.FoldersDrawer
import com.example.ui.MainViewModel
import com.example.ui.NoteEditorScreen
import com.example.ui.NotesListScreen
import com.example.ui.theme.DroidNotesTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DroidNotesTheme {
                val viewModel: MainViewModel = viewModel()
                MainAppNavHost(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppNavHost(viewModel: MainViewModel) {
    val showAuthScreen by viewModel.showAuthScreen.collectAsState()
    val showFolderDrawer by viewModel.showFolderDrawer.collectAsState()
    val notes by viewModel.notes.collectAsState()

    var activeEditingNoteId by remember { mutableStateOf<String?>(null) }

    // Headless background WebView for silent CloudKit sync when authenticated
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    if (isAuthenticated && !showAuthScreen) {
        Box(modifier = Modifier.size(0.dp)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(1, 1)
                        viewModel.attachWebView(this, isHeadless = true)
                        loadUrl(ICloudNotesSyncBridge.ICLOUD_NOTES_URL)
                    }
                }
            )
        }
    }

    val currentEditingNote = remember(activeEditingNoteId, notes) {
        notes.find { it.id == activeEditingNoteId }
    }

    when {
        showAuthScreen -> {
            AuthWebViewScreen(
                viewModel = viewModel,
                onDismiss = { viewModel.setShowAuthScreen(false) }
            )
            BackHandler {
                viewModel.setShowAuthScreen(false)
            }
        }

        currentEditingNote != null -> {
            NoteEditorScreen(
                note = currentEditingNote,
                viewModel = viewModel,
                onBack = { activeEditingNoteId = null }
            )
        }

        else -> {
            NotesListScreen(
                viewModel = viewModel,
                onNoteClick = { note ->
                    activeEditingNoteId = note.id
                },
                onCreateNote = { isChecklist ->
                    val newNote = viewModel.createNote(
                        title = "",
                        body = "",
                        isChecklist = isChecklist
                    )
                    activeEditingNoteId = newNote.id
                }
            )
        }
    }

    if (showFolderDrawer) {
        FoldersDrawer(
            viewModel = viewModel,
            onDismiss = { viewModel.setShowFolderDrawer(false) }
        )
    }
}

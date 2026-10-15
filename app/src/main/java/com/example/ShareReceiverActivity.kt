package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.data.local.AppDatabase
import com.example.data.repository.NotesRepository
import com.example.sync.ICloudNotesSyncBridge
import com.example.ui.theme.AppleNotesGold
import com.example.ui.theme.AppleNotesGreen
import com.example.ui.theme.DroidNotesTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)

        setContent {
            DroidNotesTheme {
                ShareReceiverContent(
                    sharedText = text,
                    sharedSubject = subject,
                    onOpenApp = {
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(mainIntent)
                        finish()
                    },
                    onDismiss = {
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun ShareReceiverContent(
    sharedText: String,
    sharedSubject: String?,
    onOpenApp: () -> Unit,
    onDismiss: () -> Unit
) {
    var isSaved by remember { mutableStateOf(false) }
    var noteTitle by remember { mutableStateOf("New Note") }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(sharedText) {
        if (sharedText.isNotBlank()) {
            val db = AppDatabase.getInstance(context)
            val repository = NotesRepository(db.notesDao())
            val title = sharedSubject?.ifBlank { null }
                ?: sharedText.lines().firstOrNull { it.isNotBlank() }?.take(60)
                ?: "Shared Note"
            noteTitle = title

            launch(Dispatchers.IO) {
                val createdNote = repository.createNote(
                    title = title,
                    body = sharedText,
                    folderId = "quick_notes",
                    folderName = "Quick Notes"
                )

                // Trigger sync bridge push
                val bridge = ICloudNotesSyncBridge(context, this) { _, _ -> }
                bridge.pushNoteToICloud(createdNote)

                isSaved = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("share_receiver_card")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.CheckCircle else Icons.Default.NoteAdd,
                    contentDescription = "Save Note",
                    tint = if (isSaved) AppleNotesGreen else AppleNotesGold,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isSaved) "Saved to iCloud Notes!" else "Adding to iCloud Notes...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "\"$noteTitle\"",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "iCloud",
                        tint = AppleNotesGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Saved in Quick Notes • Queued to MacBook",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_dismiss_button")
                    ) {
                        Text("Done")
                    }

                    Button(
                        onClick = onOpenApp,
                        colors = ButtonDefaults.buttonColors(containerColor = AppleNotesGold),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_open_app_button")
                    ) {
                        Text("View Notes")
                    }
                }
            }
        }
    }
}

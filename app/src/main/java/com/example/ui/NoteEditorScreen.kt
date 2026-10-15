package com.example.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChecklistItem
import com.example.data.local.NoteEntity
import com.example.data.local.SyncStatus
import com.example.ui.theme.AppleNotesGold
import com.example.ui.theme.AppleNotesGoldDark
import com.example.ui.theme.AppleNotesGreen
import com.example.ui.theme.AppleNotesRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    note: NoteEntity,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val folders by viewModel.folders.collectAsState()

    var title by remember(note.id) { mutableStateOf(note.title) }
    var body by remember(note.id) { mutableStateOf(note.body) }
    var isPinned by remember(note.id) { mutableStateOf(note.isPinned) }
    var isChecklist by remember(note.id) { mutableStateOf(note.isChecklist) }
    var folderId by remember(note.id) { mutableStateOf(note.folderId) }
    var folderName by remember(note.id) { mutableStateOf(note.folderName) }

    val checklistItems = remember(note.id) {
        mutableStateListOf<ChecklistItem>().apply {
            addAll(note.parseChecklist())
        }
    }

    var newChecklistText by remember { mutableStateOf("") }
    var showFolderMenu by remember { mutableStateOf(false) }

    // Auto-save debouncing
    fun saveChanges() {
        val finalBody = if (isChecklist) {
            checklistItems.joinToString("\n") {
                if (it.isChecked) "[x] ${it.text}" else "[ ] ${it.text}"
            }
        } else {
            body
        }
        viewModel.updateNote(
            id = note.id,
            title = title,
            body = finalBody,
            isChecklist = isChecklist,
            items = if (isChecklist) checklistItems.toList() else emptyList()
        )
    }

    BackHandler {
        saveChanges()
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showFolderMenu = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("select_folder_chip")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Folder",
                                tint = AppleNotesGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = folderName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppleNotesGold
                            )
                        }

                        DropdownMenu(
                            expanded = showFolderMenu,
                            onDismissRequest = { showFolderMenu = false }
                        ) {
                            folders.forEach { f ->
                                DropdownMenuItem(
                                    text = { Text(f.name) },
                                    onClick = {
                                        folderId = f.id
                                        folderName = f.name
                                        showFolderMenu = false
                                        saveChanges()
                                    }
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            saveChanges()
                            onBack()
                        },
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppleNotesGold
                        )
                    }
                },
                actions = {
                    // Checklist toggle
                    IconButton(
                        onClick = {
                            isChecklist = !isChecklist
                            if (isChecklist && checklistItems.isEmpty() && body.isNotBlank()) {
                                body.lines().filter { it.isNotBlank() }.forEachIndexed { idx, line ->
                                    val cleanText = line.removePrefix("[ ] ").removePrefix("[x] ").removePrefix("- ")
                                    checklistItems.add(ChecklistItem(UUID.randomUUID().toString(), cleanText, false))
                                }
                            }
                            saveChanges()
                        },
                        modifier = Modifier.testTag("toggle_checklist_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = "Checklist Mode",
                            tint = if (isChecklist) AppleNotesGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Pin toggle
                    IconButton(
                        onClick = {
                            isPinned = !isPinned
                            viewModel.togglePin(note)
                        },
                        modifier = Modifier.testTag("toggle_pin_button")
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin Note",
                            tint = if (isPinned) AppleNotesGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Share
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, title)
                                putExtra(Intent.EXTRA_TEXT, "$title\n\n$body")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Note"))
                        },
                        modifier = Modifier.testTag("share_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Delete
                    IconButton(
                        onClick = {
                            viewModel.deleteNote(note.id)
                            onBack()
                        },
                        modifier = Modifier.testTag("delete_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Note",
                            tint = AppleNotesRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Timestamp and sync badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val formattedDate = remember(note.updatedAt) {
                    val sdf = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
                    sdf.format(Date(note.updatedAt))
                }
                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (note.syncStatus == SyncStatus.SYNCED) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                        contentDescription = "Sync Status",
                        tint = if (note.syncStatus == SyncStatus.SYNCED) AppleNotesGreen else AppleNotesGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (note.syncStatus == SyncStatus.SYNCED) "Synced with Mac" else "Queued to iCloud",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            // Title field
            BasicTextField(
                value = title,
                onValueChange = {
                    title = it
                    saveChanges()
                },
                textStyle = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(AppleNotesGold),
                decorationBox = { innerTextField ->
                    if (title.isBlank()) {
                        Text(
                            text = "Title",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("note_title_input")
            )

            if (isChecklist) {
                // Interactive checklist list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(checklistItems, key = { _, item -> item.id }) { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (item.isChecked) AppleNotesGold else MaterialTheme.colorScheme.surface,
                                border = if (!item.isChecked) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.onSurfaceVariant) else null,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        checklistItems[index] = item.copy(isChecked = !item.isChecked)
                                        saveChanges()
                                    }
                                    .testTag("checklist_checkbox_$index")
                            ) {
                                if (item.isChecked) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Checked",
                                        tint = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.padding(3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            BasicTextField(
                                value = item.text,
                                onValueChange = { newTxt ->
                                    checklistItems[index] = item.copy(text = newTxt)
                                    saveChanges()
                                },
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                cursorBrush = SolidColor(AppleNotesGold),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("checklist_text_$index")
                            )

                            IconButton(
                                onClick = {
                                    checklistItems.removeAt(index)
                                    saveChanges()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove Item",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Add new item row
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Checklist Item",
                                tint = AppleNotesGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            BasicTextField(
                                value = newChecklistText,
                                onValueChange = { newChecklistText = it },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (newChecklistText.isNotBlank()) {
                                        checklistItems.add(
                                            ChecklistItem(UUID.randomUUID().toString(), newChecklistText.trim(), false)
                                        )
                                        newChecklistText = ""
                                        saveChanges()
                                    }
                                }),
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(AppleNotesGold),
                                decorationBox = { inner ->
                                    if (newChecklistText.isBlank()) {
                                        Text(
                                            text = "Add checklist item...",
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                    inner()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("new_checklist_input")
                            )
                        }
                    }
                }
            } else {
                // Multi-line body text field
                BasicTextField(
                    value = body,
                    onValueChange = {
                        body = it
                        saveChanges()
                    },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(AppleNotesGold),
                    decorationBox = { innerTextField ->
                        if (body.isBlank()) {
                            Text(
                                text = "Start typing your note...",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("note_body_input")
                )
            }
        }
    }
}

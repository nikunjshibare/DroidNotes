package com.example.data.repository

import com.example.data.local.ChecklistItem
import com.example.data.local.FolderEntity
import com.example.data.local.NoteEntity
import com.example.data.local.NotesDao
import com.example.data.local.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class NotesRepository(private val notesDao: NotesDao) {

    fun getAllNotes(): Flow<List<NoteEntity>> = notesDao.getAllActiveNotes()

    fun getNotesByFolder(folderId: String): Flow<List<NoteEntity>> =
        if (folderId == "all" || folderId.isBlank()) {
            notesDao.getAllActiveNotes()
        } else {
            notesDao.getNotesByFolder(folderId)
        }

    fun searchNotes(query: String): Flow<List<NoteEntity>> = notesDao.searchNotes(query)

    fun getNoteById(id: String): Flow<NoteEntity?> = notesDao.getNoteById(id)

    suspend fun getNoteByIdOnce(id: String): NoteEntity? = notesDao.getNoteByIdOnce(id)

    fun getAllFolders(): Flow<List<FolderEntity>> = notesDao.getAllFolders()

    suspend fun createNote(
        title: String,
        body: String,
        folderId: String = "notes",
        folderName: String = "Notes",
        isPinned: Boolean = false,
        isChecklist: Boolean = false,
        checklistItems: List<ChecklistItem> = emptyList()
    ): NoteEntity {
        val snippet = body.lines().firstOrNull { it.isNotBlank() && it != title }?.take(80) ?: ""
        val now = System.currentTimeMillis()
        val newNote = NoteEntity(
            id = "note-${UUID.randomUUID()}",
            title = title.ifBlank { "New Note" },
            snippet = snippet,
            body = body,
            folderId = folderId,
            folderName = folderName,
            isPinned = isPinned,
            isChecklist = isChecklist,
            checklistItemsJson = NoteEntity.encodeChecklist(checklistItems),
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_UPLOAD
        )
        notesDao.upsertNote(newNote)
        return newNote
    }

    suspend fun updateNote(
        id: String,
        title: String,
        body: String,
        folderId: String? = null,
        folderName: String? = null,
        isChecklist: Boolean? = null,
        checklistItems: List<ChecklistItem>? = null
    ) {
        val existing = notesDao.getNoteByIdOnce(id) ?: return
        val snippet = body.lines().firstOrNull { it.isNotBlank() && it != title }?.take(80) ?: ""
        val updated = existing.copy(
            title = title.ifBlank { "Untitled" },
            snippet = snippet,
            body = body,
            folderId = folderId ?: existing.folderId,
            folderName = folderName ?: existing.folderName,
            isChecklist = isChecklist ?: existing.isChecklist,
            checklistItemsJson = checklistItems?.let { NoteEntity.encodeChecklist(it) } ?: existing.checklistItemsJson,
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_UPLOAD
        )
        notesDao.upsertNote(updated)
    }

    suspend fun togglePin(id: String, currentPinned: Boolean) {
        notesDao.updatePinStatus(id, !currentPinned, System.currentTimeMillis(), SyncStatus.PENDING_UPLOAD)
    }

    suspend fun toggleChecklistItem(noteId: String, itemId: String) {
        val note = notesDao.getNoteByIdOnce(noteId) ?: return
        val items = note.parseChecklist().toMutableList()
        val index = items.indexOfFirst { it.id == itemId }
        if (index != -1) {
            val item = items[index]
            items[index] = item.copy(isChecked = !item.isChecked)
            val updated = note.copy(
                checklistItemsJson = NoteEntity.encodeChecklist(items),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
            notesDao.upsertNote(updated)
        }
    }

    suspend fun deleteNote(id: String) {
        notesDao.softDeleteNote(id, System.currentTimeMillis(), SyncStatus.PENDING_UPLOAD)
    }

    suspend fun createFolder(name: String, icon: String = "folder") {
        val id = "folder-${UUID.randomUUID().toString().take(8)}"
        val folder = FolderEntity(
            id = id,
            name = name.trim().ifBlank { "New Folder" },
            iconName = icon,
            noteCount = 0,
            sortOrder = 10
        )
        notesDao.upsertFolder(folder)
    }

    suspend fun getPendingUploads(): List<NoteEntity> = notesDao.getPendingUploadNotes()

    suspend fun markSynced(id: String, changeTag: String = "") {
        val note = notesDao.getNoteByIdOnce(id) ?: return
        notesDao.upsertNote(
            note.copy(
                syncStatus = SyncStatus.SYNCED,
                cloudKitRecordChangeTag = changeTag.ifEmpty { note.cloudKitRecordChangeTag }
            )
        )
    }

    suspend fun reconcileCloudKitBatch(
        notes: List<NoteEntity>,
        folders: List<FolderEntity>
    ) {
        notesDao.reconcileCloudKitBatch(notes, folders)
    }

    suspend fun seedInitialDataIfEmpty() {
        // Seed default folders
        val defaultFolders = listOf(
            FolderEntity("all", "All iCloud Notes", iconName = "all", isDefault = true, sortOrder = 0),
            FolderEntity("quick_notes", "Quick Notes", iconName = "quick_note", isDefault = true, sortOrder = 1),
            FolderEntity("notes", "Notes", iconName = "folder", isDefault = true, sortOrder = 2),
            FolderEntity("work", "Mac Project", iconName = "laptop", sortOrder = 3),
            FolderEntity("travel", "Kyoto Itinerary", iconName = "flight", sortOrder = 4)
        )
        notesDao.upsertFolders(defaultFolders)

        val existing = notesDao.getNoteByIdOnce("note-welcome-icloud")
        if (existing == null) {
            val sampleChecklist = listOf(
                ChecklistItem("1", "Log in via iCloud Notes WebView (Secure Apple ID)", isChecked = true),
                ChecklistItem("2", "Capture CloudKit session cookies (X-APPLE-WEBAUTH-*)", isChecked = true),
                ChecklistItem("3", "Verify notes mirrored from MacBook Notes.app", isChecked = false),
                ChecklistItem("4", "Create or edit a note offline on Android", isChecked = false),
                ChecklistItem("5", "Watch changes appear on Mac within seconds", isChecked = false)
            )

            val notes = listOf(
                NoteEntity(
                    id = "note-welcome-icloud",
                    title = "Welcome to DroidNotes for iCloud",
                    snippet = "Bidirectional Apple Notes sync for Android without any Mac setup.",
                    body = """
                        Welcome to DroidNotes for iCloud!
                        
                        This native Android client directly synchronizes with your Apple Notes on macOS and iOS via iCloud CloudKit web session.
                        
                        Key Capabilities:
                        • Instant offline access backed by local SQLite Room Database.
                        • Zero setup on your MacBook: No scripts, no server, no configuration.
                        • Full support for pinned notes, checklist items, and folders.
                        • System Share Target: Save text and links directly from Chrome or Reddit into Apple Notes.
                    """.trimIndent(),
                    folderId = "quick_notes",
                    folderName = "Quick Notes",
                    isPinned = true,
                    isChecklist = false,
                    syncStatus = SyncStatus.SYNCED,
                    updatedAt = System.currentTimeMillis() - 1000 * 60 * 30
                ),
                NoteEntity(
                    id = "note-sync-checklist",
                    title = "iCloud Sync Readiness Checklist",
                    snippet = "Interactive checklist for testing Mac <-> Android synchronization.",
                    body = "Interactive checklist for testing Mac <-> Android synchronization.",
                    folderId = "notes",
                    folderName = "Notes",
                    isPinned = true,
                    isChecklist = true,
                    checklistItemsJson = NoteEntity.encodeChecklist(sampleChecklist),
                    syncStatus = SyncStatus.SYNCED,
                    updatedAt = System.currentTimeMillis() - 1000 * 60 * 15
                ),
                NoteEntity(
                    id = "note-mac-setup",
                    title = "MacBook Pro M-Series Notes",
                    snippet = "Terminal aliases, Homebrew packages, and display configs synced from macOS.",
                    body = """
                        MacBook Pro M-Series Notes
                        
                        Homebrew essentials:
                        brew install ripgrep fzf gh bat
                        
                        Display profiles:
                        • Studio Display 5K DCI-P3
                        • Night Shift at 19:30
                        
                        Synced seamlessly via CloudKit container com.apple.notes.
                    """.trimIndent(),
                    folderId = "work",
                    folderName = "Mac Project",
                    isPinned = false,
                    isChecklist = false,
                    syncStatus = SyncStatus.SYNCED,
                    updatedAt = System.currentTimeMillis() - 1000 * 60 * 120
                )
            )
            notesDao.upsertNotes(notes)
        }
    }
}

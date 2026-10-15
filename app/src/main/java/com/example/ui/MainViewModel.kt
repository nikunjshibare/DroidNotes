package com.example.ui

import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChecklistItem
import com.example.data.local.FolderEntity
import com.example.data.local.NoteEntity
import com.example.data.local.SyncStatus
import com.example.data.repository.NotesRepository
import com.example.sync.ICloudNotesSyncBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = NotesRepository(database.notesDao())

    val syncBridge = ICloudNotesSyncBridge(
        context = application,
        scope = viewModelScope,
        onNotesCaptured = { notes, folders ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.reconcileCloudKitBatch(notes, folders)
            }
        }
    )

    val isAuthenticated: StateFlow<Boolean> = syncBridge.isAuthenticated
    val syncState: StateFlow<String> = syncBridge.syncState

    private val _selectedFolderId = MutableStateFlow("all")
    val selectedFolderId: StateFlow<String> = _selectedFolderId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showAuthScreen = MutableStateFlow(false)
    val showAuthScreen: StateFlow<Boolean> = _showAuthScreen.asStateFlow()

    private val _showFolderDrawer = MutableStateFlow(false)
    val showFolderDrawer: StateFlow<Boolean> = _showFolderDrawer.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val folders: StateFlow<List<FolderEntity>> = repository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combine folder selection & search query to produce reactive notes list
    val notes: StateFlow<List<NoteEntity>> = combine(_selectedFolderId, _searchQuery) { folderId, query ->
        Pair(folderId, query)
    }.flatMapLatest { (folderId, query) ->
        if (query.isNotBlank()) {
            repository.searchNotes(query)
        } else {
            repository.getNotesByFolder(folderId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun selectFolder(folderId: String) {
        _selectedFolderId.value = folderId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setShowAuthScreen(show: Boolean) {
        _showAuthScreen.value = show
    }

    fun setShowFolderDrawer(show: Boolean) {
        _showFolderDrawer.value = show
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.togglePin(note.id, note.isPinned)
            syncBridge.pushNoteToICloud(note.copy(isPinned = !note.isPinned))
        }
    }

    fun toggleChecklistItem(noteId: String, itemId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleChecklistItem(noteId, itemId)
            repository.getNoteByIdOnce(noteId)?.let { updated ->
                syncBridge.pushNoteToICloud(updated)
            }
        }
    }

    fun createNote(
        title: String = "",
        body: String = "",
        folderId: String = if (_selectedFolderId.value == "all") "notes" else _selectedFolderId.value,
        isChecklist: Boolean = false,
        checklistItems: List<ChecklistItem> = emptyList()
    ): NoteEntity {
        var createdNote: NoteEntity? = null
        viewModelScope.launch(Dispatchers.IO) {
            val folderName = folders.value.find { it.id == folderId }?.name ?: "Notes"
            val note = repository.createNote(
                title = title,
                body = body,
                folderId = folderId,
                folderName = folderName,
                isChecklist = isChecklist,
                checklistItems = checklistItems
            )
            createdNote = note
            syncBridge.pushNoteToICloud(note)
        }
        return createdNote ?: NoteEntity(
            id = "pending",
            title = title,
            body = body,
            folderId = folderId,
            isChecklist = isChecklist
        )
    }

    fun updateNote(
        id: String,
        title: String,
        body: String,
        isChecklist: Boolean? = null,
        items: List<ChecklistItem>? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateNote(id, title, body, isChecklist = isChecklist, checklistItems = items)
            repository.getNoteByIdOnce(id)?.let { updated ->
                syncBridge.pushNoteToICloud(updated)
            }
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNote(id)
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.createFolder(name)
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            _isRefreshing.value = true
            syncBridge.triggerSync()
            // Check for any pending local uploads and push them
            val pending = repository.getPendingUploads()
            for (note in pending) {
                syncBridge.pushNoteToICloud(note)
            }
            kotlinx.coroutines.delay(1200)
            _isRefreshing.value = false
        }
    }

    fun attachWebView(webView: WebView, isHeadless: Boolean = false) {
        syncBridge.attachToWebView(webView, isHeadless)
    }

    fun logout() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies {
            cookieManager.flush()
            syncBridge.checkSessionCookies()
        }
    }

    fun addSharedContent(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val lines = text.lines().filter { it.isNotBlank() }
            val title = lines.firstOrNull()?.take(50) ?: "Shared from Android"
            val note = repository.createNote(
                title = title,
                body = text,
                folderId = "quick_notes",
                folderName = "Quick Notes"
            )
            syncBridge.pushNoteToICloud(note)
        }
    }
}

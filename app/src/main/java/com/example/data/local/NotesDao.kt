package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDao {

    // --- NOTES QUERIES ---

    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND folderId = :folderId ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByFolder(folderId: String): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes 
        WHERE isDeleted = 0 
        AND (title LIKE '%' || :query || '%' OR snippet LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%')
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getNoteById(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteByIdOnce(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE syncStatus = 'PENDING_UPLOAD' AND isDeleted = 0")
    suspend fun getPendingUploadNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 AND syncStatus = 'PENDING_UPLOAD'")
    suspend fun getPendingDeleteNotes(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotes(notes: List<NoteEntity>)

    @Query("UPDATE notes SET isPinned = :isPinned, updatedAt = :updatedAt, syncStatus = :syncStatus WHERE id = :id")
    suspend fun updatePinStatus(id: String, isPinned: Boolean, updatedAt: Long = System.currentTimeMillis(), syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD)

    @Query("UPDATE notes SET isDeleted = 1, updatedAt = :timestamp, syncStatus = :syncStatus WHERE id = :id")
    suspend fun softDeleteNote(id: String, timestamp: Long = System.currentTimeMillis(), syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun hardDeleteNote(id: String)

    @Query("SELECT COUNT(*) FROM notes WHERE isDeleted = 0")
    fun getNotesCount(): Flow<Int>

    // --- FOLDERS QUERIES ---

    @Query("SELECT * FROM folders ORDER BY sortOrder ASC, name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getFolderById(id: String): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolder(folder: FolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolders(folders: List<FolderEntity>)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolder(id: String)

    @Transaction
    suspend fun reconcileCloudKitBatch(
        cloudNotes: List<NoteEntity>,
        cloudFolders: List<FolderEntity>
    ) {
        if (cloudFolders.isNotEmpty()) {
            upsertFolders(cloudFolders)
        }
        for (remoteNote in cloudNotes) {
            val local = getNoteByIdOnce(remoteNote.id)
            if (local == null) {
                // New note from CloudKit
                upsertNote(remoteNote.copy(syncStatus = SyncStatus.SYNCED))
            } else if (local.syncStatus == SyncStatus.PENDING_UPLOAD) {
                // If local has pending changes, check timestamps for conflict
                if (remoteNote.updatedAt > local.updatedAt) {
                    // Remote is newer: mark conflict
                    upsertNote(local.copy(syncStatus = SyncStatus.CONFLICT))
                }
                // Keep local edit queued for upload
            } else {
                // Clean update from CloudKit
                upsertNote(remoteNote.copy(syncStatus = SyncStatus.SYNCED))
            }
        }
    }
}

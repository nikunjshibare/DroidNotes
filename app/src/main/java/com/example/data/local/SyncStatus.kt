package com.example.data.local

/**
 * Synchronization state for local Apple Notes items:
 * - SYNCED: Up to date with CloudKit container com.apple.notes.
 * - PENDING_UPLOAD: Edited locally on Android, queued for push to iCloud Notes.
 * - CONFLICT: Local change conflicts with a newer CloudKit changeTag from MacBook.
 */
enum class SyncStatus {
    SYNCED,
    PENDING_UPLOAD,
    CONFLICT
}

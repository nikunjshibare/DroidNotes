package com.example.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String {
        return status.name
    }

    @TypeConverter
    fun toSyncStatus(value: String?): SyncStatus {
        return try {
            if (value != null) SyncStatus.valueOf(value) else SyncStatus.SYNCED
        } catch (_: Exception) {
            SyncStatus.SYNCED
        }
    }
}

package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val iconName: String = "folder",
    val noteCount: Int = 0,
    val sortOrder: Int = 0,
    val isDefault: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

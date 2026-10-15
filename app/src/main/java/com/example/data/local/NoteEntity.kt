package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["updatedAt"]),
        Index(value = ["isPinned"])
    ]
)
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val snippet: String = "",
    val body: String = "",
    val folderId: String = "notes",
    val folderName: String = "Notes",
    val isPinned: Boolean = false,
    val isChecklist: Boolean = false,
    val checklistItemsJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val cloudKitRecordChangeTag: String = "",
    val isDeleted: Boolean = false
) {
    fun parseChecklist(): List<ChecklistItem> {
        if (checklistItemsJson.isBlank() || checklistItemsJson == "[]") return emptyList()
        return try {
            val jsonArray = JSONArray(checklistItemsJson)
            val items = mutableListOf<ChecklistItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                items.add(
                    ChecklistItem(
                        id = obj.optString("id", i.toString()),
                        text = obj.optString("text", ""),
                        isChecked = obj.optBoolean("isChecked", false)
                    )
                )
            }
            items
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        fun encodeChecklist(items: List<ChecklistItem>): String {
            val jsonArray = JSONArray()
            items.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("text", item.text)
                    put("isChecked", item.isChecked)
                }
                jsonArray.put(obj)
            }
            return jsonArray.toString()
        }
    }
}

data class ChecklistItem(
    val id: String,
    val text: String,
    val isChecked: Boolean = false
)

package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey
    val id: String, // e.g. "chapter_orot_1" or "paragraph_orot_orot_hatechiya_1"
    val type: String, // "CHAPTER" or "PARAGRAPH"
    val targetId: String, // ID of chapter or paragraph
    val bookId: String = "orot",
    val chapterId: String,
    val chapterTitle: String,
    val snippet: String,
    val createdAt: Long = System.currentTimeMillis()
)

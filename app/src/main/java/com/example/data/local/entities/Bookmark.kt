package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey
    val id: String, // e.g. "chapter_orot_1" or "paragraph_orot_orot_hatechiya_1"
    val type: String, // TYPE_CHAPTER or TYPE_PARAGRAPH
    val targetId: String, // ID of chapter or paragraph
    val bookId: String = "orot",
    val chapterId: String,
    val chapterTitle: String,
    val snippet: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        // Stored verbatim in the bookmarks table. Read them from here rather than
        // writing the literals again: the UI once compared against "chapter" while
        // the repository wrote "CHAPTER", which silently emptied the filter tabs.
        const val TYPE_CHAPTER = "CHAPTER"
        const val TYPE_PARAGRAPH = "PARAGRAPH"
    }
}

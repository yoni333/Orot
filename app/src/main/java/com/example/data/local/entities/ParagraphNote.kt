package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "paragraph_notes")
data class ParagraphNote(
    @PrimaryKey
    val paragraphId: String,
    val chapterId: String,
    val chapterTitle: String,
    val paragraphLetter: String,
    val noteContent: String,
    val snippet: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

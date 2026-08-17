package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "highlights")
data class Highlight(
    @PrimaryKey
    val id: String,
    val paragraphId: String,
    val chapterId: String,
    val chapterTitle: String,
    val selectedText: String,
    val startIndex: Int = -1,
    val endIndex: Int = -1,
    val colorHex: String,
    val createdAt: Long = System.currentTimeMillis()
)

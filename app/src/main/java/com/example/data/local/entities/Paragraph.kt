package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "paragraphs",
    foreignKeys = [
        ForeignKey(
            entity = Chapter::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["chapterId"])]
)
data class Paragraph(
    @PrimaryKey
    val id: String, // e.g. "orot_orot_hatechiya_1"
    val chapterId: String,
    val textContent: String,
    val paragraphLetter: String, // e.g. "א"
    val orderIndex: Int
)

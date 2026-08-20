package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.entities.Book
import com.example.data.local.entities.Bookmark
import com.example.data.local.entities.Chapter
import com.example.data.local.entities.Highlight
import com.example.data.local.entities.Paragraph
import com.example.data.local.entities.ParagraphNote
import com.example.data.local.entities.RecentSearch

@Database(
    entities = [Book::class, Chapter::class, Paragraph::class, Bookmark::class, Highlight::class, RecentSearch::class, ParagraphNote::class],
    version = 7,
    exportSchema = false
)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao

    companion object {
        @Volatile
        private var INSTANCE: LibraryDatabase? = null

        fun getDatabase(context: Context): LibraryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LibraryDatabase::class.java,
                    "library_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

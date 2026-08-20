package com.example.data.repository

import android.content.Context
import com.example.data.local.LibraryDao
import com.example.data.local.entities.Book
import com.example.data.local.entities.Bookmark
import com.example.data.local.entities.Chapter
import com.example.data.local.entities.Highlight
import com.example.data.local.entities.Paragraph
import com.example.data.local.entities.ParagraphNote
import com.example.data.local.entities.RecentSearch
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

data class OrotData(
    val book: Book,
    val chapters: List<Chapter>,
    val paragraphs: List<Paragraph>
)

class LibraryRepository(
    private val context: Context,
    private val dao: LibraryDao
) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", false)

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun getTextSizeSp(): Float = prefs.getFloat("text_size_sp", 18f)

    fun setTextSizeSp(size: Float) {
        prefs.edit().putFloat("text_size_sp", size).apply()
    }

    fun getFontFamily(): String = prefs.getString("font_family", "sans_serif") ?: "sans_serif"

    fun setFontFamily(fontFamily: String) {
        prefs.edit().putString("font_family", fontFamily).apply()
    }

    fun getAllBooks(): Flow<List<Book>> = dao.getAllBooks()
    
    fun getChaptersForBook(bookId: String): Flow<List<Chapter>> = dao.getChaptersForBook(bookId)
    
    fun getParagraphsForChapter(chapterId: String): Flow<List<Paragraph>> = dao.getParagraphsForChapter(chapterId)
    
    fun searchParagraphs(query: String): Flow<List<Paragraph>> = dao.searchParagraphs(query)

    fun getChapter(chapterId: String): Flow<Chapter> = dao.getChapter(chapterId)

    fun getCachedChapterIds(): Flow<List<String>> = dao.getCachedChapterIds()

    fun getTotalCachedParagraphsCount(): Flow<Int> = dao.getTotalCachedParagraphsCount()

    fun getAllBookmarks(): Flow<List<Bookmark>> = dao.getAllBookmarks()

    fun getBookmarkedTargetIds(): Flow<List<String>> = dao.getBookmarkedTargetIds()

    fun isTargetBookmarked(targetId: String): Flow<Boolean> = dao.isTargetBookmarked(targetId)

    suspend fun toggleChapterBookmark(chapter: Chapter, isCurrentlyBookmarked: Boolean) {
        withContext(Dispatchers.IO) {
            if (isCurrentlyBookmarked) {
                dao.deleteBookmarkByTargetId(chapter.id)
            } else {
                val bookmark = Bookmark(
                    id = "chapter_${chapter.id}",
                    type = "CHAPTER",
                    targetId = chapter.id,
                    bookId = chapter.bookId,
                    chapterId = chapter.id,
                    chapterTitle = chapter.title,
                    snippet = "פרק שלם: ${chapter.title}",
                    createdAt = System.currentTimeMillis()
                )
                dao.insertBookmark(bookmark)
            }
        }
    }

    suspend fun toggleParagraphBookmark(paragraph: Paragraph, chapterTitle: String, isCurrentlyBookmarked: Boolean) {
        withContext(Dispatchers.IO) {
            if (isCurrentlyBookmarked) {
                dao.deleteBookmarkByTargetId(paragraph.id)
            } else {
                val snippet = if (paragraph.textContent.length > 120) {
                    paragraph.textContent.take(120).trimEnd() + "..."
                } else {
                    paragraph.textContent
                }
                val bookmark = Bookmark(
                    id = "paragraph_${paragraph.id}",
                    type = "PARAGRAPH",
                    targetId = paragraph.id,
                    bookId = "orot",
                    chapterId = paragraph.chapterId,
                    chapterTitle = chapterTitle,
                    snippet = "אות ${paragraph.paragraphLetter}: $snippet",
                    createdAt = System.currentTimeMillis()
                )
                dao.insertBookmark(bookmark)
            }
        }
    }

    suspend fun removeBookmarkById(bookmarkId: String) {
        withContext(Dispatchers.IO) {
            dao.deleteBookmark(bookmarkId)
        }
    }

    suspend fun removeBookmarkByTargetId(targetId: String) {
        withContext(Dispatchers.IO) {
            dao.deleteBookmarkByTargetId(targetId)
        }
    }

    fun getAllHighlights(): Flow<List<Highlight>> = dao.getAllHighlights()

    fun getHighlightsForChapter(chapterId: String): Flow<List<Highlight>> = dao.getHighlightsForChapter(chapterId)

    fun getHighlightsForParagraph(paragraphId: String): Flow<List<Highlight>> = dao.getHighlightsForParagraph(paragraphId)

    suspend fun saveHighlight(
        paragraphId: String,
        chapterId: String,
        chapterTitle: String,
        selectedText: String,
        startIndex: Int = -1,
        endIndex: Int = -1,
        colorHex: String
    ) {
        withContext(Dispatchers.IO) {
            val highlight = Highlight(
                id = "hl_${System.currentTimeMillis()}_${(1000..9999).random()}",
                paragraphId = paragraphId,
                chapterId = chapterId,
                chapterTitle = chapterTitle,
                selectedText = selectedText,
                startIndex = startIndex,
                endIndex = endIndex,
                colorHex = colorHex,
                createdAt = System.currentTimeMillis()
            )
            dao.insertHighlight(highlight)
        }
    }

    suspend fun removeHighlight(highlightId: String) {
        withContext(Dispatchers.IO) {
            dao.deleteHighlight(highlightId)
        }
    }

    suspend fun removeHighlightsForParagraph(paragraphId: String) {
        withContext(Dispatchers.IO) {
            dao.deleteHighlightsForParagraph(paragraphId)
        }
    }

    // Recent Searches
    fun getRecentSearches(): Flow<List<RecentSearch>> = dao.getRecentSearches()

    suspend fun saveRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            withContext(Dispatchers.IO) {
                dao.insertRecentSearch(RecentSearch(query = trimmed, timestamp = System.currentTimeMillis()))
            }
        }
    }

    suspend fun deleteRecentSearch(query: String) {
        withContext(Dispatchers.IO) {
            dao.deleteRecentSearch(query)
        }
    }

    suspend fun clearAllRecentSearches() {
        withContext(Dispatchers.IO) {
            dao.clearAllRecentSearches()
        }
    }

    // Paragraph Notes
    fun getAllNotes(): Flow<List<ParagraphNote>> = dao.getAllNotes()

    fun getNotesForChapter(chapterId: String): Flow<List<ParagraphNote>> = dao.getNotesForChapter(chapterId)

    fun getNoteForParagraph(paragraphId: String): Flow<ParagraphNote?> = dao.getNoteForParagraph(paragraphId)

    suspend fun saveNote(note: ParagraphNote) {
        withContext(Dispatchers.IO) {
            if (note.noteContent.isBlank()) {
                dao.deleteNote(note.paragraphId)
            } else {
                dao.insertNote(note)
            }
        }
    }

    suspend fun deleteNote(paragraphId: String) {
        withContext(Dispatchers.IO) {
            dao.deleteNote(paragraphId)
        }
    }

    /**
     * Ensures that the requested chapter content is cached locally for offline reading.
     * If the chapter is already in Room DB, no-op (offline ready).
     * If accessed for the first time, extracts and caches the chapter paragraphs into Room.
     */
    suspend fun ensureChapterCached(chapterId: String) {
        withContext(Dispatchers.IO) {
            val count = dao.getParagraphCountForChapter(chapterId)
            if (count > 0) {
                // Content already cached in local database for offline reading
                return@withContext
            }

            // First-time access: Parse source scraped data and cache the chapter into Room
            try {
                val jsonString = context.assets.open("orot_data.json").bufferedReader().use { it.readText() }
                val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()
                val adapter = moshi.adapter(OrotData::class.java)
                val orotData = adapter.fromJson(jsonString)

                if (orotData != null) {
                    // Ensure book and chapters are registered
                    dao.insertBook(orotData.book)
                    dao.insertChapters(orotData.chapters)

                    // Find and cache paragraphs for this chapter
                    val chapterParagraphs = orotData.paragraphs.filter { it.chapterId == chapterId }
                    if (chapterParagraphs.isNotEmpty()) {
                        dao.insertParagraphs(chapterParagraphs)
                    } else {
                        // If no specific filtered paragraph, save all to guarantee complete offline data
                        dao.insertParagraphs(orotData.paragraphs)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Pre-caches the entire book and all chapters for complete offline reading.
     */
    suspend fun cacheAllContent() {
        withContext(Dispatchers.IO) {
            try {
                val jsonString = context.assets.open("orot_data.json").bufferedReader().use { it.readText() }
                val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()
                val adapter = moshi.adapter(OrotData::class.java)
                val orotData = adapter.fromJson(jsonString)

                if (orotData != null) {
                    dao.insertFullBookData(orotData.book, orotData.chapters, orotData.paragraphs)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun fetchAndSaveOrot() {
        withContext(Dispatchers.IO) {
            try {
                val chapterCount = dao.getChaptersForBook("orot").firstOrNull()?.size ?: 0
                val paragraphCount = dao.getTotalCachedParagraphsCount().firstOrNull() ?: 0
                if (chapterCount >= 146 && paragraphCount >= 300) {
                    // Already fully populated
                    return@withContext
                }

                val jsonString = context.assets.open("orot_data.json").bufferedReader().use { it.readText() }
                
                val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()
                
                val adapter = moshi.adapter(OrotData::class.java)
                val orotData = adapter.fromJson(jsonString)
                
                if (orotData != null) {
                    dao.insertFullBookData(orotData.book, orotData.chapters, orotData.paragraphs)
                }
            } catch (e: Exception) {
                android.util.Log.e("LibraryRepository", "Error parsing or inserting orot_data.json", e)
            }
        }
    }
}

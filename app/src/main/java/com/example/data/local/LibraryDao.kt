package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entities.Book
import com.example.data.local.entities.Bookmark
import com.example.data.local.entities.Chapter
import com.example.data.local.entities.Highlight
import com.example.data.local.entities.Paragraph
import com.example.data.local.entities.ParagraphNote
import com.example.data.local.entities.RecentSearch
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("SELECT * FROM books")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY orderIndex ASC")
    fun getChaptersForBook(bookId: String): Flow<List<Chapter>>

    @Query("SELECT * FROM paragraphs WHERE chapterId = :chapterId ORDER BY orderIndex ASC")
    fun getParagraphsForChapter(chapterId: String): Flow<List<Paragraph>>

    // The whole book is a few hundred rows, so the table of contents holds all of it
    // rather than querying a chapter at a time as the reader expands each one.
    @Query("SELECT * FROM paragraphs ORDER BY chapterId, orderIndex ASC")
    fun getAllParagraphs(): Flow<List<Paragraph>>
    
    @Query("SELECT * FROM paragraphs WHERE textContent LIKE '%' || :query || '%' ORDER BY chapterId, orderIndex ASC")
    fun searchParagraphs(query: String): Flow<List<Paragraph>>

    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    fun getChapter(chapterId: String): Flow<Chapter>

    @Query("SELECT DISTINCT chapterId FROM paragraphs")
    fun getCachedChapterIds(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM paragraphs WHERE chapterId = :chapterId")
    suspend fun getParagraphCountForChapter(chapterId: String): Int

    @Query("SELECT COUNT(*) FROM paragraphs")
    fun getTotalCachedParagraphsCount(): Flow<Int>

    // Bookmark queries
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Query("SELECT targetId FROM bookmarks")
    fun getBookmarkedTargetIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE targetId = :targetId)")
    fun isTargetBookmarked(targetId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE targetId = :targetId")
    suspend fun deleteBookmarkByTargetId(targetId: String)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: String)

    // Highlight queries
    @Query("SELECT * FROM highlights ORDER BY createdAt DESC")
    fun getAllHighlights(): Flow<List<Highlight>>

    @Query("SELECT * FROM highlights WHERE chapterId = :chapterId ORDER BY createdAt ASC")
    fun getHighlightsForChapter(chapterId: String): Flow<List<Highlight>>

    @Query("SELECT * FROM highlights WHERE paragraphId = :paragraphId")
    fun getHighlightsForParagraph(paragraphId: String): Flow<List<Highlight>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: Highlight)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlight(id: String)

    @Query("DELETE FROM highlights WHERE paragraphId = :paragraphId")
    suspend fun deleteHighlightsForParagraph(paragraphId: String)

    // Recent Searches queries
    @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSearches(limit: Int = 15): Flow<List<RecentSearch>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSearch(recentSearch: RecentSearch)

    @Query("DELETE FROM recent_searches WHERE `query` = :query")
    suspend fun deleteRecentSearch(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clearAllRecentSearches()

    // Paragraph Notes queries
    @Query("SELECT * FROM paragraph_notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<ParagraphNote>>

    @Query("SELECT * FROM paragraph_notes WHERE chapterId = :chapterId")
    fun getNotesForChapter(chapterId: String): Flow<List<ParagraphNote>>

    @Query("SELECT * FROM paragraph_notes WHERE paragraphId = :paragraphId")
    fun getNoteForParagraph(paragraphId: String): Flow<ParagraphNote?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: ParagraphNote)

    @Query("DELETE FROM paragraph_notes WHERE paragraphId = :paragraphId")
    suspend fun deleteNote(paragraphId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<Chapter>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParagraphs(paragraphs: List<Paragraph>)

    @Transaction
    suspend fun insertFullBookData(book: Book, chapters: List<Chapter>, paragraphs: List<Paragraph>) {
        insertBook(book)
        insertChapters(chapters)
        insertParagraphs(paragraphs)
    }
}

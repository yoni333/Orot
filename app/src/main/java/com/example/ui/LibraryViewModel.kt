package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.Book
import com.example.data.local.entities.Bookmark
import com.example.data.local.entities.Chapter
import com.example.data.local.entities.Highlight
import com.example.data.local.entities.Paragraph
import com.example.data.local.entities.ParagraphNote
import com.example.data.local.entities.RecentSearch
import com.example.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(private val repository: LibraryRepository) : ViewModel() {

    init {
        fetchAndSaveOrot()
        viewModelScope.launch {
            repository.getChaptersForBook("orot").collect { chList ->
                if (chList.isNotEmpty() && _selectedChapterId.value == null) {
                    selectChapter(chList.first().id)
                }
            }
        }
    }

    val books: StateFlow<List<Book>> = repository.getAllBooks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    val chapters: StateFlow<List<Chapter>> = repository.getChaptersForBook("orot")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedChapterId = MutableStateFlow<String?>(null)
    val selectedChapterId: StateFlow<String?> = _selectedChapterId.asStateFlow()

    val cachedChapterIds: StateFlow<List<String>> = repository.getCachedChapterIds()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalCachedParagraphs: StateFlow<Int> = repository.getTotalCachedParagraphsCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentParagraphs: StateFlow<List<Paragraph>> = _selectedChapterId
        .flatMapLatest { chapterId ->
            if (chapterId != null) {
                repository.getParagraphsForChapter(chapterId)
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isDarkMode = MutableStateFlow(repository.isDarkMode())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _textSizeSp = MutableStateFlow(repository.getTextSizeSp())
    val textSizeSp: StateFlow<Float> = _textSizeSp.asStateFlow()

    private val _fontFamily = MutableStateFlow(repository.getFontFamily())
    val fontFamily: StateFlow<String> = _fontFamily.asStateFlow()

    val bookmarks: StateFlow<List<Bookmark>> = repository.getAllBookmarks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val bookmarkedTargetIds: StateFlow<Set<String>> = repository.getBookmarkedTargetIds()
        .map { it.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    val allHighlights: StateFlow<List<Highlight>> = repository.getAllHighlights()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentSearches: StateFlow<List<RecentSearch>> = repository.getRecentSearches()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allNotes: StateFlow<List<ParagraphNote>> = repository.getAllNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentChapterNotes: StateFlow<Map<String, ParagraphNote>> = _selectedChapterId
        .flatMapLatest { chapterId ->
            if (chapterId != null) {
                repository.getNotesForChapter(chapterId).map { notesList ->
                    notesList.associateBy { it.paragraphId }
                }
            } else {
                kotlinx.coroutines.flow.flowOf(emptyMap())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentHighlights: StateFlow<List<Highlight>> = _selectedChapterId
        .flatMapLatest { chapterId ->
            if (chapterId != null) {
                repository.getHighlightsForChapter(chapterId)
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<Paragraph>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isNotBlank()) {
                repository.searchParagraphs(query)
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectChapter(chapterId: String) {
        _selectedChapterId.value = chapterId
        // Caching mechanism: Ensure scraped content is cached into Room database upon access
        viewModelScope.launch {
            try {
                repository.ensureChapterCached(chapterId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cacheAllChapters() {
        viewModelScope.launch {
            try {
                repository.cacheAllContent()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun submitSearchQuery(query: String) {
        val trimmed = query.trim()
        _searchQuery.value = trimmed
        if (trimmed.isNotBlank()) {
            viewModelScope.launch {
                repository.saveRecentSearch(trimmed)
            }
        }
    }

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch {
            repository.deleteRecentSearch(query)
        }
    }

    fun clearAllRecentSearches() {
        viewModelScope.launch {
            repository.clearAllRecentSearches()
        }
    }

    fun toggleTheme() {
        val newMode = !_isDarkMode.value
        _isDarkMode.value = newMode
        repository.setDarkMode(newMode)
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        repository.setDarkMode(enabled)
    }

    fun setTextSize(size: Float) {
        val clamped = size.coerceIn(14f, 32f)
        _textSizeSp.value = clamped
        repository.setTextSizeSp(clamped)
    }

    fun increaseTextSize() {
        setTextSize(_textSizeSp.value + 2f)
    }

    fun decreaseTextSize() {
        setTextSize(_textSizeSp.value - 2f)
    }

    fun setFontFamily(fontFamily: String) {
        _fontFamily.value = fontFamily
        repository.setFontFamily(fontFamily)
    }

    fun toggleChapterBookmark(chapter: Chapter) {
        val isCurrentlyBookmarked = bookmarkedTargetIds.value.contains(chapter.id)
        viewModelScope.launch {
            repository.toggleChapterBookmark(chapter, isCurrentlyBookmarked)
        }
    }

    fun toggleParagraphBookmark(paragraph: Paragraph, chapterTitle: String) {
        val isCurrentlyBookmarked = bookmarkedTargetIds.value.contains(paragraph.id)
        viewModelScope.launch {
            repository.toggleParagraphBookmark(paragraph, chapterTitle, isCurrentlyBookmarked)
        }
    }

    fun removeBookmark(bookmarkId: String) {
        viewModelScope.launch {
            repository.removeBookmarkById(bookmarkId)
        }
    }

    fun highlightParagraphOrText(
        paragraph: Paragraph,
        chapterTitle: String,
        selectedText: String,
        startIndex: Int = -1,
        endIndex: Int = -1,
        colorHex: String
    ) {
        viewModelScope.launch {
            repository.saveHighlight(
                paragraphId = paragraph.id,
                chapterId = paragraph.chapterId,
                chapterTitle = chapterTitle,
                selectedText = selectedText,
                startIndex = startIndex,
                endIndex = endIndex,
                colorHex = colorHex
            )
        }
    }

    fun deleteHighlight(highlightId: String) {
        viewModelScope.launch {
            repository.removeHighlight(highlightId)
        }
    }

    fun clearHighlightsForParagraph(paragraphId: String) {
        viewModelScope.launch {
            repository.removeHighlightsForParagraph(paragraphId)
        }
    }

    fun saveNote(paragraph: Paragraph, chapterTitle: String, noteContent: String) {
        val trimmed = noteContent.trim()
        viewModelScope.launch {
            if (trimmed.isEmpty()) {
                repository.deleteNote(paragraph.id)
            } else {
                val snippet = if (paragraph.textContent.length > 80) {
                    paragraph.textContent.take(80) + "..."
                } else {
                    paragraph.textContent
                }
                repository.saveNote(
                    ParagraphNote(
                        paragraphId = paragraph.id,
                        chapterId = paragraph.chapterId,
                        chapterTitle = chapterTitle,
                        paragraphLetter = paragraph.paragraphLetter,
                        noteContent = trimmed,
                        snippet = snippet,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun deleteNote(paragraphId: String) {
        viewModelScope.launch {
            repository.deleteNote(paragraphId)
        }
    }

    fun fetchAndSaveOrot() {
        viewModelScope.launch {
            try {
                repository.fetchAndSaveOrot()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class LibraryViewModelFactory(private val repository: LibraryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LibraryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

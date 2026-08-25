package com.example

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entities.Bookmark
import com.example.data.local.entities.Chapter
import com.example.data.local.entities.Highlight
import com.example.data.local.entities.Paragraph
import com.example.data.local.entities.ParagraphNote
import com.example.data.local.entities.RecentSearch
import com.example.ui.LibraryViewModel
import com.example.ui.LibraryViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.example.util.DocxExporter

// Color highlight definitions
data class HighlightColorOption(
    val name: String,
    val hex: String,
    val color: Color
)

val HighlightColors = listOf(
    HighlightColorOption("צהוב", "#FFE082", Color(0xFFFFE082)),
    HighlightColorOption("ירוק", "#A5D6A7", Color(0xFFA5D6A7)),
    HighlightColorOption("תכלת", "#90CAF9", Color(0xFF90CAF9)),
    HighlightColorOption("ורוד", "#F48FB1", Color(0xFFF48FB1)),
    HighlightColorOption("סגול", "#CE93D8", Color(0xFFCE93D8)),
    HighlightColorOption("כתום", "#FFAB91", Color(0xFFFFAB91))
)

fun parseHighlightColor(hex: String, defaultColor: Color = Color(0xFFFFE082)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        defaultColor
    }
}

fun extractSentences(text: String): List<String> {
    val raw = text.split(Regex("(?<=[.!?:;])\\s+"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
    return if (raw.size > 1) raw else listOf(text)
}

class MainActivity : ComponentActivity() {

    private val viewModel: LibraryViewModel by viewModels {
        LibraryViewModelFactory((application as MyApplication).container.libraryRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            var showTableOfContents by remember { mutableStateOf(false) }
            var showBookmarksSheet by remember { mutableStateOf(false) }
            var showSettingsSheet by remember { mutableStateOf(false) }

            MyApplicationTheme(darkTheme = isDarkMode) {
                // Force RTL layout direction for Hebrew
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        LibraryScreen(
                            viewModel = viewModel,
                            isDarkMode = isDarkMode,
                            onToggleTheme = { viewModel.toggleTheme() },
                            showTableOfContents = showTableOfContents,
                            onOpenTOC = { showTableOfContents = true },
                            onDismissTOC = { showTableOfContents = false },
                            showBookmarksSheet = showBookmarksSheet,
                            onOpenBookmarks = { showBookmarksSheet = true },
                            onDismissBookmarks = { showBookmarksSheet = false },
                            showSettingsSheet = showSettingsSheet,
                            onOpenSettings = { showSettingsSheet = true },
                            onDismissSettings = { showSettingsSheet = false },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    showTableOfContents: Boolean,
    onOpenTOC: () -> Unit,
    onDismissTOC: () -> Unit,
    showBookmarksSheet: Boolean,
    onOpenBookmarks: () -> Unit,
    onDismissBookmarks: () -> Unit,
    showSettingsSheet: Boolean,
    onOpenSettings: () -> Unit,
    onDismissSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        viewModel.fetchAndSaveOrot()
    }

    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val selectedChapterId by viewModel.selectedChapterId.collectAsStateWithLifecycle()
    val paragraphs by viewModel.currentParagraphs.collectAsStateWithLifecycle()
    val cachedChapterIds by viewModel.cachedChapterIds.collectAsStateWithLifecycle()
    val textSizeSp by viewModel.textSizeSp.collectAsStateWithLifecycle()
    val fontFamily by viewModel.fontFamily.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val bookmarkedTargetIds by viewModel.bookmarkedTargetIds.collectAsStateWithLifecycle()
    val currentHighlights by viewModel.currentHighlights.collectAsStateWithLifecycle()
    val allHighlights by viewModel.allHighlights.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
    val currentChapterNotes by viewModel.currentChapterNotes.collectAsStateWithLifecycle()
    
    val keyboardController = LocalSoftwareKeyboardController.current
    var paragraphToHighlight by remember { mutableStateOf<Paragraph?>(null) }
    var paragraphForNote by remember { mutableStateOf<Paragraph?>(null) }
    
    val isSearching = searchQuery.isNotBlank()
    val displayParagraphs = if (isSearching) searchResults else paragraphs

    val context = LocalContext.current
    var notesToExport by remember { mutableStateOf<List<ParagraphNote>?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ) { uri: Uri? ->
        uri?.let { destUri ->
            try {
                context.contentResolver.openOutputStream(destUri)?.use { outputStream ->
                    notesToExport?.let { list ->
                        DocxExporter.exportToStream(list, outputStream)
                        Toast.makeText(context, "קובץ הוורד נשמר בהצלחה!", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "שגיאה בשמירת הקובץ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(chapters, selectedChapterId) {
        if (chapters.isNotEmpty() && selectedChapterId == null) {
            viewModel.selectChapter(chapters.first().id)
        }
    }

    if (showTableOfContents) {
        TableOfContentsBottomSheet(
            chapters = chapters,
            selectedChapterId = selectedChapterId,
            cachedChapterIds = cachedChapterIds,
            onSelectChapter = { chapterId ->
                viewModel.selectChapter(chapterId)
            },
            onDismiss = onDismissTOC
        )
    }

    if (showSettingsSheet) {
        SettingsBottomSheet(
            textSizeSp = textSizeSp,
            onTextSizeChange = { viewModel.setTextSize(it) },
            onDecreaseTextSize = { viewModel.decreaseTextSize() },
            onIncreaseTextSize = { viewModel.increaseTextSize() },
            fontFamily = fontFamily,
            onFontFamilyChange = { viewModel.setFontFamily(it) },
            isDarkMode = isDarkMode,
            onToggleTheme = onToggleTheme,
            onDismiss = onDismissSettings
        )
    }

    if (showBookmarksSheet) {
        BookmarksBottomSheet(
            bookmarks = bookmarks,
            highlights = allHighlights,
            notes = allNotes,
            onSelectBookmark = { bookmark ->
                viewModel.selectChapter(bookmark.chapterId)
            },
            onDeleteBookmark = { bookmark ->
                viewModel.removeBookmark(bookmark.id)
            },
            onSelectHighlight = { highlight ->
                viewModel.selectChapter(highlight.chapterId)
            },
            onDeleteHighlight = { highlight ->
                viewModel.deleteHighlight(highlight.id)
            },
            onSelectNote = { note ->
                viewModel.selectChapter(note.chapterId)
            },
            onDeleteNote = { note ->
                viewModel.deleteNote(note.paragraphId)
            },
            onExportNotesWord = {
                if (allNotes.isEmpty()) {
                    Toast.makeText(context, "אין עדיין הערות שמורות לייצוא", Toast.LENGTH_SHORT).show()
                } else {
                    notesToExport = allNotes
                    createDocumentLauncher.launch("הערות_ספר_אורות.docx")
                }
            },
            onShareNotesWord = {
                if (allNotes.isEmpty()) {
                    Toast.makeText(context, "אין עדיין הערות שמורות לשיתוף", Toast.LENGTH_SHORT).show()
                } else {
                    try {
                        val shareIntent = DocxExporter.createShareIntent(context, allNotes)
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "שתף / פתח קובץ וורד"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "שגיאה בפתיחת הקובץ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = onDismissBookmarks
        )
    }

    if (paragraphForNote != null) {
        val paragraph = paragraphForNote!!
        val currentChapter = chapters.find { it.id == selectedChapterId }
        val chapterTitle = chapters.find { it.id == paragraph.chapterId }?.title ?: (currentChapter?.title ?: "אורות")
        val existingNote = currentChapterNotes[paragraph.id] ?: allNotes.find { it.paragraphId == paragraph.id }

        ParagraphNoteDialog(
            paragraph = paragraph,
            chapterTitle = chapterTitle,
            existingNote = existingNote,
            onSaveNote = { content ->
                viewModel.saveNote(paragraph, chapterTitle, content)
                paragraphForNote = null
            },
            onDeleteNote = {
                viewModel.deleteNote(paragraph.id)
                paragraphForNote = null
            },
            onDismiss = {
                paragraphForNote = null
            }
        )
    }

    if (paragraphToHighlight != null) {
        val paragraph = paragraphToHighlight!!
        val currentChapter = chapters.find { it.id == selectedChapterId }
        val chapterTitle = chapters.find { it.id == paragraph.chapterId }?.title ?: (currentChapter?.title ?: "אורות")
        val existingHls = allHighlights.filter { it.paragraphId == paragraph.id }

        HighlightDialog(
            paragraph = paragraph,
            chapterTitle = chapterTitle,
            existingHighlights = existingHls,
            onSaveHighlight = { selectedText, colorHex ->
                viewModel.highlightParagraphOrText(
                    paragraph = paragraph,
                    chapterTitle = chapterTitle,
                    selectedText = selectedText,
                    colorHex = colorHex
                )
                paragraphToHighlight = null
            },
            onClearHighlights = {
                viewModel.clearHighlightsForParagraph(paragraph.id)
                paragraphToHighlight = null
            },
            onDismiss = {
                paragraphToHighlight = null
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onOpenTOC,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .size(40.dp)
                    .testTag("toc_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "תוכן העניינים",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "אורות",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .size(40.dp)
                    .testTag("open_settings_header_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "הגדרות",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { 
                Text(
                    text = "חפש בכתבי הרב...",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.submitSearchQuery(searchQuery)
                                keyboardController?.hide()
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("submit_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "בצע חיפוש ושמור",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.updateSearchQuery("")
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "נקה טקסט",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    viewModel.submitSearchQuery(searchQuery)
                    keyboardController?.hide()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = if (recentSearches.isNotEmpty()) 8.dp else 16.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
                .testTag("search_text_field"),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            shape = CircleShape,
            singleLine = true
        )

        // Recent Searches Bar
        if (recentSearches.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "חיפושים קודמים",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "אחרונים:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                LazyRow(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("recent_searches_row"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(
                        count = recentSearches.size,
                        key = { index -> recentSearches[index].query }
                    ) { index ->
                        val item = recentSearches[index]
                        val isCurrentQuery = searchQuery == item.query
                        Surface(
                            onClick = {
                                viewModel.submitSearchQuery(item.query)
                                keyboardController?.hide()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrentQuery) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isCurrentQuery) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag("recent_search_chip_${item.query}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = item.query,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCurrentQuery) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isCurrentQuery) FontWeight.Bold else FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            viewModel.deleteRecentSearch(item.query)
                                        }
                                        .testTag("delete_recent_search_${item.query}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "מחק חיפוש",
                                        tint = if (isCurrentQuery) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "נקה הכל",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { viewModel.clearAllRecentSearches() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("clear_all_recent_searches_button")
                )
            }
        }

        // Main Content Area (flex-1)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Chips
            if (!isSearching) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    item {
                        Chip(
                            text = "תוכן העניינים ☰",
                            isSelected = false,
                            onClick = onOpenTOC
                        )
                    }
                    item {
                        Chip(
                            text = "סימניות (${bookmarks.size})",
                            isSelected = false,
                            onClick = onOpenBookmarks
                        )
                    }
                    item {
                        Chip(
                            text = "הערות (${allNotes.size})",
                            isSelected = false,
                            onClick = onOpenBookmarks
                        )
                    }
                    item {
                        Chip(
                            text = "הדגשות (${allHighlights.size})",
                            isSelected = false,
                            onClick = onOpenBookmarks
                        )
                    }
                    items(
                        count = chapters.size,
                        key = { index -> chapters[index].id }
                    ) { index ->
                        val chapter = chapters[index]
                        Chip(
                            text = chapter.title,
                            isSelected = chapter.id == selectedChapterId,
                            isCached = cachedChapterIds.contains(chapter.id),
                            onClick = { viewModel.selectChapter(chapter.id) }
                        )
                    }
                }
            }

            val currentChapter = chapters.find { it.id == selectedChapterId }
            val isChapterCached = selectedChapterId != null && cachedChapterIds.contains(selectedChapterId)
            val isChapterBookmarked = selectedChapterId != null && bookmarkedTargetIds.contains(selectedChapterId)

            val composeFontFamily = when(fontFamily) {
                "serif" -> androidx.compose.ui.text.font.FontFamily.Serif
                "monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
                else -> androidx.compose.ui.text.font.FontFamily.SansSerif
            }

            // Reading Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                // Quote marks background (decorative)
                Text(
                    text = "״",
                    fontSize = 60.sp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 16.dp, y = (-8).dp),
                    fontFamily = MaterialTheme.typography.titleLarge.fontFamily
                )
                Text(
                    text = "״",
                    fontSize = 60.sp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-16).dp, y = 8.dp),
                    fontFamily = MaterialTheme.typography.titleLarge.fontFamily
                )

                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            ) {
                                val totalSavedCount = bookmarks.size + allHighlights.size
                                IconButton(onClick = onOpenBookmarks, modifier = Modifier.size(36.dp)) {
                                    BadgedBox(
                                        badge = {
                                            if (totalSavedCount > 0) {
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                ) {
                                                    Text("$totalSavedCount")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FormatListBulleted,
                                            contentDescription = "סימניות והדגשות",
                                            tint = if (totalSavedCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = if (isSearching) "תוצאות חיפוש" else (currentChapter?.title ?: ""),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )

                                if (!isSearching && currentChapter != null) {
                                    IconButton(
                                        onClick = { viewModel.toggleChapterBookmark(currentChapter) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("bookmark_chapter_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isChapterBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = if (isChapterBookmarked) "הסר סימניה מפרק" else "שמור פרק בסימניות",
                                            tint = if (isChapterBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(36.dp))
                                }
                            }
                        }
                    }

                    items(
                        count = displayParagraphs.size,
                        key = { index -> displayParagraphs[index].id }
                    ) { index ->
                        val paragraph = displayParagraphs[index]
                        val isParagraphBookmarked = bookmarkedTargetIds.contains(paragraph.id)
                        val chapterTitle = chapters.find { it.id == paragraph.chapterId }?.title ?: (currentChapter?.title ?: "אורות")
                        val paragraphHighlights = currentHighlights.filter { it.paragraphId == paragraph.id }
                        val hasHighlight = paragraphHighlights.isNotEmpty()
                        val firstHighlightColor = if (hasHighlight) parseHighlightColor(paragraphHighlights.first().colorHex) else Color.Transparent
                        val existingNote = currentChapterNotes[paragraph.id] ?: allNotes.find { it.paragraphId == paragraph.id }
                        val hasNote = existingNote != null && existingNote.noteContent.isNotBlank()

                        @OptIn(ExperimentalFoundationApi::class)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isParagraphBookmarked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    else if (hasHighlight) firstHighlightColor.copy(alpha = if (isDarkMode) 0.15f else 0.18f)
                                    else Color.Transparent,
                            border = if (isParagraphBookmarked) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                     else if (hasHighlight) BorderStroke(1.dp, firstHighlightColor.copy(alpha = 0.6f))
                                     else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = { /* normal tap */ },
                                    onLongClick = {
                                        paragraphToHighlight = paragraph
                                    }
                                )
                                .testTag("paragraph_item_${paragraph.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = paragraph.paragraphLetter,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        // Feather / Note button (נוצה)
                                        IconButton(
                                            onClick = { paragraphForNote = paragraph },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("note_paragraph_${paragraph.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.HistoryEdu,
                                                contentDescription = if (hasNote) "הצג וערוך הערה (נוצה)" else "הוסף הערה לפסקה (נוצה)",
                                                tint = if (hasNote) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }

                                        // Highlight action button
                                        IconButton(
                                            onClick = { paragraphToHighlight = paragraph },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("highlight_btn_${paragraph.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.BorderColor,
                                                contentDescription = "הדגש פסקה או משפט",
                                                tint = if (hasHighlight) firstHighlightColor else MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }

                                        // Bookmark button
                                        IconButton(
                                            onClick = {
                                                viewModel.toggleParagraphBookmark(paragraph, chapterTitle)
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("bookmark_paragraph_${paragraph.id}")
                                        ) {
                                            Icon(
                                                imageVector = if (isParagraphBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = if (isParagraphBookmarked) "הסר סימניה מפסקה" else "שמור פסקה בסימניות",
                                                tint = if (isParagraphBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    if (hasHighlight) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(firstHighlightColor)
                                                .align(Alignment.CenterVertically)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }

                                    // Build annotated text with color spans
                                    val annotatedText = remember(paragraph.textContent, paragraphHighlights, isDarkMode) {
                                        buildAnnotatedString {
                                            val fullText = paragraph.textContent
                                            if (paragraphHighlights.isEmpty()) {
                                                append(fullText)
                                            } else {
                                                val wholeHighlight = paragraphHighlights.find { 
                                                    it.selectedText == fullText || (it.startIndex == -1 && it.endIndex == -1 && it.selectedText.isBlank()) 
                                                }
                                                if (wholeHighlight != null) {
                                                    val hlColor = parseHighlightColor(wholeHighlight.colorHex)
                                                    withStyle(SpanStyle(background = hlColor.copy(alpha = if (isDarkMode) 0.35f else 0.45f))) {
                                                        append(fullText)
                                                    }
                                                } else {
                                                    var currentIndex = 0
                                                    val matches = paragraphHighlights.mapNotNull { hl ->
                                                        val start = fullText.indexOf(hl.selectedText)
                                                        if (start >= 0) {
                                                            Triple(start, start + hl.selectedText.length, hl)
                                                        } else null
                                                    }.sortedBy { it.first }

                                                    if (matches.isEmpty()) {
                                                        append(fullText)
                                                    } else {
                                                        for ((start, end, hl) in matches) {
                                                            if (start > currentIndex && currentIndex < fullText.length) {
                                                                append(fullText.substring(currentIndex, minOf(start, fullText.length)))
                                                            }
                                                            if (start < fullText.length) {
                                                                val safeEnd = minOf(end, fullText.length)
                                                                if (safeEnd > start) {
                                                                    val hlColor = parseHighlightColor(hl.colorHex)
                                                                    withStyle(SpanStyle(background = hlColor.copy(alpha = if (isDarkMode) 0.35f else 0.45f))) {
                                                                        append(fullText.substring(start, safeEnd))
                                                                    }
                                                                    currentIndex = safeEnd
                                                                }
                                                            }
                                                        }
                                                        if (currentIndex < fullText.length) {
                                                            append(fullText.substring(currentIndex))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = annotatedText,
                                        fontSize = textSizeSp.sp,
                                        fontFamily = composeFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Start,
                                        lineHeight = (textSizeSp * 1.6f).sp,
                                        modifier = Modifier.weight(1f) // Text takes all horizontal space in this row
                                    )
                                } // end of text row

                                // Display saved note banner if present
                                if (hasNote && existingNote != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        onClick = { paragraphForNote = paragraph },
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("paragraph_note_preview_${paragraph.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.HistoryEdu,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = existingNote.noteContent,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "ערוך הערה",
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (displayParagraphs.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (isSearching) Icons.Default.SearchOff else Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (isSearching) "לא נמצאו תוצאות עבור \"$searchQuery\"" else "אין פסקאות להצגה",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                if (isSearching && recentSearches.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "חיפושים קודמים שלך:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(
                                            count = recentSearches.size,
                                            key = { index -> "empty_recent_${recentSearches[index].query}" }
                                        ) { index ->
                                            val queryTerm = recentSearches[index].query
                                            Surface(
                                                onClick = {
                                                    viewModel.submitSearchQuery(queryTerm)
                                                    keyboardController?.hide()
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = queryTerm,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TextSizeControls(
    textSizeSp: Float,
    onTextSizeChange: (Float) -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onDecrease,
                enabled = textSizeSp > 14f,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("decrease_text_size_button")
            ) {
                Text(
                    text = "א-",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (textSizeSp > 14f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                )
            }

            Slider(
                value = textSizeSp,
                onValueChange = onTextSizeChange,
                valueRange = 14f..32f,
                steps = 8,
                modifier = Modifier
                    .weight(1f)
                    .testTag("text_size_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline
                )
            )

            IconButton(
                onClick = onIncrease,
                enabled = textSizeSp < 32f,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("increase_text_size_button")
            ) {
                Text(
                    text = "א+",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (textSizeSp < 32f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                )
            }

            Text(
                text = "${textSizeSp.toInt()}pt",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun Chip(
    text: String,
    isSelected: Boolean,
    isCached: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isCached) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "שמור במטמון",
                    modifier = Modifier.size(13.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun getChapterSectionName(title: String): String {
    return when {
        title.startsWith("ארץ ישראל") -> "ארץ ישראל"
        title.startsWith("המלחמה") -> "המלחמה"
        title.startsWith("ישראל ותחייתו") -> "ישראל ותחייתו"
        title.startsWith("אורות התחיה") -> "אורות התחיה"
        title.startsWith("קריאה גדולה") -> "קריאה גדולה"
        title.startsWith("למהלך האידיאות") -> "למהלך האידיאות"
        title.startsWith("זרעונים") -> "זרעונים"
        title.startsWith("אורות ישראל") -> "אורות ישראל"
        else -> "אחר"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableOfContentsBottomSheet(
    chapters: List<Chapter>,
    selectedChapterId: String?,
    cachedChapterIds: List<String>,
    onSelectChapter: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var filterQuery by remember { mutableStateOf("") }
    var selectedSection by remember { mutableStateOf("all") }

    val sections = remember(chapters) {
        listOf("all" to "הכל (${chapters.size})") + listOf(
            "ארץ ישראל",
            "המלחמה",
            "ישראל ותחייתו",
            "אורות התחיה",
            "קריאה גדולה",
            "למהלך האידיאות",
            "זרעונים",
            "אורות ישראל"
        ).mapNotNull { sec ->
            val count = chapters.count { getChapterSectionName(it.title) == sec }
            if (count > 0) sec to "$sec ($count)" else null
        }
    }

    val filteredChapters = chapters.filter { chapter ->
        val matchesQuery = filterQuery.isBlank() || chapter.title.contains(filterQuery, ignoreCase = true)
        val matchesSection = selectedSection == "all" || getChapterSectionName(chapter.title) == selectedSection
        matchesQuery && matchesSection
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("toc_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "תוכן העניינים",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "${chapters.size} פרקים ומאמרים",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Quick Filter within TOC
            TextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                placeholder = {
                    Text(
                        text = "סנן פרק לפי כותרת...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search chapters",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (filterQuery.isNotEmpty()) {
                        IconButton(onClick = { filterQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "נקה",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        shape = CircleShape
                    ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                shape = CircleShape,
                singleLine = true
            )

            // Section Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                items(
                    count = sections.size,
                    key = { index -> sections[index].first }
                ) { index ->
                    val (key, label) = sections[index]
                    val isSelected = selectedSection == key
                    Surface(
                        onClick = { selectedSection = key },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Chapter list
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                items(
                    count = filteredChapters.size,
                    key = { index -> filteredChapters[index].id }
                ) { index ->
                    val chapter = filteredChapters[index]
                    val isSelected = chapter.id == selectedChapterId
                    val isCached = cachedChapterIds.contains(chapter.id)
                    val sectionName = getChapterSectionName(chapter.title)

                    Surface(
                        onClick = {
                            onSelectChapter(chapter.id)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("toc_chapter_${chapter.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                            shape = CircleShape
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${chapter.orderIndex + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = chapter.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                        ) {
                                            Text(
                                                text = sectionName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        if (isSelected) {
                                            Text(
                                                text = "• פרק נוכחי",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }

                            if (isCached) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = "זמין ללא אינטרנט",
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksBottomSheet(
    bookmarks: List<Bookmark>,
    highlights: List<Highlight> = emptyList(),
    notes: List<ParagraphNote> = emptyList(),
    onSelectBookmark: (Bookmark) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit,
    onSelectHighlight: (Highlight) -> Unit = {},
    onDeleteHighlight: (Highlight) -> Unit = {},
    onSelectNote: (ParagraphNote) -> Unit = {},
    onDeleteNote: (ParagraphNote) -> Unit = {},
    onExportNotesWord: () -> Unit = {},
    onShareNotesWord: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var selectedFilter by remember { mutableStateOf("all") } // "all", "chapter", "paragraph", "note", "highlight"

    val totalCount = bookmarks.size + highlights.size + notes.size

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("bookmarks_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "סימניות, הערות והדגשות",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "$totalCount פריטים",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "all",
                        onClick = { selectedFilter = "all" },
                        label = { Text("הכל ($totalCount)") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "note",
                        onClick = { selectedFilter = "note" },
                        label = { Text("✍️ הערות (${notes.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "chapter",
                        onClick = { selectedFilter = "chapter" },
                        label = { Text("פרקים (${bookmarks.count { it.type == "chapter" }})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "paragraph",
                        onClick = { selectedFilter = "paragraph" },
                        label = { Text("פסקאות (${bookmarks.count { it.type == "paragraph" }})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "highlight",
                        onClick = { selectedFilter = "highlight" },
                        label = { Text("הדגשות (${highlights.size})") }
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Export to Word banner when notes exist
            if (notes.isNotEmpty() && (selectedFilter == "all" || selectedFilter == "note")) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("export_notes_word_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "הורדת כל ההערות לוורד",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "קובץ מסמך .docx מעוצב בעברית",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = onExportNotesWord,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("download_word_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "הורד קובץ וורד",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("הורד", fontSize = 13.sp)
                            }

                            FilledTonalButton(
                                onClick = onShareNotesWord,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("share_word_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "שתף קובץ וורד",
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }

            val showBookmarks = selectedFilter == "all" || selectedFilter == "chapter" || selectedFilter == "paragraph"
            val showHighlights = selectedFilter == "all" || selectedFilter == "highlight"
            val showNotes = selectedFilter == "all" || selectedFilter == "note"

            val filteredBookmarks = when (selectedFilter) {
                "chapter" -> bookmarks.filter { it.type == "chapter" }
                "paragraph" -> bookmarks.filter { it.type == "paragraph" }
                else -> bookmarks
            }

            val hasItems = (showBookmarks && filteredBookmarks.isNotEmpty()) || 
                           (showHighlights && highlights.isNotEmpty()) || 
                           (showNotes && notes.isNotEmpty())

            if (!hasItems) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (totalCount == 0) "אין סימניות, הערות או הדגשות עדיין" else "אין פריטים בקטגוריה זו",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "לחץ על סמל הנוצה כדי להוסיף הערה אישית לפסקה, או שמור בסימניות",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    // Notes section
                    if (showNotes && notes.isNotEmpty()) {
                        item {
                            Text(
                                text = "✍️ הערות נוצה אישיות (${notes.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(
                            count = notes.size,
                            key = { index -> "note_${notes[index].paragraphId}" }
                        ) { index ->
                            val note = notes[index]

                            Surface(
                                onClick = {
                                    onSelectNote(note)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("saved_note_item_${note.paragraphId}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.padding(end = 12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.HistoryEdu,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "הערה",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${note.chapterTitle} • אות ${note.paragraphLetter}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = note.noteContent,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                maxLines = 2,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteNote(note) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("delete_note_${note.paragraphId}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "מחק הערה",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Highlights section if relevant
                    if (showHighlights && highlights.isNotEmpty()) {
                        item {
                            if (showNotes && notes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            Text(
                                text = "🎨 הדגשות טקסט (${highlights.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(
                            count = highlights.size,
                            key = { index -> "hl_${highlights[index].id}" }
                        ) { index ->
                            val highlight = highlights[index]
                            val hlColor = parseHighlightColor(highlight.colorHex)

                            Surface(
                                onClick = {
                                    onSelectHighlight(highlight)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, hlColor.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("highlight_saved_item_${highlight.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(hlColor)
                                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = highlight.chapterTitle,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (highlight.selectedText.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = hlColor.copy(alpha = 0.25f)
                                                ) {
                                                    Text(
                                                        text = highlight.selectedText,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 2,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteHighlight(highlight) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("delete_highlight_${highlight.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "מחק הדגשה",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bookmarks section
                    if (showBookmarks && filteredBookmarks.isNotEmpty()) {
                        if ((showHighlights && highlights.isNotEmpty()) || (showNotes && notes.isNotEmpty())) {
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "🔖 סימניות (${filteredBookmarks.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }

                        items(
                            count = filteredBookmarks.size,
                            key = { index -> "bm_${filteredBookmarks[index].id}" }
                        ) { index ->
                            val bookmark = filteredBookmarks[index]
                            val isChapter = bookmark.type == "chapter"

                            Surface(
                                onClick = {
                                    onSelectBookmark(bookmark)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bookmark_item_${bookmark.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isChapter) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.padding(end = 12.dp)
                                        ) {
                                            Text(
                                                text = if (isChapter) "פרק" else "פסקה",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isChapter) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = bookmark.chapterTitle,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (bookmark.snippet.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = bookmark.snippet,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    maxLines = 2,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteBookmark(bookmark) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("delete_bookmark_${bookmark.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "מחק סימניה",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HighlightDialog(
    paragraph: Paragraph,
    chapterTitle: String,
    existingHighlights: List<Highlight>,
    onSaveHighlight: (selectedText: String, colorHex: String) -> Unit,
    onClearHighlights: () -> Unit,
    onDismiss: () -> Unit
) {
    val sentences = remember(paragraph.textContent) { extractSentences(paragraph.textContent) }
    var selectedTextOption by remember { mutableStateOf(paragraph.textContent) }
    var isWholeParagraphSelected by remember { mutableStateOf(true) }
    var selectedColor by remember { mutableStateOf(HighlightColors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BorderColor,
                        contentDescription = null,
                        tint = selectedColor.color,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "הדגשת טקסט בצבעים",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$chapterTitle • אות ${paragraph.paragraphLetter}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Select section: Whole paragraph vs sentences
                Text(
                    text = "בחר קטע להדגשה:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                FilterChip(
                    selected = isWholeParagraphSelected,
                    onClick = {
                        isWholeParagraphSelected = true
                        selectedTextOption = paragraph.textContent
                    },
                    label = { Text("✨ כל הפסקה (מלאה)") },
                    modifier = Modifier.fillMaxWidth().testTag("select_full_paragraph_chip")
                )

                if (sentences.size > 1) {
                    Text(
                        text = "או בחר משפט ספציפי:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        sentences.forEachIndexed { idx, sentence ->
                            val isThisSelected = !isWholeParagraphSelected && selectedTextOption == sentence
                            Surface(
                                onClick = {
                                    isWholeParagraphSelected = false
                                    selectedTextOption = sentence
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isThisSelected) selectedColor.color.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isThisSelected) selectedColor.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("select_sentence_$idx")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 1}.",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = sentence,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 3,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                // Color Selection
                Text(
                    text = "בחר צבע הדגשה:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HighlightColors.forEach { colorOption ->
                        val isSelected = selectedColor.hex == colorOption.hex
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedColor = colorOption }
                                .padding(4.dp)
                                .testTag("color_option_${colorOption.hex}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(colorOption.color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.DarkGray.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "נבחר",
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = colorOption.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Live Preview
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = selectedColor.color.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, selectedColor.color),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "תצוגה מקדימה:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedTextOption,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveHighlight(selectedTextOption, selectedColor.hex)
                },
                modifier = Modifier.testTag("save_highlight_button")
            ) {
                Text("הדגש ושמור")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (existingHighlights.isNotEmpty()) {
                    TextButton(
                        onClick = onClearHighlights,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("clear_highlight_button")
                    ) {
                        Text("הסר הדגשה")
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("cancel_highlight_button")
                ) {
                    Text("ביטול")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    textSizeSp: Float,
    onTextSizeChange: (Float) -> Unit,
    onDecreaseTextSize: () -> Unit,
    onIncreaseTextSize: () -> Unit,
    fontFamily: String,
    onFontFamilyChange: (String) -> Unit,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("settings_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "הגדרות תצוגה",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // Text Size
            Text(
                text = "גודל גופן",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TextSizeControls(
                textSizeSp = textSizeSp,
                onTextSizeChange = onTextSizeChange,
                onDecrease = onDecreaseTextSize,
                onIncrease = onIncreaseTextSize
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Font Type
            Text(
                text = "סוג גופן",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "sans_serif" to "מודרני",
                    "serif" to "קלאסי",
                    "monospace" to "מכונת כתיבה"
                ).forEach { (id, label) ->
                    val isSelected = fontFamily == id
                    Surface(
                        onClick = { onFontFamilyChange(id) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = label,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Dark Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "מצב לילה",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "שנה את ערכת הנושא",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { onToggleTheme() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "כל התוכן הועתק מויקיטקסט והוא תחת רישיון Creative Commons ייחוס-שיתוף זהה 4.0\nליצירת קשר והוספת ספרים: yoni333@gmail.com",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun ParagraphNoteDialog(
    paragraph: Paragraph,
    chapterTitle: String,
    existingNote: ParagraphNote?,
    onSaveNote: (String) -> Unit,
    onDeleteNote: () -> Unit,
    onDismiss: () -> Unit
) {
    var noteText by remember { mutableStateOf(existingNote?.noteContent ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("paragraph_note_dialog"),
        icon = {
            Icon(
                imageVector = Icons.Default.HistoryEdu,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "הערה אישית לפסקה",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$chapterTitle • אות ${paragraph.paragraphLetter}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Paragraph excerpt preview
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "טקסט הפסקה:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = paragraph.textContent,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                // Note input text field
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("הערות, ביאורים או חידושים") },
                    placeholder = { Text("כתוב כאן הערה או ביאור אישי לפסקה זו...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .testTag("note_input_field"),
                    maxLines = 8,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                if (existingNote != null) {
                    Text(
                        text = "💾 ההערה נשמרת אוטומטית מקומית במכשיר",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveNote(noteText) },
                modifier = Modifier.testTag("save_note_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("שמור הערה")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (existingNote != null) {
                    TextButton(
                        onClick = onDeleteNote,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("delete_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("מחק")
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("cancel_note_button")
                ) {
                    Text("ביטול")
                }
            }
        }
    )
}

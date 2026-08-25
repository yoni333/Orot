package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entities.Bookmark
import com.example.data.local.entities.Highlight
import com.example.data.local.entities.Paragraph
import com.example.data.local.entities.ParagraphNote
import com.example.ui.LibraryViewModel
import com.example.ui.components.Chip
import com.example.ui.components.parseHighlightColor
import com.example.ui.dialogs.HighlightDialog
import com.example.ui.dialogs.ParagraphNoteDialog
import com.example.ui.sheets.BookmarksBottomSheet
import com.example.ui.sheets.SettingsBottomSheet
import com.example.ui.sheets.TableOfContentsBottomSheet
import com.example.util.DocxExporter

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

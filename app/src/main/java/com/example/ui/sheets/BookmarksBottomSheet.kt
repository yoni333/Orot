package com.example.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.Bookmark
import com.example.data.local.entities.Highlight
import com.example.data.local.entities.ParagraphNote
import com.example.ui.components.parseHighlightColor

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
                        label = { Text("פרקים (${bookmarks.count { it.type == Bookmark.TYPE_CHAPTER }})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "paragraph",
                        onClick = { selectedFilter = "paragraph" },
                        label = { Text("פסקאות (${bookmarks.count { it.type == Bookmark.TYPE_PARAGRAPH }})") }
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
                "chapter" -> bookmarks.filter { it.type == Bookmark.TYPE_CHAPTER }
                "paragraph" -> bookmarks.filter { it.type == Bookmark.TYPE_PARAGRAPH }
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
                            val isChapter = bookmark.type == Bookmark.TYPE_CHAPTER

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

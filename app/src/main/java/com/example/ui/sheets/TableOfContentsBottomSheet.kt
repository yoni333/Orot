package com.example.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.Chapter
import com.example.data.local.entities.Paragraph

private const val FILTER_SNIPPET_LENGTH = 90

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableOfContentsBottomSheet(
    chapters: List<Chapter>,
    paragraphsByChapter: Map<String, List<Paragraph>>,
    selectedChapterId: String?,
    cachedChapterIds: List<String>,
    onSelectChapter: (String) -> Unit,
    onSelectParagraph: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    // An expanded chapter can carry 72 letter chips, so the sheet needs the full height
    // rather than opening half way.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var filterQuery by remember { mutableStateOf("") }
    // Opens on the chapter being read, so the letters of the current place are the first
    // thing in view. Single expansion: opening one closes the other.
    var expandedChapterId by remember { mutableStateOf(selectedChapterId) }

    val isFiltering = filterQuery.isNotBlank()

    // Filtering searches paragraph text as well as chapter titles, so a half-remembered
    // phrase finds its paragraph. Chapter order comes from the chapters list.
    val filterMatches: List<Pair<Chapter, Paragraph>> = remember(filterQuery, chapters, paragraphsByChapter) {
        if (filterQuery.isBlank()) {
            emptyList()
        } else {
            chapters.flatMap { chapter ->
                val titleMatches = chapter.title.contains(filterQuery, ignoreCase = true)
                (paragraphsByChapter[chapter.id] ?: emptyList())
                    .filter { titleMatches || it.textContent.contains(filterQuery, ignoreCase = true) }
                    .map { chapter to it }
            }
        }
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
                        text = if (isFiltering) {
                            "${filterMatches.size} תוצאות"
                        } else {
                            "${chapters.size} פרקים ומאמרים"
                        },
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
                        text = "סנן פרק או פסקה...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search chapters and paragraphs",
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
                    .padding(bottom = 12.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        shape = CircleShape
                    )
                    .testTag("toc_filter_field"),
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

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (isFiltering) {
                FilterResults(
                    matches = filterMatches,
                    query = filterQuery,
                    onSelectParagraph = onSelectParagraph,
                    onDismiss = onDismiss
                )
            } else {
                ChapterAccordion(
                    chapters = chapters,
                    paragraphsByChapter = paragraphsByChapter,
                    selectedChapterId = selectedChapterId,
                    cachedChapterIds = cachedChapterIds,
                    expandedChapterId = expandedChapterId,
                    onToggleExpanded = { chapterId ->
                        expandedChapterId = if (expandedChapterId == chapterId) null else chapterId
                    },
                    onSelectChapter = onSelectChapter,
                    onSelectParagraph = onSelectParagraph,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun ChapterAccordion(
    chapters: List<Chapter>,
    paragraphsByChapter: Map<String, List<Paragraph>>,
    selectedChapterId: String?,
    cachedChapterIds: List<String>,
    expandedChapterId: String?,
    onToggleExpanded: (String) -> Unit,
    onSelectChapter: (String) -> Unit,
    onSelectParagraph: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        // Bounded: the sheet sizes itself to its content, and a list left to fill would
        // measure against the sheet's own unresolved height.
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
    ) {
        items(
            count = chapters.size,
            key = { index -> chapters[index].id }
        ) { index ->
            val chapter = chapters[index]
            val isSelected = chapter.id == selectedChapterId
            val isExpanded = chapter.id == expandedChapterId
            val isCached = cachedChapterIds.contains(chapter.id)
            val chapterParagraphs = paragraphsByChapter[chapter.id] ?: emptyList()

            Column {
                Surface(
                    // Tapping the row still opens the chapter at its first paragraph, as
                    // it always has; the chevron beside it is what reveals the letters.
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
                            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
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
                                    Text(
                                        text = "${chapterParagraphs.size} פסקאות",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
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
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "זמין ללא אינטרנט",
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // An inner clickable, so it consumes the tap instead of opening
                        // the chapter like the surrounding row would.
                        IconButton(
                            onClick = { onToggleExpanded(chapter.id) },
                            enabled = chapterParagraphs.isNotEmpty(),
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("toc_expand_${chapter.id}")
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "סגור פסקאות" else "הצג פסקאות",
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (isExpanded && chapterParagraphs.isNotEmpty()) {
                    ParagraphLetterGrid(
                        chapterId = chapter.id,
                        paragraphs = chapterParagraphs,
                        onSelectParagraph = onSelectParagraph,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParagraphLetterGrid(
    chapterId: String,
    paragraphs: List<Paragraph>,
    onSelectParagraph: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
    ) {
        paragraphs.forEach { paragraph ->
            Surface(
                onClick = {
                    onSelectParagraph(chapterId, paragraph.id)
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.testTag("toc_paragraph_${paragraph.id}")
            ) {
                Text(
                    // Letters run up to עב׳, so the chip sizes to its content above a
                    // minimum rather than to a fixed width.
                    text = paragraph.paragraphLetter,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(min = 40.dp)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun FilterResults(
    matches: List<Pair<Chapter, Paragraph>>,
    query: String,
    onSelectParagraph: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    if (matches.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "לא נמצאו פרקים או פסקאות עבור \"$query\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    androidx.compose.foundation.lazy.LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        // Bounded: the sheet sizes itself to its content, and a list left to fill would
        // measure against the sheet's own unresolved height.
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
    ) {
        items(
            count = matches.size,
            key = { index -> matches[index].second.id }
        ) { index ->
            val (chapter, paragraph) = matches[index]
            Surface(
                onClick = {
                    onSelectParagraph(chapter.id, paragraph.id)
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("toc_paragraph_${paragraph.id}")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = paragraph.paragraphLetter,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = chapter.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = paragraph.textContent.take(FILTER_SNIPPET_LENGTH).let {
                            if (paragraph.textContent.length > FILTER_SNIPPET_LENGTH) "$it..." else it
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

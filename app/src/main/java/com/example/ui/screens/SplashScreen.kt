package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    // The title is split into two lines deliberately rather than left to wrap: as one
    // string it needed ~460dp, so on a 360-412dp phone it broke wherever it happened
    // to fit ("אורות - למרן" / "הרב קוק"). Two Texts also let the name carry real
    // display weight while the attribution stays subordinate.
    //
    // BoxWithConstraints drives the type scale off the actual width - one fixed size
    // cannot serve both a 320dp phone and an 800dp tablet. safeDrawingPadding keeps
    // everything clear of the status bar, nav bar and cutouts, since MainActivity
    // runs edge-to-edge.
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        val titleSize = when {
            maxWidth < 340.dp -> 44.sp
            maxWidth < 400.dp -> 52.sp
            maxWidth < 600.dp -> 60.sp
            else -> 76.sp
        }
        val subtitleSize = when {
            maxWidth < 340.dp -> 20.sp
            maxWidth < 400.dp -> 22.sp
            maxWidth < 600.dp -> 24.sp
            else -> 30.sp
        }
        val creditSize = if (maxWidth < 340.dp) 14.sp else 18.sp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "אורות",
                fontSize = titleSize,
                lineHeight = titleSize * 1.15f,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "למרן הרב קוק",
                fontSize = subtitleSize,
                lineHeight = subtitleSize * 1.3f,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = "לעילוי נשמת חיים סרור",
            fontSize = creditSize,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 48.dp)
        )
    }
}

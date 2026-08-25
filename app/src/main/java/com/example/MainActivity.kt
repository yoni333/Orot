package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.LibraryViewModel
import com.example.ui.LibraryViewModelFactory
import com.example.ui.screens.LibraryScreen
import com.example.ui.theme.MyApplicationTheme

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
                            // Bottom inset is deliberately dropped: the reading card runs all the
                            // way to the screen edge and the reader list carries the nav bar
                            // inset as contentPadding instead, so there is no dead strip below it.
                            modifier = Modifier.padding(
                                top = innerPadding.calculateTopPadding(),
                                start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                                end = innerPadding.calculateEndPadding(LocalLayoutDirection.current)
                            )
                        )
                    }
                }
            }
        }
    }
}

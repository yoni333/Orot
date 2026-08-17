package com.example.di

import android.content.Context
import com.example.data.local.LibraryDatabase
import com.example.data.repository.LibraryRepository

class AppContainer(private val context: Context) {

    private val database by lazy {
        LibraryDatabase.getDatabase(context)
    }

    val libraryRepository by lazy {
        LibraryRepository(context, database.libraryDao())
    }
}

package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_pdfs")
data class RecentPdfEntity(
    @PrimaryKey val filePath: String,
    val fileName: String,
    val pageCount: Int,
    val fileSizeBytes: Long,
    val lastOpenedTimestamp: Long,
    val lastPage: Int = 0
)

@Entity(tableName = "favorite_pdfs")
data class FavoritePdfEntity(
    @PrimaryKey val filePath: String,
    val isFavorite: Boolean = true,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "pdf_bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val pageIndex: Int,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "pdf_notes")
data class PdfNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val pageIndex: Int,
    val noteText: String,
    val colorHex: String = "#FFF9C4",
    val timestamp: Long = System.currentTimeMillis()
)

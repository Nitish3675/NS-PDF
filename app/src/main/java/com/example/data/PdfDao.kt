package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {
    @Query("SELECT * FROM recent_pdfs ORDER BY lastOpenedTimestamp DESC LIMIT 30")
    fun getRecentPdfs(): Flow<List<RecentPdfEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(recent: RecentPdfEntity)

    @Query("DELETE FROM recent_pdfs WHERE filePath = :filePath")
    suspend fun deleteRecent(filePath: String)

    @Query("DELETE FROM recent_pdfs")
    suspend fun clearRecent()

    @Query("SELECT filePath FROM favorite_pdfs WHERE isFavorite = 1")
    fun getFavoritePaths(): Flow<List<String>>

    @Query("SELECT COUNT(*) > 0 FROM favorite_pdfs WHERE filePath = :filePath AND isFavorite = 1")
    fun isFavorite(filePath: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setFavorite(favorite: FavoritePdfEntity)

    @Query("DELETE FROM favorite_pdfs WHERE filePath = :filePath")
    suspend fun removeFavorite(filePath: String)

    @Query("SELECT * FROM pdf_bookmarks WHERE filePath = :filePath ORDER BY pageIndex ASC")
    fun getBookmarksForPdf(filePath: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM pdf_bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Long)

    @Query("SELECT * FROM pdf_notes WHERE filePath = :filePath ORDER BY timestamp DESC")
    fun getNotesForPdf(filePath: String): Flow<List<PdfNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: PdfNoteEntity)

    @Query("DELETE FROM pdf_notes WHERE id = :id")
    suspend fun deleteNote(id: Long)
}

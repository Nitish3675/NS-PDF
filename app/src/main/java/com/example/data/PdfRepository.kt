package com.example.data

import kotlinx.coroutines.flow.Flow

class PdfRepository(private val pdfDao: PdfDao) {
    val recentPdfs: Flow<List<RecentPdfEntity>> = pdfDao.getRecentPdfs()
    val favoritePaths: Flow<List<String>> = pdfDao.getFavoritePaths()

    suspend fun addRecent(recent: RecentPdfEntity) = pdfDao.insertRecent(recent)
    suspend fun removeRecent(filePath: String) = pdfDao.deleteRecent(filePath)
    suspend fun clearRecent() = pdfDao.clearRecent()

    fun isFavorite(filePath: String): Flow<Boolean> = pdfDao.isFavorite(filePath)
    suspend fun toggleFavorite(filePath: String, isCurrentlyFavorite: Boolean) {
        if (isCurrentlyFavorite) {
            pdfDao.removeFavorite(filePath)
        } else {
            pdfDao.setFavorite(FavoritePdfEntity(filePath = filePath, isFavorite = true))
        }
    }

    fun getBookmarks(filePath: String): Flow<List<BookmarkEntity>> = pdfDao.getBookmarksForPdf(filePath)
    suspend fun addBookmark(filePath: String, pageIndex: Int, title: String) =
        pdfDao.insertBookmark(BookmarkEntity(filePath = filePath, pageIndex = pageIndex, title = title))
    suspend fun removeBookmark(id: Long) = pdfDao.deleteBookmark(id)

    fun getNotes(filePath: String): Flow<List<PdfNoteEntity>> = pdfDao.getNotesForPdf(filePath)
    suspend fun addNote(filePath: String, pageIndex: Int, text: String, colorHex: String = "#FFF9C4") =
        pdfDao.insertNote(PdfNoteEntity(filePath = filePath, pageIndex = pageIndex, noteText = text, colorHex = colorHex))
    suspend fun removeNote(id: Long) = pdfDao.deleteNote(id)
}

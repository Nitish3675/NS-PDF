package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BookmarkEntity
import com.example.data.PdfNoteEntity
import com.example.data.PdfRepository
import com.example.data.RecentPdfEntity
import com.example.pdf.OcrEngine
import com.example.pdf.PdfEngine
import com.example.pdf.SamplePdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class FileSortOption {
    NAME_ASC,
    NAME_DESC,
    DATE_DESC,
    DATE_ASC,
    SIZE_DESC,
    SIZE_ASC
}

class PdfViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PdfRepository
    private val prefs = application.getSharedPreferences("ns_pdf_prefs", Context.MODE_PRIVATE)

    private val _isOnboardingCompleted = MutableStateFlow(
        prefs.getBoolean("onboarding_completed", false)
    )
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _rawFiles = MutableStateFlow<List<File>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(FileSortOption.DATE_DESC)
    val sortOption: StateFlow<FileSortOption> = _sortOption.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _selectedFiles = MutableStateFlow<Set<File>>(emptySet())
    val selectedFiles: StateFlow<Set<File>> = _selectedFiles.asStateFlow()

    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PdfRepository(db.pdfDao())

        // Ensure sample PDFs exist on first launch
        viewModelScope.launch(Dispatchers.IO) {
            SamplePdfGenerator.ensureSamplePdfsExist(application)
            refreshFiles()
        }
    }

    val recentPdfs: StateFlow<List<RecentPdfEntity>> = repository.recentPdfs
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favoritePaths: StateFlow<List<String>> = repository.favoritePaths
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Filtered and sorted files
    val displayedFiles: StateFlow<List<File>> = combine(_rawFiles, _searchQuery, _sortOption) { files, query, sort ->
        val filtered = if (query.isBlank()) {
            files
        } else {
            files.filter { it.name.contains(query, ignoreCase = true) }
        }

        when (sort) {
            FileSortOption.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            FileSortOption.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            FileSortOption.DATE_DESC -> filtered.sortedByDescending { it.lastModified() }
            FileSortOption.DATE_ASC -> filtered.sortedBy { it.lastModified() }
            FileSortOption.SIZE_DESC -> filtered.sortedByDescending { it.length() }
            FileSortOption.SIZE_ASC -> filtered.sortedBy { it.length() }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(sort: FileSortOption) {
        _sortOption.value = sort
    }

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    fun toggleFileSelection(file: File) {
        val current = _selectedFiles.value.toMutableSet()
        if (current.contains(file)) {
            current.remove(file)
        } else {
            current.add(file)
        }
        _selectedFiles.value = current
        _isMultiSelectMode.value = current.isNotEmpty()
    }

    fun selectAllFiles() {
        _selectedFiles.value = _rawFiles.value.toSet()
        _isMultiSelectMode.value = true
    }

    fun clearSelection() {
        _selectedFiles.value = emptySet()
        _isMultiSelectMode.value = false
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        _isOnboardingCompleted.value = true
    }

    fun resetOnboarding() {
        prefs.edit().putBoolean("onboarding_completed", false).apply()
        _isOnboardingCompleted.value = false
    }

    fun clearMessage() {
        _operationMessage.value = null
    }

    fun refreshFiles() {
        val context = getApplication<Application>()
        val docsDir = File(context.filesDir, "documents")
        if (!docsDir.exists()) docsDir.mkdirs()

        val list = docsDir.listFiles { file ->
            file.isFile && file.extension.equals("pdf", ignoreCase = true)
        }?.toList() ?: emptyList()

        _rawFiles.value = list
    }

    fun recordPdfOpened(file: File, lastPage: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = PdfEngine.getPageCount(file)
            val recent = RecentPdfEntity(
                filePath = file.absolutePath,
                fileName = file.name,
                pageCount = count,
                fileSizeBytes = file.length(),
                lastOpenedTimestamp = System.currentTimeMillis(),
                lastPage = lastPage
            )
            repository.addRecent(recent)
        }
    }

    fun toggleFavorite(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val isFav = favoritePaths.value.contains(file.absolutePath)
            repository.toggleFavorite(file.absolutePath, isFav)
        }
    }

    fun deleteFile(file: File, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            if (file.exists()) {
                file.delete()
            }
            repository.removeRecent(file.absolutePath)
            repository.toggleFavorite(file.absolutePath, true) // will remove
            refreshFiles()
            _operationMessage.value = "Deleted ${file.name}"
            withContext(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }
    }

    fun deleteSelectedFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val toDelete = _selectedFiles.value
            var count = 0
            for (f in toDelete) {
                if (f.exists()) {
                    f.delete()
                    count++
                }
                repository.removeRecent(f.absolutePath)
            }
            clearSelection()
            refreshFiles()
            _operationMessage.value = "Deleted $count files"
        }
    }

    fun renameFile(file: File, newName: String): Boolean {
        var cleanName = newName.trim()
        if (!cleanName.endsWith(".pdf", ignoreCase = true)) {
            cleanName += ".pdf"
        }
        val target = File(file.parentFile, cleanName)
        if (target.exists()) {
            _operationMessage.value = "A file with that name already exists"
            return false
        }
        val success = file.renameTo(target)
        if (success) {
            refreshFiles()
            _operationMessage.value = "Renamed to $cleanName"
        } else {
            _operationMessage.value = "Failed to rename file"
        }
        return success
    }

    fun importPdfFromUri(uri: Uri, onComplete: (File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            val context = getApplication<Application>()
            val docsDir = File(context.filesDir, "documents")
            if (!docsDir.exists()) docsDir.mkdirs()

            var fileName = "Imported_${System.currentTimeMillis()}.pdf"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) fileName = name
                }
            }
            if (!fileName.endsWith(".pdf", ignoreCase = true)) fileName += ".pdf"

            val destFile = File(docsDir, fileName)
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                refreshFiles()
                recordPdfOpened(destFile)
                _operationMessage.value = "Imported $fileName successfully"
                withContext(Dispatchers.Main) {
                    onComplete(destFile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _operationMessage.value = "Failed to import file: ${e.localizedMessage}"
                withContext(Dispatchers.Main) {
                    onComplete(null)
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun createPdfFromText(title: String, content: String, onComplete: (File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            val context = getApplication<Application>()
            val docsDir = File(context.filesDir, "documents")
            var cleanTitle = title.trim().ifBlank { "Untitled_Note" }
            if (!cleanTitle.endsWith(".pdf", ignoreCase = true)) cleanTitle += ".pdf"
            val outFile = File(docsDir, cleanTitle)

            val success = PdfEngine.createPdfFromText(title, content, outFile)
            if (success) {
                refreshFiles()
                recordPdfOpened(outFile)
                _operationMessage.value = "Created $cleanTitle"
                withContext(Dispatchers.Main) { onComplete(outFile) }
            } else {
                _operationMessage.value = "Failed to create document"
                withContext(Dispatchers.Main) { onComplete(null) }
            }
            _isProcessing.value = false
        }
    }

    fun mergeFiles(sourceFiles: List<File>, outputName: String, onComplete: (File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            val context = getApplication<Application>()
            val docsDir = File(context.filesDir, "documents")
            var name = outputName.trim().ifBlank { "Merged_Document" }
            if (!name.endsWith(".pdf", ignoreCase = true)) name += ".pdf"
            val outFile = File(docsDir, name)

            val success = PdfEngine.mergePdfs(sourceFiles, outFile)
            if (success) {
                refreshFiles()
                recordPdfOpened(outFile)
                _operationMessage.value = "Merged ${sourceFiles.size} PDFs into $name"
                withContext(Dispatchers.Main) { onComplete(outFile) }
            } else {
                _operationMessage.value = "Merge failed"
                withContext(Dispatchers.Main) { onComplete(null) }
            }
            _isProcessing.value = false
        }
    }

    fun splitFile(sourceFile: File, pageIndices: List<Int>, outputName: String, onComplete: (File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            val context = getApplication<Application>()
            val docsDir = File(context.filesDir, "documents")
            var name = outputName.trim().ifBlank { "${sourceFile.nameWithoutExtension}_Split" }
            if (!name.endsWith(".pdf", ignoreCase = true)) name += ".pdf"
            val outFile = File(docsDir, name)

            val success = PdfEngine.splitPdf(sourceFile, pageIndices, outFile)
            if (success) {
                refreshFiles()
                recordPdfOpened(outFile)
                _operationMessage.value = "Extracted ${pageIndices.size} pages to $name"
                withContext(Dispatchers.Main) { onComplete(outFile) }
            } else {
                _operationMessage.value = "Split failed"
                withContext(Dispatchers.Main) { onComplete(null) }
            }
            _isProcessing.value = false
        }
    }

    fun compressFile(sourceFile: File, quality: Int, scale: Float, outputName: String, onComplete: (File?, Long, Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            val context = getApplication<Application>()
            val docsDir = File(context.filesDir, "documents")
            var name = outputName.trim().ifBlank { "${sourceFile.nameWithoutExtension}_Compressed" }
            if (!name.endsWith(".pdf", ignoreCase = true)) name += ".pdf"
            val outFile = File(docsDir, name)

            val origSize = sourceFile.length()
            val success = PdfEngine.compressPdf(sourceFile, quality, scale, outFile)
            if (success) {
                val newSize = outFile.length()
                refreshFiles()
                recordPdfOpened(outFile)
                _operationMessage.value = "Compressed from ${origSize / 1024} KB to ${newSize / 1024} KB"
                withContext(Dispatchers.Main) { onComplete(outFile, origSize, newSize) }
            } else {
                _operationMessage.value = "Compression failed"
                withContext(Dispatchers.Main) { onComplete(null, 0L, 0L) }
            }
            _isProcessing.value = false
        }
    }

    fun convertImagesToPdf(imageFiles: List<File>, pageSize: String, outputName: String, onComplete: (File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            val context = getApplication<Application>()
            val docsDir = File(context.filesDir, "documents")
            var name = outputName.trim().ifBlank { "Images_Converted" }
            if (!name.endsWith(".pdf", ignoreCase = true)) name += ".pdf"
            val outFile = File(docsDir, name)

            val success = PdfEngine.convertImagesToPdf(imageFiles, pageSize, 20, outFile)
            if (success) {
                refreshFiles()
                recordPdfOpened(outFile)
                _operationMessage.value = "Converted ${imageFiles.size} images to PDF"
                withContext(Dispatchers.Main) { onComplete(outFile) }
            } else {
                _operationMessage.value = "Image conversion failed"
                withContext(Dispatchers.Main) { onComplete(null) }
            }
            _isProcessing.value = false
        }
    }

    fun watermarkFile(
        sourceFile: File,
        watermarkText: String,
        textColor: Int,
        fontSize: Float,
        rotation: Float,
        outputName: String,
        onComplete: (File?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            val context = getApplication<Application>()
            val docsDir = File(context.filesDir, "documents")
            var name = outputName.trim().ifBlank { "${sourceFile.nameWithoutExtension}_Watermarked" }
            if (!name.endsWith(".pdf", ignoreCase = true)) name += ".pdf"
            val outFile = File(docsDir, name)

            val success = PdfEngine.addWatermark(sourceFile, watermarkText, textColor, fontSize, rotation, outFile)
            if (success) {
                refreshFiles()
                recordPdfOpened(outFile)
                _operationMessage.value = "Applied watermark to $name"
                withContext(Dispatchers.Main) { onComplete(outFile) }
            } else {
                _operationMessage.value = "Watermark failed"
                withContext(Dispatchers.Main) { onComplete(null) }
            }
            _isProcessing.value = false
        }
    }

    fun addHeaderFooter(
        sourceFile: File,
        header: String?,
        footer: String?,
        includePageNumbers: Boolean,
        outputName: String,
        onComplete: (File?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            val context = getApplication<Application>()
            val docsDir = File(context.filesDir, "documents")
            var name = outputName.trim().ifBlank { "${sourceFile.nameWithoutExtension}_Numbered" }
            if (!name.endsWith(".pdf", ignoreCase = true)) name += ".pdf"
            val outFile = File(docsDir, name)

            val success = PdfEngine.addHeaderFooterPageNumbers(sourceFile, header, footer, includePageNumbers, outFile)
            if (success) {
                refreshFiles()
                recordPdfOpened(outFile)
                _operationMessage.value = "Applied headers & page numbers"
                withContext(Dispatchers.Main) { onComplete(outFile) }
            } else {
                _operationMessage.value = "Header & page numbering failed"
                withContext(Dispatchers.Main) { onComplete(null) }
            }
            _isProcessing.value = false
        }
    }

    fun rotateFile(sourceFile: File, degrees: Int, targetPages: List<Int>?, outputName: String, onComplete: (File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            val context = getApplication<Application>()
            val docsDir = File(context.filesDir, "documents")
            var name = outputName.trim().ifBlank { "${sourceFile.nameWithoutExtension}_Rotated" }
            if (!name.endsWith(".pdf", ignoreCase = true)) name += ".pdf"
            val outFile = File(docsDir, name)

            val success = PdfEngine.rotatePages(sourceFile, degrees, targetPages, outFile)
            if (success) {
                refreshFiles()
                recordPdfOpened(outFile)
                _operationMessage.value = "Rotated pages by $degrees°"
                withContext(Dispatchers.Main) { onComplete(outFile) }
            } else {
                _operationMessage.value = "Page rotation failed"
                withContext(Dispatchers.Main) { onComplete(null) }
            }
            _isProcessing.value = false
        }
    }

    fun saveEditedPdf(
        sourceFile: File,
        annotationsMap: Map<Int, List<PdfEngine.AnnotationItem>>,
        outputName: String,
        onComplete: (File?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            val context = getApplication<Application>()
            val docsDir = File(context.filesDir, "documents")
            var name = outputName.trim().ifBlank { "${sourceFile.nameWithoutExtension}_Edited" }
            if (!name.endsWith(".pdf", ignoreCase = true)) name += ".pdf"
            val outFile = File(docsDir, name)

            val success = PdfEngine.saveAnnotationsToPdf(sourceFile, annotationsMap, outFile)
            if (success) {
                refreshFiles()
                recordPdfOpened(outFile)
                _operationMessage.value = "Saved edited document to $name"
                withContext(Dispatchers.Main) { onComplete(outFile) }
            } else {
                _operationMessage.value = "Failed to save edited document"
                withContext(Dispatchers.Main) { onComplete(null) }
            }
            _isProcessing.value = false
        }
    }

    fun getBookmarks(filePath: String) = repository.getBookmarks(filePath)
    fun addBookmark(filePath: String, pageIndex: Int, title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addBookmark(filePath, pageIndex, title)
            _operationMessage.value = "Bookmark added for page ${pageIndex + 1}"
        }
    }
    fun removeBookmark(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { repository.removeBookmark(id) }
    }

    fun getNotes(filePath: String) = repository.getNotes(filePath)
    fun addNote(filePath: String, pageIndex: Int, text: String, colorHex: String = "#FFF9C4") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addNote(filePath, pageIndex, text, colorHex)
            _operationMessage.value = "Note added on page ${pageIndex + 1}"
        }
    }
    fun removeNote(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { repository.removeNote(id) }
    }
}

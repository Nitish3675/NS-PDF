package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.pdf.PdfEngine
import com.example.pdf.PdfPrintDocumentAdapter
import com.example.ui.viewmodel.PdfViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    file: File,
    viewModel: PdfViewModel,
    onBack: () -> Unit,
    onOpenEditor: (File) -> Unit
) {
    val context = LocalContext.current
    var totalPages by remember { mutableIntStateOf(1) }
    var currentPage by remember { mutableIntStateOf(0) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPage by remember { mutableStateOf(true) }

    var isDarkModeFilter by remember { mutableStateOf(false) }
    var viewRotation by remember { mutableFloatStateOf(0f) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    var showJumpDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteInputText by remember { mutableStateOf("") }
    var showBookmarksSheet by remember { mutableStateOf(false) }

    val bookmarks by viewModel.getBookmarks(file.absolutePath).collectAsState(initial = emptyList())
    val notes by viewModel.getNotes(file.absolutePath).collectAsState(initial = emptyList())

    val isCurrentPageBookmarked = bookmarks.any { it.pageIndex == currentPage }

    // Initialize page count & record to recents
    LaunchedEffect(file.absolutePath) {
        withContext(Dispatchers.IO) {
            val count = PdfEngine.getPageCount(file).coerceAtLeast(1)
            totalPages = count
            viewModel.recordPdfOpened(file, 0)
        }
    }

    // Load current page bitmap
    LaunchedEffect(file.absolutePath, currentPage) {
        isLoadingPage = true
        withContext(Dispatchers.IO) {
            val bmp = PdfEngine.renderPageToBitmap(file, currentPage, scale = 2.0f)
            currentBitmap = bmp
            isLoadingPage = false
        }
    }

    fun shareCurrentPdf() {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Document"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkModeFilter) Color(0xFF18181B) else MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("reader_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Page ${currentPage + 1} of $totalPages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = {
                if (isCurrentPageBookmarked) {
                    val b = bookmarks.find { it.pageIndex == currentPage }
                    if (b != null) viewModel.removeBookmark(b.id)
                } else {
                    viewModel.addBookmark(file.absolutePath, currentPage, "Page ${currentPage + 1}")
                }
            }) {
                Icon(
                    imageVector = if (isCurrentPageBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (isCurrentPageBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(onClick = { isDarkModeFilter = !isDarkModeFilter }) {
                Icon(
                    imageVector = if (isDarkModeFilter) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Dark Mode Toggle"
                )
            }

            IconButton(onClick = { showBookmarksSheet = true }) {
                Icon(Icons.Default.NoteAdd, contentDescription = "Notes and Bookmarks")
            }

            IconButton(onClick = { shareCurrentPdf() }) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
        }

        // Main Viewer Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(if (isDarkModeFilter) Color(0xFF121212) else Color(0xFFEBEBEF))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(1f, 4.5f)
                        if (zoomScale > 1f) {
                            panOffsetX += pan.x
                            panOffsetY += pan.y
                        } else {
                            panOffsetX = 0f
                            panOffsetY = 0f
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingPage) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                val bmp = currentBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Page Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .graphicsLayer {
                                scaleX = zoomScale
                                scaleY = zoomScale
                                translationX = panOffsetX
                                translationY = panOffsetY
                                rotationZ = viewRotation
                            }
                    )
                } else {
                    Text("Could not render page", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        // Bottom Reader Navigation Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev page
                IconButton(
                    onClick = {
                        if (currentPage > 0) {
                            currentPage--
                            zoomScale = 1f
                            panOffsetX = 0f
                            panOffsetY = 0f
                        }
                    },
                    enabled = currentPage > 0
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Page")
                }

                // Page jump button
                TextButton(
                    onClick = { showJumpDialog = true },
                    modifier = Modifier.testTag("jump_to_page_button")
                ) {
                    Text(
                        text = "Page ${currentPage + 1} / $totalPages",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Next page
                IconButton(
                    onClick = {
                        if (currentPage < totalPages - 1) {
                            currentPage++
                            zoomScale = 1f
                            panOffsetX = 0f
                            panOffsetY = 0f
                        }
                    },
                    enabled = currentPage < totalPages - 1
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Page")
                }

                // Rotate view
                IconButton(onClick = { viewRotation = (viewRotation + 90f) % 360f }) {
                    Icon(Icons.Default.CropRotate, contentDescription = "Rotate View")
                }

                // Print
                IconButton(onClick = { PdfPrintDocumentAdapter.print(context, file) }) {
                    Icon(Icons.Default.Print, contentDescription = "Print")
                }

                // Edit Button
                IconButton(
                    onClick = { onOpenEditor(file) },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit in Annotator",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // Jump to Page Dialog
    if (showJumpDialog) {
        var sliderValue by remember { mutableFloatStateOf((currentPage + 1).toFloat()) }
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("Jump to Page") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Page ${sliderValue.toInt()} of $totalPages",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 1f..totalPages.toFloat(),
                        steps = (totalPages - 2).coerceAtLeast(0),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    currentPage = (sliderValue.toInt() - 1).coerceIn(0, totalPages - 1)
                    zoomScale = 1f
                    showJumpDialog = false
                }) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bookmarks & Notes Bottom Sheet
    if (showBookmarksSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBookmarksSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notes & Bookmarks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showAddNoteDialog = true }) {
                        Text("+ Add Note")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Bookmarks (${bookmarks.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                if (bookmarks.isEmpty()) {
                    Text("No bookmarks added yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    for (b in bookmarks) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                currentPage = b.pageIndex
                                showBookmarksSheet = false
                            }) {
                                Text("Page ${b.pageIndex + 1}: ${b.title}")
                            }
                            IconButton(onClick = { viewModel.removeBookmark(b.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Page Notes (${notes.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                if (notes.isEmpty()) {
                    Text("No notes added yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(modifier = Modifier.height(180.dp)) {
                        items(notes) { note ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Page ${note.pageIndex + 1}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(note.noteText, fontSize = 14.sp)
                                    }
                                    IconButton(onClick = { viewModel.removeNote(note.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Add Note Dialog
    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("Add Note for Page ${currentPage + 1}") },
            text = {
                OutlinedTextField(
                    value = noteInputText,
                    onValueChange = { noteInputText = it },
                    placeholder = { Text("Enter your comment or memo...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (noteInputText.isNotBlank()) {
                            viewModel.addNote(file.absolutePath, currentPage, noteInputText.trim())
                            noteInputText = ""
                            showAddNoteDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

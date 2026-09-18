package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.pdf.PdfEngine
import com.example.pdf.PdfPrintDocumentAdapter
import com.example.ui.components.PdfThumbnailView
import com.example.ui.viewmodel.FileSortOption
import com.example.ui.viewmodel.PdfViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    viewModel: PdfViewModel,
    onOpenPdf: (File) -> Unit,
    onEditPdf: (File) -> Unit
) {
    val context = LocalContext.current
    val displayedFiles by viewModel.displayedFiles.collectAsState()
    val favoritePaths by viewModel.favoritePaths.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var fileToRename by remember { mutableStateOf<File?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var fileForDetails by remember { mutableStateOf<File?>(null) }
    var showBatchMergeDialog by remember { mutableStateOf(false) }
    var batchMergeName by remember { mutableStateOf("Merged_Documents") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importPdfFromUri(uri) { importedFile ->
                if (importedFile != null) {
                    onOpenPdf(importedFile)
                }
            }
        }
    }

    fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareMultipleFiles(files: List<File>) {
        try {
            val uris = ArrayList<Uri>()
            for (f in files) {
                uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f))
            }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "application/pdf"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${files.size} Documents"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Multi-select top bar or Standard Top Bar
        if (isMultiSelectMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.clearSelection() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close selection")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${selectedFiles.size} Selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Row {
                    IconButton(onClick = { viewModel.selectAllFiles() }) {
                        Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                    }
                    if (selectedFiles.size >= 2) {
                        IconButton(onClick = { showBatchMergeDialog = true }) {
                            Icon(Icons.Default.MergeType, contentDescription = "Merge Selected")
                        }
                    }
                    IconButton(onClick = { shareMultipleFiles(selectedFiles.toList()) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Selected")
                    }
                    IconButton(onClick = { viewModel.deleteSelectedFiles() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = Color.Red)
                    }
                }
            }
        } else {
            // Standard Title & Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "File Manager",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${displayedFiles.size} PDF files stored locally",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.testTag("files_import_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = "Import PDF",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleGridView() },
                        modifier = Modifier.testTag("toggle_view_mode_button")
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle View"
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("sort_options_button")
                        ) {
                            Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort Options")
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Name: A to Z") },
                                onClick = { viewModel.setSortOption(FileSortOption.NAME_ASC); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Name: Z to A") },
                                onClick = { viewModel.setSortOption(FileSortOption.NAME_DESC); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Date: Newest First") },
                                onClick = { viewModel.setSortOption(FileSortOption.DATE_DESC); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Date: Oldest First") },
                                onClick = { viewModel.setSortOption(FileSortOption.DATE_ASC); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Size: Largest First") },
                                onClick = { viewModel.setSortOption(FileSortOption.SIZE_DESC); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Size: Smallest First") },
                                onClick = { viewModel.setSortOption(FileSortOption.SIZE_ASC); showSortMenu = false }
                            )
                        }
                    }
                }
            }

            // Search text field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .testTag("files_search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Document List or Grid
        if (displayedFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (searchQuery.isEmpty()) "No documents in storage" else "No matching documents",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayedFiles) { file ->
                    val isSelected = selectedFiles.contains(file)
                    val isFav = favoritePaths.contains(file.absolutePath)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isMultiSelectMode) {
                                    viewModel.toggleFileSelection(file)
                                } else {
                                    onOpenPdf(file)
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                PdfThumbnailView(
                                    file = file,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                )
                                if (isMultiSelectMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { viewModel.toggleFileSelection(file) },
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    )
                                } else if (isFav) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Starred",
                                        tint = Color(0xFFFFA000),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = file.nameWithoutExtension,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${file.length() / 1024} KB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp, top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayedFiles) { file ->
                    val isSelected = selectedFiles.contains(file)
                    val isFav = favoritePaths.contains(file.absolutePath)
                    var showItemMenu by remember { mutableStateOf(false) }

                    val formattedDate = remember(file.lastModified()) {
                        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isMultiSelectMode) {
                                    viewModel.toggleFileSelection(file)
                                } else {
                                    onOpenPdf(file)
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isMultiSelectMode) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { viewModel.toggleFileSelection(file) }
                                )
                            }

                            PdfThumbnailView(
                                file = file,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "$formattedDate • ${file.length() / 1024} KB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!isMultiSelectMode) {
                                IconButton(
                                    onClick = { viewModel.toggleFavorite(file) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Star",
                                        tint = if (isFav) Color(0xFFFFA000) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Box {
                                    IconButton(
                                        onClick = { showItemMenu = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Options"
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showItemMenu,
                                        onDismissRequest = { showItemMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Open in Reader") },
                                            onClick = { showItemMenu = false; onOpenPdf(file) },
                                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Edit & Annotate") },
                                            onClick = { showItemMenu = false; onEditPdf(file) },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Share") },
                                            onClick = { showItemMenu = false; shareFile(file) },
                                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Print") },
                                            onClick = { showItemMenu = false; PdfPrintDocumentAdapter.print(context, file) },
                                            leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Rename") },
                                            onClick = {
                                                showItemMenu = false
                                                renameInput = file.nameWithoutExtension
                                                fileToRename = file
                                            },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Details") },
                                            onClick = { showItemMenu = false; fileForDetails = file },
                                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Select Multiple") },
                                            onClick = {
                                                showItemMenu = false
                                                viewModel.toggleFileSelection(file)
                                            },
                                            leadingIcon = { Icon(Icons.Default.SelectAll, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete", color = Color.Red) },
                                            onClick = { showItemMenu = false; fileToDelete = file },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (fileToDelete != null) {
        val target = fileToDelete!!
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete Document?") },
            text = { Text("Are you sure you want to permanently delete \"${target.name}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFile(target)
                        fileToDelete = null
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Dialog
    if (fileToRename != null) {
        val target = fileToRename!!
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("New file name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renameFile(target, renameInput)
                            fileToRename = null
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Details Dialog
    if (fileForDetails != null) {
        val target = fileForDetails!!
        val pageCount = remember(target.absolutePath) { PdfEngine.getPageCount(target) }
        val dateStr = remember(target.lastModified()) {
            SimpleDateFormat("MMMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date(target.lastModified()))
        }

        AlertDialog(
            onDismissRequest = { fileForDetails = null },
            title = { Text("Document Info") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Name: ${target.name}", fontWeight = FontWeight.Bold)
                    Text(text = "Pages: $pageCount")
                    Text(text = "File Size: ${target.length() / 1024} KB (${target.length()} bytes)")
                    Text(text = "Modified: $dateStr")
                    Text(text = "Location: ${target.parentFile?.name}")
                }
            },
            confirmButton = {
                TextButton(onClick = { fileForDetails = null }) {
                    Text("OK")
                }
            }
        )
    }

    // Batch Merge Dialog
    if (showBatchMergeDialog) {
        AlertDialog(
            onDismissRequest = { showBatchMergeDialog = false },
            title = { Text("Merge ${selectedFiles.size} Documents") },
            text = {
                Column {
                    Text("Enter a name for the merged output PDF:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = batchMergeName,
                        onValueChange = { batchMergeName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.mergeFiles(selectedFiles.toList(), batchMergeName) {
                            showBatchMergeDialog = false
                            viewModel.clearSelection()
                        }
                    }
                ) {
                    Text("Merge")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchMergeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

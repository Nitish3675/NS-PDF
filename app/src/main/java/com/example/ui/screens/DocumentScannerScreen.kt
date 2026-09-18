package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.pdf.ImageFilters
import com.example.pdf.PdfEngine
import com.example.ui.viewmodel.PdfViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ScannedPage(
    val originalFile: File,
    var processedBitmap: Bitmap? = null,
    var filterType: ImageFilters.FilterMode = ImageFilters.FilterMode.DOCUMENT_ENHANCE,
    var contrastFactor: Float = 1.25f
)


@Composable
fun DocumentScannerScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit,
    onPdfGenerated: (File) -> Unit,
    onRunOcr: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val pages = remember { mutableStateListOf<ScannedPage>() }
    var selectedPageIndex by remember { mutableIntStateOf(0) }
    var isSaving by remember { mutableStateOf(false) }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            val file = File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
            try {
                context.contentResolver.openInputStream(tempCameraUri!!)?.use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }
                val rawBmp = BitmapFactory.decodeFile(file.absolutePath)
                if (rawBmp != null) {
                    val filtered = ImageFilters.applyFilter(rawBmp, ImageFilters.FilterMode.DOCUMENT_ENHANCE, 0f, 1.25f)
                    val page = ScannedPage(file, filtered, ImageFilters.FilterMode.DOCUMENT_ENHANCE, 1.25f)
                    pages.add(page)
                    selectedPageIndex = pages.size - 1
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        for (uri in uris) {
            val file = File(context.cacheDir, "gallery_${System.currentTimeMillis()}_${pages.size}.jpg")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }
                val rawBmp = BitmapFactory.decodeFile(file.absolutePath)
                if (rawBmp != null) {
                    val filtered = ImageFilters.applyFilter(rawBmp, ImageFilters.FilterMode.DOCUMENT_ENHANCE, 0f, 1.25f)
                    val page = ScannedPage(file, filtered, ImageFilters.FilterMode.DOCUMENT_ENHANCE, 1.25f)
                    pages.add(page)
                    selectedPageIndex = pages.size - 1
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun launchCamera() {
        val cacheFile = File(context.cacheDir, "temp_camera.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
        tempCameraUri = uri
        cameraLauncher.launch(uri)
    }

    fun applyCurrentFilter(type: ImageFilters.FilterMode, contrast: Float) {
        if (pages.isEmpty() || selectedPageIndex !in pages.indices) return
        val current = pages[selectedPageIndex]
        val rawBmp = BitmapFactory.decodeFile(current.originalFile.absolutePath) ?: return
        val newBmp = ImageFilters.applyFilter(rawBmp, type, 0f, contrast)
        pages[selectedPageIndex] = current.copy(
            processedBitmap = newBmp,
            filterType = type,
            contrastFactor = contrast
        )
    }

    fun saveAsPdf() {
        if (pages.isEmpty()) return
        isSaving = true
        CoroutineScope(Dispatchers.IO).launch {
            val docsDir = File(context.filesDir, "documents")
            if (!docsDir.exists()) docsDir.mkdirs()
            val outFile = File(docsDir, "Scanned_${System.currentTimeMillis()}.pdf")

            // Save processed bitmaps to temp files
            val tempImageFiles = mutableListOf<File>()
            for ((idx, page) in pages.withIndex()) {
                val tempImg = File(context.cacheDir, "page_${idx}.jpg")
                val fos = FileOutputStream(tempImg)
                val bmp = page.processedBitmap ?: BitmapFactory.decodeFile(page.originalFile.absolutePath)
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                fos.flush()
                fos.close()
                tempImageFiles.add(tempImg)
            }

            val success = PdfEngine.convertImagesToPdf(tempImageFiles, "A4", 20, outFile)
            withContext(Dispatchers.Main) {
                isSaving = false
                if (success) {
                    viewModel.refreshFiles()
                    viewModel.recordPdfOpened(outFile)
                    onPdfGenerated(outFile)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Document Scanner",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (pages.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            val cur = pages.getOrNull(selectedPageIndex)?.processedBitmap
                            if (cur != null) onRunOcr(cur)
                        }
                    ) {
                        Icon(Icons.Default.FormatColorText, contentDescription = "Run OCR", tint = MaterialTheme.colorScheme.primary)
                    }

                    Button(
                        onClick = { saveAsPdf() },
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save PDF")
                        }
                    }
                }
            }
        }

        // Main preview or empty state
        if (pages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Scan Physical Documents",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Capture receipts, documents, book pages, or whiteboard notes with automatic contrast enhance.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { launchCamera() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Use Camera")
                        }

                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("From Gallery")
                        }
                    }
                }
            }
        } else {
            // Document Page Preview with applied filter
            val curPage = pages[selectedPageIndex]
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF212124))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val bmp = curPage.processedBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Scanned Page",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Filter selection controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Page thumbnails row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scanned Pages (${pages.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Row {
                            IconButton(onClick = { launchCamera() }) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = "Add page", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "From Gallery", modifier = Modifier.size(20.dp))
                            }
                            IconButton(
                                onClick = {
                                    if (pages.size > 1) {
                                        pages.removeAt(selectedPageIndex)
                                        selectedPageIndex = selectedPageIndex.coerceAtMost(pages.size - 1)
                                    } else {
                                        pages.clear()
                                        selectedPageIndex = 0
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete page", tint = Color.Red, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(pages) { index, p ->
                            val isSel = index == selectedPageIndex
                            val bmp = p.processedBitmap
                            Box(
                                modifier = Modifier
                                    .size(60.dp, 80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSel) 2.5.dp else 1.dp,
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedPageIndex = index }
                            ) {
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .fillMaxWidth()
                                        .padding(vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("P.${index + 1}", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Filter mode buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val filters = listOf(
                            Triple(ImageFilters.FilterMode.ORIGINAL, "Original", Icons.Default.Tune),
                            Triple(ImageFilters.FilterMode.DOCUMENT_ENHANCE, "Enhance", Icons.Default.AutoFixHigh),
                            Triple(ImageFilters.FilterMode.BLACK_AND_WHITE, "B&W", Icons.Default.InvertColors),
                            Triple(ImageFilters.FilterMode.GRAYSCALE, "Grayscale", Icons.Default.Tune)
                        )

                        for ((ft, name, icon) in filters) {
                            val isSelected = curPage.filterType == ft
                            OutlinedButton(
                                onClick = { applyCurrentFilter(ft, curPage.contrastFactor) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(name, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

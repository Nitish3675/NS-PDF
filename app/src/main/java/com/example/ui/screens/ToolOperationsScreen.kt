package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdf.PdfEngine
import com.example.ui.components.PdfThumbnailView
import com.example.ui.viewmodel.PdfViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@Composable
fun ToolOperationsScreen(
    toolType: String,
    viewModel: PdfViewModel,
    onBack: () -> Unit,
    onPdfGenerated: (File) -> Unit
) {
    val context = LocalContext.current
    val displayedFiles by viewModel.displayedFiles.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var selectedFile by remember { mutableStateOf<File?>(displayedFiles.firstOrNull()) }

    when (toolType) {
        "tool_merge" -> {
            MergeToolContent(
                allFiles = displayedFiles,
                isProcessing = isProcessing,
                onBack = onBack,
                onMerge = { files, name ->
                    viewModel.mergeFiles(files, name) { out ->
                        if (out != null) onPdfGenerated(out)
                    }
                }
            )
        }
        "tool_split" -> {
            SplitToolContent(
                allFiles = displayedFiles,
                isProcessing = isProcessing,
                onBack = onBack,
                onSplit = { file, pages, name ->
                    viewModel.splitFile(file, pages, name) { out ->
                        if (out != null) onPdfGenerated(out)
                    }
                }
            )
        }
        "tool_compress" -> {
            CompressToolContent(
                allFiles = displayedFiles,
                isProcessing = isProcessing,
                onBack = onBack,
                onCompress = { file, quality, scale, name ->
                    viewModel.compressFile(file, quality, scale, name) { out, _, _ ->
                        if (out != null) onPdfGenerated(out)
                    }
                }
            )
        }
        "tool_img_to_pdf" -> {
            ImageToPdfToolContent(
                isProcessing = isProcessing,
                onBack = onBack,
                onConvert = { images, size, name ->
                    viewModel.convertImagesToPdf(images, size, name) { out ->
                        if (out != null) onPdfGenerated(out)
                    }
                }
            )
        }
        "tool_watermark" -> {
            WatermarkToolContent(
                allFiles = displayedFiles,
                isProcessing = isProcessing,
                onBack = onBack,
                onWatermark = { file, text, color, size, rot, name ->
                    viewModel.watermarkFile(file, text, color, size, rot, name) { out ->
                        if (out != null) onPdfGenerated(out)
                    }
                }
            )
        }
        "tool_page_numbers" -> {
            PageNumbersToolContent(
                allFiles = displayedFiles,
                isProcessing = isProcessing,
                onBack = onBack,
                onApply = { file, header, footer, pageNums, name ->
                    viewModel.addHeaderFooter(file, header, footer, pageNums, name) { out ->
                        if (out != null) onPdfGenerated(out)
                    }
                }
            )
        }
        "tool_create_note" -> {
            CreateNoteToolContent(
                isProcessing = isProcessing,
                onBack = onBack,
                onCreate = { title, body ->
                    viewModel.createPdfFromText(title, body) { out ->
                        if (out != null) onPdfGenerated(out)
                    }
                }
            )
        }
        else -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Unknown Tool: $toolType")
            }
        }
    }
}

// ----------------------------------------------------
// MERGE TOOL
// ----------------------------------------------------
@Composable
fun MergeToolContent(
    allFiles: List<File>,
    isProcessing: Boolean,
    onBack: () -> Unit,
    onMerge: (List<File>, String) -> Unit
) {
    val selectedFiles = remember { mutableStateListOf<File>() }
    var outputName by remember { mutableStateOf("Combined_Documents") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Merge PDFs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select 2 or more PDFs to combine:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    for (f in allFiles) {
                        val isChecked = selectedFiles.contains(f)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selectedFiles.remove(f) else selectedFiles.add(f)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (it) selectedFiles.add(f) else selectedFiles.remove(f)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text("${f.length() / 1024} KB", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = outputName,
                onValueChange = { outputName = it },
                label = { Text("Output PDF Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onMerge(selectedFiles.toList(), outputName) },
                enabled = selectedFiles.size >= 2 && !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.MergeType, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Merge ${selectedFiles.size} Documents")
                }
            }
        }
    }
}

// ----------------------------------------------------
// SPLIT TOOL
// ----------------------------------------------------
@Composable
fun SplitToolContent(
    allFiles: List<File>,
    isProcessing: Boolean,
    onBack: () -> Unit,
    onSplit: (File, List<Int>, String) -> Unit
) {
    var chosenFile by remember { mutableStateOf<File?>(allFiles.firstOrNull()) }
    var totalPages by remember { mutableIntStateOf(1) }
    var pageInput by remember { mutableStateOf("1") }
    var outputName by remember { mutableStateOf("Extracted_Pages") }

    LaunchedEffect(chosenFile) {
        if (chosenFile != null) {
            totalPages = PdfEngine.getPageCount(chosenFile!!).coerceAtLeast(1)
            outputName = "${chosenFile!!.nameWithoutExtension}_Extracted"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Split & Extract Pages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Choose Source PDF:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    for (f in allFiles.take(6)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { chosenFile = f }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = chosenFile == f, onClick = { chosenFile = f })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Document contains $totalPages pages.", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = pageInput,
                onValueChange = { pageInput = it },
                label = { Text("Pages to extract (e.g. 1, 2, 3)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = outputName,
                onValueChange = { outputName = it },
                label = { Text("Output PDF Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val indices = pageInput.split(",")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .map { it - 1 }
                        .filter { it in 0 until totalPages }

                    if (chosenFile != null && indices.isNotEmpty()) {
                        onSplit(chosenFile!!, indices, outputName)
                    }
                },
                enabled = chosenFile != null && !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Extract Selected Pages")
                }
            }
        }
    }
}

// ----------------------------------------------------
// COMPRESS TOOL
// ----------------------------------------------------
@Composable
fun CompressToolContent(
    allFiles: List<File>,
    isProcessing: Boolean,
    onBack: () -> Unit,
    onCompress: (File, Int, Float, String) -> Unit
) {
    var chosenFile by remember { mutableStateOf<File?>(allFiles.firstOrNull()) }
    var compressionProfile by remember { mutableIntStateOf(1) } // 0 = High (85%), 1 = Balanced (65%), 2 = Maximum (45%)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Compress PDF File Size", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Choose Document to Compress:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    for (f in allFiles.take(6)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { chosenFile = f }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = chosenFile == f, onClick = { chosenFile = f })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text("${f.length() / 1024} KB", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Choose Compression Level:", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            listOf(
                Triple(0, "High Quality (80%)", "Minimal reduction, preserves sharp text and graphics"),
                Triple(1, "Balanced (60%)", "Recommended: up to 50-60% size reduction with good clarity"),
                Triple(2, "Maximum Compression (40%)", "Maximum shrinkage for easy emailing and sharing")
            ).forEach { (id, title, desc) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { compressionProfile = id },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (compressionProfile == id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = compressionProfile == id, onClick = { compressionProfile = id })
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (chosenFile != null) {
                        val (q, scale) = when (compressionProfile) {
                            0 -> Pair(80, 1.4f)
                            2 -> Pair(40, 0.9f)
                            else -> Pair(60, 1.1f)
                        }
                        onCompress(chosenFile!!, q, scale, "${chosenFile!!.nameWithoutExtension}_Compressed")
                    }
                },
                enabled = chosenFile != null && !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.Compress, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compress Document")
                }
            }
        }
    }
}

// ----------------------------------------------------
// IMAGE TO PDF TOOL
// ----------------------------------------------------
@Composable
fun ImageToPdfToolContent(
    isProcessing: Boolean,
    onBack: () -> Unit,
    onConvert: (List<File>, String, String) -> Unit
) {
    val context = LocalContext.current
    val imageFiles = remember { mutableStateListOf<File>() }
    var pageSize by remember { mutableStateOf("A4") }
    var outputName by remember { mutableStateOf("Images_Document") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        for (uri in uris) {
            val file = File(context.cacheDir, "img_${System.currentTimeMillis()}_${imageFiles.size}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            imageFiles.add(file)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Convert Images to PDF", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Button(
                onClick = { photoPicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Photos from Gallery (${imageFiles.size} Selected)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Page Format:", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                listOf("A4", "LETTER", "FIT").forEach { opt ->
                    OutlinedButton(
                        onClick = { pageSize = opt },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (pageSize == opt) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(opt)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = outputName,
                onValueChange = { outputName = it },
                label = { Text("Output PDF Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onConvert(imageFiles.toList(), pageSize, outputName) },
                enabled = imageFiles.isNotEmpty() && !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Generate PDF")
                }
            }
        }
    }
}

// ----------------------------------------------------
// WATERMARK TOOL
// ----------------------------------------------------
@Composable
fun WatermarkToolContent(
    allFiles: List<File>,
    isProcessing: Boolean,
    onBack: () -> Unit,
    onWatermark: (File, String, Int, Float, Float, String) -> Unit
) {
    var chosenFile by remember { mutableStateOf<File?>(allFiles.firstOrNull()) }
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }
    var fontSize by remember { mutableFloatStateOf(44f) }
    var rotation by remember { mutableFloatStateOf(-45f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Watermark Document", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select PDF to Watermark:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    for (f in allFiles.take(5)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { chosenFile = f }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = chosenFile == f, onClick = { chosenFile = f })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = watermarkText,
                onValueChange = { watermarkText = it },
                label = { Text("Watermark Text") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick watermark presets
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("CONFIDENTIAL", "DRAFT", "COPY", "APPROVED").forEach { preset ->
                    OutlinedButton(
                        onClick = { watermarkText = preset },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(preset, fontSize = 9.sp, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (chosenFile != null && watermarkText.isNotBlank()) {
                        val color = android.graphics.Color.parseColor("#44D32F2F")
                        onWatermark(chosenFile!!, watermarkText, color, fontSize, rotation, "${chosenFile!!.nameWithoutExtension}_Watermarked")
                    }
                },
                enabled = chosenFile != null && !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Apply Watermark")
                }
            }
        }
    }
}

// ----------------------------------------------------
// PAGE NUMBERS & HEADER/FOOTER TOOL
// ----------------------------------------------------
@Composable
fun PageNumbersToolContent(
    allFiles: List<File>,
    isProcessing: Boolean,
    onBack: () -> Unit,
    onApply: (File, String?, String?, Boolean, String) -> Unit
) {
    var chosenFile by remember { mutableStateOf<File?>(allFiles.firstOrNull()) }
    var headerText by remember { mutableStateOf("") }
    var footerText by remember { mutableStateOf("") }
    var includePageNumbers by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Header, Footer & Page Numbers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Target Document:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    for (f in allFiles.take(5)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { chosenFile = f }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = chosenFile == f, onClick = { chosenFile = f })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = headerText,
                onValueChange = { headerText = it },
                label = { Text("Header Text (Top-Left)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = footerText,
                onValueChange = { footerText = it },
                label = { Text("Footer Text (Bottom-Left)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = includePageNumbers, onCheckedChange = { includePageNumbers = it })
                Spacer(modifier = Modifier.width(6.dp))
                Text("Include 'Page X of N' numbering at bottom-right")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (chosenFile != null) {
                        onApply(chosenFile!!, headerText.ifBlank { null }, footerText.ifBlank { null }, includePageNumbers, "${chosenFile!!.nameWithoutExtension}_Numbered")
                    }
                },
                enabled = chosenFile != null && !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Apply to Document")
                }
            }
        }
    }
}

// ----------------------------------------------------
// CREATE PDF NOTE TOOL
// ----------------------------------------------------
@Composable
fun CreateNoteToolContent(
    isProcessing: Boolean,
    onBack: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Create New PDF Document", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Document Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Document Content / Paragraphs") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                maxLines = 20
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreate(title, content)
                    }
                },
                enabled = title.isNotBlank() && !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.Create, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate PDF Note")
                }
            }
        }
    }
}

package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Square
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdf.PdfEngine
import com.example.ui.components.SignaturePad
import com.example.ui.viewmodel.PdfViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class EditorTool {
    PEN,
    HIGHLIGHTER,
    TEXT,
    RECTANGLE,
    CIRCLE,
    SIGNATURE,
    REDACTION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfEditorScreen(
    file: File,
    viewModel: PdfViewModel,
    onBack: () -> Unit,
    onSaved: (File) -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(1) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPage by remember { mutableStateOf(true) }

    var selectedTool by remember { mutableStateOf(EditorTool.PEN) }
    var strokeColor by remember { mutableStateOf(Color(0xFFD32F2F)) }
    var strokeWidth by remember { mutableFloatStateOf(6f) }

    // Page -> List of annotations
    val annotationsMap = remember { mutableStateMapOf<Int, MutableList<PdfEngine.AnnotationItem>>() }
    val currentPoints = remember { mutableStateListOf<Offset>() }
    var dragStartOffset by remember { mutableStateOf<Offset?>(null) }
    var dragEndOffset by remember { mutableStateOf<Offset?>(null) }

    var showTextInputDialog by remember { mutableStateOf(false) }
    var textInputString by remember { mutableStateOf("") }
    var textTapPosition by remember { mutableStateOf<Offset?>(null) }

    var showSignatureSheet by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveFileName by remember { mutableStateOf("${file.nameWithoutExtension}_Edited") }

    // Load page count
    LaunchedEffect(file.absolutePath) {
        withContext(Dispatchers.IO) {
            val count = PdfEngine.getPageCount(file).coerceAtLeast(1)
            totalPages = count
        }
    }

    // Load page bitmap
    LaunchedEffect(file.absolutePath, currentPage) {
        isLoadingPage = true
        withContext(Dispatchers.IO) {
            val bmp = PdfEngine.renderPageToBitmap(file, currentPage, scale = 1.8f)
            currentBitmap = bmp
            isLoadingPage = false
        }
    }

    fun getPageAnnotations(): MutableList<PdfEngine.AnnotationItem> {
        return annotationsMap.getOrPut(currentPage) { mutableStateListOf() }
    }

    fun undoLastAnnotation() {
        val list = getPageAnnotations()
        if (list.isNotEmpty()) {
            list.removeAt(list.size - 1)
        }
    }

    val palette = listOf(
        Color(0xFFD32F2F), // Crimson Red
        Color(0xFF1E88E5), // Blue
        Color(0xFF43A047), // Green
        Color(0xFFFDD835), // Yellow
        Color(0xFF000000), // Black
        Color(0xFF8E24AA)  // Purple
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E24))
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("editor_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "PDF Annotator",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Page ${currentPage + 1} of $totalPages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { undoLastAnnotation() },
                    modifier = Modifier.testTag("editor_undo_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                }

                IconButton(
                    onClick = { getPageAnnotations().clear() }
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear Page")
                }

                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.testTag("editor_save_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save")
                }
            }
        }

        // Secondary Palette Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Colors
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (c in palette) {
                    val isSelected = strokeColor == c
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { strokeColor = c }
                    )
                }
            }

            // Stroke thickness
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(3f to "Thin", 6f to "Med", 14f to "Thick").forEach { (width, label) ->
                    val isSel = strokeWidth == width
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { strokeWidth = width }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Canvas Area
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF2B2B30)),
            contentAlignment = Alignment.Center
        ) {
            val containerWidth = constraints.maxWidth.toFloat()
            val containerHeight = constraints.maxHeight.toFloat()

            if (isLoadingPage) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                val bmp = currentBitmap
                if (bmp != null) {
                    val bmpWidth = bmp.width.toFloat()
                    val bmpHeight = bmp.height.toFloat()
                    val renderScale = minOf(containerWidth / bmpWidth, containerHeight / bmpHeight)
                    val dispW = bmpWidth * renderScale
                    val dispH = bmpHeight * renderScale

                    Box(
                        modifier = Modifier
                            .size(dispW.dp * 0.35f, dispH.dp * 0.35f) // approximate dp mapping
                            .fillMaxSize()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "PDF Page",
                            modifier = Modifier.fillMaxSize()
                        )

                        // Interactive Annotation Layer
                        Canvas(
                            modifier = Modifier
                                .matchParentSize()
                                .pointerInput(selectedTool) {
                                    if (selectedTool == EditorTool.TEXT) {
                                        detectTapGestures { offset ->
                                            textTapPosition = offset
                                            showTextInputDialog = true
                                        }
                                    } else {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                dragStartOffset = offset
                                                dragEndOffset = offset
                                                if (selectedTool == EditorTool.PEN || selectedTool == EditorTool.HIGHLIGHTER) {
                                                    currentPoints.clear()
                                                    currentPoints.add(offset)
                                                }
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                dragEndOffset = change.position
                                                if (selectedTool == EditorTool.PEN || selectedTool == EditorTool.HIGHLIGHTER) {
                                                    currentPoints.add(change.position)
                                                }
                                            },
                                            onDragEnd = {
                                                val start = dragStartOffset
                                                val end = dragEndOffset
                                                val list = getPageAnnotations()

                                                when (selectedTool) {
                                                    EditorTool.PEN -> {
                                                        if (currentPoints.size > 1) {
                                                            list.add(
                                                                PdfEngine.AnnotationItem.Freehand(
                                                                    points = currentPoints.map { PointF(it.x, it.y) },
                                                                    color = android.graphics.Color.argb(
                                                                        (strokeColor.alpha * 255).toInt(),
                                                                        (strokeColor.red * 255).toInt(),
                                                                        (strokeColor.green * 255).toInt(),
                                                                        (strokeColor.blue * 255).toInt()
                                                                    ),
                                                                    strokeWidth = strokeWidth,
                                                                    isHighlighter = false
                                                                )
                                                            )
                                                        }
                                                        currentPoints.clear()
                                                    }
                                                    EditorTool.HIGHLIGHTER -> {
                                                        if (currentPoints.size > 1) {
                                                            list.add(
                                                                PdfEngine.AnnotationItem.Freehand(
                                                                    points = currentPoints.map { PointF(it.x, it.y) },
                                                                    color = android.graphics.Color.argb(
                                                                        110,
                                                                        (strokeColor.red * 255).toInt(),
                                                                        (strokeColor.green * 255).toInt(),
                                                                        (strokeColor.blue * 255).toInt()
                                                                    ),
                                                                    strokeWidth = 24f,
                                                                    isHighlighter = true
                                                                )
                                                            )
                                                        }
                                                        currentPoints.clear()
                                                    }
                                                    EditorTool.RECTANGLE -> {
                                                        if (start != null && end != null) {
                                                            list.add(
                                                                PdfEngine.AnnotationItem.Shape(
                                                                    type = PdfEngine.ShapeType.RECTANGLE,
                                                                    left = minOf(start.x, end.x),
                                                                    top = minOf(start.y, end.y),
                                                                    right = maxOf(start.x, end.x),
                                                                    bottom = maxOf(start.y, end.y),
                                                                    color = android.graphics.Color.argb(255, (strokeColor.red * 255).toInt(), (strokeColor.green * 255).toInt(), (strokeColor.blue * 255).toInt()),
                                                                    strokeWidth = strokeWidth
                                                                )
                                                            )
                                                        }
                                                    }
                                                    EditorTool.CIRCLE -> {
                                                        if (start != null && end != null) {
                                                            list.add(
                                                                PdfEngine.AnnotationItem.Shape(
                                                                    type = PdfEngine.ShapeType.CIRCLE,
                                                                    left = minOf(start.x, end.x),
                                                                    top = minOf(start.y, end.y),
                                                                    right = maxOf(start.x, end.x),
                                                                    bottom = maxOf(start.y, end.y),
                                                                    color = android.graphics.Color.argb(255, (strokeColor.red * 255).toInt(), (strokeColor.green * 255).toInt(), (strokeColor.blue * 255).toInt()),
                                                                    strokeWidth = strokeWidth
                                                                )
                                                            )
                                                        }
                                                    }
                                                    EditorTool.REDACTION -> {
                                                        if (start != null && end != null) {
                                                            list.add(
                                                                PdfEngine.AnnotationItem.Redaction(
                                                                    rect = RectF(
                                                                        minOf(start.x, end.x),
                                                                        minOf(start.y, end.y),
                                                                        maxOf(start.x, end.x),
                                                                        maxOf(start.y, end.y)
                                                                    )
                                                                )
                                                            )
                                                        }
                                                    }
                                                    else -> {}
                                                }
                                                dragStartOffset = null
                                                dragEndOffset = null
                                            }
                                        )
                                    }
                                }
                        ) {
                            // Draw existing page annotations
                            val items = getPageAnnotations()
                            for (item in items) {
                                when (item) {
                                    is PdfEngine.AnnotationItem.Freehand -> {
                                        if (item.points.size > 1) {
                                            val p = Path()
                                            p.moveTo(item.points[0].x, item.points[0].y)
                                            for (k in 1 until item.points.size) {
                                                p.lineTo(item.points[k].x, item.points[k].y)
                                            }
                                            drawPath(
                                                path = p,
                                                color = Color(item.color),
                                                style = Stroke(
                                                    width = item.strokeWidth,
                                                    cap = StrokeCap.Round,
                                                    join = StrokeJoin.Round
                                                )
                                            )
                                        }
                                    }
                                    is PdfEngine.AnnotationItem.Shape -> {
                                        val c = Color(item.color)
                                        when (item.type) {
                                            PdfEngine.ShapeType.RECTANGLE -> {
                                                drawRect(
                                                    color = c,
                                                    topLeft = Offset(item.left, item.top),
                                                    size = Size(item.right - item.left, item.bottom - item.top),
                                                    style = Stroke(width = item.strokeWidth)
                                                )
                                            }
                                            PdfEngine.ShapeType.CIRCLE -> {
                                                drawOval(
                                                    color = c,
                                                    topLeft = Offset(item.left, item.top),
                                                    size = Size(item.right - item.left, item.bottom - item.top),
                                                    style = Stroke(width = item.strokeWidth)
                                                )
                                            }
                                            else -> {}
                                        }
                                    }
                                    is PdfEngine.AnnotationItem.Redaction -> {
                                        drawRect(
                                            color = Color.Black,
                                            topLeft = Offset(item.rect.left, item.rect.top),
                                            size = Size(item.rect.width(), item.rect.height())
                                        )
                                    }
                                    is PdfEngine.AnnotationItem.Signature -> {
                                        drawImage(
                                            image = item.bitmap.asImageBitmap(),
                                            topLeft = Offset(item.left, item.top)
                                        )
                                    }
                                    is PdfEngine.AnnotationItem.Text -> {
                                        // Draw simplified text outline
                                        drawRect(
                                            color = Color(0xFFFFF9C4),
                                            topLeft = Offset(item.x, item.y - 20f),
                                            size = Size((item.text.length * 9).toFloat(), 24f)
                                        )
                                    }
                                }
                            }

                            // Draw currently in-progress freehand stroke
                            if (currentPoints.size > 1) {
                                val p = Path()
                                p.moveTo(currentPoints[0].x, currentPoints[0].y)
                                for (k in 1 until currentPoints.size) {
                                    p.lineTo(currentPoints[k].x, currentPoints[k].y)
                                }
                                drawPath(
                                    path = p,
                                    color = if (selectedTool == EditorTool.HIGHLIGHTER) strokeColor.copy(alpha = 0.45f) else strokeColor,
                                    style = Stroke(
                                        width = if (selectedTool == EditorTool.HIGHLIGHTER) 24f else strokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            // Draw currently in-progress rectangle / redaction
                            val start = dragStartOffset
                            val end = dragEndOffset
                            if (start != null && end != null) {
                                val left = minOf(start.x, end.x)
                                val top = minOf(start.y, end.y)
                                val width = maxOf(start.x, end.x) - left
                                val height = maxOf(start.y, end.y) - top

                                if (selectedTool == EditorTool.RECTANGLE) {
                                    drawRect(
                                        color = strokeColor,
                                        topLeft = Offset(left, top),
                                        size = Size(width, height),
                                        style = Stroke(width = strokeWidth)
                                    )
                                } else if (selectedTool == EditorTool.CIRCLE) {
                                    drawOval(
                                        color = strokeColor,
                                        topLeft = Offset(left, top),
                                        size = Size(width, height),
                                        style = Stroke(width = strokeWidth)
                                    )
                                } else if (selectedTool == EditorTool.REDACTION) {
                                    drawRect(
                                        color = Color.Black.copy(alpha = 0.8f),
                                        topLeft = Offset(left, top),
                                        size = Size(width, height)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Tools Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Page selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (currentPage > 0) currentPage-- },
                        enabled = currentPage > 0
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev")
                    }
                    Text(
                        text = "Page ${currentPage + 1} of $totalPages",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    IconButton(
                        onClick = { if (currentPage < totalPages - 1) currentPage++ },
                        enabled = currentPage < totalPages - 1
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                    }
                }

                // Tool Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val tools = listOf(
                        Triple(EditorTool.PEN, Icons.Default.Draw, "Pen"),
                        Triple(EditorTool.HIGHLIGHTER, Icons.Default.Highlight, "Highlight"),
                        Triple(EditorTool.TEXT, Icons.Default.TextFields, "Text"),
                        Triple(EditorTool.RECTANGLE, Icons.Outlined.Square, "Box"),
                        Triple(EditorTool.CIRCLE, Icons.Outlined.Circle, "Circle"),
                        Triple(EditorTool.SIGNATURE, Icons.Default.FormatPaint, "Sign"),
                        Triple(EditorTool.REDACTION, Icons.Default.VisibilityOff, "Redact")
                    )

                    for ((t, icon, name) in tools) {
                        val isSelected = selectedTool == t
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable {
                                    if (t == EditorTool.SIGNATURE) {
                                        showSignatureSheet = true
                                    } else {
                                        selectedTool = t
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = name,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = name,
                                fontSize = 10.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Text Input Dialog
    if (showTextInputDialog) {
        AlertDialog(
            onDismissRequest = { showTextInputDialog = false },
            title = { Text("Add Text Annotation") },
            text = {
                OutlinedTextField(
                    value = textInputString,
                    onValueChange = { textInputString = it },
                    placeholder = { Text("Enter text to stamp on page...") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pos = textTapPosition ?: Offset(80f, 150f)
                        if (textInputString.isNotBlank()) {
                            getPageAnnotations().add(
                                PdfEngine.AnnotationItem.Text(
                                    text = textInputString,
                                    x = pos.x,
                                    y = pos.y,
                                    color = android.graphics.Color.BLACK,
                                    textSize = 16f,
                                    hasBackground = true
                                )
                            )
                        }
                        textInputString = ""
                        showTextInputDialog = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextInputDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Signature Sheet
    if (showSignatureSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSignatureSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            SignaturePad(
                onSignatureConfirmed = { signatureBmp ->
                    getPageAnnotations().add(
                        PdfEngine.AnnotationItem.Signature(
                            bitmap = signatureBmp,
                            left = 120f,
                            top = 260f,
                            width = 240f,
                            height = 120f
                        )
                    )
                    showSignatureSheet = false
                },
                onCancel = { showSignatureSheet = false }
            )
        }
    }

    // Save Output Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Annotated PDF") },
            text = {
                Column {
                    Text("Your edits will be saved as a new document, protecting your original file.")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = saveFileName,
                        onValueChange = { saveFileName = it },
                        label = { Text("File name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        viewModel.saveEditedPdf(file, annotationsMap, saveFileName) { savedFile ->
                            if (savedFile != null) {
                                onSaved(savedFile)
                            }
                        }
                    }
                ) {
                    Text("Save Document")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

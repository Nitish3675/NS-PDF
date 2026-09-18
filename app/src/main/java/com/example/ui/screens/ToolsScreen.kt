package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ToolDefinition(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun ToolsScreen(
    onToolSelected: (String) -> Unit
) {
    val categories = listOf(
        Pair(
            "Scan & Convert",
            listOf(
                ToolDefinition("scanner", "Document Scanner", "Scan pages with camera, apply smart B&W/color filters", Icons.Default.DocumentScanner, Color(0xFF43A047)),
                ToolDefinition("ocr", "OCR Text Extractor", "Extract editable text from images or scanned pages", Icons.Default.FormatColorText, Color(0xFF1E88E5)),
                ToolDefinition("tool_img_to_pdf", "Image to PDF", "Convert photos and receipts into clean PDF documents", Icons.Default.Image, Color(0xFFE53935)),
                ToolDefinition("tool_pdf_to_img", "PDF to Images", "Export pages as high-resolution JPG or PNG images", Icons.Default.ViewCarousel, Color(0xFF00ACC1)),
                ToolDefinition("tool_create_note", "Create PDF Note", "Type new PDF with formatted title, sections, and margins", Icons.Default.Create, Color(0xFF5E35B1))
            )
        ),
        Pair(
            "Organize & Optimize",
            listOf(
                ToolDefinition("tool_merge", "Merge PDFs", "Combine multiple documents into a single file", Icons.Default.MergeType, Color(0xFFFB8C00)),
                ToolDefinition("tool_split", "Split PDF", "Extract specific pages or page ranges into a new PDF", Icons.Default.FlipToBack, Color(0xFF8E24AA)),
                ToolDefinition("tool_compress", "Compress PDF", "Reduce file size significantly while retaining readability", Icons.Default.Compress, Color(0xFF00897B)),
                ToolDefinition("tool_rotate", "Rotate Pages", "Fix landscape/portrait orientation (90°, 180°, 270°)", Icons.Default.CropRotate, Color(0xFF3949AB))
            )
        ),
        Pair(
            "Security & Markup",
            listOf(
                ToolDefinition("tool_sign", "Digital Signature", "Draw signature and stamp onto document pages", Icons.Default.Draw, Color(0xFFD81B60)),
                ToolDefinition("tool_watermark", "Watermark PDF", "Add diagonal text watermark (Confidential, Draft, etc.)", Icons.Default.TextFields, Color(0xFF00897B)),
                ToolDefinition("tool_redact", "Permanent Redaction", "Irreversibly erase sensitive PII, SSN, and numbers", Icons.Default.VisibilityOff, Color(0xFF4E342E)),
                ToolDefinition("tool_page_numbers", "Header & Page Numbers", "Add 'Page X of N' and custom document headers", Icons.Default.FormatColorText, Color(0xFF0277BD))
            )
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = "PDF Studio Tools",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Complete on-device PDF utility and document suite",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            for ((categoryName, tools) in categories) {
                item {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }

                items(tools.size) { index ->
                    val tool = tools[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToolSelected(tool.id) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tool.color.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = tool.icon,
                                    contentDescription = tool.title,
                                    tint = tool.color,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tool.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tool.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    strokeColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Black,
    strokeWidth: Float = 6f,
    onSignatureConfirmed: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    val paths = remember { mutableStateListOf<List<Offset>>() }
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Draw Your Signature",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(androidx.compose.ui.graphics.Color.White)
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp)
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPath = listOf(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPath = currentPath + change.position
                        },
                        onDragEnd = {
                            if (currentPath.isNotEmpty()) {
                                paths.add(currentPath)
                                currentPath = emptyList()
                            }
                        }
                    )
                }
                .testTag("signature_canvas")
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                // Draw past completed paths
                for (p in paths) {
                    if (p.size > 1) {
                        for (i in 0 until p.size - 1) {
                            drawLine(
                                color = strokeColor,
                                start = p[i],
                                end = p[i + 1],
                                strokeWidth = strokeWidth,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }
                }
                // Draw current drawing path
                if (currentPath.size > 1) {
                    for (i in 0 until currentPath.size - 1) {
                        drawLine(
                            color = strokeColor,
                            start = currentPath[i],
                            end = currentPath[i + 1],
                            strokeWidth = strokeWidth,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
            }

            // Dotted baseline guide
            Text(
                text = "Sign on the line above ________________________",
                style = MaterialTheme.typography.labelSmall,
                color = androidx.compose.ui.graphics.Color.LightGray,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = {
                    paths.clear()
                    currentPath = emptyList()
                },
                modifier = Modifier.testTag("clear_signature_button")
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Clear")
                Text("Clear", modifier = Modifier.padding(start = 4.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag("cancel_signature_button")
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        // Render paths to bitmap
                        val bmp = Bitmap.createBitmap(600, 300, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bmp)
                        // Transparent or white background
                        canvas.drawColor(Color.TRANSPARENT)
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.BLACK
                            this.strokeWidth = strokeWidth * 1.5f
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                        }

                        val allPaths = paths.toList() + (if (currentPath.isNotEmpty()) listOf(currentPath) else emptyList())
                        for (pathPoints in allPaths) {
                            if (pathPoints.size > 1) {
                                val androidPath = Path()
                                androidPath.moveTo(pathPoints[0].x * 1.5f, pathPoints[0].y * 1.5f)
                                for (j in 1 until pathPoints.size) {
                                    androidPath.lineTo(pathPoints[j].x * 1.5f, pathPoints[j].y * 1.5f)
                                }
                                canvas.drawPath(androidPath, paint)
                            }
                        }

                        onSignatureConfirmed(bmp)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("save_signature_button")
                ) {
                    Icon(Icons.Default.Done, contentDescription = "Done")
                    Text("Apply", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

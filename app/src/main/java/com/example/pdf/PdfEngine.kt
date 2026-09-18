package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object PdfEngine {

    // Annotation Data Models
    enum class ShapeType { RECTANGLE, CIRCLE, LINE, ARROW }

    sealed class AnnotationItem {
        data class Freehand(
            val points: List<PointF>,
            val color: Int,
            val strokeWidth: Float,
            val isHighlighter: Boolean = false
        ) : AnnotationItem()

        data class Text(
            val text: String,
            val x: Float,
            val y: Float,
            val color: Int,
            val textSize: Float,
            val hasBackground: Boolean = false
        ) : AnnotationItem()

        data class Shape(
            val type: ShapeType,
            val left: Float,
            val top: Float,
            val right: Float,
            val bottom: Float,
            val color: Int,
            val strokeWidth: Float,
            val isFilled: Boolean = false
        ) : AnnotationItem()

        data class Signature(
            val bitmap: Bitmap,
            val left: Float,
            val top: Float,
            val width: Float,
            val height: Float
        ) : AnnotationItem()

        data class Redaction(
            val rect: RectF,
            val color: Int = Color.BLACK
        ) : AnnotationItem()
    }

    /**
     * Retrieves total number of pages in a PDF file
     */
    fun getPageCount(file: File): Int {
        if (!file.exists() || file.length() == 0L) return 0
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            renderer.pageCount
        } catch (e: Exception) {
            e.printStackTrace()
            0
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Renders a specific page to a Bitmap
     */
    fun renderPageToBitmap(file: File, pageIndex: Int, scale: Float = 1.5f): Bitmap? {
        if (!file.exists() || file.length() == 0L) return null
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        return try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null

            page = renderer.openPage(pageIndex)
            val width = (page.width * scale).toInt().coerceAtLeast(100)
            val height = (page.height * scale).toInt().coerceAtLeast(100)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try { page?.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Renders a fast thumbnail of page 0 or requested page
     */
    fun renderThumbnail(file: File, pageIndex: Int = 0, targetWidth: Int = 240): Bitmap? {
        if (!file.exists() || file.length() == 0L) return null
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        return try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null

            page = renderer.openPage(pageIndex)
            val scale = targetWidth.toFloat() / page.width.toFloat()
            val width = targetWidth
            val height = (page.height * scale).toInt().coerceAtLeast(100)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try { page?.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Merge multiple PDF files into one output PDF
     */
    fun mergePdfs(sourceFiles: List<File>, outputFile: File): Boolean {
        if (sourceFiles.isEmpty()) return false
        val doc = PdfDocument()
        var globalPageNum = 1

        try {
            for (file in sourceFiles) {
                if (!file.exists() || file.length() == 0L) continue
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                val pageCount = renderer.pageCount

                for (i in 0 until pageCount) {
                    val page = renderer.openPage(i)
                    val pw = page.width
                    val ph = page.height

                    // Render source page at high resolution
                    val scale = 2.0f
                    val bmp = Bitmap.createBitmap((pw * scale).toInt(), (ph * scale).toInt(), Bitmap.Config.ARGB_8888)
                    val c = Canvas(bmp)
                    c.drawColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    page.close()

                    // Start new document page matching original dimensions
                    val pageInfo = PdfDocument.PageInfo.Builder(pw, ph, globalPageNum++).create()
                    val docPage = doc.startPage(pageInfo)
                    val docCanvas = docPage.canvas

                    val destRect = Rect(0, 0, pw, ph)
                    docCanvas.drawBitmap(bmp, null, destRect, null)
                    doc.finishPage(docPage)
                    bmp.recycle()
                }

                renderer.close()
                pfd.close()
            }

            writeDocument(doc, outputFile)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try { doc.close() } catch (_: Exception) {}
        }
    }

    /**
     * Splits a PDF by extracting specific page indices (0-indexed)
     */
    fun splitPdf(sourceFile: File, pageIndices: List<Int>, outputFile: File): Boolean {
        if (!sourceFile.exists() || pageIndices.isEmpty()) return false
        val doc = PdfDocument()

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)

            var outPageNum = 1
            for (idx in pageIndices) {
                if (idx < 0 || idx >= renderer.pageCount) continue
                val page = renderer.openPage(idx)
                val pw = page.width
                val ph = page.height

                val scale = 2.0f
                val bmp = Bitmap.createBitmap((pw * scale).toInt(), (ph * scale).toInt(), Bitmap.Config.ARGB_8888)
                val c = Canvas(bmp)
                c.drawColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val pageInfo = PdfDocument.PageInfo.Builder(pw, ph, outPageNum++).create()
                val docPage = doc.startPage(pageInfo)
                val destRect = Rect(0, 0, pw, ph)
                docPage.canvas.drawBitmap(bmp, null, destRect, null)
                doc.finishPage(docPage)
                bmp.recycle()
            }

            writeDocument(doc, outputFile)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try { doc.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Compress PDF with chosen quality (e.g. 85, 65, 45) and downscale factor
     */
    fun compressPdf(sourceFile: File, qualityPercent: Int, downscaleFactor: Float, outputFile: File): Boolean {
        if (!sourceFile.exists()) return false
        val doc = PdfDocument()

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val count = renderer.pageCount

            for (i in 0 until count) {
                val page = renderer.openPage(i)
                val pw = page.width
                val ph = page.height

                val renderScale = downscaleFactor.coerceIn(0.6f, 1.8f)
                val rawBmp = Bitmap.createBitmap((pw * renderScale).toInt(), (ph * renderScale).toInt(), Bitmap.Config.ARGB_8888)
                val c = Canvas(rawBmp)
                c.drawColor(Color.WHITE)
                page.render(rawBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                // Compress as JPEG to achieve genuine size reduction
                val stream = ByteArrayOutputStream()
                rawBmp.compress(Bitmap.CompressFormat.JPEG, qualityPercent.coerceIn(20, 95), stream)
                val compressedBytes = stream.toByteArray()
                val compressedBmp = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)
                rawBmp.recycle()

                val pageInfo = PdfDocument.PageInfo.Builder(pw, ph, i + 1).create()
                val docPage = doc.startPage(pageInfo)
                val destRect = Rect(0, 0, pw, ph)
                docPage.canvas.drawBitmap(compressedBmp, null, destRect, null)
                doc.finishPage(docPage)
                compressedBmp.recycle()
            }

            writeDocument(doc, outputFile)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try { doc.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Convert Image files to a clean multi-page or single-page PDF
     */
    fun convertImagesToPdf(
        imageFiles: List<File>,
        pageSizeName: String = "A4", // A4, Letter, Fit
        marginPt: Int = 20,
        outputFile: File
    ): Boolean {
        if (imageFiles.isEmpty()) return false
        val doc = PdfDocument()

        val (stdW, stdH) = when (pageSizeName.uppercase()) {
            "LETTER" -> Pair(612, 792)
            "LEGAL" -> Pair(612, 1008)
            else -> Pair(595, 842) // A4 default
        }

        try {
            var pageNum = 1
            for (imgFile in imageFiles) {
                if (!imgFile.exists()) continue
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(imgFile.absolutePath, options)
                val imgW = options.outWidth
                val imgH = options.outHeight
                if (imgW <= 0 || imgH <= 0) continue

                // Determine page size
                val (pageW, pageH) = if (pageSizeName.equals("FIT", ignoreCase = true)) {
                    Pair(imgW, imgH)
                } else {
                    // Check orientation
                    if (imgW > imgH) Pair(stdH, stdW) else Pair(stdW, stdH)
                }

                // Decode full image
                val fullBmp = BitmapFactory.decodeFile(imgFile.absolutePath) ?: continue

                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, pageNum++).create()
                val docPage = doc.startPage(pageInfo)
                val canvas = docPage.canvas

                val availW = (pageW - 2 * marginPt).coerceAtLeast(50)
                val availH = (pageH - 2 * marginPt).coerceAtLeast(50)

                val scale = minOf(availW.toFloat() / fullBmp.width.toFloat(), availH.toFloat() / fullBmp.height.toFloat())
                val destW = (fullBmp.width * scale).toInt()
                val destH = (fullBmp.height * scale).toInt()
                val left = (pageW - destW) / 2
                val top = (pageH - destH) / 2

                canvas.drawColor(Color.WHITE)
                val destRect = Rect(left, top, left + destW, top + destH)
                canvas.drawBitmap(fullBmp, null, destRect, null)

                doc.finishPage(docPage)
                fullBmp.recycle()
            }

            writeDocument(doc, outputFile)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try { doc.close() } catch (_: Exception) {}
        }
    }

    /**
     * Export all PDF pages as image files (JPG or PNG)
     */
    fun convertPdfToImages(
        sourceFile: File,
        outputDir: File,
        format: String = "JPG",
        quality: Int = 90
    ): List<File> {
        val result = mutableListOf<File>()
        if (!sourceFile.exists()) return result
        if (!outputDir.exists()) outputDir.mkdirs()

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val baseName = sourceFile.nameWithoutExtension

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val pw = (page.width * 2.0f).toInt()
                val ph = (page.height * 2.0f).toInt()

                val bmp = Bitmap.createBitmap(pw, ph, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.drawColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val ext = if (format.equals("PNG", ignoreCase = true)) "png" else "jpg"
                val imgFile = File(outputDir, "${baseName}_Page_${i + 1}.$ext")
                val fos = FileOutputStream(imgFile)
                val compressFormat = if (ext == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                bmp.compress(compressFormat, quality, fos)
                fos.flush()
                fos.close()
                bmp.recycle()

                result.add(imgFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
        return result
    }

    /**
     * Adds watermark text or logo across target pages
     */
    fun addWatermark(
        sourceFile: File,
        watermarkText: String,
        textColor: Int = Color.parseColor("#44D32F2F"),
        fontSize: Float = 48f,
        rotationDegrees: Float = -45f,
        outputFile: File
    ): Boolean {
        if (!sourceFile.exists()) return false
        val doc = PdfDocument()

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                textSize = fontSize
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val pw = page.width
                val ph = page.height

                val scale = 2.0f
                val bmp = Bitmap.createBitmap((pw * scale).toInt(), (ph * scale).toInt(), Bitmap.Config.ARGB_8888)
                val c = Canvas(bmp)
                c.drawColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val pageInfo = PdfDocument.PageInfo.Builder(pw, ph, i + 1).create()
                val docPage = doc.startPage(pageInfo)
                val docCanvas = docPage.canvas

                // Draw original page
                docCanvas.drawBitmap(bmp, null, Rect(0, 0, pw, ph), null)

                // Overlay watermark rotated at center
                docCanvas.save()
                docCanvas.translate(pw / 2f, ph / 2f)
                docCanvas.rotate(rotationDegrees)
                docCanvas.drawText(watermarkText, 0f, 0f, paint)
                docCanvas.restore()

                doc.finishPage(docPage)
                bmp.recycle()
            }

            writeDocument(doc, outputFile)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try { doc.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Adds header, footer, and page numbers ("Page X of N")
     */
    fun addHeaderFooterPageNumbers(
        sourceFile: File,
        headerText: String?,
        footerText: String?,
        includePageNumbers: Boolean,
        outputFile: File
    ): Boolean {
        if (!sourceFile.exists()) return false
        val doc = PdfDocument()

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#424242")
                textSize = 9.5f
            }

            for (i in 0 until totalPages) {
                val page = renderer.openPage(i)
                val pw = page.width
                val ph = page.height

                val scale = 2.0f
                val bmp = Bitmap.createBitmap((pw * scale).toInt(), (ph * scale).toInt(), Bitmap.Config.ARGB_8888)
                val c = Canvas(bmp)
                c.drawColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val pageInfo = PdfDocument.PageInfo.Builder(pw, ph, i + 1).create()
                val docPage = doc.startPage(pageInfo)
                val docCanvas = docPage.canvas

                docCanvas.drawBitmap(bmp, null, Rect(0, 0, pw, ph), null)

                // Header
                if (!headerText.isNullOrBlank()) {
                    textPaint.textAlign = Paint.Align.LEFT
                    docCanvas.drawText(headerText, 30f, 25f, textPaint)
                }

                // Footer text left
                if (!footerText.isNullOrBlank()) {
                    textPaint.textAlign = Paint.Align.LEFT
                    docCanvas.drawText(footerText, 30f, ph - 20f, textPaint)
                }

                // Page numbering right
                if (includePageNumbers) {
                    textPaint.textAlign = Paint.Align.RIGHT
                    docCanvas.drawText("Page ${i + 1} of $totalPages", (pw - 30).toFloat(), ph - 20f, textPaint)
                }

                doc.finishPage(docPage)
                bmp.recycle()
            }

            writeDocument(doc, outputFile)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try { doc.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Rotate pages by 90, 180, or 270 degrees
     */
    fun rotatePages(
        sourceFile: File,
        rotationDegrees: Int,
        targetPages: List<Int>?, // null for all
        outputFile: File
    ): Boolean {
        if (!sourceFile.exists()) return false
        val doc = PdfDocument()

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)

            for (i in 0 until renderer.pageCount) {
                val shouldRotate = targetPages == null || targetPages.contains(i)
                val page = renderer.openPage(i)
                val origW = page.width
                val origH = page.height

                val scale = 2.0f
                val bmp = Bitmap.createBitmap((origW * scale).toInt(), (origH * scale).toInt(), Bitmap.Config.ARGB_8888)
                val c = Canvas(bmp)
                c.drawColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val (outW, outH) = if (shouldRotate && (rotationDegrees == 90 || rotationDegrees == 270)) {
                    Pair(origH, origW)
                } else {
                    Pair(origW, origH)
                }

                val pageInfo = PdfDocument.PageInfo.Builder(outW, outH, i + 1).create()
                val docPage = doc.startPage(pageInfo)
                val docCanvas = docPage.canvas

                if (shouldRotate && rotationDegrees != 0) {
                    docCanvas.save()
                    docCanvas.translate(outW / 2f, outH / 2f)
                    docCanvas.rotate(rotationDegrees.toFloat())
                    val destRect = Rect(-origW / 2, -origH / 2, origW / 2, origH / 2)
                    docCanvas.drawBitmap(bmp, null, destRect, null)
                    docCanvas.restore()
                } else {
                    docCanvas.drawBitmap(bmp, null, Rect(0, 0, outW, outH), null)
                }

                doc.finishPage(docPage)
                bmp.recycle()
            }

            writeDocument(doc, outputFile)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try { doc.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Saves drawing annotations, text boxes, shapes, signatures onto the PDF
     * creating a new _Edited.pdf file without mutating original!
     */
    fun saveAnnotationsToPdf(
        sourceFile: File,
        annotationsMap: Map<Int, List<AnnotationItem>>,
        outputFile: File
    ): Boolean {
        if (!sourceFile.exists()) return false
        val doc = PdfDocument()

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val pw = page.width
                val ph = page.height

                val scale = 2.0f
                val bmp = Bitmap.createBitmap((pw * scale).toInt(), (ph * scale).toInt(), Bitmap.Config.ARGB_8888)
                val c = Canvas(bmp)
                c.drawColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val pageInfo = PdfDocument.PageInfo.Builder(pw, ph, i + 1).create()
                val docPage = doc.startPage(pageInfo)
                val docCanvas = docPage.canvas

                // Draw base rendered page
                docCanvas.drawBitmap(bmp, null, Rect(0, 0, pw, ph), null)

                // Render annotations for this page
                val items = annotationsMap[i]
                if (!items.isNullOrEmpty()) {
                    for (item in items) {
                        drawAnnotation(docCanvas, item)
                    }
                }

                doc.finishPage(docPage)
                bmp.recycle()
            }

            writeDocument(doc, outputFile)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try { doc.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    private fun drawAnnotation(canvas: Canvas, item: AnnotationItem) {
        when (item) {
            is AnnotationItem.Freehand -> {
                if (item.points.size < 2) return
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = item.color
                    strokeWidth = item.strokeWidth
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    if (item.isHighlighter) {
                        alpha = 110
                    }
                }
                val path = Path()
                path.moveTo(item.points[0].x, item.points[0].y)
                for (j in 1 until item.points.size) {
                    path.lineTo(item.points[j].x, item.points[j].y)
                }
                canvas.drawPath(path, paint)
            }

            is AnnotationItem.Text -> {
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = item.color
                    textSize = item.textSize
                }
                if (item.hasBackground) {
                    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#FFFDE7")
                    }
                    val textBounds = Rect()
                    textPaint.getTextBounds(item.text, 0, item.text.length, textBounds)
                    canvas.drawRoundRect(
                        RectF(
                            item.x - 4f,
                            item.y - textBounds.height() - 4f,
                            item.x + textBounds.width() + 4f,
                            item.y + 4f
                        ),
                        4f,
                        4f,
                        bgPaint
                    )
                }
                canvas.drawText(item.text, item.x, item.y, textPaint)
            }

            is AnnotationItem.Shape -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = item.color
                    strokeWidth = item.strokeWidth
                    style = if (item.isFilled) Paint.Style.FILL else Paint.Style.STROKE
                }
                when (item.type) {
                    ShapeType.RECTANGLE -> {
                        canvas.drawRect(item.left, item.top, item.right, item.bottom, paint)
                    }
                    ShapeType.CIRCLE -> {
                        canvas.drawOval(RectF(item.left, item.top, item.right, item.bottom), paint)
                    }
                    ShapeType.LINE -> {
                        canvas.drawLine(item.left, item.top, item.right, item.bottom, paint)
                    }
                    ShapeType.ARROW -> {
                        canvas.drawLine(item.left, item.top, item.right, item.bottom, paint)
                        // Arrow head
                        val angle = atan2((item.bottom - item.top).toDouble(), (item.right - item.left).toDouble())
                        val arrowLen = 16f
                        val arrowAngle = Math.PI / 6
                        val x1 = item.right - arrowLen * cos(angle - arrowAngle).toFloat()
                        val y1 = item.bottom - arrowLen * sin(angle - arrowAngle).toFloat()
                        val x2 = item.right - arrowLen * cos(angle + arrowAngle).toFloat()
                        val y2 = item.bottom - arrowLen * sin(angle + arrowAngle).toFloat()
                        canvas.drawLine(item.right, item.bottom, x1, y1, paint)
                        canvas.drawLine(item.right, item.bottom, x2, y2, paint)
                    }
                }
            }

            is AnnotationItem.Signature -> {
                val destRect = RectF(item.left, item.top, item.left + item.width, item.top + item.height)
                canvas.drawBitmap(item.bitmap, null, destRect, null)
            }

            is AnnotationItem.Redaction -> {
                val paint = Paint().apply {
                    color = item.color
                    style = Paint.Style.FILL
                }
                canvas.drawRect(item.rect, paint)
            }
        }
    }

    /**
     * Create a new blank PDF from text content
     */
    fun createPdfFromText(title: String, content: String, outputFile: File): Boolean {
        val doc = PdfDocument()
        val pageW = 595
        val pageH = 842

        try {
            val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1B1B1F")
                textSize = 22f
                isFakeBoldText = true
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#333333")
                textSize = 12f
            }

            canvas.drawText(title, 50f, 70f, titlePaint)

            // Draw divider
            val divPaint = Paint().apply { color = Color.parseColor("#E0E0E0") }
            canvas.drawLine(50f, 90f, (pageW - 50).toFloat(), 90f, divPaint)

            // Wrap and draw lines
            val lines = content.split("\n")
            var y = 125f
            for (rawLine in lines) {
                var remaining = rawLine
                while (remaining.isNotEmpty() && y < pageH - 50) {
                    val count = bodyPaint.breakText(remaining, true, (pageW - 100).toFloat(), null)
                    val lineToDraw = remaining.substring(0, count)
                    canvas.drawText(lineToDraw, 50f, y, bodyPaint)
                    y += 18f
                    remaining = remaining.substring(count)
                }
            }

            doc.finishPage(page)
            writeDocument(doc, outputFile)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try { doc.close() } catch (_: Exception) {}
        }
    }

    private fun writeDocument(doc: PdfDocument, outputFile: File) {
        if (!outputFile.parentFile.exists()) {
            outputFile.parentFile.mkdirs()
        }
        val fos = FileOutputStream(outputFile)
        doc.writeTo(fos)
        fos.flush()
        fos.close()
    }
}

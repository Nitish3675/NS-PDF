package com.example.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

object ImageFilters {

    enum class FilterMode {
        ORIGINAL,
        GRAYSCALE,
        BLACK_AND_WHITE,
        DOCUMENT_ENHANCE
    }

    fun applyFilter(
        source: Bitmap,
        mode: FilterMode,
        brightness: Float = 0f, // -100 to 100
        contrast: Float = 1f    // 0.5 to 2.0
    ): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (mode) {
            FilterMode.ORIGINAL -> {
                if (brightness != 0f || contrast != 1f) {
                    val cm = ColorMatrix()
                    // Contrast & brightness
                    val scale = contrast
                    val translate = brightness + (1f - scale) * 128f
                    val matrix = floatArrayOf(
                        scale, 0f, 0f, 0f, translate,
                        0f, scale, 0f, 0f, translate,
                        0f, 0f, scale, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                    cm.set(matrix)
                    paint.colorFilter = ColorMatrixColorFilter(cm)
                }
                canvas.drawBitmap(source, 0f, 0f, paint)
            }

            FilterMode.GRAYSCALE -> {
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                if (brightness != 0f || contrast != 1f) {
                    val scale = contrast
                    val translate = brightness + (1f - scale) * 128f
                    val bcMatrix = ColorMatrix(floatArrayOf(
                        scale, 0f, 0f, 0f, translate,
                        0f, scale, 0f, 0f, translate,
                        0f, 0f, scale, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    ))
                    cm.postConcat(bcMatrix)
                }
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(source, 0f, 0f, paint)
            }

            FilterMode.DOCUMENT_ENHANCE -> {
                // Boost contrast and clean background
                val cm = ColorMatrix()
                // Desaturate slightly, high contrast, slight brightness boost
                val sat = ColorMatrix()
                sat.setSaturation(0.3f)
                val scale = 1.4f * contrast
                val translate = (brightness + 15f) + (1f - scale) * 128f
                val bc = ColorMatrix(floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.setConcat(bc, sat)
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(source, 0f, 0f, paint)
            }

            FilterMode.BLACK_AND_WHITE -> {
                // First draw grayscale
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(source, 0f, 0f, paint)

                // High threshold binarization for clean crisp text
                val pixels = IntArray(width * height)
                result.getPixels(pixels, 0, width, 0, 0, width, height)
                val threshold = (128 + brightness.toInt()).coerceIn(40, 220)

                for (i in pixels.indices) {
                    val color = pixels[i]
                    val r = Color.red(color)
                    val g = Color.green(color)
                    val b = Color.blue(color)
                    val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                    val binaryVal = if (lum < threshold) Color.BLACK else Color.WHITE
                    pixels[i] = binaryVal
                }
                result.setPixels(pixels, 0, width, 0, 0, width, height)
            }
        }

        return result
    }

    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360 == 0f) return source
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}

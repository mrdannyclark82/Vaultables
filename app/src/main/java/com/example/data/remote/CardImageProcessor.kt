package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import java.io.ByteArrayOutputStream

data class CardImageQuality(
    val isClear: Boolean,
    val issues: List<String>
)

object CardImageProcessor {
    private const val MAX_UPLOAD_BYTES = 2_000_000
    fun assessQuality(context: Context, imageUri: Uri): CardImageQuality {
        val bitmap = decodeBitmap(context, imageUri) ?: return CardImageQuality(
            isClear = false,
            issues = listOf("The photo could not be read. Capture it again.")
        )
        val preview = Bitmap.createScaledBitmap(bitmap, 256, (bitmap.height * 256f / bitmap.width).toInt().coerceAtLeast(1), true)
        val pixels = IntArray(preview.width * preview.height)
        preview.getPixels(pixels, 0, preview.width, 0, 0, preview.width, preview.height)

        val luminance = DoubleArray(pixels.size)
        var sum = 0.0
        var glarePixels = 0
        pixels.forEachIndexed { index, pixel ->
            val value = 0.2126 * Color.red(pixel) + 0.7152 * Color.green(pixel) + 0.0722 * Color.blue(pixel)
            luminance[index] = value
            sum += value
            if (value > 245) glarePixels++
        }
        val average = sum / luminance.size
        val contrast = kotlin.math.sqrt(luminance.sumOf { (it - average) * (it - average) } / luminance.size)
        var edgeDifference = 0.0
        var edgeCount = 0
        for (y in 0 until preview.height - 1) {
            for (x in 0 until preview.width - 1) {
                val index = y * preview.width + x
                edgeDifference += kotlin.math.abs(luminance[index] - luminance[index + 1])
                edgeDifference += kotlin.math.abs(luminance[index] - luminance[index + preview.width])
                edgeCount += 2
            }
        }

        val issues = buildList {
            if (bitmap.width < 1000 || bitmap.height < 1000) add("Move closer so the card text is readable.")
            if (average < 55) add("Add more light; the card is too dark.")
            if (average > 220) add("Reduce direct light; the card is overexposed.")
            if (glarePixels.toDouble() / luminance.size > 0.08) add("Tilt the card to remove glare before recapturing.")
            if (contrast < 24 || edgeDifference / edgeCount < 12) add("Hold the camera steady and let it focus before capturing.")
        }
        preview.recycle()
        bitmap.recycle()
        return CardImageQuality(isClear = issues.isEmpty(), issues = issues)
    }

    fun prepareForUpload(context: Context, imageUri: String, maxDimension: Int = 2048): ByteArray? {
        val bitmap = decodeBitmap(context, Uri.parse(imageUri)) ?: return null
        val scale = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
        val uploadBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else {
            bitmap
        }
        return ByteArrayOutputStream().use { output ->
            var quality = 92
            do {
                output.reset()
                uploadBitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
                quality -= 8
            } while (output.size() > MAX_UPLOAD_BYTES && quality >= 60)
            if (uploadBitmap !== bitmap) uploadBitmap.recycle()
            bitmap.recycle()
            output.toByteArray().takeIf { it.size <= MAX_UPLOAD_BYTES }
        }
    }

    private fun decodeBitmap(context: Context, imageUri: Uri): Bitmap? =
        context.contentResolver.openInputStream(imageUri)?.use(BitmapFactory::decodeStream)
}

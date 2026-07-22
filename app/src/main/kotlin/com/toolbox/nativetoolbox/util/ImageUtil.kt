package com.toolbox.nativetoolbox.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayOutputStream

object ImageUtil {

    /** 从 Uri 解码 Bitmap(限制最大边,防 OOM) */
    fun loadBitmap(context: Context, uri: Uri, maxDim: Int = 4096): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        } catch (e: Exception) {
            null
        }
    }

    /** 压缩/转码为指定格式字节 */
    fun encode(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(format, quality, out)
        return out.toByteArray()
    }

    fun webpFormat(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY
        else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP

    /** 保存图片到相册 Pictures/AstroKit */
    fun saveToPictures(
        context: Context,
        fileName: String,
        bytes: ByteArray,
        mimeType: String
    ): Result<String> {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AstroKit")
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            ) ?: return Result.failure(Exception("无法创建文件"))
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: return Result.failure(Exception("无法写入文件"))
            Result.success("Pictures/AstroKit/$fileName")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 文本 → 二维码 Bitmap */
    fun generateQr(text: String, size: Int = 720): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1
            )
            val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
            val pixels = IntArray(size * size)
            for (y in 0 until size) {
                for (x in 0 until size) {
                    pixels[y * size + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            Bitmap.createBitmap(pixels, size, size, Bitmap.Config.RGB_565)
        } catch (e: Exception) {
            null
        }
    }

    /** 从图片识别二维码/条码 */
    fun decodeQr(bitmap: Bitmap): String? {
        return try {
            // 大图缩到 1024 内提高识别速度
            val scaled = if (bitmap.width > 1024 || bitmap.height > 1024) {
                val ratio = minOf(1024f / bitmap.width, 1024f / bitmap.height)
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt(),
                    (bitmap.height * ratio).toInt(),
                    true
                )
            } else bitmap
            val pixels = IntArray(scaled.width * scaled.height)
            scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
            val source = RGBLuminanceSource(scaled.width, scaled.height, pixels)
            val binary = BinaryBitmap(HybridBinarizer(source))
            val hints = mapOf(DecodeHintType.TRY_HARDER to true)
            MultiFormatReader().decode(binary, hints).text
        } catch (e: Exception) {
            null
        }
    }
}

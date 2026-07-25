package com.toolbox.nativetoolbox.util

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmenterOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 人像分割(ML Kit,模型随 APK 离线运行)。
 * 返回带透明背景的抠图,或按给定背景色合成。
 */
object Matting {

    /** 返回 alpha 蒙版应用后的透明底人像,失败返回 null */
    suspend fun cutout(src: Bitmap, threshold: Float = 0.55f, feather: Boolean = true): Bitmap? {
        val segmenter = Segmentation.getClient(
            SegmenterOptions.Builder()
                .setDetectorMode(SegmenterOptions.SINGLE_IMAGE_MODE)
                .build()
        )
        val mask = suspendCancellableCoroutine { cont ->
            segmenter.process(InputImage.fromBitmap(src, 0))
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        } ?: run { segmenter.close(); return null }

        val mw = mask.width
        val mh = mask.height
        val buf = mask.buffer
        buf.rewind()
        val conf = FloatArray(mw * mh)
        for (i in conf.indices) conf[i] = buf.float

        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(src.width * src.height)
        src.getPixels(pixels, 0, src.width, 0, 0, src.width, src.height)
        // 蒙版尺寸与原图一致(SINGLE_IMAGE_MODE 下 ML Kit 返回原尺寸)
        for (y in 0 until src.height) {
            val my = y * mh / src.height
            for (x in 0 until src.width) {
                val mx = x * mw / src.width
                val c = conf[my * mw + mx]
                val i = y * src.width + x
                val a = when {
                    c >= threshold + 0.15f -> 255
                    c <= threshold - 0.15f -> 0
                    feather -> (((c - (threshold - 0.15f)) / 0.30f) * 255).toInt().coerceIn(0, 255)
                    else -> if (c >= threshold) 255 else 0
                }
                pixels[i] = (a shl 24) or (pixels[i] and 0x00FFFFFF)
            }
        }
        out.setPixels(pixels, 0, src.width, 0, 0, src.width, src.height)
        segmenter.close()
        return out
    }

    /** 透明底人像 + 纯色背景合成 */
    fun compose(cut: Bitmap, bg: Int): Bitmap {
        val out = Bitmap.createBitmap(cut.width, cut.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        canvas.drawColor(bg)
        canvas.drawBitmap(cut, 0f, 0f, null)
        return out
    }
}

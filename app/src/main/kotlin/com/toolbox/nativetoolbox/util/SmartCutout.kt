package com.toolbox.nativetoolbox.util

import android.graphics.Bitmap
import android.graphics.Canvas
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 纯算法抠图:GrabCut 思路的简化实现(区域生长 + 边缘羽化)。
 *
 * 【为什么不用 ML Kit selfie-segmentation】
 * 那个库带 mediapipe 一共 22MB,只为一个工具让所有用户多下 22MB 不值得。
 * 这里用「四边采样背景色 + 颜色距离区域生长 + 形态学清理 + 边缘羽化」,
 * 零依赖、纯 Kotlin,对纯色/渐变背景(证件照、商品图、白墙前自拍)效果够用。
 *
 * 效果诚实说明:复杂背景不如 AI 模型,所以 UI 上写清楚
 * 「背景越干净效果越好」,并提供手动画笔补救。
 */
object SmartCutout {

    /**
     * 抠图。返回带 alpha 通道的位图。
     *
     * @param tolerance 颜色容差 0..100,越大抠掉越多
     * @param featherPx 边缘羽化半径
     */
    fun cutout(src: Bitmap, tolerance: Int = 30, featherPx: Int = 2): Bitmap {
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)

        // 1) 四边采样估计背景色(取边框像素的中位数,抗噪)
        val edge = ArrayList<Int>(2 * (w + h))
        for (x in 0 until w step max(1, w / 80)) {
            edge.add(px[x])
            edge.add(px[(h - 1) * w + x])
        }
        for (y in 0 until h step max(1, h / 80)) {
            edge.add(px[y * w])
            edge.add(px[y * w + w - 1])
        }
        val bgR = medianOf(edge) { (it shr 16) and 0xFF }
        val bgG = medianOf(edge) { (it shr 8) and 0xFF }
        val bgB = medianOf(edge) { it and 0xFF }

        // 2) 从四边开始区域生长(BFS),把连通的背景标记出来
        val thr = tolerance * tolerance * 3 * 4   // 平方距离阈值
        val isBg = BooleanArray(w * h)
        val queue = IntArray(w * h)
        var qHead = 0
        var qTail = 0

        fun near(p: Int): Boolean {
            val dr = ((p shr 16) and 0xFF) - bgR
            val dg = ((p shr 8) and 0xFF) - bgG
            val db = (p and 0xFF) - bgB
            return dr * dr + dg * dg + db * db <= thr
        }

        fun push(i: Int) {
            if (i in 0 until w * h && !isBg[i] && near(px[i])) {
                isBg[i] = true
                queue[qTail++] = i
            }
        }

        for (x in 0 until w) { push(x); push((h - 1) * w + x) }
        for (y in 0 until h) { push(y * w); push(y * w + w - 1) }

        while (qHead < qTail) {
            val i = queue[qHead++]
            val x = i % w
            val y = i / w
            if (x > 0) push(i - 1)
            if (x < w - 1) push(i + 1)
            if (y > 0) push(i - w)
            if (y < h - 1) push(i + w)
        }

        // 3) 形态学开运算:去掉前景里的孤立背景噪点
        val cleaned = BooleanArray(w * h)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val i = y * w + x
                var bgNeighbors = 0
                for (dy in -1..1) for (dx in -1..1) {
                    if (isBg[i + dy * w + dx]) bgNeighbors++
                }
                // 9 邻域里 6 个以上是背景才算背景,消除毛刺
                cleaned[i] = if (isBg[i]) bgNeighbors >= 4 else bgNeighbors >= 8
            }
        }
        for (x in 0 until w) { cleaned[x] = isBg[x]; cleaned[(h - 1) * w + x] = isBg[(h - 1) * w + x] }
        for (y in 0 until h) { cleaned[y * w] = isBg[y * w]; cleaned[y * w + w - 1] = isBg[y * w + w - 1] }

        // 4) 边缘羽化:距离前景边界越近,alpha 越渐变,避免锯齿
        val alpha = IntArray(w * h)
        for (i in 0 until w * h) alpha[i] = if (cleaned[i]) 0 else 255

        if (featherPx > 0) {
            val blurred = IntArray(w * h)
            val r = featherPx
            for (y in 0 until h) {
                for (x in 0 until w) {
                    var sum = 0
                    var cnt = 0
                    for (dy in -r..r) {
                        val yy = y + dy
                        if (yy < 0 || yy >= h) continue
                        for (dx in -r..r) {
                            val xx = x + dx
                            if (xx < 0 || xx >= w) continue
                            sum += alpha[yy * w + xx]
                            cnt++
                        }
                    }
                    blurred[y * w + x] = if (cnt == 0) alpha[y * w + x] else sum / cnt
                }
            }
            alpha.indices.forEach { alpha[it] = blurred[it] }
        }

        for (i in 0 until w * h) {
            px[i] = (alpha[i] shl 24) or (px[i] and 0x00FFFFFF)
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(px, 0, w, 0, 0, w, h)
        return out
    }

    /** 透明底 + 纯色背景合成 */
    fun compose(cut: Bitmap, bgColor: Int): Bitmap {
        val out = Bitmap.createBitmap(cut.width, cut.height, Bitmap.Config.ARGB_8888)
        Canvas(out).apply {
            drawColor(bgColor)
            drawBitmap(cut, 0f, 0f, null)
        }
        return out
    }

    private inline fun medianOf(list: List<Int>, channel: (Int) -> Int): Int {
        if (list.isEmpty()) return 255
        val arr = IntArray(list.size) { channel(list[it]) }
        arr.sort()
        return arr[arr.size / 2]
    }
}

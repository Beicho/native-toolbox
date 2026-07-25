package com.toolbox.nativetoolbox.util

import android.content.Context
import com.toolbox.nativetoolbox.net.AstroApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * 按需资源下发。
 *
 * 手机号归属地库(4.3MB)和拼音表(288KB)原来打包在 assets 里,
 * 所有用户都得下,但只有 2 个工具用得到。改成首次使用时从自有服务器拉。
 *
 * 【为什么不用 Google Play 动态分发】
 * 国内大量设备没有 Google 服务,Play 的 on-demand module 直接不可用。
 * 走自己的服务器反而更稳:没有流量限制、国内直连快、不依赖任何第三方。
 */
object AssetProvisioner {

    /** 一个可下发的资源 */
    enum class Asset(
        val fileName: String,
        val displayName: String,
        val approxBytes: Long,
    ) {
        PHONE_DB("phone.dat", "手机号归属地库", 4_484_792L),
        PINYIN("pinyin.tsv", "拼音字库", 293_546L),
    }

    private const val BASE = "http://39.98.89.19:50003/assets"
    private const val DIR = "provisioned"

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun dir(): File =
        File(appContext.filesDir, DIR).apply { if (!exists()) mkdirs() }

    fun localFile(asset: Asset): File = File(dir(), asset.fileName)

    fun isReady(asset: Asset): Boolean {
        val f = localFile(asset)
        // 允许 ±5% 误差,防止服务端小幅更新导致误判
        return f.exists() && f.length() > asset.approxBytes * 0.9
    }

    /**
     * 打开资源流。优先用下发到本地的,其次回退到 assets(兼容还打包着的旧版本)。
     * 两个都没有时返回 null,调用方应提示用户下载。
     */
    fun openStream(asset: Asset): java.io.InputStream? {
        val local = localFile(asset)
        if (local.exists() && local.length() > 1024) {
            return runCatching { local.inputStream() }.getOrNull()
        }
        return runCatching { appContext.assets.open(asset.fileName) }.getOrNull()
    }

    /** 资源是否可用(本地已下发,或还打包在 assets 里) */
    fun isAvailable(asset: Asset): Boolean {
        if (isReady(asset)) return true
        return runCatching {
            appContext.assets.open(asset.fileName).use { true }
        }.getOrDefault(false)
    }

    data class Progress(val downloaded: Long, val total: Long) {
        val percent: Int get() = if (total <= 0) 0 else (downloaded * 100 / total).toInt()
    }

    /**
     * 下载资源。边下边写临时文件,完成后原子改名 ——
     * 中途断网不会留下半个损坏的库文件。
     */
    suspend fun download(
        asset: Asset,
        onProgress: (Progress) -> Unit = {},
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val target = localFile(asset)
            if (isReady(asset)) return@runCatching target

            val tmp = File(dir(), "${asset.fileName}.part")
            val conn = URL("$BASE/${asset.fileName}").openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 30000
            conn.setRequestProperty("X-Astro-Client", "android")
            val code = conn.responseCode
            if (code !in 200..299) {
                conn.disconnect()
                error("服务器返回 $code")
            }
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: asset.approxBytes

            conn.inputStream.use { input ->
                tmp.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        onProgress(Progress(done, total))
                    }
                }
            }
            conn.disconnect()

            if (tmp.length() < asset.approxBytes * 0.5) {
                tmp.delete()
                error("下载不完整,请重试")
            }
            if (target.exists()) target.delete()
            if (!tmp.renameTo(target)) error("保存失败")
            target
        }
    }

    fun delete(asset: Asset): Boolean = localFile(asset).delete()

    /** 已占用空间,给设置页展示 */
    fun usedBytes(): Long = Asset.entries.sumOf { localFile(it).length() }
}

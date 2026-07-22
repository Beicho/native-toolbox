package com.toolbox.nativetoolbox.util

import com.toolbox.nativetoolbox.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val tag: String,
    val versionName: String,
    val notes: String,
    val apkUrl: String,
    val htmlUrl: String
)

/** GitHub Releases 更新检查(零第三方依赖) */
object UpdateChecker {

    private const val API = "https://api.github.com/repos/Beicho/native-toolbox/releases/latest"

    /** 返回比当前新的版本,没有更新返回 null */
    suspend fun check(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(API).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "AstroKit/${BuildConfig.VERSION_NAME}")
            }
            try {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val tag = json.optString("tag_name")
                val remote = tag.removePrefix("v")
                if (remote.isEmpty() || !isNewer(remote, BuildConfig.VERSION_NAME)) {
                    null
                } else {
                    var apkUrl = ""
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val a = assets.getJSONObject(i)
                            if (a.optString("name").endsWith(".apk")) {
                                apkUrl = a.optString("browser_download_url")
                                break
                            }
                        }
                    }
                    UpdateInfo(
                        tag = tag,
                        versionName = remote,
                        notes = json.optString("body").take(600),
                        apkUrl = apkUrl,
                        htmlUrl = json.optString("html_url")
                    )
                }
            } finally {
                conn.disconnect()
            }
        }
    }

    /** 语义化版本比较:remote 是否比 local 新 */
    fun isNewer(remote: String, local: String): Boolean {
        val r = remote.split(".").mapNotNull { it.takeWhile(Char::isDigit).toIntOrNull() }
        val l = local.split(".").mapNotNull { it.takeWhile(Char::isDigit).toIntOrNull() }
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv != lv) return rv > lv
        }
        return false
    }
}

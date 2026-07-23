package com.toolbox.nativetoolbox.net

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Astro 云服务客户端:App 内所有联网功能的唯一出口。
 * 统一 envelope:{ok:true,data:...} / {ok:false,err:"人话"}。
 * 断网/超时自动降级到最近一次成功缓存(结果带 cachedAt 标记,页面须明示"缓存于 xx")。
 */
object AstroApi {

    private const val BASE = "http://39.98.89.19:50003/v1"
    private const val CACHE_DIR = "astro-api"

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** 请求结果:data 为服务端 data 字段;cachedAt > 0 表示这是断网降级的缓存数据 */
    data class ApiResult(val data: JSONObject, val cachedAt: Long = 0L)

    class ApiException(message: String) : Exception(message)

    suspend fun get(path: String, params: Map<String, String> = emptyMap()): Result<ApiResult> =
        request("GET", path, params, body = null)

    suspend fun post(path: String, body: JSONObject): Result<ApiResult> =
        request("POST", path, emptyMap(), body)

    private suspend fun request(
        method: String,
        path: String,
        params: Map<String, String>,
        body: JSONObject?,
    ): Result<ApiResult> = withContext(Dispatchers.IO) {
        val query = if (params.isEmpty()) "" else
            "?" + params.entries.joinToString("&") { "${it.key}=${java.net.URLEncoder.encode(it.value, "UTF-8")}" }
        val url = "$BASE$path$query"
        val cacheKey = sha1(url + (body?.toString() ?: ""))

        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = method
            conn.connectTimeout = 5000
            conn.readTimeout = 30000
            conn.setRequestProperty("X-Astro-Client", "android")
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val code = conn.responseCode
            val text = if (code in 200..299) conn.inputStream.bufferedReader().use { it.readText() }
            else conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()

            val json = runCatching { JSONObject(text) }.getOrNull()
                ?: return@withContext fallback(cacheKey, "服务响应异常")
            if (!json.optBoolean("ok")) {
                return@withContext Result.failure(ApiException(json.optString("err", "服务暂不可用")))
            }
            val data = json.optJSONObject("data") ?: JSONObject()
            if (method == "GET") writeCache(cacheKey, data) // 只缓存幂等 GET
            Result.success(ApiResult(data))
        } catch (e: Exception) {
            fallback(cacheKey, "网络不可用")
        }
    }

    /** 网络失败时读最近成功缓存,没有缓存才真正失败 */
    private fun fallback(cacheKey: String, err: String): Result<ApiResult> {
        val f = cacheFile(cacheKey)
        if (f.exists()) {
            runCatching {
                val obj = JSONObject(f.readText())
                return Result.success(
                    ApiResult(obj.getJSONObject("data"), obj.optLong("at"))
                )
            }
        }
        return Result.failure(ApiException(err))
    }

    private fun writeCache(key: String, data: JSONObject) {
        runCatching {
            cacheFile(key).writeText(
                JSONObject().put("at", System.currentTimeMillis()).put("data", data).toString()
            )
        }
    }

    private fun cacheFile(key: String): File {
        val dir = File(appContext.cacheDir, CACHE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$key.json")
    }

    private fun sha1(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }
}

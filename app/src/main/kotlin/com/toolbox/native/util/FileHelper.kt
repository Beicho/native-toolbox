package com.toolbox.native.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.OutputStream

object FileHelper {

    private const val APP_FOLDER = "综合工具包"

    /**
     * 保存文件到公共下载目录（Android 10+ 使用 MediaStore）
     */
    fun saveToDownloads(
        context: Context,
        fileName: String,
        content: ByteArray
    ): Result<String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(context, fileName, content)
            } else {
                saveViaFile(context, fileName, content)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Android 10+ MediaStore API
     */
    private fun saveViaMediaStore(
        context: Context,
        fileName: String,
        content: ByteArray
    ): Result<String> {
        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$APP_FOLDER")
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: return Result.failure(Exception("无法创建文件"))

        resolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(content)
            outputStream.flush()
        } ?: return Result.failure(Exception("无法写入文件"))

        val path = "${Environment.DIRECTORY_DOWNLOADS}/$APP_FOLDER/$fileName"
        return Result.success(path)
    }

    /**
     * Android 9 及以下直接文件操作
     */
    private fun saveViaFile(
        context: Context,
        fileName: String,
        content: ByteArray
    ): Result<String> {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val appDir = File(downloadDir, APP_FOLDER)

        if (!appDir.exists()) {
            appDir.mkdirs()
        }

        val file = File(appDir, fileName)
        file.writeBytes(content)

        return Result.success(file.absolutePath)
    }

    /**
     * 保存到应用内部存储（降级方案）
     */
    fun saveToInternalStorage(
        context: Context,
        fileName: String,
        content: ByteArray
    ): Result<String> {
        return try {
            val appDir = File(context.filesDir, APP_FOLDER)
            if (!appDir.exists()) {
                appDir.mkdirs()
            }

            val file = File(appDir, fileName)
            file.writeBytes(content)

            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 读取 URI 内容
     */
    fun readFromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取文件大小
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                it.statSize
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 格式化文件大小
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}

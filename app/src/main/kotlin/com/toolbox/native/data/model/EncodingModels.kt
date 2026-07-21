package com.toolbox.native.data.model

data class EncodingFile(
    val uri: String,
    val name: String,
    val size: Long,
    val detectedEncoding: String = "未知",
    val targetEncoding: String = "UTF-8",
    val status: FileStatus = FileStatus.PENDING
)

enum class FileStatus {
    PENDING,
    CONVERTING,
    SUCCESS,
    ERROR
}

enum class EncodingType(val displayName: String, val charset: String) {
    UTF8("UTF-8", "UTF-8"),
    UTF8_BOM("UTF-8 BOM", "UTF-8"),
    GBK("GBK", "GBK"),
    GB2312("GB2312", "GB2312"),
    GB18030("GB18030", "GB18030"),
    UTF16_LE("UTF-16 LE", "UTF-16LE"),
    UTF16_BE("UTF-16 BE", "UTF-16BE"),
    BIG5("BIG5", "Big5"),
    SHIFT_JIS("Shift-JIS", "Shift_JIS"),
    ISO_8859_1("ISO-8859-1", "ISO-8859-1");

    companion object {
        fun fromCharset(charset: String): EncodingType? {
            return entries.find { it.charset.equals(charset, ignoreCase = true) }
        }
    }
}

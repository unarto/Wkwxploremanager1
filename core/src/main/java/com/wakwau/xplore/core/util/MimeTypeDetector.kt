package com.wakwau.xplore.core.util

import java.net.URLConnection
import java.util.Locale

enum class FileCategory {
    FOLDER,
    ARCHIVE,
    APK,
    TEXT,
    CODE,
    IMAGE,
    AUDIO,
    VIDEO,
    DOCUMENT,
    DATABASE,
    UNKNOWN
}

object MimeTypeDetector {
    private val commonMimes = mapOf(
        "txt" to "text/plain",
        "html" to "text/html",
        "htm" to "text/html",
        "css" to "text/css",
        "js" to "application/javascript",
        "json" to "application/json",
        "xml" to "application/xml",
        "png" to "image/png",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "gif" to "image/gif",
        "webp" to "image/webp",
        "svg" to "image/svg+xml",
        "mp3" to "audio/mpeg",
        "wav" to "audio/wav",
        "ogg" to "audio/ogg",
        "mp4" to "video/mp4",
        "mkv" to "video/x-matroska",
        "pdf" to "application/pdf",
        "zip" to "application/zip",
        "tar" to "application/x-tar",
        "gz" to "application/gzip",
        "7z" to "application/x-7z-compressed",
        "rar" to "application/vnd.rar",
        "apk" to "application/vnd.android.package-archive",
        "kt" to "text/x-kotlin",
        "kts" to "text/x-kotlin",
        "java" to "text/x-java-source",
        "md" to "text/markdown",
        "gradle" to "text/x-gradle",
        "properties" to "text/plain",
        "env" to "text/plain",
        "log" to "text/plain"
    )

    fun getMimeType(fileName: String): String {
        val extension = getExtension(fileName).lowercase(Locale.ROOT)
        if (extension.isEmpty()) return "application/octet-stream"

        val directMatch = commonMimes[extension]
        if (directMatch != null) return directMatch

        return try {
            val guess = URLConnection.guessContentTypeFromName(fileName)
            guess ?: "application/octet-stream"
        } catch (e: Throwable) {
            "application/octet-stream"
        }
    }

    fun getExtension(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf('.')
        return if (lastDotIndex > 0 && lastDotIndex < fileName.length - 1) {
            fileName.substring(lastDotIndex + 1).lowercase(Locale.ROOT)
        } else {
            ""
        }
    }

    fun getCategory(fileName: String, isDirectory: Boolean): FileCategory {
        if (isDirectory) return FileCategory.FOLDER
        val ext = getExtension(fileName)
        return when (ext) {
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso" -> FileCategory.ARCHIVE
            "apk", "aab", "xapk", "apks" -> FileCategory.APK
            "txt", "md", "log", "cfg", "ini", "properties", "env", "csv", "tsv" -> FileCategory.TEXT
            "kt", "kts", "java", "c", "cpp", "h", "cs", "py", "js", "ts", "jsx", "tsx", "html", "htm", "css", "json", "xml", "yaml", "yml", "gradle", "sh", "bat", "sql" -> FileCategory.CODE
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "ico", "heic" -> FileCategory.IMAGE
            "mp3", "wav", "flac", "ogg", "m4a", "aac", "wma", "opus", "mid" -> FileCategory.AUDIO
            "mp4", "mkv", "avi", "mov", "webm", "wmv", "3gp", "flv" -> FileCategory.VIDEO
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp" -> FileCategory.DOCUMENT
            "db", "sqlite", "sqlite3", "realm" -> FileCategory.DATABASE
            else -> FileCategory.UNKNOWN
        }
    }

    fun isTextOrCode(fileName: String): Boolean {
        val cat = getCategory(fileName, false)
        return cat == FileCategory.TEXT || cat == FileCategory.CODE
    }
}

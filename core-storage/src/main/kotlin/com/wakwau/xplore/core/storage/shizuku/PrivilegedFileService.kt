package com.wakwau.xplore.core.storage.shizuku

import android.os.Bundle
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.system.exitProcess
import android.system.Os
import android.system.OsConstants

class PrivilegedFileService : IPrivilegedFileService.Stub() {

    private fun resolvePhysicalPath(path: String): String {
        return path.replaceFirst("^/storage/emulated/([0-9]+)".toRegex(), "/data/media/$1")
            .replaceFirst("^/sdcard".toRegex(), "/data/media/0")
    }

    override fun destroy() {
        exitProcess(0)
    }

    override fun listDirectory(path: String): List<Bundle> {
        val physicalPath = resolvePhysicalPath(path)
        val dir = File(physicalPath)
        val files = dir.listFiles() ?: throw IOException("Failed to list directory (Permission denied): $path")
        
        return files.map { file ->
            Bundle().apply {
                putString(ShizukuIpcConstants.KEY_NAME, file.name)
                putString(ShizukuIpcConstants.KEY_PATH, path.trimEnd('/') + "/" + file.name)
                putLong(ShizukuIpcConstants.KEY_SIZE, file.length())
                putLong(ShizukuIpcConstants.KEY_LAST_MODIFIED, file.lastModified())
                putBoolean(ShizukuIpcConstants.KEY_IS_DIRECTORY, file.isDirectory)
                putBoolean(ShizukuIpcConstants.KEY_IS_HIDDEN, file.isHidden)
            }
        }
    }

    override fun exists(path: String): Boolean {
        val physicalPath = resolvePhysicalPath(path)
        return try {
            Os.access(physicalPath, OsConstants.F_OK)
            true
        } catch (e: Exception) {
            File(physicalPath).exists()
        }
    }

    override fun delete(path: String): Boolean {
        val physicalPath = resolvePhysicalPath(path)
        val file = File(physicalPath)
        if (!file.exists()) return false

        val pathClean = path.trim().trimEnd('/')
        if (pathClean.isEmpty() || pathClean == "/" || pathClean.equals("/storage", ignoreCase = true) || pathClean.equals("/storage/emulated", ignoreCase = true) || pathClean.equals("/system", ignoreCase = true)) {
            throw IllegalStateException("Cannot delete system or root directory: $path")
        }

        val isSymlink = java.nio.file.Files.isSymbolicLink(file.toPath())
        if (isSymlink) {
            if (!file.delete() && file.exists()) throw IllegalStateException("Failed to delete symlink: $path")
        } else if (file.isDirectory) {
            deleteDirectoryRecursivelySafe(file)
        } else {
            if (!file.delete() && file.exists()) throw IllegalStateException("Failed to delete file: $path")
        }
        return true
    }

    private fun deleteDirectoryRecursivelySafe(dir: File): Boolean {
        val stack = ArrayDeque<File>()
        stack.addLast(dir)
        val filesToDelete = ArrayDeque<File>()

        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            filesToDelete.addFirst(current)

            val isSymlink = java.nio.file.Files.isSymbolicLink(current.toPath())
            if (!isSymlink && current.isDirectory) {
                val children = current.listFiles() ?: continue
                for (child in children) {
                    stack.addLast(child)
                }
            }
        }

        for (file in filesToDelete) {
            if (!file.delete() && file.exists()) {
                throw IllegalStateException("Failed to delete: ${file.absolutePath}")
            }
        }
        return true
    }

    override fun rename(sourcePath: String, destPath: String): Boolean {
        val src = File(resolvePhysicalPath(sourcePath))
        val dest = File(resolvePhysicalPath(destPath))
        return src.renameTo(dest) || throw IOException("Failed to rename $sourcePath to $destPath")
    }

    override fun createDirectory(path: String): Boolean {
        val file = File(resolvePhysicalPath(path))
        return file.mkdirs() || throw IOException("Failed to create directory: $path")
    }

    override fun length(path: String): Long = File(resolvePhysicalPath(path)).length()

    override fun lastModified(path: String): Long = File(resolvePhysicalPath(path)).lastModified()

    override fun isDirectory(path: String): Boolean = File(resolvePhysicalPath(path)).isDirectory

    override fun openFileForRead(path: String): android.os.ParcelFileDescriptor? {
        val file = File(resolvePhysicalPath(path))
        if (!file.exists()) throw FileNotFoundException("File not found for read: $path")
        return android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun openFileForWrite(path: String): android.os.ParcelFileDescriptor? {
        val file = File(resolvePhysicalPath(path))
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            if (!file.createNewFile()) throw IOException("Failed to create file for write: $path")
        }
        return android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_WRITE_ONLY or android.os.ParcelFileDescriptor.MODE_TRUNCATE)
    }
}

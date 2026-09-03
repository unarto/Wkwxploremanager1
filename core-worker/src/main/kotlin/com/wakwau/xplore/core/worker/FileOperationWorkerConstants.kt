// [Jalur Class/Modul]: core-worker/src/main/kotlin/com/wakwau/xplore/core/worker/FileOperationWorkerConstants.kt
// [Penjelasan]: Objek konstanta terpusat untuk kunci serialisasi payload data WorkManager, nama unique work, konfigurasi saluran notifikasi, dan ID notifikasi foreground service operasi berkas.
package com.wakwau.xplore.core.worker

object FileOperationWorkerConstants {
    const val KEY_OPERATION_TYPE = "OPERATION_TYPE"
    const val KEY_SOURCES = "SOURCES"
    const val KEY_DESTINATION = "DESTINATION"
    const val KEY_RESOLVED_ITEMS = "RESOLVED_ITEMS"
    const val KEY_PATH = "path"
    const val KEY_ROOT_ID = "rootId"
    const val KEY_ORIGINAL_NAME = "originalName"
    const val KEY_TARGET_NAME = "targetName"
    const val KEY_DEST_DIR_PATH = "destDirPath"
    const val KEY_DEST_DIR_ROOT_ID = "destDirRootId"
    const val KEY_TARGET_PATH = "targetPath"
    const val KEY_TARGET_ROOT_ID = "targetRootId"
    const val KEY_IS_DIRECTORY = "isDirectory"
    const val KEY_CHOICE = "choice"

    const val UNIQUE_WORK_NAME = "file_operation_unique_work"
    const val CHANNEL_ID = "file_operation_channel"
    const val NOTIFICATION_ID = 1001
}


// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/shizuku/ShizukuIpcConstants.kt
// [Penjelasan]: Konstanta terpusat untuk bundle keys IPC Shizuku, process name suffix, dan timeout/retry connection.
package com.wakwau.xplore.core.storage.shizuku

internal object ShizukuIpcConstants {
    const val PROCESS_NAME_SUFFIX = "privileged_storage"
    
    // Timeouts and Limits
    const val BIND_TIMEOUT_MS = 5000L
    const val RECONNECT_DELAY_MS = 1000L
    const val MAX_RECONNECT_ATTEMPTS = 3
    
    // Bundle Keys
    const val KEY_NAME = "name"
    const val KEY_PATH = "path"
    const val KEY_SIZE = "size"
    const val KEY_LAST_MODIFIED = "lastModified"
    const val KEY_IS_DIRECTORY = "isDirectory"
    const val KEY_IS_HIDDEN = "isHidden"
}

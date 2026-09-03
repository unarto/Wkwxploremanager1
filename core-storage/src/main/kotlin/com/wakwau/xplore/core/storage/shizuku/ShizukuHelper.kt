// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/shizuku/ShizukuHelper.kt
// [Penjelasan]: Kelas pembantu untuk berinteraksi dengan Shizuku, yang kini mendelegasikan state lifecycle ke ShizukuConnectionManager untuk menghindari God Class.
package com.wakwau.xplore.core.storage.shizuku

object ShizukuHelper {
    @Volatile
    private var connectionManager: ShizukuConnectionManager? = null

    suspend fun getPrivilegedService(packageName: String): IPrivilegedFileService? {
        val manager = connectionManager ?: synchronized(this) {
            connectionManager ?: ShizukuConnectionManager(packageName).also {
                connectionManager = it
            }
        }
        return manager.getServiceWithRetry()
    }
}


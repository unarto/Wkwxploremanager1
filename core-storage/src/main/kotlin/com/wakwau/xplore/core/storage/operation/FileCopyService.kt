// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/operation/FileCopyService.kt
// [Penjelasan]: Background Service untuk mengeksekusi I/O (Copy, Move, Delete) di luar scope ViewModel.
package com.wakwau.xplore.core.storage.operation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.wakwau.xplore.core.storage.conflict.ConflictChoice
import com.wakwau.xplore.core.storage.conflict.ResolvedTransferItem
import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.operation.FileOperationError
import com.wakwau.xplore.core.storage.model.StorageLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class FileCopyService : Service() {
    
    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_CANCEL = "ACTION_CANCEL"
        const val KEY_OPERATION_TYPE = "KEY_OPERATION_TYPE"
        const val KEY_SOURCES = "KEY_SOURCES"
        const val KEY_DESTINATION = "KEY_DESTINATION"
        const val KEY_RESOLVED_ITEMS = "KEY_RESOLVED_ITEMS"
        const val KEY_PATH = "KEY_PATH"
        const val KEY_ROOT_ID = "KEY_ROOT_ID"
        const val KEY_DEST_DIR_PATH = "KEY_DEST_DIR_PATH"
        const val KEY_DEST_DIR_ROOT_ID = "KEY_DEST_DIR_ROOT_ID"
        const val KEY_TARGET_PATH = "KEY_TARGET_PATH"
        const val KEY_TARGET_ROOT_ID = "KEY_TARGET_ROOT_ID"
        const val KEY_ORIGINAL_NAME = "KEY_ORIGINAL_NAME"
        const val KEY_TARGET_NAME = "KEY_TARGET_NAME"
        const val KEY_IS_DIRECTORY = "KEY_IS_DIRECTORY"
        const val KEY_CHOICE = "KEY_CHOICE"
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var currentOperationJob: Job? = null
    
    private val notificationId = 1234
    private val channelId = "file_operation_channel"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val typeStr = intent.getStringExtra(KEY_OPERATION_TYPE) ?: return START_NOT_STICKY
                val type = BackgroundOperationType.valueOf(typeStr)
                
                val sourcesJson = intent.getStringExtra(KEY_SOURCES)
                val destJson = intent.getStringExtra(KEY_DESTINATION)
                val resolvedJson = intent.getStringExtra(KEY_RESOLVED_ITEMS)
                
                // Fallback name if context strings are missing
                val opName = "Memproses Berkas..."
                
                val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ForegroundInfo(notificationId, createNotification(opName, 0, 100), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC).notification
                } else {
                    createNotification(opName, 0, 100)
                }
                
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                    } else {
                        startForeground(notificationId, notification)
                    }
                } catch (e: Exception) {
                    // Ignore foreground start exception
                }
                
                if (resolvedJson != null) {
                    val resolvedItems = parseResolvedItems(resolvedJson)
                    startResolvedOperation(type, resolvedItems)
                } else if (sourcesJson != null) {
                    val sources = parseStorageLocations(sourcesJson)
                    val destination = destJson?.let { parseStorageLocation(JSONObject(it)) }
                    startOperation(type, sources, destination)
                }
            }
            ACTION_CANCEL -> {
                currentOperationJob?.cancel()
                serviceScope.launch {
                    FileCopyServiceLocator.emitProgress(FileOperationResult.Cancelled)
                    stopForeground(true)
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    // A dummy wrapper for older SDKs compatibility inside service scope
    private class ForegroundInfo(val id: Int, val notification: android.app.Notification, val type: Int)

    private fun startOperation(type: BackgroundOperationType, sources: List<StorageLocation>, destination: StorageLocation?) {
        currentOperationJob?.cancel()
        currentOperationJob = serviceScope.launch {
            val fileRepository = FileCopyServiceLocator.fileRepository
            
            if (fileRepository == null) {
                FileCopyServiceLocator.emitProgress(FileOperationResult.Failure(FileOperationError.UNKNOWN))
                stopSelf()
                return@launch
            }

            try {
                var isFailedOrCancelled = false
                when (type) {
                    BackgroundOperationType.COPY -> {
                        if (destination == null) {
                            FileCopyServiceLocator.emitProgress(FileOperationResult.Failure(FileOperationError.INVALID_LOCATION))
                            stopSelf()
                            return@launch
                        }
                        for (source in sources) {
                            if (!currentCoroutineContext().isActive) {
                                FileCopyServiceLocator.emitProgress(FileOperationResult.Cancelled)
                                break
                            }
                            val destLoc = createTargetLocation(source, destination)
                            fileRepository.copy(source, destLoc).collect { result ->
                                handleProgress(result)
                                if (result is FileOperationResult.Failure || result is FileOperationResult.Cancelled) {
                                    isFailedOrCancelled = true
                                }
                            }
                            if (isFailedOrCancelled) break
                        }
                    }
                    BackgroundOperationType.MOVE -> {
                        if (destination == null) {
                            FileCopyServiceLocator.emitProgress(FileOperationResult.Failure(FileOperationError.INVALID_LOCATION))
                            stopSelf()
                            return@launch
                        }
                        for (source in sources) {
                            if (!currentCoroutineContext().isActive) {
                                FileCopyServiceLocator.emitProgress(FileOperationResult.Cancelled)
                                break
                            }
                            val destLoc = createTargetLocation(source, destination)
                            fileRepository.move(source, destLoc).collect { result ->
                                handleProgress(result)
                                if (result is FileOperationResult.Failure || result is FileOperationResult.Cancelled) {
                                    isFailedOrCancelled = true
                                }
                            }
                            if (isFailedOrCancelled) break
                        }
                    }
                    BackgroundOperationType.DELETE -> {
                        val totalCount = sources.size.toLong()
                        var deletedCount = 0L
                        for (source in sources) {
                            if (!currentCoroutineContext().isActive) {
                                FileCopyServiceLocator.emitProgress(FileOperationResult.Cancelled)
                                break
                            }
                            when (val result = fileRepository.delete(source)) {
                                is FileOperationResult.Failure -> {
                                    FileCopyServiceLocator.emitProgress(FileOperationResult.Failure(result.error))
                                    isFailedOrCancelled = true
                                    break
                                }
                                is FileOperationResult.Cancelled -> {
                                    FileCopyServiceLocator.emitProgress(FileOperationResult.Cancelled)
                                    isFailedOrCancelled = true
                                    break
                                }
                                else -> {
                                    deletedCount++
                                    val fileName = source.path.trimEnd('/').substringAfterLast('/')
                                    handleProgress(FileOperationResult.Success(FileOperationProgress(deletedCount, totalCount, fileName)))
                                }
                            }
                        }
                    }
                }
            } finally {
                stopForeground(true)
                stopSelf()
            }
        }
    }

    private fun startResolvedOperation(type: BackgroundOperationType, resolvedItems: List<ResolvedTransferItem>) {
        currentOperationJob?.cancel()
        currentOperationJob = serviceScope.launch {
            val fileRepository = FileCopyServiceLocator.fileRepository
            
            if (fileRepository == null) {
                FileCopyServiceLocator.emitProgress(FileOperationResult.Failure(FileOperationError.UNKNOWN))
                stopSelf()
                return@launch
            }

            try {
                var isFailedOrCancelled = false
                for (item in resolvedItems) {
                    if (!currentCoroutineContext().isActive) {
                        FileCopyServiceLocator.emitProgress(FileOperationResult.Cancelled)
                        break
                    }
                    if (item.choice == ConflictChoice.SKIP) continue
                    
                    val flow = if (type == BackgroundOperationType.COPY) {
                        fileRepository.copy(item.source, item.targetLocation)
                    } else {
                        fileRepository.move(item.source, item.targetLocation)
                    }
                    
                    flow.collect { result ->
                        handleProgress(result)
                        if (result is FileOperationResult.Failure || result is FileOperationResult.Cancelled) {
                            isFailedOrCancelled = true
                        }
                    }
                    if (isFailedOrCancelled) break
                }
            } finally {
                stopForeground(true)
                stopSelf()
            }
        }
    }

    private var lastProgressUpdateTime = 0L
    private suspend fun handleProgress(result: FileOperationResult<FileOperationProgress>) {
        if (result is FileOperationResult.Success) {
            val currentTime = System.currentTimeMillis()
            val p = result.data
            val isComplete = p.bytesWritten >= p.totalBytes
            
            if (isComplete || currentTime - lastProgressUpdateTime > 200) {
                lastProgressUpdateTime = currentTime
                FileCopyServiceLocator.emitProgress(result)
                val progressPercentage = if (p.totalBytes > 0) ((p.bytesWritten.toFloat() / p.totalBytes) * 100).toInt() else 0
                
                val notification = createNotification(p.fileName, progressPercentage, 100)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(notificationId, notification)
            }
        } else {
            FileCopyServiceLocator.emitProgress(result)
        }
    }

    private fun createTargetLocation(source: StorageLocation, destination: StorageLocation): StorageLocation {
        return if (destination.path.startsWith(StorageConstants.CONTENT_SCHEME_PREFIX)) {
            destination
        } else {
            val sourceName = source.path.trimEnd('/').substringAfterLast('/')
            val cleanDestPath = if (destination.path.endsWith("/")) {
                "${destination.path}$sourceName"
            } else {
                "${destination.path}/$sourceName"
            }
            StorageLocation(path = cleanDestPath, rootId = destination.rootId)
        }
    }

    private fun parseResolvedItems(jsonStr: String): List<ResolvedTransferItem> {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<ResolvedTransferItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val source = StorageLocation(
                path = obj.getString(KEY_PATH),
                rootId = obj.getString(KEY_ROOT_ID)
            )
            val destDir = StorageLocation(
                path = obj.getString(KEY_DEST_DIR_PATH),
                rootId = obj.getString(KEY_DEST_DIR_ROOT_ID)
            )
            val target = StorageLocation(
                path = obj.getString(KEY_TARGET_PATH),
                rootId = obj.getString(KEY_TARGET_ROOT_ID)
            )
            val origName = obj.getString(KEY_ORIGINAL_NAME)
            val targetName = obj.getString(KEY_TARGET_NAME)
            val isDir = obj.getBoolean(KEY_IS_DIRECTORY)
            val choice = ConflictChoice.valueOf(obj.getString(KEY_CHOICE))
            list.add(
                ResolvedTransferItem(
                    source = source,
                    destinationDir = destDir,
                    targetLocation = target,
                    originalName = origName,
                    targetName = targetName,
                    isDirectory = isDir,
                    choice = choice
                )
            )
        }
        return list
    }

    private fun parseStorageLocations(jsonStr: String): List<StorageLocation> {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<StorageLocation>()
        for (i in 0 until array.length()) {
            list.add(parseStorageLocation(array.getJSONObject(i)))
        }
        return list
    }

    private fun parseStorageLocation(obj: JSONObject): StorageLocation {
        return StorageLocation(
            path = obj.getString(KEY_PATH),
            rootId = obj.getString(KEY_ROOT_ID)
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = getString(com.wakwau.xplore.storage.R.string.notification_channel_name)
            val channelDesc = getString(com.wakwau.xplore.storage.R.string.notification_channel_desc)
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = channelDesc
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String, progress: Int, max: Int): android.app.Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(com.wakwau.xplore.storage.R.string.notification_title_processing))
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(max, progress, false)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

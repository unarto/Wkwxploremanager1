// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/operation/FileCopyServiceManager.kt
// [Penjelasan]: Implementasi BackgroundOperationManager mendelegasikan I/O ke FileCopyService.
package com.wakwau.xplore.core.storage.operation

import android.content.Context
import android.content.Intent
import com.wakwau.xplore.core.storage.conflict.ResolvedTransferItem
import com.wakwau.xplore.core.storage.model.StorageLocation
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.content.ContextCompat

class FileCopyServiceManager(
    private val context: Context
) : BackgroundOperationManager {

    override fun enqueueOperation(
        type: BackgroundOperationType,
        sources: List<StorageLocation>,
        destination: StorageLocation?
    ) {
        val sourcesJson = JSONArray()
        sources.forEach { 
            val obj = JSONObject()
            obj.put(FileCopyService.KEY_PATH, it.path)
            obj.put(FileCopyService.KEY_ROOT_ID, it.rootId)
            sourcesJson.put(obj)
        }

        val intent = Intent(context, FileCopyService::class.java).apply {
            action = FileCopyService.ACTION_START
            putExtra(FileCopyService.KEY_OPERATION_TYPE, type.name)
            putExtra(FileCopyService.KEY_SOURCES, sourcesJson.toString())
            
            if (destination != null) {
                val destObj = JSONObject()
                destObj.put(FileCopyService.KEY_PATH, destination.path)
                destObj.put(FileCopyService.KEY_ROOT_ID, destination.rootId)
                putExtra(FileCopyService.KEY_DESTINATION, destObj.toString())
            }
        }
        
        ContextCompat.startForegroundService(context, intent)
    }

    override fun enqueueResolvedOperation(
        type: BackgroundOperationType,
        resolvedItems: List<ResolvedTransferItem>
    ) {
        val resolvedArray = JSONArray()
        resolvedItems.forEach { item ->
            val obj = JSONObject()
            obj.put(FileCopyService.KEY_PATH, item.source.path)
            obj.put(FileCopyService.KEY_ROOT_ID, item.source.rootId)
            obj.put(FileCopyService.KEY_DEST_DIR_PATH, item.destinationDir.path)
            obj.put(FileCopyService.KEY_DEST_DIR_ROOT_ID, item.destinationDir.rootId)
            obj.put(FileCopyService.KEY_TARGET_PATH, item.targetLocation.path)
            obj.put(FileCopyService.KEY_TARGET_ROOT_ID, item.targetLocation.rootId)
            obj.put(FileCopyService.KEY_ORIGINAL_NAME, item.originalName)
            obj.put(FileCopyService.KEY_TARGET_NAME, item.targetName)
            obj.put(FileCopyService.KEY_IS_DIRECTORY, item.isDirectory)
            obj.put(FileCopyService.KEY_CHOICE, item.choice.name)
            resolvedArray.put(obj)
        }

        val intent = Intent(context, FileCopyService::class.java).apply {
            action = FileCopyService.ACTION_START
            putExtra(FileCopyService.KEY_OPERATION_TYPE, type.name)
            putExtra(FileCopyService.KEY_RESOLVED_ITEMS, resolvedArray.toString())
        }

        ContextCompat.startForegroundService(context, intent)
    }

    override fun cancelOperation() {
        val intent = Intent(context, FileCopyService::class.java).apply {
            action = FileCopyService.ACTION_CANCEL
        }
        context.startService(intent)
    }

    override fun observeProgress(): Flow<FileOperationResult<FileOperationProgress>> {
        return FileCopyServiceLocator.progressFlow
    }
}

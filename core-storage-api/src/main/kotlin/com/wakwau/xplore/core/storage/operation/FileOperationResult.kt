package com.wakwau.xplore.core.storage.operation

sealed class FileOperationResult<out T> {
    data class Success<out T>(val data: T) : FileOperationResult<T>()
    data class Failure(val error: FileOperationError) : FileOperationResult<Nothing>()
    object Cancelled : FileOperationResult<Nothing>()
    data class Completed(val operationType: BackgroundOperationType) : FileOperationResult<Nothing>()
}

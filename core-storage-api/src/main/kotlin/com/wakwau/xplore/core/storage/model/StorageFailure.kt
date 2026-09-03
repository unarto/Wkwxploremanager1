package com.wakwau.xplore.core.storage.model

sealed class StorageFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class PermissionDenied(message: String = "Storage permission denied") : StorageFailure(message)
    class InsufficientSpace(message: String = "Insufficient storage space") : StorageFailure(message)
    class FileNotFound(message: String = "File or directory not found") : StorageFailure(message)
    class AccessRestricted(message: String = "Storage access is restricted") : StorageFailure(message)
    class UnknownError(message: String = "Unknown storage error", cause: Throwable? = null) : StorageFailure(message, cause)
}

import sys

file_path = 'core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/bridge/CrossFilesystemTransferBridge.kt'
with open(file_path, 'r') as f:
    content = f.read()

target_str = """    fun copyCross(
        source: StorageLocation,
        destination: StorageLocation,
        sourceType: StorageBackendType,
        destType: StorageBackendType
    ): Flow<FileOperationProgress> = flow {
        val totalBytes = calculateCrossSize(source, sourceType)"""

replacement_str = """    fun copyCross(
        source: StorageLocation,
        destination: StorageLocation,
        sourceType: StorageBackendType,
        destType: StorageBackendType
    ): Flow<FileOperationProgress> = flow {
        emit(FileOperationProgress(0L, 0L, source.path.trimEnd('/').substringAfterLast('/')))
        val totalBytes = calculateCrossSize(source, sourceType)"""

content = content.replace(target_str, replacement_str)

target_str2 = """    fun moveCross(
        source: StorageLocation,
        destination: StorageLocation,
        sourceType: StorageBackendType,
        destType: StorageBackendType
    ): Flow<FileOperationProgress> = flow {
        val totalBytes = calculateCrossSize(source, sourceType)"""

replacement_str2 = """    fun moveCross(
        source: StorageLocation,
        destination: StorageLocation,
        sourceType: StorageBackendType,
        destType: StorageBackendType
    ): Flow<FileOperationProgress> = flow {
        emit(FileOperationProgress(0L, 0L, source.path.trimEnd('/').substringAfterLast('/')))
        val totalBytes = calculateCrossSize(source, sourceType)"""

content = content.replace(target_str2, replacement_str2)

with open(file_path, 'w') as f:
    f.write(content)


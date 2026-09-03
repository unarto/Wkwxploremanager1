import sys

file_path = 'core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/shizuku/SafShizukuFileSystem.kt'
with open(file_path, 'r') as f:
    content = f.read()

target_str = """    override fun copy(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> = flow {
        if (!Shizuku.pingBinder()) {
            throw IOException("Shizuku is not running")
        }
        val isSourceDir = transferHandler.isDirectory(service, source.path)
        val totalBytes = transferHandler.calculateTotalSize(service, source.path)"""

replacement_str = """    override fun copy(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> = flow {
        if (!Shizuku.pingBinder()) {
            throw IOException("Shizuku is not running")
        }
        emit(FileOperationProgress(0L, 0L, source.path.trimEnd('/').substringAfterLast('/')))
        val isSourceDir = transferHandler.isDirectory(service, source.path)
        val totalBytes = transferHandler.calculateTotalSize(service, source.path)"""

content = content.replace(target_str, replacement_str)

target_str2 = """    override fun move(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> = flow {
        if (!Shizuku.pingBinder()) {
            throw IOException("Shizuku is not running")
        }
        val isSourceDir = transferHandler.isDirectory(service, source.path)
        val totalBytes = transferHandler.calculateTotalSize(service, source.path)"""

replacement_str2 = """    override fun move(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> = flow {
        if (!Shizuku.pingBinder()) {
            throw IOException("Shizuku is not running")
        }
        emit(FileOperationProgress(0L, 0L, source.path.trimEnd('/').substringAfterLast('/')))
        val isSourceDir = transferHandler.isDirectory(service, source.path)
        val totalBytes = transferHandler.calculateTotalSize(service, source.path)"""

content = content.replace(target_str2, replacement_str2)

with open(file_path, 'w') as f:
    f.write(content)


import sys

file_path = 'core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/saf/SafFileSystem.kt'
with open(file_path, 'r') as f:
    content = f.read()

target_str = """    override fun copy(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> = flow {
        val sourceDoc = SafUtils.getDocumentFileFromUri(context, source.path)
            ?: throw FileNotFoundException("Source not found: ${source.path}")
        
        val destDoc = SafUtils.getDocumentFileFromUri(context, destination.path)
            ?: throw FileNotFoundException("Destination not found: ${destination.path}")
            
        if (!destDoc.isDirectory) {
            throw IllegalArgumentException("Destination must be a directory")
        }

        val isSourceDir = sourceDoc.isDirectory
        var totalCopied = 0L
        val totalBytes = if (isSourceDir) calculateTotalSize(sourceDoc) else sourceDoc.length()"""

replacement_str = """    override fun copy(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> = flow {
        val sourceDoc = SafUtils.getDocumentFileFromUri(context, source.path)
            ?: throw FileNotFoundException("Source not found: ${source.path}")
        
        val destDoc = SafUtils.getDocumentFileFromUri(context, destination.path)
            ?: throw FileNotFoundException("Destination not found: ${destination.path}")
            
        if (!destDoc.isDirectory) {
            throw IllegalArgumentException("Destination must be a directory")
        }

        emit(FileOperationProgress(0L, 0L, sourceDoc.name ?: "Unknown"))

        val isSourceDir = sourceDoc.isDirectory
        var totalCopied = 0L
        val totalBytes = if (isSourceDir) calculateTotalSize(sourceDoc) else sourceDoc.length()"""

content = content.replace(target_str, replacement_str)

target_str2 = """    override fun move(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> = flow {
        val sourceDoc = SafUtils.getDocumentFileFromUri(context, source.path)
            ?: throw FileNotFoundException("Source not found: ${source.path}")
            
        val destDoc = SafUtils.getDocumentFileFromUri(context, destination.path)
            ?: throw FileNotFoundException("Destination not found: ${destination.path}")
            
        if (!destDoc.isDirectory) {
            throw IllegalArgumentException("Destination must be a directory")
        }

        val isSourceDir = sourceDoc.isDirectory
        var totalCopied = 0L
        val totalBytes = if (isSourceDir) calculateTotalSize(sourceDoc) else sourceDoc.length()"""

replacement_str2 = """    override fun move(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> = flow {
        val sourceDoc = SafUtils.getDocumentFileFromUri(context, source.path)
            ?: throw FileNotFoundException("Source not found: ${source.path}")
            
        val destDoc = SafUtils.getDocumentFileFromUri(context, destination.path)
            ?: throw FileNotFoundException("Destination not found: ${destination.path}")
            
        if (!destDoc.isDirectory) {
            throw IllegalArgumentException("Destination must be a directory")
        }

        emit(FileOperationProgress(0L, 0L, sourceDoc.name ?: "Unknown"))

        val isSourceDir = sourceDoc.isDirectory
        var totalCopied = 0L
        val totalBytes = if (isSourceDir) calculateTotalSize(sourceDoc) else sourceDoc.length()"""

content = content.replace(target_str2, replacement_str2)

with open(file_path, 'w') as f:
    f.write(content)


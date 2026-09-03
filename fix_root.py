import sys

file_path = 'core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/root/RootFileSystem.kt'
with open(file_path, 'r') as f:
    content = f.read()

target_str = """    override fun copy(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> = flow {
        val sourceFile = SuFile(source.path)
        val destFile = SuFile(destination.path)

        if (!sourceFile.exists()) {
            throw FileNotFoundException("Source not found: ${source.path}")
        }

        if (sourceFile.absolutePath == destFile.absolutePath) {
            throw IllegalArgumentException("Source and destination are the same")
        }

        if (sourceFile.isDirectory && destFile.absolutePath.startsWith(sourceFile.absolutePath + "/")) {
            throw IllegalArgumentException("Cannot copy a directory into itself")
        }

        var totalCopied = 0L
        val totalBytes = if (sourceFile.isDirectory) calculateTotalSize(sourceFile) else sourceFile.length()"""

replacement_str = """    override fun copy(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> = flow {
        val sourceFile = SuFile(source.path)
        val destFile = SuFile(destination.path)

        if (!sourceFile.exists()) {
            throw FileNotFoundException("Source not found: ${source.path}")
        }

        if (sourceFile.absolutePath == destFile.absolutePath) {
            throw IllegalArgumentException("Source and destination are the same")
        }

        if (sourceFile.isDirectory && destFile.absolutePath.startsWith(sourceFile.absolutePath + "/")) {
            throw IllegalArgumentException("Cannot copy a directory into itself")
        }

        emit(FileOperationProgress(0L, 0L, sourceFile.name))

        var totalCopied = 0L
        val totalBytes = if (sourceFile.isDirectory) calculateTotalSize(sourceFile) else sourceFile.length()"""

content = content.replace(target_str, replacement_str)

target_str2 = """    override fun move(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> = flow {
        val sourceFile = SuFile(source.path)
        val destFile = SuFile(destination.path)

        if (!sourceFile.exists()) {
            throw FileNotFoundException("Source not found: ${source.path}")
        }

        val sourceCanonical = sourceFile.absolutePath
        val destCanonical = destFile.absolutePath

        if (sourceCanonical == destCanonical) {
            throw IOException("Source and destination are the same")
        }

        if (sourceFile.isDirectory && destCanonical.startsWith("$sourceCanonical/")) {
            throw IOException("Cannot move a directory into itself")
        }

        val sourceLength = if (sourceFile.isFile) sourceFile.length() else 0L
        val isSourceDir = sourceFile.isDirectory"""

replacement_str2 = """    override fun move(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> = flow {
        val sourceFile = SuFile(source.path)
        val destFile = SuFile(destination.path)

        if (!sourceFile.exists()) {
            throw FileNotFoundException("Source not found: ${source.path}")
        }

        val sourceCanonical = sourceFile.absolutePath
        val destCanonical = destFile.absolutePath

        if (sourceCanonical == destCanonical) {
            throw IOException("Source and destination are the same")
        }

        if (sourceFile.isDirectory && destCanonical.startsWith("$sourceCanonical/")) {
            throw IOException("Cannot move a directory into itself")
        }

        emit(FileOperationProgress(0L, 0L, sourceFile.name))

        val sourceLength = if (sourceFile.isFile) sourceFile.length() else calculateTotalSize(sourceFile)
        val isSourceDir = sourceFile.isDirectory"""

content = content.replace(target_str2, replacement_str2)

with open(file_path, 'w') as f:
    f.write(content)


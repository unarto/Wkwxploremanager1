    private fun deleteDirectoryRecursivelySafe(dir: File) {
        val stack = ArrayDeque<File>()
        stack.addLast(dir)
        val filesToDelete = ArrayDeque<File>()
        
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            filesToDelete.addFirst(current)
            
            val isSymlink = java.nio.file.Files.isSymbolicLink(current.toPath())
            if (!isSymlink && current.isDirectory) {
                val children = current.listFiles() ?: continue
                for (child in children) {
                    stack.addLast(child)
                }
            }
        }
        
        for (file in filesToDelete) {
            if (!file.delete() && file.exists()) {
                throw IOException("Failed to delete: ${file.absolutePath}")
            }
        }
    }

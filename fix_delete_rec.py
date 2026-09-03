import re

with open('core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/root/RootFileSystem.kt', 'r') as f:
    content = f.read()

replacement = """    private fun deleteDirectoryRecursively(dir: SuFile): Boolean {
        val children = dir.listFiles()
        if (children != null) {
            for (child in children) {
                if (child.isDirectory) {
                    if (!deleteDirectoryRecursively(child)) return false
                } else {
                    if (!child.delete()) return false
                }
            }
        }
        return dir.delete()
    }"""

content = re.sub(
    r'    private fun deleteDirectoryRecursively\(dir: SuFile\): Boolean \{.*?\n    \}',
    replacement,
    content,
    flags=re.DOTALL
)

with open('core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/root/RootFileSystem.kt', 'w') as f:
    f.write(content)

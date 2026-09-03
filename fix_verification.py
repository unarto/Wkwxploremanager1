import re

with open('core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/root/RootFileSystem.kt', 'r') as f:
    content = f.read()

# Fix createDirectory
content = re.sub(
    r'val created = targetFile\.mkdirs\(\) \|\| targetFile\.mkdir\(\) \|\| Shell\.cmd\("mkdir -p \$\{escapeShellArg\(targetPath\)\}"\)\.exec\(\)\.isSuccess\n\s+if \(!created && !targetFile\.exists\(\)\) \{\n\s+throw IOException\("Failed to create root directory: \$targetPath"\)\n\s+\}',
    r'targetFile.mkdirs() || targetFile.mkdir() || Shell.cmd("mkdir -p ${escapeShellArg(targetPath)}").exec().isSuccess\n        if (!targetFile.exists()) {\n            throw IOException("Failed to create root directory: $targetPath")\n        }',
    content
)

# Fix delete
content = re.sub(
    r'val deleted = if \(suFile\.isDirectory\) \{\n\s+deleteDirectoryRecursively\(suFile\) \|\| Shell\.cmd\("rm -rf \$\{escapeShellArg\(location\.path\)\}"\)\.exec\(\)\.isSuccess\n\s+\} else \{\n\s+suFile\.delete\(\) \|\| Shell\.cmd\("rm -f \$\{escapeShellArg\(location\.path\)\}"\)\.exec\(\)\.isSuccess\n\s+\}\n\n\s+if \(!deleted && suFile\.exists\(\)\) \{\n\s+throw IOException\("Failed to delete root file: \$\{location\.path\}"\)\n\s+\}',
    r'if (suFile.isDirectory) {\n            deleteDirectoryRecursively(suFile) || Shell.cmd("rm -rf ${escapeShellArg(location.path)}").exec().isSuccess\n        } else {\n            suFile.delete() || Shell.cmd("rm -f ${escapeShellArg(location.path)}").exec().isSuccess\n        }\n\n        if (suFile.exists()) {\n            throw IOException("Failed to delete root file: ${location.path}")\n        }',
    content
)

# Fix rename
content = re.sub(
    r'val renamed = sourceFile\.renameTo\(targetFile\) \|\| Shell\.cmd\("mv \$\{escapeShellArg\(location\.path\)\} \$\{escapeShellArg\(targetPath\)\}"\)\.exec\(\)\.isSuccess\n\s+if \(!renamed && !targetFile\.exists\(\)\) \{\n\s+throw IOException\("Failed to rename root file: \$\{location\.path\}"\)\n\s+\}',
    r'sourceFile.renameTo(targetFile) || Shell.cmd("mv ${escapeShellArg(location.path)} ${escapeShellArg(targetPath)}").exec().isSuccess\n\n        if (!targetFile.exists()) {\n            throw IOException("Failed to rename root file: ${location.path}")\n        }',
    content
)

with open('core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/root/RootFileSystem.kt', 'w') as f:
    f.write(content)

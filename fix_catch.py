import os
import glob

directory = 'core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem'
for filepath in glob.iglob(directory + '/**/*.kt', recursive=True):
    with open(filepath, 'r') as f:
        content = f.read()
    new_content = content.replace('catch (_: Exception) {}', 'catch (e: Exception) { android.util.Log.w("FileSystem", "Failed to clean partial file", e) }')
    if new_content != content:
        with open(filepath, 'w') as f:
            f.write(new_content)
        print(f"Updated {filepath}")

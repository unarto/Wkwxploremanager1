import re
with open('filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/detail/FileDetailChecksumTab.kt', 'r') as f:
    text = f.read()

# I want to make sure we don't have too many or too few braces. Let's just run ktlint or compile.

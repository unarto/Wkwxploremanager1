package com.wakwau.xplore.filemanager.ui.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wakwau.xplore.core.ui.theme.XPloreTheme

@Composable
fun FileListLoading(
    modifier: Modifier = Modifier
) {
    val colors = XPloreTheme.colors

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = colors.primary,
            modifier = Modifier.size(36.dp),
            strokeWidth = 3.dp
        )
    }
}

// [Jalur Class]: com.wakwau.xplore.filemanager.ui.list.FileListEmpty
// [Penjelasan]: Komponen UI composable untuk menampilkan pesan status ketika direktori folder terpilih tidak memiliki berkas/subfolder.
package com.wakwau.xplore.filemanager.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakwau.xplore.core.ui.theme.XPloreTheme
import com.wakwau.xplore.filemanager.ui.R

@Composable
fun FileListEmpty(
    modifier: Modifier = Modifier
) {
    val colors = XPloreTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            tint = colors.textTertiary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.msg_empty_directory),
            color = colors.textTertiary,
            fontSize = 14.sp
        )
    }
}

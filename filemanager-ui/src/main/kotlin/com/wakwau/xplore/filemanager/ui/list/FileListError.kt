// [Jalur Class]: com.wakwau.xplore.filemanager.ui.list.FileListError
// [Penjelasan]: Komponen UI composable untuk menampilkan pesan kesalahan saat memuat berkas beserta tombol coba lagi.
package com.wakwau.xplore.filemanager.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakwau.xplore.core.ui.theme.XPloreTheme
import com.wakwau.xplore.filemanager.ui.R

@Composable
fun FileListError(
    error: String,
    onRetry: () -> Unit,
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
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(12.dp))
        val errorText = when (error) {
            "NOT_FOUND" -> stringResource(R.string.err_not_found)
            "ACCESS_DENIED" -> stringResource(R.string.err_access_denied)
            "INVALID_LOCATION" -> stringResource(R.string.err_invalid_location)
            "INVALID_NAME" -> stringResource(R.string.err_invalid_name)
            "ALREADY_EXISTS" -> stringResource(R.string.err_already_exists)
            "IO_ERROR" -> stringResource(R.string.err_io_error)
            "NOT_SUPPORTED" -> stringResource(R.string.err_not_supported)
            else -> error.ifEmpty { stringResource(R.string.msg_error_loading) }
        }

        Text(
            text = errorText,
            color = colors.textPrimary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onRetry,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
        ) {
            Text(stringResource(R.string.btn_retry), color = colors.primary)
        }
    }
}

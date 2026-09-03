// [Jalur Class]: com.wakwau.xplore.filemanager.ui.component.ProgressDialog
// [Penjelasan]: Dialog kemajuan operasi berkas (salin, pindah, hapus) yang menampilkan indikator garis persentase dan rincian transfer byte.
package com.wakwau.xplore.filemanager.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.wakwau.xplore.core.storage.operation.FileOperationProgress
import com.wakwau.xplore.core.ui.theme.XPloreTheme
import com.wakwau.xplore.core.util.ByteFormatter
import com.wakwau.xplore.filemanager.ui.R

@Composable
fun ProgressDialog(
    operationName: String,
    progress: FileOperationProgress?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XPloreTheme.colors

    Dialog(onDismissRequest = { /* Cannot dismiss by tapping outside */ }) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceHighlight)
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.msg_operation_progress, operationName),
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val pct = progress?.percentage ?: 0f
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = colors.primary,
                trackColor = colors.surface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (progress != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${ByteFormatter.format(progress.bytesWritten)} / ${ByteFormatter.format(progress.totalBytes)}",
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${(pct * 100).toInt()}%",
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.title_processing_operation),
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.secondary)
                ) {
                    Text(stringResource(R.string.btn_cancel_operation))
                }
            }
        }
    }
}

// [Jalur Class]: com.wakwau.xplore.filemanager.ui.detail.FileDetailChecksumTab
// [Penjelasan]: Tab komputasi checksum kriptografis berkas yang menampilkan hash MD5, SHA-1, dan SHA-256 secara otomatis saat tab dibuka beserta tombol penyalinan cepat.
package com.wakwau.xplore.filemanager.ui.detail

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.ui.theme.XPloreTheme
import com.wakwau.xplore.filemanager.ui.R
import com.wakwau.xplore.filemanager.ui.state.ChecksumState

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun FileDetailChecksumTab(
    item: FileItem,
    checksumState: ChecksumState,
    onCalculateChecksum: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = XPloreTheme.colors
    val clipboardManager = LocalClipboardManager.current
    var userConfirmed by remember { mutableStateOf(false) }
    val isMassive = item.metadata.size > 524288000L

    LaunchedEffect(item, userConfirmed) {
        if (checksumState is ChecksumState.Idle) {
            if (!isMassive || userConfirmed) {
                onCalculateChecksum(item)
            }
        }
    }

    val copyToClipboard: (String, String) -> Unit = { text, label ->
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(context, context.getString(R.string.toast_copied_to_clipboard, label), Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        if (checksumState is ChecksumState.Idle && isMassive && !userConfirmed) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.msg_massive_file_checksum_warning),
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedButton(
                    onClick = { userConfirmed = true },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(text = stringResource(R.string.btn_calculate_hash), fontSize = 12.sp)
                }
            }
        } else {
            when (checksumState) {
                is ChecksumState.Idle,
                is ChecksumState.Calculating -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = colors.primary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.msg_calculating_hash),
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                }
            }
            is ChecksumState.Success -> {
                val checksum = checksumState.checksum

                // 1. MD5
                ChecksumDetailRow(
                    label = stringResource(R.string.label_md5_colon),
                    hash = checksum.md5,
                    onCopy = { copyToClipboard(checksum.md5, context.getString(R.string.label_md5_colon)) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. SHA1
                ChecksumDetailRow(
                    label = stringResource(R.string.label_sha1_colon),
                    hash = checksum.sha1,
                    onCopy = { copyToClipboard(checksum.sha1, context.getString(R.string.label_sha1_colon)) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. SHA256
                ChecksumDetailRow(
                    label = stringResource(R.string.label_sha256_colon),
                    hash = checksum.sha256,
                    onCopy = { copyToClipboard(checksum.sha256, context.getString(R.string.label_sha256_colon)) }
                )
            }
            is ChecksumState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val errorTxt = checksumState.errorRes?.let { stringResource(it) } ?: checksumState.message ?: ""
                    Text(
                        text = stringResource(R.string.msg_checksum_error, errorTxt),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onCalculateChecksum(item) },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.btn_retry), fontSize = 12.sp)
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun ChecksumDetailRow(
    label: String,
    hash: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XPloreTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.width(70.dp)
        )
        Text(
            text = hash,
            color = colors.textPrimary,
            fontSize = 12.5.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onCopy,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = stringResource(R.string.cd_copy_checksum, label),
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

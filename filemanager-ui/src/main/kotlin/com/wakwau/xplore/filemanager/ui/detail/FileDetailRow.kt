// [Jalur Class]: com.wakwau.xplore.filemanager.ui.detail.FileDetailRow
// [Penjelasan]: Komponen UI baris terisolasi untuk menampilkan label metadata berkas beserta nilai teks dan tombol aksi trailing (salin, ubah, atau hapus).
package com.wakwau.xplore.filemanager.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakwau.xplore.core.ui.theme.XPloreTheme

@Composable
fun FileDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    actionIcon: ImageVector? = null,
    actionContentDescription: String? = null,
    onActionClick: (() -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null
) {
    val colors = XPloreTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (leadingContent != null) {
                leadingContent()
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = value,
                color = colors.textPrimary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (actionIcon != null && onActionClick != null) {
            IconButton(
                onClick = onActionClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = actionContentDescription,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

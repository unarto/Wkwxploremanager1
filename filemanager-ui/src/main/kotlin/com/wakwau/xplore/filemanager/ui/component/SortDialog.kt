// [Jalur Class]: com.wakwau.xplore.filemanager.ui.component.SortDialog
// [Penjelasan]: Dialog Material 3 untuk memilih kriteria pengurutan berkas (Nama, Tanggal, Ukuran, Tipe) beserta arah pengurutan.

package com.wakwau.xplore.filemanager.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.wakwau.xplore.filemanager.ui.R
import com.wakwau.xplore.core.storage.preferences.FileSortDirection
import com.wakwau.xplore.core.storage.preferences.FileSortOrder

@Composable
fun SortDialog(
    currentOrder: FileSortOrder,
    currentDirection: FileSortDirection,
    onDismiss: () -> Unit,
    onSortApplied: (FileSortOrder, FileSortDirection) -> Unit
) {
    var selectedOrder by remember { mutableStateOf(currentOrder) }
    var selectedDirection by remember { mutableStateOf(currentDirection) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.title_sort_files)) },
        text = {
            Column {
                Text(text = stringResource(R.string.label_sort_by), style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                SortOptionRow(
                    label = stringResource(R.string.label_sort_name),
                    selected = selectedOrder == FileSortOrder.NAME,
                    onClick = { selectedOrder = FileSortOrder.NAME }
                )
                SortOptionRow(
                    label = stringResource(R.string.label_sort_date),
                    selected = selectedOrder == FileSortOrder.DATE,
                    onClick = { selectedOrder = FileSortOrder.DATE }
                )
                SortOptionRow(
                    label = stringResource(R.string.label_sort_size),
                    selected = selectedOrder == FileSortOrder.SIZE,
                    onClick = { selectedOrder = FileSortOrder.SIZE }
                )
                SortOptionRow(
                    label = stringResource(R.string.label_sort_type),
                    selected = selectedOrder == FileSortOrder.TYPE,
                    onClick = { selectedOrder = FileSortOrder.TYPE }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = stringResource(R.string.label_sort_direction), style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                SortOptionRow(
                    label = stringResource(R.string.label_sort_asc),
                    selected = selectedDirection == FileSortDirection.ASCENDING,
                    onClick = { selectedDirection = FileSortDirection.ASCENDING }
                )
                SortOptionRow(
                    label = stringResource(R.string.label_sort_desc),
                    selected = selectedDirection == FileSortDirection.DESCENDING,
                    onClick = { selectedDirection = FileSortDirection.DESCENDING }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSortApplied(selectedOrder, selectedDirection)
                onDismiss()
            }) {
                Text(stringResource(R.string.btn_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel_operation))
            }
        }
    )
}

@Composable
private fun SortOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/component/ConflictResolutionDialog.kt
// [Penjelasan]: Dialog UI untuk menangani benturan nama berkas/folder ganda dengan pilihan SKIP, OVERWRITE, RENAME, serta opsi Terapkan ke Semua.
package com.wakwau.xplore.filemanager.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.wakwau.xplore.core.storage.conflict.ConflictChoice
import com.wakwau.xplore.core.ui.components.AppDialog
import com.wakwau.xplore.filemanager.ui.state.OperationUiState

@Composable
fun ConflictResolutionDialog(
    conflictState: OperationUiState.ConflictResolution,
    onDecision: (ConflictChoice, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val conflict = conflictState.currentConflict ?: return
    var applyToAll by remember { mutableStateOf(false) }

    AppDialog(
        title = "Konflik Nama ${if (conflict.isDirectory) "Folder" else "Berkas"}",
        confirmButtonText = "",
        onConfirm = {},
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Text(
                text = "${conflict.targetName} sudah ada di lokasi tujuan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Item ${conflictState.currentConflictIndex + 1} dari ${conflictState.pendingConflicts.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = applyToAll,
                    onCheckedChange = { applyToAll = it }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Terapkan ke semua benturan berikutnya",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onDecision(ConflictChoice.SKIP, applyToAll) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Lewati")
                }
                Spacer(modifier = Modifier.width(4.dp))
                OutlinedButton(
                    onClick = { onDecision(ConflictChoice.OVERWRITE, applyToAll) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Timpa")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = { onDecision(ConflictChoice.RENAME, applyToAll) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ganti Nama")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Batal")
            }
        }
    }
}

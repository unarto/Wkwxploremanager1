// [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/search/FileSearchDialog.kt
// [Penjelasan]: Dialog pencarian berkas "Temukan" persis antarmuka X-plore dengan input kata kunci, tombol dropdown riwayat, switch cari di arsip, dropdown filter target (semua/file/folder), teks panduan wildcard, serta tombol BATAL dan OK tanpa hardcoding.
package com.wakwau.xplore.filemanager.ui.search

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.search.FileSearchQuery
import com.wakwau.xplore.core.storage.search.SearchTargetType
import com.wakwau.xplore.core.ui.theme.XPloreTheme
import com.wakwau.xplore.filemanager.ui.R
import com.wakwau.xplore.filemanager.ui.state.SearchUiState

@Composable
fun FileSearchDialog(
    state: SearchUiState,
    currentLocation: StorageLocation?,
    onDismiss: () -> Unit,
    onSearch: (FileSearchQuery) -> Unit,
    onCancelSearch: () -> Unit,
    onFileClick: (FileItem) -> Unit = {},
    onClearHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!state.isSearchDialogOpen) return

    val colors = XPloreTheme.colors
    var keyword by remember { mutableStateOf("") }
    var searchType by remember { mutableStateOf(SearchTargetType.ALL) }
    var searchInArchives by remember { mutableStateOf(false) }
    var expandedTypeMenu by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    val scopeLocation = state.searchScope ?: currentLocation

    Dialog(
        onDismissRequest = {
            if (state.isSearching) onCancelSearch()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 480.dp)
                .border(1.dp, colors.treeLineColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .testTag("file_search_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.title_search_temukan),
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (state.isSearching) onCancelSearch()
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("search_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = colors.treeLineColor.copy(alpha = 0.3f),
                    thickness = 0.5.dp
                )

                // Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Keyword input with History dropdown trigger
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        label = { Text(stringResource(R.string.label_temukan)) },
                        trailingIcon = {
                            IconButton(
                                onClick = { showHistoryDialog = true },
                                modifier = Modifier.testTag("search_history_icon_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = stringResource(R.string.cd_search_history),
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.treeLineColor.copy(alpha = 0.5f),
                            focusedLabelColor = colors.primary,
                            unfocusedLabelColor = colors.textSecondary
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_keyword_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row: Cari di arsip toggle & Target dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Cari di arsip
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { searchInArchives = !searchInArchives }
                        ) {
                            Text(
                                text = stringResource(R.string.label_cari_di_arsip),
                                color = colors.textPrimary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = searchInArchives,
                                onCheckedChange = { searchInArchives = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.primary,
                                    checkedTrackColor = colors.primary.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.testTag("search_in_archives_switch")
                            )
                        }

                        // Right: Filter Target (semua / file / folder)
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { expandedTypeMenu = true }
                                    .padding(vertical = 4.dp, horizontal = 6.dp)
                                    .testTag("search_target_type_dropdown")
                            ) {
                                Text(
                                    text = stringResource(R.string.label_temukan),
                                    color = colors.textSecondary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val targetLabel = when (searchType) {
                                    SearchTargetType.ALL -> stringResource(R.string.filter_target_all)
                                    SearchTargetType.FILE -> stringResource(R.string.filter_target_file)
                                    SearchTargetType.FOLDER -> stringResource(R.string.filter_target_folder)
                                }
                                Text(
                                    text = targetLabel,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = expandedTypeMenu,
                                onDismissRequest = { expandedTypeMenu = false }
                            ) {
                                val targets = listOf(
                                    SearchTargetType.FILE to stringResource(R.string.filter_target_file),
                                    SearchTargetType.FOLDER to stringResource(R.string.filter_target_folder),
                                    SearchTargetType.ALL to stringResource(R.string.filter_target_all)
                                )
                                targets.forEach { (type, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(
                                                    selected = searchType == type,
                                                    onClick = null,
                                                    colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(text = label, color = colors.textPrimary)
                                            }
                                        },
                                        onClick = {
                                            searchType = type
                                            expandedTypeMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Wildcard Help Text
                    Text(
                        text = stringResource(R.string.search_help_text),
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.testTag("search_help_text")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons (BATAL & OK)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                if (state.isSearching) onCancelSearch()
                                onDismiss()
                            },
                            modifier = Modifier.testTag("search_cancel_button")
                        ) {
                            Text(
                                text = stringResource(R.string.btn_batal),
                                color = colors.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        TextButton(
                            onClick = {
                                if (scopeLocation != null) {
                                    val query = FileSearchQuery(
                                        location = scopeLocation,
                                        keyword = keyword.trim(),
                                        searchType = searchType,
                                        searchInArchives = searchInArchives
                                    )
                                    onSearch(query)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.testTag("search_ok_button")
                        ) {
                            Text(
                                text = stringResource(R.string.btn_ok),
                                color = colors.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHistoryDialog) {
        SearchHistoryDialog(
            history = state.searchHistory,
            onSelectKeyword = { selected ->
                keyword = selected
            },
            onClearHistory = onClearHistory,
            onDismiss = { showHistoryDialog = false }
        )
    }
}

// [Jalur Class/Modul]: com.wakwau.xplore.filemanager.ui.search.FileSearchDialog
// [Penjelasan]: Dialog pencarian berkas terintegrasi dengan filter kata kunci, ekstensi, batas ukuran, serta tampilan daftar hasil pencarian nyata dengan navigasi langsung ke berkas/folder.
package com.wakwau.xplore.filemanager.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.search.FileSearchQuery
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.wakwau.xplore.core.storage.search.SearchTargetType
import com.wakwau.xplore.core.ui.components.FileIcon
import com.wakwau.xplore.core.ui.theme.XPloreTheme
import com.wakwau.xplore.core.util.ByteFormatter
import com.wakwau.xplore.core.util.DateFormatter
import com.wakwau.xplore.core.util.MimeTypeDetector
import com.wakwau.xplore.filemanager.ui.R
import com.wakwau.xplore.filemanager.ui.icon.StorageIconMapper
import com.wakwau.xplore.filemanager.ui.state.SearchUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileSearchDialog(
    state: SearchUiState,
    currentLocation: StorageLocation?,
    onDismiss: () -> Unit,
    onSearch: (FileSearchQuery) -> Unit,
    onCancelSearch: () -> Unit,
    onFileClick: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isSearchDialogOpen) return

    val colors = XPloreTheme.colors

    var keyword by remember { mutableStateOf("") }
    var searchType by remember { mutableStateOf(SearchTargetType.ALL) }
    var searchInArchives by remember { mutableStateOf(false) }
    var expandedTypeMenu by remember { mutableStateOf(false) }
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
                .fillMaxHeight(0.85f)
                .widthIn(max = 500.dp)
                .border(1.dp, colors.treeLineColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .testTag("file_search_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceElevated)
                        .padding(12.dp)
                        .border(width = 0.5.dp, color = colors.treeLineColor.copy(alpha = 0.3f)),
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
                        text = stringResource(R.string.label_search_title),
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (state.isSearching) onCancelSearch()
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(48.dp)
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

                // Form section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        label = { Text(stringResource(R.string.label_keyword)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_keyword_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Cari di arsip", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = searchInArchives,
                                onCheckedChange = { searchInArchives = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = colors.primary)
                            )
                        }
                        
                        ExposedDropdownMenuBox(
                            expanded = expandedTypeMenu,
                            onExpandedChange = { expandedTypeMenu = !expandedTypeMenu },
                            modifier = Modifier.width(140.dp)
                        ) {
                            OutlinedTextField(
                                value = when (searchType) {
                                    SearchTargetType.ALL -> "semua"
                                    SearchTargetType.FILE -> "file"
                                    SearchTargetType.FOLDER -> "folder"
                                },
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTypeMenu) },
                                modifier = Modifier.menuAnchor(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedTypeMenu,
                                onDismissRequest = { expandedTypeMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("semua") },
                                    onClick = { 
                                        searchType = SearchTargetType.ALL
                                        expandedTypeMenu = false 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("file") },
                                    onClick = { 
                                        searchType = SearchTargetType.FILE
                                        expandedTypeMenu = false 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("folder") },
                                    onClick = { 
                                        searchType = SearchTargetType.FOLDER
                                        expandedTypeMenu = false 
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (scopeLocation != null) {
                                val query = FileSearchQuery(
                                    location = scopeLocation,
                                    keyword = keyword.trim(),
                                    searchType = searchType,
                                    searchInArchives = searchInArchives
                                )
                                onSearch(query)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("search_submit_button"),
                        enabled = !state.isSearching && scopeLocation != null
                    ) {
                        if (state.isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = colors.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.msg_searching))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_search))
                        }
                    }
                }

                HorizontalDivider(
                    color = colors.treeLineColor.copy(alpha = 0.3f),
                    thickness = 0.5.dp
                )

                // Results / Status Section
                when {
                    state.isSearching -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = colors.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.msg_searching),
                                    color = colors.textSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    state.searchError != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.searchError,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp
                            )
                        }
                    }
                    state.hasSearched && state.results.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.msg_no_results),
                                color = colors.textSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                    state.results.isNotEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.label_search_results),
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "(${state.results.size})",
                                    color = colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(
                                        width = 0.5.dp,
                                        color = colors.treeLineColor.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .testTag("search_results_list"),
                                contentPadding = PaddingValues(4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(
                                    items = state.results,
                                    key = { it.location.path }
                                ) { item ->
                                    SearchResultRow(
                                        item = item,
                                        onClick = { onFileClick(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// [Jalur Class/Modul]: com.wakwau.xplore.filemanager.ui.search.SearchResultRow
// [Penjelasan]: Komponen perender satu baris hasil pencarian berkas yang menampilkan ikon kategori/folder, nama berkas, jalur path, ukuran, dan tanggal modifikasi.
@Composable
private fun SearchResultRow(
    item: FileItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XPloreTheme.colors
    val isDir = item.type == FileType.DIRECTORY
    val ext = item.name.substringAfterLast('.', "")
    val category = MimeTypeDetector.getCategory(item.name, isDir)
    val isInternalStorage = StorageIconMapper.isInternalStorage(item)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("search_result_item_${item.name}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileIcon(
            category = category,
            isDirectory = isDir,
            isInternalStorage = isInternalStorage,
            extension = ext,
            size = 24.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.location.path,
                color = colors.textSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!isDir) {
                    Text(
                        text = ByteFormatter.format(item.metadata.size),
                        color = colors.textSecondary,
                        fontSize = 10.sp
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                Text(
                    text = DateFormatter.formatShort(item.metadata.modifiedTime),
                    color = colors.textSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}


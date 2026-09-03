package com.wakwau.xplore.filemanager.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
// [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/settings/SettingsTreeScreen.kt
// [Penjelasan]: Menggunakan Icons.AutoMirrored untuk navigasi kembali yang kompatibel dengan tata letak RTL (Right-to-Left).
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Switch
import androidx.annotation.StringRes
import com.wakwau.xplore.core.storage.preferences.AppLanguage
import com.wakwau.xplore.core.storage.preferences.AppThemeMode
import com.wakwau.xplore.core.storage.preferences.FileSystemAccessMode
import com.wakwau.xplore.core.storage.preferences.SettingsState
import com.wakwau.xplore.core.ui.theme.LocalXPloreColors
import com.wakwau.xplore.filemanager.ui.R
import com.wakwau.xplore.treeview.component.ComposeTreeView
import com.wakwau.xplore.treeview.interaction.TreeInteraction
import com.wakwau.xplore.treeview.model.TreeNode
import com.wakwau.xplore.treeview.state.TreeState

object SettingsTreeScreenDefaults {
    val RowHeight = 48.dp
    val GroupIconSize = 24.dp
    val OptionIndentation = 40.dp
    val IconTitleSpacing = 16.dp
    val RadioLabelSpacing = 8.dp
    val RowHorizontalPadding = 8.dp
}

sealed class SettingsItem {
    data class Group(val id: String, @StringRes val titleRes: Int, val icon: ImageVector) : SettingsItem()
    data class ThemeOption(val mode: AppThemeMode, @StringRes val labelRes: Int, val isSelected: Boolean) : SettingsItem()
    data class LanguageOption(val language: AppLanguage, @StringRes val labelRes: Int, val isSelected: Boolean) : SettingsItem()
    data class FileSystemAccessOption(val mode: FileSystemAccessMode, @StringRes val labelRes: Int, val isSelected: Boolean) : SettingsItem()
    data class RootReadOnlyOption(@StringRes val labelRes: Int, val isChecked: Boolean) : SettingsItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTreeScreen(
    settingsState: SettingsState,
    onThemeSelected: (AppThemeMode) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onFileSystemAccessModeSelected: (FileSystemAccessMode) -> Unit,
    onRootReadOnlyChanged: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = remember { SettingsTreeEngine() }
    val colors = LocalXPloreColors.current

    LaunchedEffect(settingsState) {
        engine.updateState(settingsState)
    }

    val interaction = remember {
        object : TreeInteraction<SettingsItem> {
            override fun onToggle(node: TreeNode<SettingsItem>) {
                engine.toggleNode(node)
            }

            override fun onNodeClick(node: TreeNode<SettingsItem>) {
                when (val item = node.data) {
                    is SettingsItem.Group -> engine.toggleNode(node)
                    is SettingsItem.ThemeOption -> onThemeSelected(item.mode)
                    is SettingsItem.LanguageOption -> onLanguageSelected(item.language)
                    is SettingsItem.FileSystemAccessOption -> onFileSystemAccessModeSelected(item.mode)
                    is SettingsItem.RootReadOnlyOption -> onRootReadOnlyChanged(!item.isChecked)
                }
            }

            override fun onNodeLongClick(node: TreeNode<SettingsItem>) {}
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().background(colors.backgroundBrush),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/settings/SettingsTreeScreen.kt
                        // [Penjelasan]: Menggunakan varian AutoMirrored agar panah kembali secara otomatis terbalik di bahasa RTL.
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.topBarContainer,
                    titleContentColor = colors.textPrimary,
                    navigationIconContentColor = colors.textPrimary
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ComposeTreeView(
                treeState = engine.treeState,
                interaction = interaction,
                key = { _, it -> "${it.node.data.hashCode()}_${it.node.id}" },
                modifier = Modifier.fillMaxSize()
            ) { node ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SettingsTreeScreenDefaults.RowHeight)
                        .padding(horizontal = SettingsTreeScreenDefaults.RowHorizontalPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (val item = node.data) {
                        is SettingsItem.Group -> {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                modifier = Modifier.size(SettingsTreeScreenDefaults.GroupIconSize),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(SettingsTreeScreenDefaults.IconTitleSpacing))
                            Text(
                                text = stringResource(item.titleRes),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        is SettingsItem.ThemeOption -> {
                            Spacer(modifier = Modifier.width(SettingsTreeScreenDefaults.OptionIndentation))
                            RadioButton(
                                selected = item.isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colors.primary,
                                    unselectedColor = colors.focusBorder
                                )
                            )
                            Spacer(modifier = Modifier.width(SettingsTreeScreenDefaults.RadioLabelSpacing))
                            Text(
                                text = stringResource(item.labelRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        is SettingsItem.LanguageOption -> {
                            Spacer(modifier = Modifier.width(SettingsTreeScreenDefaults.OptionIndentation))
                            RadioButton(
                                selected = item.isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colors.primary,
                                    unselectedColor = colors.focusBorder
                                )
                            )
                            Spacer(modifier = Modifier.width(SettingsTreeScreenDefaults.RadioLabelSpacing))
                            Text(
                                text = stringResource(item.labelRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        is SettingsItem.FileSystemAccessOption -> {
                            Spacer(modifier = Modifier.width(SettingsTreeScreenDefaults.OptionIndentation))
                            RadioButton(
                                selected = item.isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colors.primary,
                                    unselectedColor = colors.focusBorder
                                )
                            )
                            Spacer(modifier = Modifier.width(SettingsTreeScreenDefaults.RadioLabelSpacing))
                            Text(
                                text = stringResource(item.labelRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        is SettingsItem.RootReadOnlyOption -> {
                            Spacer(modifier = Modifier.width(SettingsTreeScreenDefaults.OptionIndentation))
                            Switch(
                                checked = item.isChecked,
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(SettingsTreeScreenDefaults.RadioLabelSpacing))
                            Text(
                                text = stringResource(item.labelRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

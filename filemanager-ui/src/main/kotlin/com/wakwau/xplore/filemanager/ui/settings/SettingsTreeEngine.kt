package com.wakwau.xplore.filemanager.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import com.wakwau.xplore.core.storage.preferences.AppLanguage
import com.wakwau.xplore.core.storage.preferences.AppThemeMode
import com.wakwau.xplore.core.storage.preferences.FileSystemAccessMode
import com.wakwau.xplore.core.storage.preferences.SettingsState
import com.wakwau.xplore.filemanager.ui.R
import com.wakwau.xplore.treeview.model.TreeNode
import com.wakwau.xplore.treeview.state.TreeState

class SettingsTreeEngine(val treeState: TreeState<SettingsItem> = TreeState()) {

    fun updateState(settingsState: SettingsState) {
        val oldThemeGroupExpanded = treeState.roots.find { (it.data as? SettingsItem.Group)?.id == SettingsConstants.GROUP_THEME }?.isExpanded ?: true
        val oldLangGroupExpanded = treeState.roots.find { (it.data as? SettingsItem.Group)?.id == SettingsConstants.GROUP_LANG }?.isExpanded ?: true
        val oldFsGroupExpanded = treeState.roots.find { (it.data as? SettingsItem.Group)?.id == SettingsConstants.GROUP_FILESYSTEM }?.isExpanded ?: true

        val themeGroup = TreeNode<SettingsItem>(SettingsItem.Group(SettingsConstants.GROUP_THEME, R.string.group_theme_display, Icons.Default.Palette))
        themeGroup.addChild(TreeNode(SettingsItem.ThemeOption(AppThemeMode.DARK, R.string.theme_option_dark, settingsState.themeMode == AppThemeMode.DARK)))
        themeGroup.addChild(TreeNode(SettingsItem.ThemeOption(AppThemeMode.LIGHT, R.string.theme_option_light, settingsState.themeMode == AppThemeMode.LIGHT)))
        themeGroup.addChild(TreeNode(SettingsItem.ThemeOption(AppThemeMode.SYSTEM, R.string.theme_option_system, settingsState.themeMode == AppThemeMode.SYSTEM)))

        val langGroup = TreeNode<SettingsItem>(SettingsItem.Group(SettingsConstants.GROUP_LANG, R.string.group_language, Icons.Default.Language))
        langGroup.addChild(TreeNode(SettingsItem.LanguageOption(AppLanguage.INDONESIAN, R.string.lang_option_indonesian, settingsState.language == AppLanguage.INDONESIAN)))
        langGroup.addChild(TreeNode(SettingsItem.LanguageOption(AppLanguage.ENGLISH, R.string.lang_option_english, settingsState.language == AppLanguage.ENGLISH)))
        langGroup.addChild(TreeNode(SettingsItem.LanguageOption(AppLanguage.SYSTEM, R.string.lang_option_system, settingsState.language == AppLanguage.SYSTEM)))

        val fsGroup = TreeNode<SettingsItem>(SettingsItem.Group(SettingsConstants.GROUP_FILESYSTEM, R.string.group_filesystem, Icons.Default.Security))
        fsGroup.addChild(TreeNode(SettingsItem.FileSystemAccessOption(FileSystemAccessMode.NORMAL, R.string.fs_option_normal, settingsState.fileSystemAccessMode == FileSystemAccessMode.NORMAL)))
        fsGroup.addChild(TreeNode(SettingsItem.FileSystemAccessOption(FileSystemAccessMode.SHIZUKU, R.string.fs_option_shizuku, settingsState.fileSystemAccessMode == FileSystemAccessMode.SHIZUKU)))
        fsGroup.addChild(TreeNode(SettingsItem.FileSystemAccessOption(FileSystemAccessMode.ROOT, R.string.fs_option_root, settingsState.fileSystemAccessMode == FileSystemAccessMode.ROOT)))
        fsGroup.addChild(TreeNode(SettingsItem.RootReadOnlyOption(R.string.fs_option_root_readonly, settingsState.isRootReadOnly)))

        treeState.setRoots(listOf(themeGroup, langGroup, fsGroup))
        
        if (oldThemeGroupExpanded) {
            treeState.expand(themeGroup)
        }
        if (oldLangGroupExpanded) {
            treeState.expand(langGroup)
        }
        if (oldFsGroupExpanded) {
            treeState.expand(fsGroup)
        }
    }

    fun toggleNode(node: TreeNode<SettingsItem>) {
        treeState.toggle(node)
    }
}

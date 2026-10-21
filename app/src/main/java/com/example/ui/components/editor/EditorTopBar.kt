package com.example.ui.components.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.components.EditorTopAppBar

/**
 * Top app bar for NoteEditorScreen.
 * Encapsulates navigation, undo/redo, status toggles, and note action menu.
 */
@Composable
fun EditorTopBar(
    amber: Color,
    editorBgColor: Color,
    isPaperDark: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    isPinned: Boolean,
    isLocked: Boolean,
    syncEnabled: Boolean,
    isArchived: Boolean,
    reminderTimestamp: Long?,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onReminderClick: () -> Unit,
    onShareClick: () -> Unit,
    onExportPdfClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleLock: () -> Unit,
    onOpenPaperStyle: () -> Unit,
    onToggleSync: () -> Unit,
    onOpenStats: () -> Unit,
    onToggleArchive: () -> Unit,
    modifier: Modifier = Modifier,
    noteTitle: String = ""
) {
    EditorTopAppBar(
        amber = amber,
        editorBgColor = editorBgColor,
        isPaperDark = isPaperDark,
        canUndo = canUndo,
        canRedo = canRedo,
        isPinned = isPinned,
        isLocked = isLocked,
        syncEnabled = syncEnabled,
        isArchived = isArchived,
        reminderTimestamp = reminderTimestamp,
        showMenu = showMenu,
        onShowMenuChange = onShowMenuChange,
        onBack = onBack,
        onUndo = onUndo,
        onRedo = onRedo,
        onReminderClick = onReminderClick,
        onShareClick = onShareClick,
        onExportPdfClick = onExportPdfClick,
        onTogglePin = onTogglePin,
        onToggleLock = onToggleLock,
        onOpenPaperStyle = onOpenPaperStyle,
        onToggleSync = onToggleSync,
        onOpenStats = onOpenStats,
        onToggleArchive = onToggleArchive,
        modifier = modifier
    )
}

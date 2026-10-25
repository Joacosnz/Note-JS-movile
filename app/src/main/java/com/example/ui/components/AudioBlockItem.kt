package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.model.NoteBlock
import com.example.ui.components.editor.AudioBlockItem as EditorAudioBlockItem

@Composable
fun AudioBlockItem(
    block: NoteBlock.Audio,
    onToggleTranscription: () -> Unit,
    onDeleteBlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    EditorAudioBlockItem(
        block = block,
        onToggleTranscription = onToggleTranscription,
        onDeleteBlock = onDeleteBlock,
        modifier = modifier
    )
}

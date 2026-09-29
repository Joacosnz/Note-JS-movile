package com.example.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun BatchDeleteDialog(
    count: Int,
    isTrashTab: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val title = if (isTrashTab) "¿Eliminar $count notas definitivamente?" else "¿Enviar $count notas a la papelera?"
    val description = if (isTrashTab) {
        "Esta acción no se puede deshacer."
    } else {
        "Las notas se moverán a la papelera."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(description) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(if (isTrashTab) "Eliminar" else "Mover a papelera")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

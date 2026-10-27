package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InsertInlineMathDialog(
    initialCode: String = "",
    isEditing: Boolean = false,
    isDark: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var latexCode by remember { mutableStateOf(initialCode) }
    val amber = com.example.ui.theme.NoteJsAmber

    val quickTemplates = listOf(
        "Ec. lineal" to "ax + b = c",
        "Fracción" to "\\frac{a}{b}",
        "Exponente" to "x^{2}",
        "Raíz" to "\\sqrt{x}",
        "Límite" to "\\lim_{x \\to 0}",
        "Suma" to "\\sum_{i=1}^{n}",
        "Integral" to "\\int_{0}^{1}"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Functions,
                    contentDescription = null,
                    tint = amber,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditing) "Editar ecuación en línea" else "Ecuación en línea",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Escribe el código LaTeX de tu fórmula (ej: \\frac{1}{2}):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = latexCode,
                    onValueChange = { latexCode = it },
                    placeholder = {
                        Text(
                            text = "\\frac{1}{2}",
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray
                        )
                    },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = amber,
                        cursorColor = amber
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick shortcuts
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickTemplates, key = { it.first }) { (label, template) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = amber.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, amber.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable {
                                latexCode = if (latexCode.isEmpty()) template else "$latexCode $template"
                            }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) amber else Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Preview
                Text(
                    text = "Vista previa:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF121212) else Color(0xFFF3F4F6),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        KaTeXMathView(
                            latex = latexCode.ifBlank { "x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}" },
                            isDark = isDark,
                            color = if (isDark) amber else Color(0xFFB45309),
                            displayMode = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (latexCode.isNotBlank()) {
                        onConfirm(latexCode.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = amber,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                enabled = latexCode.isNotBlank()
            ) {
                Text(
                    text = if (isEditing) "Guardar" else "Insertar",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            Row {
                if (isEditing && onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Eliminar", color = Color(0xFFEF4444))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

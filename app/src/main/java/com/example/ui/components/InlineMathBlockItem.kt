package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NoteBlock

@Composable
fun InlineMathBlockItem(
    block: NoteBlock.Math,
    onUpdateBlock: (NoteBlock.Math) -> Unit,
    onDeleteBlock: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(block.latexCode.isEmpty()) }
    val amber = Color(0xFFFFB800)

    if (isEditing) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
            border = BorderStroke(1.dp, amber.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Functions,
                            contentDescription = null,
                            tint = amber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Fórmula en línea (LaTeX)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = if (isDark) Color(0xFF9CA3AF) else Color(0xFF4B5563)
                        )
                    }

                    Row {
                        IconButton(
                            onClick = onDeleteBlock,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(
                            onClick = { isEditing = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = amber,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(
                                horizontal = 10.dp,
                                vertical = 4.dp
                            ),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Listo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDark) Color(0xFF121212) else Color.White,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    if (block.latexCode.isEmpty()) {
                        Text(
                            text = "Ej: \\frac{1}{2}  o  x^2 + y^2 = r^2",
                            style = TextStyle(
                                color = Color(0xFF9CA3AF),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        )
                    }

                    BasicTextField(
                        value = block.latexCode,
                        onValueChange = { newCode ->
                            onUpdateBlock(block.copy(latexCode = newCode))
                        },
                        textStyle = TextStyle(
                            color = if (isDark) amber else Color(0xFFB45309),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(amber),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    } else {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isEditing = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                NativeMathView(
                    expression = block.latexCode.ifBlank { "Fórmula vacía" },
                    isDark = isDark,
                    fontSize = 17.sp,
                    color = if (isDark) amber else Color(0xFFB45309),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NoteBlock

@Composable
fun MathBlockItem(
    block: NoteBlock.Math,
    onUpdateBlock: (NoteBlock.Math) -> Unit,
    onDeleteBlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(block.latexCode.isEmpty()) }
    val isDark = isSystemInDarkTheme()
    val amber = Color(0xFFFFB800)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        if (isEditing) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1C1C1E),
                border = BorderStroke(1.dp, amber.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Functions,
                            contentDescription = null,
                            tint = amber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fórmula Matemática (LaTeX)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = Color(0xFF9CA3AF)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF121212), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        if (block.latexCode.isEmpty()) {
                            Text(
                                text = "Ej: \\frac{a}{b} + \\sqrt{x}",
                                style = TextStyle(
                                    color = Color(0xFF6B7280),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp
                                )
                            )
                        }

                        BasicTextField(
                            value = block.latexCode,
                            onValueChange = { newCode ->
                                onUpdateBlock(block.copy(latexCode = newCode))
                            },
                            textStyle = TextStyle(
                                color = amber,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            ),
                            cursorBrush = SolidColor(amber),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDeleteBlock) {
                            Text("🗑️ Eliminar", color = Color(0xFFEF4444), fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { isEditing = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = amber,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Previsualizar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                        .clickable { isEditing = true },
                    contentAlignment = Alignment.Center
                ) {
                    NativeMathView(
                        expression = block.latexCode.ifBlank { "Fórmula vacía" },
                        isDark = isDark,
                        fontSize = 20.sp,
                        color = if (isDark) amber else Color(0xFFB45309),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

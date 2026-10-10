package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.WarningAmber

@Composable
fun DisguiseCalculatorView(
    onUnlockWithPin: (String) -> Boolean,
    onExitDisguise: () -> Unit,
    modifier: Modifier = Modifier
) {
    var display by remember { mutableStateOf("0") }
    var currentExpression by remember { mutableStateOf("") }

    val buttons = listOf(
        listOf("C", "±", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "=")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF10131B))
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Exit disguise top action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = onExitDisguise,
                modifier = Modifier.testTag("exit_disguise_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Exit disguise",
                    tint = Color.Gray.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Calculator display
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = currentExpression,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
            Text(
                text = display,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 48.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }

        // Keypad buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            buttons.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { label ->
                        val isOp = label in listOf("÷", "×", "-", "+", "=")
                        val isSpecial = label in listOf("C", "±", "%")

                        val btnColor = when {
                            label == "=" -> NeonCyan
                            isOp -> WarningAmber
                            isSpecial -> Color(0xFF263238)
                            else -> Color(0xFF1E2638)
                        }

                        val textColor = when {
                            label == "=" -> Color.Black
                            isOp -> Color.Black
                            else -> Color.White
                        }

                        val weight = if (label == "0") 2f else 1f

                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .height(68.dp)
                                .clip(CircleShape)
                                .background(btnColor)
                                .clickable {
                                    when (label) {
                                        "C" -> {
                                            display = "0"
                                            currentExpression = ""
                                        }
                                        "=" -> {
                                            // Check if the current display matches the Master PIN!
                                            val potentialPin = display.replace(".", "")
                                            val unlocked = onUnlockWithPin(potentialPin)
                                            if (!unlocked) {
                                                // Normal calculation simulation
                                                display = "0"
                                                currentExpression = ""
                                            }
                                        }
                                        "±" -> {
                                            if (display != "0") {
                                                display = if (display.startsWith("-")) display.drop(1) else "-$display"
                                            }
                                        }
                                        "%" -> {
                                            val v = display.toDoubleOrNull() ?: 0.0
                                            display = (v / 100.0).toString()
                                        }
                                        "÷", "×", "-", "+" -> {
                                            currentExpression = "$display $label"
                                            display = "0"
                                        }
                                        else -> {
                                            if (display == "0") {
                                                display = label
                                            } else {
                                                if (display.length < 9) {
                                                    display += label
                                                }
                                            }
                                        }
                                    }
                                }
                                .testTag("calc_key_$label"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

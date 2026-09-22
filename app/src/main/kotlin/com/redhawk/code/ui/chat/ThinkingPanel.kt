package com.redhawk.code.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Test APK tarzı düşünme paneli.
 * - Streaming sırasında otomatik açık, canlı yazıyor
 * - Bitince otomatik kapanır, "X.Xs düşündü" başlığı
 * - Kullanıcı tıklarsa aç/kapa
 */
@Composable
fun ThinkingPanel(
    thinking: String,
    thinkingMs: Long,
    isStreaming: Boolean,
    toolLabel: String? = null,
    defaultExpanded: Boolean = false
) {
    if (thinking.isBlank() && !isStreaming) return

    var userToggled by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(defaultExpanded) }

    // Streaming başlayınca aç, bitince kapat (kullanıcı toggle'lamadıysa)
    LaunchedEffect(isStreaming) {
        if (!userToggled) {
            expanded = isStreaming
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        userToggled = true
                        expanded = !expanded
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Psychology, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when {
                                isStreaming -> "Düşünüyor…"
                                thinking.isNotBlank() -> "Düşündü"
                                else -> "Bekliyor…"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (thinkingMs > 0 && !isStreaming) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                ThinkingParser.formatDuration(thinkingMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (!expanded && thinking.isNotBlank()) {
                        Text(
                            thinking.lineSequence().firstOrNull()?.take(70) ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                if (isStreaming) {
                    ThinkingDots()
                } else {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(220)) + fadeIn(tween(180)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(120))
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 12.dp, bottom = 12.dp)
                ) {
                    Box(
                        Modifier
                            .width(2.dp)
                            .heightIn(min = 30.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = thinking.ifBlank { "Modelden ilk yanıt bekleniyor…" },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alpha(if (isStreaming) 0.85f else 1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinkingDots() {
    val inf = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { i ->
            val a by inf.animateFloat(
                initialValue = 0.3f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(500, delayMillis = i * 150, easing = LinearEasing),
                    RepeatMode.Reverse
                ),
                label = "d$i"
            )
            Box(
                Modifier
                    .size(5.dp)
                    .alpha(a)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

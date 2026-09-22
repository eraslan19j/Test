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
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.SmartToy
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
 * Canlı aktivite paneli (dosya kartı tarzı):
 * - Streaming sırasında: "Düşünüyor…" + canlı süre + açılır düşünme metni
 * - Bitince: dosya ikonlu "Düşünüldü · 4.5s" + açılır metin
 * - totalMs ViewModel'den ~60ms'de bir tazelendiği için süre kendiliğinden işler.
 */
@Composable
fun ThinkingPanel(
    thinking: String,
    thinkingMs: Long,
    totalMs: Long = 0L,
    isStreaming: Boolean,
    toolLabel: String? = null,
    defaultExpanded: Boolean = false
) {
    if (thinking.isBlank() && !isStreaming && toolLabel == null) return

    var userToggled by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(defaultExpanded) }

    // Streaming başlayınca aç, bitince kapat (kullanıcı oynamadıysa)
    LaunchedEffect(isStreaming) {
        if (!userToggled) {
            expanded = isStreaming
        }
    }

    val status = when {
        toolLabel != null -> toolLabel
        isStreaming && thinking.isNotBlank() -> "Düşünüyor…"
        isStreaming -> "Yanıt yazılıyor…"
        thinking.isNotBlank() -> "Düşünüldü"
        else -> "Tamamlandı"
    }
    // Canlı süre: akışta toplam, bitince düşünme süresi
    val liveMs = if (isStreaming) totalMs else if (thinkingMs > 0) thinkingMs else totalMs

    // Giriş animasyonu (tek seferlik)
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(200)) + expandVertically(tween(220))
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                        when {
                            toolLabel != null -> Icons.Outlined.SmartToy
                            isStreaming -> Icons.Outlined.Psychology
                            else -> Icons.Outlined.FolderOpen
                        },
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                status,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            if (liveMs > 0) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    ThinkingParser.formatDuration(liveMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        if (!expanded && thinking.isNotBlank() && toolLabel == null) {
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
                        Spacer(Modifier.width(8.dp))
                    }
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
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

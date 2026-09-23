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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class StreamState { Idle, Connecting, WaitingFirstEvent, Streaming, Done }

/**
 * İki seviyeli aktivite paneli (referans tasarım):
 * üstte "Exploring / N s explored", altta "Düşünüyor…",
 * içeride dikey çizgi + düşünme metni + "SSE · N sn ·" nabzı.
 */
@Composable
fun ThinkingPanel(
    thinking: String,
    thinkingMs: Long,
    isStreaming: Boolean,
    streamState: StreamState = StreamState.Idle,
    streamSeconds: Long = 0,
    toolLabel: String? = null
) {
    var outer by remember { mutableStateOf(true) }
    var inner by remember { mutableStateOf(true) }
    var userToggled by remember { mutableStateOf(false) }

    // Streaming bitince otomatik kapat (kullanıcı oynamadıysa)
    LaunchedEffect(isStreaming) { if (!isStreaming && !userToggled) outer = false }

    // "Exploring" / "20.1 s explored"
    val header = when {
        toolLabel != null -> toolLabel
        isStreaming -> "Exploring"
        thinkingMs > 0 -> "${formatMs(thinkingMs)} explored"
        else -> "Exploring"
    }

    Surface(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(Modifier.fillMaxWidth()) {

            // === DIŞ SATIR ===
            Row(
                Modifier.fillMaxWidth()
                    .clickable { userToggled = true; outer = !outer }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.FolderOpen, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(header, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                if (isStreaming) {
                    Text("$streamSeconds s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                }
                Icon(if (outer) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // === İÇERİK ===
            AnimatedVisibility(outer,
                enter = expandVertically(tween(200)) + fadeIn(tween(150)),
                exit = shrinkVertically(tween(150)) + fadeOut(tween(100))
            ) {
                Column {

                    // === ALT SATIR: Düşünüyor… ===
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { userToggled = true; inner = !inner }
                            .padding(start = 22.dp, end = 12.dp, top = 4.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Psychology, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isStreaming) "Düşünüyor…" else "Düşündü",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f))
                        if (isStreaming) Dots()
                        else Icon(
                            if (inner) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            null, modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // === İÇERİK: çizgi + metin + SSE ===
                    AnimatedVisibility(inner,
                        enter = expandVertically(tween(180)) + fadeIn(tween(120)),
                        exit = shrinkVertically(tween(120))
                    ) {
                        Row(
                            Modifier.fillMaxWidth()
                                .padding(start = 30.dp, end = 12.dp, bottom = 12.dp)
                        ) {
                            // Sol dikey çizgi
                            Box(
                                Modifier.width(1.5.dp).heightIn(min = 40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                                        RoundedCornerShape(1.dp)
                                    )
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        thinking.isNotBlank() -> thinking
                                        streamState == StreamState.WaitingFirstEvent ->
                                            "Modelden ilk gerçek olay bekleniyor"
                                        streamState == StreamState.Connecting ->
                                            "Sağlayıcıya bağlanılıyor…"
                                        else -> "Bekleniyor…"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.alpha(if (isStreaming) 0.85f else 1f)
                                )
                                if (isStreaming) {
                                    Spacer(Modifier.height(8.dp))
                                    Text("SSE · $streamSeconds sn ·",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Dots() {
    val inf = rememberInfiniteTransition(label = "d")
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { i ->
            val a by inf.animateFloat(
                initialValue = 0.25f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(500, delayMillis = i * 150, easing = LinearEasing),
                    RepeatMode.Reverse), label = "d$i"
            )
            Box(Modifier.size(5.dp).alpha(a).clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant))
        }
    }
}

private fun formatMs(ms: Long): String =
    if (ms < 1000) "${ms}ms" else String.format("%.1f s", ms / 1000.0)

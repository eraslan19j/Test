package com.redhawk.code.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.redhawk.code.data.provider.ProviderCatalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    vm: ChatViewModel,
    onBack: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenChats: () -> Unit = {},
    onOpenProject: () -> Unit = {},
    onOpenPermissions: () -> Unit = {}
) {
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    val ctx = LocalContext.current
    var showQuickActions by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.messages.size,
        state.messages.lastOrNull()?.content,
        state.messages.lastOrNull()?.thinking) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.chatTitle.ifEmpty { "ReDHawK AI" },
                            style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(state.modelLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) { Icon(Icons.Filled.Menu, null) }
                },
                actions = {
                    IconButton(onClick = onOpenChats) { Icon(Icons.Outlined.History, "Sohbetler") }
                    IconButton(onClick = { showModelPicker = true }) { Icon(Icons.Outlined.Memory, "Model seç") }
                    IconButton(onClick = { vm.newChat() }) { Icon(Icons.Filled.Add, "Yeni") }
                    IconButton(onClick = vm::clearChat) { Icon(Icons.Outlined.DeleteSweep, "Temizle") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (state.messages.isEmpty()) {
                Box(Modifier.weight(1f)) { EmptyChatState({ vm.setInput(it) }) }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(state.messages, key = { _, m -> m.id }) { _, m ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(200)) + slideInVertically(
                                initialOffsetY = { it / 5 }, animationSpec = tween(200))
                        ) {
                            MessageItem(m, ctx)
                        }
                    }
                }
            }

            state.error?.let { err ->
                AnimatedVisibility(visible = true, enter = fadeIn()) {
                    Surface(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ) {
                        Text("⚠ $err",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp))
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(8.dp)
                    .windowInsetsPadding(WindowInsets.ime),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(onClick = { showQuickActions = true }) {
                    Icon(Icons.Filled.Add, "Hızlı işlemler")
                }
                Spacer(Modifier.width(6.dp))
                OutlinedTextField(
                    value = state.input,
                    onValueChange = vm::onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("ReDHawK AI ile mesajlaş…") },
                    maxLines = 5,
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(Modifier.width(6.dp))
                AnimatedContent(
                    targetState = when {
                        state.isStreaming -> "stop"
                        state.input.isBlank() -> "mic"
                        else -> "send"
                    },
                    transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(120)) },
                    label = "sb"
                ) { mode ->
                    when (mode) {
                        "stop" -> FilledIconButton(onClick = vm::stop) {
                            Icon(Icons.Filled.Stop, "Durdur")
                        }
                        "mic" -> FilledIconButton(onClick = { }) {
                            Icon(Icons.Outlined.Mic, "Sesli")
                        }
                        else -> FilledIconButton(onClick = vm::send) {
                            Icon(Icons.Filled.Send, "Gönder")
                        }
                    }
                }
            }
        }
    }

    if (showModelPicker && state.currentProvider != null) {
        val provider = state.currentProvider!!
        val available = remember(provider.baseUrl, provider.model) {
            ProviderCatalog.modelsFor(provider.baseUrl, provider.model)
        }
        ModelPickerSheet(
            currentModel = provider.model,
            providerName = provider.displayName,
            availableModels = available,
            onSelect = { vm.updateModel(it) },
            onDismiss = { showModelPicker = false }
        )
    }

    if (showQuickActions) {
        QuickActionsSheet(
            enabledSkills = state.enabledSkills,
            onToggleSkill = vm::toggleSkill,
            onPickAction = { a ->
                showQuickActions = false
                when (a) {
                    "project" -> onOpenProject()
                    "permissions" -> onOpenPermissions()
                }
            },
            onDismiss = { showQuickActions = false }
        )
    }
}

@Composable
private fun MessageItem(m: UiMessage, ctx: Context) {
    if (m.isUser) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            Box(
                Modifier.widthIn(max = 320.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(m.content, color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    // Asistan
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        // Thinking paneli
        if (m.thinking.isNotBlank() || m.thinkingStreaming) {
            ThinkingPanel(
                thinking = m.thinking,
                thinkingMs = m.thinkingMs,
                isStreaming = m.thinkingStreaming,
                toolLabel = m.toolLabel
            )
            Spacer(Modifier.height(8.dp))
        }

        // Cevap
        val showBubble = m.content.isNotBlank() || !m.thinkingStreaming
        if (showBubble) {
            Box(
                Modifier.widthIn(max = 330.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = m.content.ifEmpty { "…" },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.SansSerif,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Süre + aksiyonlar
        if (!m.streaming && m.content.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (m.totalMs > 0) {
                    Icon(Icons.Outlined.Timer, null, Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(ThinkingParser.formatDuration(m.totalMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                }
                MsgAction(Icons.Outlined.ContentCopy) {
                    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("chat", m.content))
                }
                MsgAction(Icons.Outlined.Share) {
                    val i = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, m.content)
                    }
                    ctx.startActivity(Intent.createChooser(i, "Paylaş"))
                }
                MsgAction(Icons.Outlined.ThumbUp) {}
                MsgAction(Icons.Outlined.ThumbDown) {}
            }
        }
    }
}

@Composable
private fun MsgAction(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(icon, null, Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyChatState(onPick: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val inf = rememberInfiniteTransition(label = "pulse")
        val s by inf.animateFloat(0.9f, 1.05f,
            infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "s")
        Box(Modifier.size(72.dp).scale(s).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Bolt, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Merhaba, ben ReDHawK AI",
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Ne yapmak istersin?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        listOf(
            "Kodumdaki hatayı bul ve düzelt",
            "Kısa bir Türkçe özet yaz",
            "SQL sorgusu yaz: son 7 günün siparişleri",
            "Bu kod ne yapıyor? Açıkla"
        ).forEach { t ->
            Surface(
                Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp)).clickable { onPick(t) },
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(t, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.NorthEast, null, Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

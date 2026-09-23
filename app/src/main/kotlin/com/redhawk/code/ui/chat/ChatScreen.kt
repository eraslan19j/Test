package com.redhawk.code.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    // Sesli okuma motoru (TTS) — ekran kapanınca serbest bırakılır
    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    DisposableEffect(ctx) {
        var engine: android.speech.tts.TextToSpeech? = null
        engine = android.speech.tts.TextToSpeech(ctx) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                engine?.language = java.util.Locale("tr", "TR")
            }
        }
        tts = engine
        onDispose { engine?.stop(); engine?.shutdown() }
    }

    // Proje klasörü seçici (SAF): kalıcı okuma/yazma izni alınır
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) vm.setProjectFolder(uri) }

    // PERF: akış sırasında animasyonsuz kaydır (her token'da animate = kasma),
    // normal geçişlerde animasyonlu kaydır.
    LaunchedEffect(state.messages.size,
        state.messages.lastOrNull()?.content,
        state.messages.lastOrNull()?.thinking) {
        if (state.messages.isNotEmpty()) {
            if (state.isStreaming) listState.scrollToItem(state.messages.lastIndex)
            else listState.animateScrollToItem(state.messages.lastIndex)
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
                    IconButton(onClick = vm::toggleAgent) {
                        Icon(Icons.Outlined.SmartToy, "Ajan modu",
                            tint = if (state.agentMode) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
                        // PERF: her satırda giriş animasyonu yok (uzun sohbette kasma yapıyordu)
                        MessageItem(m, ctx, tts)
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

            if (!state.modelReady) {
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp)).clickable { onOpenModels() },
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Key, null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("Yapay zekayı kullanmak için sağlayıcı kur",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ChevronRight, null,
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (state.agentMode) {
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp)).clickable { folderPicker.launch(null) },
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.SmartToy, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ajan açık · ${state.projectLabel.ifBlank { "Uygulama deposu" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f))
                        Text("Klasör değiştir",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        "stop" -> FilledIconButton(onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            vm.stop()
                        }) {
                            Icon(Icons.Filled.Stop, "Durdur")
                        }
                        "mic" -> FilledIconButton(onClick = { }) {
                            Icon(Icons.Outlined.Mic, "Sesli")
                        }
                        else -> FilledIconButton(onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            vm.send()
                        }) {
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
            agentMode = state.agentMode,
            onToggleAgent = vm::toggleAgent,
            onToggleSkill = vm::toggleSkill,
            onPickAction = { a ->
                showQuickActions = false
                when (a) {
                    "project" -> folderPicker.launch(null)
                    "permissions" -> onOpenPermissions()
                }
            },
            onDismiss = { showQuickActions = false }
        )
    }

    // Dosya yazma onayı (ajan modu)
    state.pendingApproval?.let { ap ->
        AlertDialog(
            onDismissRequest = { /* bilinçli seçim şart: boş bırakıldı */ },
            title = { Text("Dosya işlemine izin verilsin mi?") },
            text = {
                Column {
                    Text(ap.call.name,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(ap.preview, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.approveTool(true) }) { Text("Onayla") }
            },
            dismissButton = {
                TextButton(onClick = { vm.approveTool(false) }) { Text("Reddet") }
            }
        )
    }
}

@Composable
private fun MessageItem(
    m: UiMessage,
    ctx: Context,
    tts: android.speech.tts.TextToSpeech?
) {
    if (m.isUser) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            val userShape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 4.dp,
                bottomStart = 16.dp, bottomEnd = 16.dp
            )
            Box(
                Modifier.widthIn(max = 320.dp)
                    .background(MaterialTheme.colorScheme.primary, userShape)
                    .clip(userShape)
                    .clickable {
                        // Kendi mesajına dokun → kopyala
                        (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                            .setPrimaryClip(ClipData.newPlainText("chat", m.content))
                        android.widget.Toast.makeText(ctx, "Kopyalandı",
                            android.widget.Toast.LENGTH_SHORT).show()
                    }
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
        // Thinking paneli (araç çalışırken de görünür)
        if (m.thinking.isNotBlank() || m.streaming || m.toolLabel != null) {
            val ss = when {
                m.toolLabel != null -> StreamState.Streaming
                !m.streaming -> StreamState.Done
                m.thinking.isBlank() && m.content.isBlank() ->
                    if (m.totalMs < 1000) StreamState.Connecting
                    else StreamState.WaitingFirstEvent
                else -> StreamState.Streaming
            }
            ThinkingPanel(
                thinking = m.thinking,
                thinkingMs = m.thinkingMs,
                isStreaming = m.streaming,
                streamState = ss,
                streamMs = m.totalMs,
                toolLabel = m.toolLabel
            )
            Spacer(Modifier.height(8.dp))
        }

        // Cevap (balonsuz, tam genişlik — referans tarzı)
        val showResponse = m.content.isNotBlank() || !m.thinkingStreaming
        if (showResponse) {
                if (m.content.isEmpty() && m.streaming) {
                    // Animasyonlu "yazıyor" göstergesi + canlı geçen süre
                    val inf = rememberInfiniteTransition(label = "typing")
                    val ph by inf.animateFloat(0f, 3f,
                        infiniteRepeatable(tween(1200, easing = LinearEasing),
                            RepeatMode.Restart),
                        label = "ph")
                    // Canlı süre sayacı (1 sn'de bir güncellenir)
                    var ms by remember(m.id) { mutableLongStateOf(0L) }
                    LaunchedEffect(m.id) {
                        val t0 = System.currentTimeMillis()
                        while (true) {
                            kotlinx.coroutines.delay(50)
                            ms = System.currentTimeMillis() - t0
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(3) { i ->
                            val a = 0.25f + 0.75f *
                                (0.5f + 0.5f * kotlin.math.sin((ph - i) * 2.094f))
                            Box(
                                Modifier.size(8.dp).clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = a)
                                    )
                            )
                            if (i < 2) Spacer(Modifier.width(6.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("yazıyor… %.1f sn".format(ms / 1000.0),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text(
                        text = m.content.ifEmpty { "⚠ Model boş yanıt döndü" },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.SansSerif,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
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
                    android.widget.Toast.makeText(ctx, "Kopyalandı",
                        android.widget.Toast.LENGTH_SHORT).show()
                }
                MsgAction(Icons.Outlined.Share) {
                    val i = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, m.content)
                    }
                    ctx.startActivity(Intent.createChooser(i, "Paylaş"))
                }
                MsgAction(Icons.Outlined.VolumeUp) {
                    val t = tts
                    if (t != null && m.content.isNotBlank()) {
                        t.stop()
                        t.speak(m.content, android.speech.tts.TextToSpeech.QUEUE_FLUSH,
                            null, "redhawk")
                    }
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

package com.redhawk.code.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.redhawk.code.data.prefs.PrefsStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizationScreen(prefs: PrefsStore, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val custom by prefs.customInstructions.collectAsState(initial = "")
    val localMem by prefs.localMemory.collectAsState(initial = false)
    val toolMem by prefs.toolMemory.collectAsState(initial = false)
    val personality by prefs.personality.collectAsState(initial = "balanced")

    var text by remember { mutableStateOf("") }
    LaunchedEffect(custom) { text = custom }

    Scaffold(
        topBar = { TopAppBar(
            title = { Text("Kişiselleştirme") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background)) }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text("Özel talimatlar", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Text("Bu cihazdaki ReDHawK AI isteklerine eklenecek talimatlar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("ReDHawK AI'ın nasıl çalışmasını istediğinizi yazın…") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = { scope.launch { prefs.setCustomInstructions(text) } }) {
                    Text("Kaydet")
                }
            }

            Spacer(Modifier.height(20.dp))

            SettingToggle(Icons.Outlined.Psychology, "Yerel belleği etkinleştir",
                "AI'ın izin verilen yapısal etkileşim ipuçlarını hatırlamasına izin ver",
                localMem) { scope.launch { prefs.setLocalMemory(it) } }

            SettingToggle(Icons.Outlined.Hub, "Araç belleğine izin ver",
                "Ajanın hassas olmayan saklı çalışma alanı tercihlerini kullanmasına izin ver",
                toolMem) { scope.launch { prefs.setToolMemory(it) } }

            SettingRow(Icons.Outlined.Delete, "Yerel belleği sil",
                "Bu cihazdaki yapısal ipuçlarını kaldır",
                trailingText = "Sil") { }

            SettingRow(Icons.Outlined.AutoAwesome, "Kişilik",
                "Varsayılan yanıt tonunu seç",
                trailingText = when (personality) {
                    "concise" -> "Kısa"; "detailed" -> "Detaylı"; else -> "Dengeli"
                }) {
                scope.launch {
                    val next = when (personality) {
                        "balanced" -> "concise"; "concise" -> "detailed"; else -> "balanced"
                    }
                    prefs.setPersonality(next)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

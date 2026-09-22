package com.redhawk.code.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.redhawk.code.data.prefs.PrefsStore
import kotlinx.coroutines.launch

/**
 * Ayarlar — buradaki HER şey çalışır durumda:
 * model davranışı (sistem promptu, sıcaklık, token) doğrudan sohbete uygulanır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: PrefsStore,
    onBack: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenPersonalization: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenLicenses: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val fastMode by prefs.fastMode.collectAsState(initial = false)
    val showContext by prefs.showContextUsage.collectAsState(initial = true)
    val suggested by prefs.suggestedPrompts.collectAsState(initial = true)
    val temperature by prefs.temperature.collectAsState(initial = 0.7f)
    val maxTokens by prefs.maxTokens.collectAsState(initial = 2048)
    val systemPrompt by prefs.systemPrompt.collectAsState(
        initial = PrefsStore.DEFAULT_SYSTEM_PROMPT
    )

    var showPromptDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text("ReDHawK Code'u bu cihaz için özelleştirin.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))

            SectionTitle("Model Davranışı")
            SettingRow(Icons.Outlined.Psychology, "Sistem promptu",
                "Yapay zekanın temel talimatı — sohbete anında uygulanır",
                trailingText = "Düzenle",
                onClick = { showPromptDialog = true })

            SliderRow(
                icon = Icons.Outlined.Thermostat,
                title = "Sıcaklık",
                subtitle = "Düşük = tutarlı, yüksek = yaratıcı",
                valueLabel = String.format("%.1f", temperature),
                value = temperature,
                range = 0f..1f,
                onChange = { scope.launch { prefs.setTemperature(it) } }
            )

            SliderRow(
                icon = Icons.Outlined.TextFields,
                title = "Maksimum token",
                subtitle = "Tek yanıtta üretilecek en fazla token",
                valueLabel = "$maxTokens",
                value = maxTokens.toFloat(),
                range = 256f..8192f,
                steps = 31,
                onChange = { scope.launch { prefs.setMaxTokens(it.toInt()) } }
            )

            Spacer(Modifier.height(24.dp))

            SectionTitle("Sohbet")
            SettingToggle(Icons.Outlined.Psychology, "Context penceresini göster",
                "Context kullanımını sohbetin yanında görünür tutun",
                showContext) { scope.launch { prefs.setShowContextUsage(it) } }

            SettingToggle(Icons.Filled.Bolt, "Hızlı mod",
                "Daha hızlı yanıt için düşük akıl yürütme kullan",
                fastMode) { scope.launch { prefs.setFastMode(it) } }

            SettingToggle(Icons.Outlined.AutoAwesome, "Önerilen promptlar",
                "Yazarken komut ve beceri önerilerini göster",
                suggested) { scope.launch { prefs.setSuggestedPrompts(it) } }

            Spacer(Modifier.height(24.dp))

            SectionTitle("Görünüm")
            SettingRow(Icons.Outlined.Brush, "Tema",
                "Sistem, açık veya koyu tema",
                trailingText = "Değiştir", onClick = onOpenAppearance)
            SettingRow(Icons.Outlined.Settings, "Diğer görünüm seçenekleri",
                "Kenar çubuğu, kontrast, yazı tipi boyutları",
                trailingText = "Aç", onClick = onOpenAppearance)

            Spacer(Modifier.height(24.dp))

            SectionTitle("Kişiselleştirme")
            SettingRow(Icons.Outlined.AutoAwesome, "Özel talimatlar ve hafıza",
                "Kişisel AI davranışı, bellek, kişilik",
                trailingText = "Aç", onClick = onOpenPersonalization)

            Spacer(Modifier.height(24.dp))

            SectionTitle("Bildirimler")
            SettingRow(Icons.Outlined.Notifications, "Bildirim tercihleri",
                "Tur, izin, soru ve tamamlanma bildirimleri",
                trailingText = "Aç", onClick = onOpenNotifications)

            Spacer(Modifier.height(24.dp))

            SectionTitle("Hesap ve kullanım")
            SettingRow(Icons.Outlined.Person, "Hesap ve istatistikler",
                "Token, sohbet ve etkinlik bilgileri",
                trailingText = "Aç", onClick = onOpenAccount)

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showPromptDialog) {
        var text by remember { mutableStateOf(systemPrompt) }
        AlertDialog(
            onDismissRequest = { showPromptDialog = false },
            title = { Text("Sistem promptu") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { prefs.setSystemPrompt(text) }
                    showPromptDialog = false
                }) { Text("Kaydet") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        scope.launch { prefs.setSystemPrompt(PrefsStore.DEFAULT_SYSTEM_PROMPT) }
                        showPromptDialog = false
                    }) { Text("Varsayılan") }
                    TextButton(onClick = { showPromptDialog = false }) { Text("İptal") }
                }
            }
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailingText != null) {
            Text(trailingText, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary)
        }
        Icon(Icons.Filled.ChevronRight, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp))
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
}

@Composable
fun SettingToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
}

@Composable
private fun SliderRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onChange: (Float) -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(valueLabel, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary)
    }
    Slider(
        value = value,
        onValueChange = onChange,
        valueRange = range,
        steps = steps,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
}

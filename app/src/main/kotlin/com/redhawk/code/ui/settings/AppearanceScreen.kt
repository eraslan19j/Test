package com.redhawk.code.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
fun AppearanceScreen(prefs: PrefsStore, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val theme by prefs.theme.collectAsState(initial = "dark")
    val sidebar by prefs.transparentSidebar.collectAsState(initial = true)
    val contrast by prefs.contrast.collectAsState(initial = 50)
    val cursor by prefs.showCursorMarks.collectAsState(initial = false)
    val reduceMotion by prefs.reduceMotion.collectAsState(initial = false)
    val uiFont by prefs.uiFontSize.collectAsState(initial = 14)
    val codeFont by prefs.codeFontSize.collectAsState(initial = 13)
    val diffStyle by prefs.diffStyle.collectAsState(initial = "color")

    Scaffold(
        topBar = { TopAppBar(
            title = { Text("Görünüm") },
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
            Spacer(Modifier.height(8.dp))

            SettingRow(Icons.Outlined.Brush, "Tema", "Sistem, açık veya koyu",
                trailingText = theme.replaceFirstChar { it.uppercase() }) {
                scope.launch {
                    val next = when (theme) { "system" -> "light"; "light" -> "dark"; else -> "system" }
                    prefs.setTheme(next)
                }
            }

            SettingToggle(Icons.Outlined.Layers, "Yarı saydam kenar çubuğu",
                "Gezinme yüzeyinin arka planla birleşmesine izin ver",
                sidebar) { scope.launch { prefs.setTransparentSidebar(it) } }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Tune, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Kontrast", fontWeight = FontWeight.Medium)
                }
                Text("$contrast", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = contrast.toFloat(),
                onValueChange = { scope.launch { prefs.setContrast(it.toInt()) } },
                valueRange = 0f..100f,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            Spacer(Modifier.height(16.dp))
            SectionTitle("Tercihler")

            SettingToggle(Icons.Outlined.Code, "İmleç işaretlerini kullan",
                "Düzenleyici imlecini ve aktif satır vurgula",
                cursor) { scope.launch { prefs.setShowCursorMarks(it) } }

            SettingToggle(Icons.Outlined.Speed, "Hareketi azalt",
                "Daha kısa geçişler ve daha az giriş animasyonu kullan",
                reduceMotion) { scope.launch { prefs.setReduceMotion(it) } }

            Spacer(Modifier.height(12.dp))
            FontSizeSlider("Arayüz yazı tipi boyutu", uiFont) {
                scope.launch { prefs.setUiFontSize(it) }
            }
            FontSizeSlider("Kod yazı tipi boyutu", codeFont) {
                scope.launch { prefs.setCodeFontSize(it) }
            }

            SettingRow(Icons.Outlined.FormatListNumbered, "Diff stili",
                "Değişiklikleri renk veya +/- işaretleriyle göster",
                trailingText = if (diffStyle == "color") "Renk" else "İşaret") {
                scope.launch {
                    prefs.setDiffStyle(if (diffStyle == "color") "marks" else "color")
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun FontSizeSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.FormatSize, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium)
        }
        Text("$value sp", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary)
    }
    Slider(
        value = value.toFloat(),
        onValueChange = { onChange(it.toInt()) },
        valueRange = 10f..22f,
        steps = 11,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
}

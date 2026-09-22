package com.redhawk.code.ui.provider

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.redhawk.code.data.db.ProviderEntity
import com.redhawk.code.data.provider.ProviderCatalog
import com.redhawk.code.data.provider.ProviderTemplate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSetupScreen(
    editing: ProviderEntity? = null,
    onBack: () -> Unit,
    onSaveNew: (template: ProviderTemplate, apiKey: String, baseUrl: String, model: String, name: String) -> Unit,
    onSaveEdit: (id: String, name: String, baseUrl: String, key: String, model: String) -> Unit,
    onOpenList: () -> Unit
) {
    val ctx = LocalContext.current
    val isEdit = editing != null
    var picked by remember { mutableStateOf<ProviderTemplate?>(null) }
    var apiKey by remember { mutableStateOf(editing?.apiKey ?: "") }
    var baseUrl by remember { mutableStateOf(editing?.baseUrl ?: "") }
    var model by remember { mutableStateOf(editing?.model ?: "") }
    var name by remember { mutableStateOf(editing?.displayName ?: "") }

    LaunchedEffect(picked) {
        picked?.let {
            apiKey = ""
            baseUrl = it.defaultBaseUrl
            model = it.defaultModel
            name = it.displayName
        }
    }

    fun openUrl(url: String) {
        if (url.isBlank()) return
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Throwable) {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (isEdit) "Sağlayıcıyı Düzenle" else "Sağlayıcı Ekle",
                            style = MaterialTheme.typography.titleMedium)
                        Text("ReDHawK AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
                },
                actions = {
                    if (!isEdit) TextButton(onClick = onOpenList) { Text("Kayıtlılar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (!isEdit) {
                Text("ÖNERİLEN — ÜCRETSİZ TIER",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Her sağlayıcı için 'API Key Al' butonu seni doğrudan key alma sayfasına götürür.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))

                ProviderCatalog.presets.forEach { p ->
                    PresetCard(p, p == picked, onPick = { picked = p },
                        onOpenKey = { openUrl(p.keyUrl) })
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(20.dp))
                Text("DİĞER SAĞLAYICILAR",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))

                val chunks = ProviderCatalog.types.chunked(2)
                chunks.forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { t ->
                            ProviderTypeCard(t, t == picked, Modifier.weight(1f)) { picked = t }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                }
            } else {
                Surface(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lightbulb, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Düzenleme modu",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Form
            val showForm = if (isEdit) true else picked != null
            if (showForm) {
                Spacer(Modifier.height(20.dp))
                val activeKeyUrl = if (isEdit) "" else picked?.keyUrl ?: ""
                if (activeKeyUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = { openUrl(activeKeyUrl) },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.OpenInNew, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("API KEY AL", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(activeKeyUrl,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                }

                Surface(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                    color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(14.dp)) {
                        OutlinedTextField(value = name, onValueChange = { name = it },
                            label = { Text("Görünen ad") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp), singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = apiKey, onValueChange = { apiKey = it },
                            label = { Text("API Anahtarı") },
                            placeholder = { Text("sk-...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp), singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it },
                            label = { Text("Uç nokta URL") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp), singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = model, onValueChange = { model = it },
                            label = { Text("Model ID") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp), singleLine = true)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (isEdit) onSaveEdit(editing!!.id, name, baseUrl, apiKey, model)
                        else picked?.let { onSaveNew(it, apiKey, baseUrl, model, name) }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = name.isNotBlank() && baseUrl.isNotBlank() &&
                        model.isNotBlank() && apiKey.isNotBlank()
                ) {
                    Text(if (isEdit) "KAYDET" else "KAYDET VE BAŞLA",
                        fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun PresetCard(
    p: ProviderTemplate,
    selected: Boolean,
    onPick: () -> Unit,
    onOpenKey: () -> Unit
) {
    Surface(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surface,
        border = if (selected) androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (p.isFree) {
                    Surface(shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) {
                        Text("ÜCRETSİZ",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Text(p.displayName,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(4.dp))
            Text(p.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPick,
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = if (selected) ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) else ButtonDefaults.buttonColors()
                ) {
                    Text(if (selected) "SEÇİLDİ" else "SEÇ", fontWeight = FontWeight.SemiBold)
                }
                if (p.keyUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = onOpenKey,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(Icons.Filled.OpenInNew, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("API KEY AL", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (p.freeHint.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lightbulb, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(p.freeHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun ProviderTypeCard(
    t: ProviderTemplate, selected: Boolean, modifier: Modifier, onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).clickable { onClick() },
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(Modifier.padding(12.dp)) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center) {
                Text(t.displayName.split(" ").first().take(2).uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(t.displayName, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
    }
}

package com.redhawk.code.ui.permissions

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.redhawk.code.agent.PermissionCatalog
import com.redhawk.code.agent.PermissionItem
import com.redhawk.code.agent.PermissionProfile
import com.redhawk.code.ui.chat.ChatViewModel

/**
 * Ajan izinleri — GERÇEK: KAYDET'e basılınca ChatViewModel üzerinden
 * DataStore'a yazılır ve ajan döngüsü her araç çağrısında denetler.
 * Profil değişince izinler hazır sete döner; tek tek de oynanabilir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(
    vm: ChatViewModel,
    onBack: () -> Unit
) {
    val profile by vm.agentProfile.collectAsState()
    val perms by vm.agentPerms.collectAsState()
    val chmod by vm.agentChmod.collectAsState()
    val ctx = LocalContext.current

    // Taslak (kaydedilmeden çıkılırsa atılır)
    var dProfile by remember { mutableStateOf(profile) }
    var dPerms by remember { mutableStateOf(perms) }
    var dChmod by remember { mutableStateOf(chmod) }
    var dirty by remember { mutableStateOf(false) }
    LaunchedEffect(profile, perms, chmod) {
        if (!dirty) {
            dProfile = profile
            dPerms = perms
            dChmod = chmod
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajan izinleri") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) { Text("İPTAL") }
                    Button(
                        onClick = {
                            val clean = dChmod.filter { it in '0'..'7' }
                                .padEnd(3, '7').take(3)
                            vm.saveAgentPerms(dProfile, dPerms, clean)
                            Toast.makeText(ctx, "İzinler kaydedildi",
                                Toast.LENGTH_SHORT).show()
                            onBack()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("KAYDET") }
                }
            }
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("🛡 Ajan izinleri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Text("Araç erişim kontrolü",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            // === PROFİL SEÇİCİ ===
            Text("Basit İzin Profili",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PermissionProfile.values().forEach { p ->
                    ProfileChip(p, p == dProfile) {
                        dProfile = p
                        dPerms = PermissionCatalog.defaultFor(p)
                        if (p == PermissionProfile.FULL) dChmod = "777"
                        dirty = true
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // === İZİN LİSTESİ ===
            PermissionCatalog.items.forEach { item ->
                PermissionRow(
                    item = item,
                    checked = item.id in dPerms,
                    onToggle = {
                        dPerms = if (it) dPerms + item.id else dPerms - item.id
                        dirty = true
                    }
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(20.dp))

            // === CHMOD BÖLÜMÜ ===
            Surface(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Gelişmiş POSIX (chmod) Ayarları",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            Text("Terminal erişimi (chmod)",
                                fontWeight = FontWeight.SemiBold)
                            Text("Sahip bazında okuma, yazma ve çalıştırma izni ver. " +
                                "Tam erişim (777) verilirse AI soru sormadan otomatik devam eder.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(dChmod,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Okuma / Yazma / Çalıştırma tablosu
                    Row(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.width(80.dp))
                        Column(Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Okuma", style = MaterialTheme.typography.labelSmall)
                        }
                        Column(Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Yazma", style = MaterialTheme.typography.labelSmall)
                        }
                        Column(Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Çalıştırma", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    ChmodRow("Kullanıcı", dChmod, 0) {
                        dChmod = updateChmod(dChmod, 0, it); dirty = true
                    }
                    ChmodRow("Grup", dChmod, 1) {
                        dChmod = updateChmod(dChmod, 1, it); dirty = true
                    }
                    ChmodRow("Diğer", dChmod, 2) {
                        dChmod = updateChmod(dChmod, 2, it); dirty = true
                    }

                    Spacer(Modifier.height(16.dp))
                    Surface(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${permissionsToRwx(dChmod)} · $dChmod · ${permissionName(dChmod)}",
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ProfileChip(
    profile: PermissionProfile,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { onClick() },
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
            Text(profile.icon)
            Spacer(Modifier.width(6.dp))
            Text(profile.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
private fun PermissionRow(
    item: PermissionItem,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(item.icon, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.SemiBold)
                Text(item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Checkbox(checked = checked, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun ChmodRow(
    label: String,
    chmod: String,
    role: Int,
    onToggle: (Int) -> Unit
) {
    // role: 0=user, 1=group, 2=other; digit: 0-7 arası bitmask
    val digit = chmod.getOrNull(role)?.digitToIntOrNull() ?: 7
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(80.dp),
            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        repeat(3) { bit ->
            // bit: 0=read(4), 1=write(2), 2=exec(1)
            val flag = 1 shl (2 - bit)
            val on = (digit and flag) != 0
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Checkbox(
                    checked = on,
                    onCheckedChange = { isOn ->
                        val newDigit = if (isOn) digit or flag else digit and flag.inv()
                        onToggle(newDigit)
                    }
                )
            }
        }
    }
}

private fun updateChmod(chmod: String, role: Int, newDigit: Int): String {
    val chars = chmod.padEnd(3, '7').toCharArray()
    chars[role] = newDigit.toString()[0]
    return String(chars)
}

private fun permissionsToRwx(chmod: String): String = chmod.map { c ->
    val d = c.digitToIntOrNull() ?: 7
    buildString {
        append(if (d and 4 != 0) 'r' else '-')
        append(if (d and 2 != 0) 'w' else '-')
        append(if (d and 1 != 0) 'x' else '-')
    }
}.joinToString("")

private fun permissionName(chmod: String): String = when (chmod) {
    "777" -> "Tam erişim"
    "755" -> "Sahip tam, diğerleri oku+çalıştır"
    "644" -> "Sahip oku+yaz, diğerleri oku"
    "700" -> "Sadece sahip"
    else -> "Özel"
}

package com.redhawk.code.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.redhawk.code.data.skills.SkillCatalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionsSheet(
    enabledSkills: Set<String>,
    agentMode: Boolean,
    onToggleAgent: () -> Unit,
    onToggleSkill: (String) -> Unit,
    onPickAction: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                Modifier
                    .padding(12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            // Başlık
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Hızlı İşlemler & Araçlar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, null)
                }
            }
            Spacer(Modifier.height(12.dp))

            // Çalışan kartlar: proje klasörü + ajan modu
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickCard(
                    title = "Çalışma Alanı",
                    subtitle = "Proje klasörünü seç",
                    icon = Icons.Outlined.Folder,
                    modifier = Modifier.weight(1f)
                ) { onPickAction("project") }

                QuickCard(
                    title = "Ajan Modu",
                    subtitle = if (agentMode) "Açık — kapatmak için dokun" else "Kapalı — açmak için dokun",
                    icon = Icons.Outlined.SmartToy,
                    modifier = Modifier.weight(1f),
                    highlight = agentMode
                ) { onToggleAgent() }
            }

            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickCard(
                    title = "Ajan İzinleri",
                    subtitle = "Araç erişim kontrolü",
                    icon = Icons.Outlined.AdminPanelSettings,
                    modifier = Modifier.weight(1f)
                ) { onPickAction("permissions") }
            }

            Spacer(Modifier.height(20.dp))

            // Tasarım & Kod Motorları
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Tune, null,
                        Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Tasarım & Kod Motorları", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Seçili motorlar kod üretimine doğrudan uygulanır",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        "${enabledSkills.size} / ${SkillCatalog.all.size} Aktif",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    SkillCatalog.all.forEach { if (it.id !in enabledSkills) onToggleSkill(it.id) }
                }) { Text("Tümünü Aç") }
                OutlinedButton(onClick = {
                    enabledSkills.toList().forEach { onToggleSkill(it) }
                }) { Text("Tümünü Sıfırla") }
            }

            Spacer(Modifier.height(12.dp))

            // Skill listesi
            LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(SkillCatalog.all, key = { it.id }) { s ->
                    SkillCard(
                        title = s.title,
                        badge = s.badge,
                        description = s.description,
                        enabled = s.id in enabledSkills,
                        onToggle = { onToggleSkill(s.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant,
        border = if (highlight) androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SkillCard(
    title: String,
    badge: String,
    description: String,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        color = if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surfaceVariant,
        border = if (enabled) androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(badge,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = enabled, onCheckedChange = { onToggle() })
        }
    }
}

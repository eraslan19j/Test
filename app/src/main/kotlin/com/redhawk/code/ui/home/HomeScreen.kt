package com.redhawk.code.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.redhawk.code.R

data class HomeItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    onOpenChat: () -> Unit = {},
    onOpenChats: () -> Unit = {},
    onOpenAgent: () -> Unit = {},
    onOpenModels: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenNativeTest: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
) {
    val items = listOf(
        HomeItem("AI Sohbet", "Sağlayıcı ile konuş",
            Icons.Outlined.Chat, onOpenChat),
        HomeItem("Son Sohbetler", "Geçmiş konuşmaları görüntüle",
            Icons.Outlined.History, onOpenChats),
        HomeItem("Ajan Çalışma Alanı", "Projeye duyarlı AI asistanı",
            Icons.Outlined.SmartToy, onOpenAgent),
        HomeItem("Sağlayıcılar", "Aktif sağlayıcıyı değiştir",
            Icons.Outlined.Memory, onOpenModels),
        HomeItem("Ayarlar", "Dil, tema ve sistem tercihleri",
            Icons.Outlined.Settings, onOpenSettings),
    )

    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Filled.Menu, "Menü")
            }
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(40.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.redhawk_round),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("ReDHawK Code", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Text("YAPAY ZEKA · AJAN ÇALIŞMA ALANI",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(32.dp))

        Text("ReDHawK Code",
            style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(20.dp))
        Text("Çalışma alanın",
            style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
        Text("İhtiyacın olan her şey, sakin ve tek bir yerde.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(20.dp))

        items.forEach { HomeCard(it); Spacer(Modifier.height(10.dp)) }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun HomeCard(item: HomeItem) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .clickable { item.onClick() },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center) {
                Icon(item.icon, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(item.subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

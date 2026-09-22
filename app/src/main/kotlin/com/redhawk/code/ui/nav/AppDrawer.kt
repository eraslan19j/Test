package com.redhawk.code.ui.nav

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

data class DrawerItem(val label: String, val icon: ImageVector, val route: String)

@Composable
fun AppDrawer(
    currentRoute: String,
    onSelect: (String) -> Unit,
    onClose: () -> Unit
) {
    val groups = listOf(
        "NAVIGATION" to listOf(
            DrawerItem("Ana ekran", Icons.Outlined.Home, "home"),
            DrawerItem("AI Sohbet", Icons.Outlined.Chat, "chat"),
            DrawerItem("Son Sohbetler", Icons.Outlined.History, "chatlist"),
        ),
        "MODEL VE DİL" to listOf(
            DrawerItem("Sağlayıcılar", Icons.Outlined.Memory, "providers"),
            DrawerItem("Yeni Sağlayıcı", Icons.Outlined.Add, "provider_setup"),
        ),
        "SİSTEM" to listOf(
            DrawerItem("Ayarlar", Icons.Outlined.Settings, "settings"),
        ),
    )

    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.background
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.redhawk_round),
                        contentDescription = null,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("ReDHawK Code",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text("AJAN ÇALIŞMA ALANI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(24.dp))

            groups.forEach { (title, items) ->
                Text(title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 6.dp))
                items.forEach { item ->
                    DrawerRow(item, item.route == currentRoute) {
                        onSelect(item.route)
                        onClose()
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun DrawerRow(item: DrawerItem, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
             else MaterialTheme.colorScheme.background
    val fg = if (selected) MaterialTheme.colorScheme.primary
             else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(bg).clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(item.icon, null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(item.label, color = fg, style = MaterialTheme.typography.bodyMedium)
    }
}

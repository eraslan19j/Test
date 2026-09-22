package com.redhawk.code.ui.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.redhawk.code.data.db.ChatEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
    chats: List<ChatEntity>,
    onOpen: (ChatEntity) -> Unit,
    onNew: () -> Unit,
    onRename: (ChatEntity, String) -> Unit,
    onDelete: (ChatEntity) -> Unit,
    onPin: (ChatEntity) -> Unit,
    onStar: (ChatEntity) -> Unit,
    onArchive: (ChatEntity) -> Unit,
    onOpenDrawer: () -> Unit
) {
    var renameTarget by remember { mutableStateOf<ChatEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<ChatEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Son Sohbetler") },
                navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Filled.Menu, null) } },
                actions = {
                    IconButton(onClick = onNew) { Icon(Icons.Filled.Add, "Yeni") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Yeni sohbet") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { pad ->
        if (chats.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.ChatBubbleOutline, null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("Henüz sohbet yok", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Yeni bir sohbet başlat",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                Modifier.padding(pad).fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(chats, key = { it.id }) { c ->
                    ChatRow(
                        c,
                        onClick = { onOpen(c) },
                        onLongPress = { deleteTarget = c },
                        onRename = { renameTarget = c },
                        onPin = { onPin(c) },
                        onStar = { onStar(c) },
                        onArchive = { onArchive(c) }
                    )
                }
            }
        }
    }

    renameTarget?.let { c ->
        var text by remember(c.id) { mutableStateOf(c.title) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Sohbeti yeniden adlandır") },
            text = { OutlinedTextField(value = text, onValueChange = { text = it }) },
            confirmButton = {
                TextButton(onClick = {
                    if (text.isNotBlank()) onRename(c, text.trim())
                    renameTarget = null
                }) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("İptal") } }
        )
    }

    deleteTarget?.let { c ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Sohbeti sil?") },
            text = { Text("\"${c.title}\" kalıcı olarak silinecek.") },
            confirmButton = {
                TextButton(onClick = { onDelete(c); deleteTarget = null }) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("İptal") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatRow(
    c: ChatEntity,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onRename: () -> Unit,
    onPin: () -> Unit,
    onStar: () -> Unit,
    onArchive: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("dd MMM HH:mm", Locale("tr")) }

    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (c.pinned) Icons.Filled.PushPin else Icons.Outlined.ChatBubbleOutline,
                    null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.title, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f), maxLines = 1)
                    if (c.starred) Icon(Icons.Filled.Star, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                }
                Text(
                    "${c.modelId} · ${fmt.format(Date(c.updatedAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, "Menü",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Yeniden adlandır") }, onClick = { menuOpen = false; onRename() },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null) })
                    DropdownMenuItem(text = { Text(if (c.pinned) "Sabitlemeyi kaldır" else "Sabitle") },
                        onClick = { menuOpen = false; onPin() },
                        leadingIcon = { Icon(Icons.Outlined.PushPin, null) })
                    DropdownMenuItem(text = { Text(if (c.starred) "Yıldızı kaldır" else "Yıldızla") },
                        onClick = { menuOpen = false; onStar() },
                        leadingIcon = { Icon(Icons.Outlined.Star, null) })
                    DropdownMenuItem(text = { Text("Arşivle") }, onClick = { menuOpen = false; onArchive() },
                        leadingIcon = { Icon(Icons.Outlined.Archive, null) })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Sil", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; onLongPress() },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) })
                }
            }
        }
    }
}

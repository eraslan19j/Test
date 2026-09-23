package com.redhawk.code.ui.files

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.redhawk.code.agent.ProjectFiles
import com.redhawk.code.ui.chat.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class FileEntry(val name: String, val isDir: Boolean, val size: String)

private fun parseEntry(line: String): FileEntry? = when {
    line.startsWith("[D] ") -> FileEntry(line.removePrefix("[D] "), true, "")
    line.startsWith("[F] ") -> {
        val rest = line.removePrefix("[F] ")
        val size = rest.substringBefore(' ')
        val name = rest.substringAfter(' ')
        if (name.isEmpty()) null else FileEntry(name, false, size)
    }
    else -> null
}

/**
 * Ajan deposu gezgini + metin editörü.
 * Aktif sohbetin klasörünü gösterir (yoksa uygulama deposu).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    vm: ChatViewModel,
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val ctx = LocalContext.current
    val appCtx = remember(ctx) { ctx.applicationContext }
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    var projectUri by remember { mutableStateOf<String?>(null) }
    var rootLabel by remember { mutableStateOf("Uygulama deposu") }
    var path by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<FileEntry>>(emptyList()) }
    var listInfo by remember { mutableStateOf<String?>(null) }
    var openPath by remember { mutableStateOf<String?>(null) }
    var editText by remember { mutableStateOf("") }
    var editOrig by remember { mutableStateOf("") }
    var showNewDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }

    fun refresh(targetUri: String? = projectUri, targetPath: String = path) {
        scope.launch {
            val out = withContext(Dispatchers.IO) {
                ProjectFiles.list(appCtx, targetUri, targetPath)
            }
            when {
                out.startsWith("HATA") -> { items = emptyList(); listInfo = out }
                out == "(boş klasör)" -> { items = emptyList(); listInfo = null }
                else -> {
                    listInfo = null
                    items = out.lines().mapNotNull { parseEntry(it) }
                }
            }
        }
    }

    fun openFile(entry: FileEntry) {
        if (entry.isDir) {
            path = if (path.isEmpty()) entry.name else "$path/${entry.name}"
            refresh(projectUri, path)
            return
        }
        val fp = if (path.isEmpty()) entry.name else "$path/${entry.name}"
        scope.launch {
            val out = withContext(Dispatchers.IO) {
                ProjectFiles.read(appCtx, projectUri, fp)
            }
            if (out.startsWith("HATA")) {
                snack.showSnackbar(out.take(120))
            } else {
                editText = out
                editOrig = out
                openPath = fp
            }
        }
    }

    fun saveFile() {
        val fp = openPath ?: return
        scope.launch {
            val out = withContext(Dispatchers.IO) {
                ProjectFiles.write(appCtx, projectUri, fp, editText)
            }
            snack.showSnackbar(out.take(120))
            if (!out.startsWith("HATA")) editOrig = editText
        }
    }

    fun deletePath(p: String) {
        scope.launch {
            val out = withContext(Dispatchers.IO) {
                ProjectFiles.delete(appCtx, projectUri, p)
            }
            snack.showSnackbar(out.take(120))
            if (openPath == p) openPath = null
            refresh()
        }
    }

    fun createFile(name: String) {
        val clean = name.trim().trim('/')
        if (clean.isEmpty()) return
        val fp = if (path.isEmpty()) clean else "$path/$clean"
        scope.launch {
            val out = withContext(Dispatchers.IO) {
                ProjectFiles.write(appCtx, projectUri, fp, "")
            }
            snack.showSnackbar(out.take(120))
            refresh()
        }
    }

    fun goBack() {
        when {
            openPath != null -> openPath = null
            path.isNotEmpty() -> {
                path = path.substringBeforeLast('/', "")
                refresh(projectUri, path)
            }
            else -> onBack()
        }
    }

    BackHandler { goBack() }

    LaunchedEffect(Unit) {
        val u = vm.activeProjectUri()
        projectUri = u
        rootLabel = ProjectFiles.displayName(appCtx, u)
        refresh(u, "")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Dosyalar", fontWeight = FontWeight.Bold,
                            maxLines = 1)
                        Text(rootLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1)
                    }
                },
                navigationIcon = {
                    if (openPath != null || path.isNotEmpty()) {
                        IconButton(onClick = { goBack() }) {
                            Icon(Icons.Filled.ArrowBack, "Geri")
                        }
                    } else {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Filled.Menu, "Menü")
                        }
                    }
                },
                actions = {
                    if (openPath == null) {
                        IconButton(onClick = { refresh() }) {
                            Icon(Icons.Outlined.Refresh, "Yenile")
                        }
                        IconButton(onClick = { showNewDialog = true }) {
                            Icon(Icons.Outlined.Add, "Yeni dosya")
                        }
                    } else {
                        val dirty = editText != editOrig
                        IconButton(onClick = { showDeleteDialog = openPath }) {
                            Icon(Icons.Outlined.Delete, "Sil")
                        }
                        IconButton(onClick = { saveFile() }, enabled = dirty) {
                            Icon(Icons.Outlined.Check, "Kaydet",
                                tint = if (dirty) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { pad ->
        AnimatedContent(
            targetState = openPath == null,
            transitionSpec = {
                (fadeIn(tween(180)) + slideInHorizontally(tween(220)) { it / 4 }) togetherWith
                    (fadeOut(tween(150)) + slideOutHorizontally(tween(200)) { -it / 4 })
            },
            label = "files"
        ) { showList ->
            if (showList) {
                Column(Modifier.padding(pad).fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.FolderOpen, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (path.isEmpty()) "Kök" else path,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.weight(1f))
                    }
                    if (listInfo != null) {
                        Text(listInfo!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp))
                    } else if (items.isEmpty()) {
                        Column(
                            Modifier.fillMaxSize().padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.FolderOpen, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Bu klasör boş",
                                style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text("Ajan dosya yazınca burada görünür.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(items,
                                key = { (if (it.isDir) "d" else "f") + it.name }) { e ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { openFile(e) },
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Row(Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (e.isDir) Icons.Outlined.Folder
                                            else Icons.Outlined.Description,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(e.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1)
                                            if (!e.isDir && e.size.isNotEmpty()) {
                                                Text(e.size,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Icon(Icons.Outlined.ChevronRight, null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Column(Modifier.padding(pad).fillMaxSize().padding(12.dp)) {
                    Text(openPath ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    if (editText != editOrig) {
                        Text("● Kaydedilmedi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }

    if (showNewDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewDialog = false },
            title = { Text("Yeni dosya") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("örn: notlar/todo.txt") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { showNewDialog = false; createFile(name) }) {
                    Text("Oluştur")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewDialog = false }) { Text("İptal") }
            }
        )
    }

    showDeleteDialog?.let { dp ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Silinsin mi?") },
            text = { Text(dp) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = null; deletePath(dp) }) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Vazgeç") }
            }
        )
    }
}

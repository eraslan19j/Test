package com.redhawk.code.ui.tools

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Hesapsız / ücretsiz AI araçları.
 * Bunlar web sitelerinin kendisidir — uygulama içinden açılır,
 * hesap gerektirenlerde kullanıcı KENDİ hesabıyla giriş yapar.
 */
data class FreeTool(val name: String, val desc: String, val url: String)

private val FREE_TOOLS = listOf(
    FreeTool(
        "Genspark AI",
        "AI arama + ajan çalışma alanı (kendi hesabınla giriş yap)",
        "https://www.genspark.ai/"
    ),
    FreeTool(
        "Video Yükseltici",
        "Hesapsız ücretsiz AI video büyütme",
        "https://free.upscaler.video/"
    ),
    FreeTool(
        "Arena AI",
        "Ücretsiz AI görsel üretimi (kampanya sayfası)",
        "https://arena.ai"
    ),
    FreeTool(
        "Duck.ai Sohbet",
        "Anahtarsız AI sohbet (web)",
        "https://duckduckgo.com/?q=DuckDuckGo+AI+Chat&ia=chat&duckai=1"
    ),
    FreeTool(
        "Pollinations",
        "Anahtarsız AI metin + görsel (web)",
        "https://pollinations.ai/"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeToolsScreen(onBack: () -> Unit, onOpenDrawer: () -> Unit) {
    var current by remember { mutableStateOf<FreeTool?>(null) }
    val tool = current

    BackHandler(enabled = tool != null) { current = null }

    if (tool == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Ücretsiz Araçlar", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) { Icon(Icons.Filled.Menu, null) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { pad ->
            LazyColumn(
                Modifier.fillMaxSize().padding(pad).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Bu araçlar sitelerin kendisidir — uygulama içinden açılır. " +
                            "Hesap isteyenlerde kendi hesabınla giriş yaparsın.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(FREE_TOOLS) { t ->
                    Card(
                        Modifier.fillMaxWidth().clickable { current = t },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                t.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                t.desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    } else {
        ToolViewer(tool = tool, onBack = { current = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolViewer(tool: FreeTool, onBack: () -> Unit) {
    val ctx = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tool.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = {
                        runCatching {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tool.url)))
                        }
                    }) { Icon(Icons.Filled.OpenInNew, "Tarayıcıda aç") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { pad ->
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(pad),
            factory = { c ->
                WebView(c).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webViewClient = WebViewClient()
                    loadUrl(tool.url)
                }
            },
            update = { v -> if (v.url != tool.url) v.loadUrl(tool.url) }
        )
    }
}

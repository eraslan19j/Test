package com.redhawk.code.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.redhawk.code.llm.model.Role

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    baseUrl: String,
    apiKey: String,
    model: String,
    modifier: Modifier = Modifier
) {
    val vm: ChatViewModel = viewModel(
        key = "chat-$baseUrl-$model",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(c: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(baseUrl, apiKey, model) as T
            }
        }
    )
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ReDHawK AI · $model") })
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = vm::onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Mesaj yaz…") },
                    maxLines = 4
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = vm::send,
                    enabled = !state.isStreaming && state.input.isNotBlank()
                ) { Icon(Icons.Default.Send, contentDescription = "Gönder") }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            state.error?.let {
                Text(
                    "Hata: $it",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(state.messages) { _, m ->
                    MessageBubble(m.role, m.content)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(role: Role, text: String) {
    val isUser = role == Role.USER
    val bg = if (isUser) MaterialTheme.colorScheme.primaryContainer
             else MaterialTheme.colorScheme.surfaceVariant
    val align = if (isUser) Alignment.End else Alignment.Start

    Column(Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Box(
            Modifier
                .widthIn(max = 320.dp)
                .background(bg, RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Text(
                text = text.ifEmpty { "…" },
                fontFamily = if (isUser) FontFamily.Default else FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

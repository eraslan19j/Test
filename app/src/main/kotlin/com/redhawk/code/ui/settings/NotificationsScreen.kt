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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.redhawk.code.data.prefs.PrefsStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(prefs: PrefsStore, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val turnDone by prefs.notifyTurnDone.collectAsState(initial = true)
    val perm by prefs.notifyPermission.collectAsState(initial = true)
    val question by prefs.notifyQuestion.collectAsState(initial = true)
    val completion by prefs.notifyCompletion.collectAsState(initial = true)

    Scaffold(
        topBar = { TopAppBar(
            title = { Text("Bildirimler") },
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
            Spacer(Modifier.height(16.dp))

            SettingRow(Icons.Outlined.Check, "Tur tamamlanma bildirimi",
                "Yanıt tamamlandığında bildir", trailingText = "Yalnızca odakta değilken") { }
            SettingToggle(Icons.Outlined.Info, "İzin bildirimleri",
                "Bir işlem onay istediğinde uyarı göster", perm) {
                scope.launch { prefs.setNotifyPermission(it) }
            }
            SettingToggle(Icons.Outlined.Chat, "Soru bildirimleri",
                "Ajan yanıtınızı beklediğinde uyarı göster", question) {
                scope.launch { prefs.setNotifyQuestion(it) }
            }
            SettingToggle(Icons.Outlined.Bolt, "Tamamlanma bildirimleri",
                "Yanıt tamamlanma bildirimlerine izin ver", completion) {
                scope.launch { prefs.setNotifyCompletion(it) }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

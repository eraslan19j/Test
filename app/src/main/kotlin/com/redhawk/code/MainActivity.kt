package com.redhawk.code

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.redhawk.code.data.db.ProviderEntity
import com.redhawk.code.data.prefs.PrefsStore
import com.redhawk.code.data.repo.ProviderRepository
import com.redhawk.code.ui.chat.ChatScreen
import com.redhawk.code.ui.chat.ChatViewModel
import com.redhawk.code.ui.chatlist.ChatsListScreen
import com.redhawk.code.ui.home.HomeScreen
import com.redhawk.code.ui.nav.AppDrawer
import com.redhawk.code.ui.nav.NavStack
import com.redhawk.code.ui.onboarding.OnboardingScreen
import com.redhawk.code.ui.provider.ProviderListScreen
import com.redhawk.code.ui.provider.ProviderSetupScreen
import com.redhawk.code.ui.settings.*
import com.redhawk.code.ui.theme.RedHawkTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(0xFF0A0A0A.toInt()))
        enableEdgeToEdge()

        var ready = false
        splash.setKeepOnScreenCondition { !ready }

        setContent {
            RedHawkTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // İlk render olur olmaz splash'i kapat
                    LaunchedEffect(Unit) { ready = true }
                    MainNav(this)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainNav(activity: ComponentActivity) {
    val ctx = LocalContext.current
    val prefs = remember { PrefsStore(ctx) }
    val providerRepo = remember { ProviderRepository(ctx) }
    val providers by providerRepo.observeAll().collectAsState(initial = emptyList())
    val selectedProviderId by prefs.selectedProviderId.collectAsState(initial = null)
    val theme by prefs.theme.collectAsState(initial = "dark")

    val nav = remember { NavStack("home") }
    val route = nav.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val chatVm: ChatViewModel = viewModel()
    val chats by chatVm.chats.collectAsState(initial = emptyList())

    // null = prefs henüz yükleniyor (boş zemin göster)
    val setupDone by prefs.setupDone.collectAsState(initial = null)

    var editingProvider by remember { mutableStateOf<ProviderEntity?>(null) }
    var onboardingSetup by remember { mutableStateOf(false) }

    // NOT: Eski LaunchedEffect(selectedProviderId, providers) döngüsü kaldırıldı.
    // Sağlayıcı seçimi TEK YÖN: kullanıcı seçer → switchProvider → prefs'e yazar.
    // Açılış yüklemesini ChatViewModel.init zaten yapıyor.

    BackHandler {
        if (!nav.pop()) activity.finish()
    }

    // ---- İlk açılış akışı (sadece setupDone=false iken) ----
    if (setupDone == null) {
        // Prefs yükleniyor: boş zemin göster
        Box(Modifier.fillMaxSize())
        return
    }
    if (!setupDone!! && !onboardingSetup) {
        OnboardingScreen(
            onSetupProvider = { onboardingSetup = true },
            onSkip = { scope.launch { prefs.setSetupDone(true) } }
        )
        return
    }
    if (!setupDone!! && onboardingSetup) {
        ProviderSetupScreen(
            editing = null,
            onBack = { onboardingSetup = false },
            onSaveNew = { template, key, url, model, name ->
                scope.launch {
                    val e = providerRepo.createFromTemplate(template, key, model, url)
                    prefs.setSelectedProvider(e.id)
                    chatVm.switchProvider(e)
                    prefs.setSetupDone(true)
                }
            },
            onSaveEdit = { _, _, _, _, _ -> },
            onOpenList = { }
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                currentRoute = route,
                onSelect = { r -> nav.navigate(r); scope.launch { drawerState.close() } },
                onClose = { scope.launch { drawerState.close() } },
                onNewChat = { chatVm.newChat(); nav.reset("chat") },
                themeLabel = if (theme == "light") "Açık" else "Koyu",
                darkTheme = theme != "light",
                onToggleTheme = {
                    scope.launch { prefs.setTheme(if (theme == "light") "dark" else "light") }
                }
            )
        }
    ) {
        AnimatedContent(
            targetState = route,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "nav"
        ) { r ->
            when (r) {
                "provider_setup" -> ProviderSetupScreen(
                    editing = editingProvider,
                    onBack = { editingProvider = null; nav.pop() },
                    onSaveNew = { template, key, url, model, name ->
                        scope.launch {
                            val e = providerRepo.createFromTemplate(template, key, model, url)
                            prefs.setSelectedProvider(e.id)
                            chatVm.switchProvider(e)
                            editingProvider = null
                            nav.reset("home")
                        }
                    },
                    onSaveEdit = { id, name, url, key, model ->
                        scope.launch {
                            providerRepo.update(id, name, url, key, model)
                            val p = providerRepo.get(id)
                            if (p != null) {
                                chatVm.switchProvider(p)
                                prefs.setSelectedProvider(id)
                            }
                            editingProvider = null
                            nav.pop()
                        }
                    },
                    onOpenList = { nav.navigate("providers") }
                )

                "providers" -> ProviderListScreen(
                    providers = providers,
                    selectedId = selectedProviderId,
                    onBack = { nav.pop() },
                    onAdd = { editingProvider = null; nav.push("provider_setup") },
                    onSelect = { p ->
                        scope.launch {
                            prefs.setSelectedProvider(p.id)
                            chatVm.switchProvider(p)
                            nav.pop()
                        }
                    },
                    onDelete = { p ->
                        scope.launch {
                            providerRepo.delete(p.id)
                            if (selectedProviderId == p.id) prefs.setSelectedProvider(null)
                        }
                    },
                    onEdit = { p -> editingProvider = p; nav.push("provider_setup") }
                )

                "home" -> HomeScreen(
                    onOpenChat = { chatVm.ensureActiveChat(); nav.push("chat") },
                    onOpenChats = { nav.push("chatlist") },
                    onOpenAgent = { chatVm.ensureActiveChat(); nav.push("chat") },
                    onOpenModels = { nav.push("providers") },
                    onOpenSettings = { nav.push("settings") },
                    onOpenNativeTest = { },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )

                "chatlist" -> ChatsListScreen(
                    chats = chats,
                    onOpen = { c -> chatVm.openChat(c.id); nav.push("chat") },
                    onNew = { chatVm.newChat(); nav.push("chat") },
                    onRename = { c, t -> chatVm.renameChat(c.id, t) },
                    onDelete = { c -> chatVm.deleteChat(c.id) },
                    onPin = { c -> chatVm.pinChat(c.id, !c.pinned) },
                    onStar = { c -> chatVm.starChat(c.id, !c.starred) },
                    onArchive = { c -> chatVm.archiveChat(c.id, true) },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )

                "chat" -> ChatScreen(
                    vm = chatVm,
                    onBack = { nav.pop() },
                    onOpenModels = { nav.push("providers") },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onOpenChats = { nav.push("chatlist") },
                    onOpenProject = { },
                    onOpenPermissions = { }
                )

                "settings" -> SettingsScreen(
                    prefs = prefs,
                    onBack = { nav.pop() },
                    onOpenAppearance = { nav.push("settings_appearance") },
                    onOpenPersonalization = { nav.push("settings_personalization") },
                    onOpenNotifications = { nav.push("settings_notifications") },
                    onOpenAccount = { nav.push("settings_account") },
                    onOpenLicenses = { }
                )
                "settings_appearance" -> AppearanceScreen(prefs) { nav.pop() }
                "settings_personalization" -> PersonalizationScreen(prefs) { nav.pop() }
                "settings_notifications" -> NotificationsScreen(prefs) { nav.pop() }
                "settings_account" -> AccountScreen(prefs) { nav.pop() }

                "files" -> com.redhawk.code.ui.files.FilesScreen(
                    vm = chatVm,
                    onBack = { nav.pop() },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )

                else -> Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(r.replaceFirstChar { it.uppercase() }) },
                            navigationIcon = {
                                IconButton(onClick = { nav.pop() }) {
                                    Icon(Icons.Filled.ArrowBack, null)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                ) { pad ->
                    Box(Modifier.padding(pad), contentAlignment = Alignment.Center) {
                        Text("Bu ekran yapım aşamasında")
                    }
                }
            }
        }
    }
}

package com.redhawk.code.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // Logo nabzı
    val pulse = rememberInfiniteTransition(label = "logo")
    val s by pulse.animateFloat(0.94f, 1.06f,
        infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "s")

    Box(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HomeBackground()

        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            AnimatedVisibility(
                visible,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -it / 3 }
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, "Menü")
                    }
                    Spacer(Modifier.width(4.dp))
                    Box(
                        Modifier.size(40.dp).scale(s).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
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
            }

            Spacer(Modifier.height(32.dp))

            AnimatedVisibility(
                visible,
                enter = fadeIn(tween(450, delayMillis = 80)) +
                    slideInVertically(tween(450, delayMillis = 80)) { it / 4 }
            ) {
                Column {
                    Text("ReDHawK Code",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(20.dp))
                    Text("Çalışma alanın",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold)
                    Text("İhtiyacın olan her şey, sakin ve tek bir yerde.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(20.dp))

            items.forEachIndexed { i, item ->
                AnimatedVisibility(
                    visible,
                    enter = fadeIn(tween(450, delayMillis = 150 + i * 90)) +
                        slideInVertically(
                            tween(450, delayMillis = 150 + i * 90,
                                easing = FastOutSlowInEasing)
                        ) { it / 3 }
                ) {
                    HomeCard(item)
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Animasyonlu arka plan: üst ışıma + süzülen iki ışık (ucuz, sadece 2 daire) */
@Composable
private fun HomeBackground() {
    val glow = MaterialTheme.colorScheme.primary
    val inf = rememberInfiniteTransition(label = "homeBg")
    val dx by inf.animateFloat(-40f, 40f,
        infiniteRepeatable(tween(9000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dx")
    val dy by inf.animateFloat(-24f, 48f,
        infiniteRepeatable(tween(13000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dy")
    val dy2 by inf.animateFloat(30f, -30f,
        infiniteRepeatable(tween(11000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dy2")

    Box(Modifier.fillMaxSize()) {
        // Üst cam ışıması (statik)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        glow.copy(alpha = 0.10f),
                        Color.Transparent,
                        Color.Transparent,
                        Color.Transparent
                    )
                )
            )
        )
        // Süzülen ışıklar
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val c1 = center.copy(x = w * 0.85f + dx, y = h * 0.18f + dy)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(glow.copy(alpha = 0.16f), Color.Transparent),
                    center = c1,
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = c1
            )
            val c2 = center.copy(x = w * 0.10f - dx, y = h * 0.75f + dy2)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFF6D00).copy(alpha = 0.10f), Color.Transparent),
                    center = c2,
                    radius = w * 0.7f
                ),
                radius = w * 0.7f,
                center = c2
            )
        }
    }
}

@Composable
private fun HomeCard(item: HomeItem) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .clickable {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                item.onClick()
            },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, null, tint = Color.White)
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

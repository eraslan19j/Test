package com.redhawk.code.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.redhawk.code.R

/**
 * İlk açılış karşılama ekranı.
 * setupDone=false iken gösterilir, bitince bir daha açılmaz.
 */
@Composable
fun OnboardingScreen(
    onSetupProvider: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        // Logo
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.redhawk_icon),
                contentDescription = null,
                modifier = Modifier.size(72.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "ReDHawK Code",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Cebindeki yapay zeka kod asistanı",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        FeatureRow(
            icon = Icons.Filled.Bolt,
            title = "Ücretsiz modellerle çalış",
            desc = "Gemini, Groq veya OpenRouter ücretsiz anahtarını gir, kota senin hesabından gitsin. Uygulama senden para istemez."
        )
        Spacer(Modifier.height(12.dp))
        FeatureRow(
            icon = Icons.Outlined.SmartToy,
            title = "Ajan modu",
            desc = "Yapay zeka dosyalarını okur, kod yazar, projende değişiklik yapar — her adımda senden onay alır."
        )
        Spacer(Modifier.height(12.dp))
        FeatureRow(
            icon = Icons.Outlined.Folder,
            title = "Projelerin cebinde",
            desc = "Çalışma klasörünü seç, sohbet geçmişin cihazında saklansın. Verilerin sende kalır."
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onSetupProvider,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Outlined.Key, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("SAĞLAYICI KUR (2 DAKİKA)", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Şimdilik atla",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, desc: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

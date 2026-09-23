package com.redhawk.code.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Premium ana ekran başlığı: nefes alan büyük logo + solan alt yazı */
@Composable
fun HomeHeader() {
    val inf = rememberInfiniteTransition(label = "header")

    // Yavaş nefes alma efekti
    val pulse by inf.animateFloat(
        0.97f, 1.03f,
        infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    // Alt yazı hafif alpha geçişi
    val subAlpha by inf.animateFloat(
        0.55f, 0.85f,
        infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
        label = "subAlpha"
    )

    Column {
        // Büyük "ReDHawK Code"
        Text(
            "ReDHawK Code",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            modifier = Modifier
                .scale(pulse)
                .graphicsLayer {
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                }
        )

        Spacer(Modifier.height(6.dp))

        // Alt yazı
        Text(
            "YAPAY ZEKA · AJAN ÇALIŞMA ALANI",
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(subAlpha)
        )
    }
}

package com.redhawk.code.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Star(
    val x: Float,       // 0..1 (yatay)
    val y: Float,       // 0..1 (dikey, döngü için)
    val size: Float,    // 0.4..2.4 px
    val speed: Float,   // saniyedeki normalize hız
    val alpha: Float,   // 0.25..0.85
    val drift: Float    // hafif x kayması
)

private data class ShootingStar(
    val startX: Float,
    val startY: Float,
    val length: Float,
    val angleDeg: Float,
    val speed: Float,
    val offset: Float   // 0..1 döngü ofseti
)

/**
 * Ana ekranın arkasındaki kayan yıldızlar.
 *  - Yavaş aşağı kayan sabit yıldızlar (twinkle ile)
 *  - Ara sıra geçen kayan yıldızlar (shooting star)
 */
@Composable
fun StarfieldBackground(
    modifier: Modifier = Modifier,
    starCount: Int = 60,
    shootingStarCount: Int = 3,
    speedMultiplier: Float = 1f
) {
    // Rastgele yıldızlar — remember ile sabit kalır
    val stars = remember(starCount) {
        List(starCount) {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 2f + 0.4f,
                speed = Random.nextFloat() * 0.06f + 0.015f,
                alpha = Random.nextFloat() * 0.6f + 0.25f,
                drift = (Random.nextFloat() - 0.5f) * 0.02f
            )
        }
    }

    val shooting = remember(shootingStarCount) {
        List(shootingStarCount) { i ->
            ShootingStar(
                startX = Random.nextFloat(),
                startY = Random.nextFloat() * 0.4f,
                length = Random.nextFloat() * 0.12f + 0.08f,
                angleDeg = Random.nextFloat() * 30f + 20f,   // 20-50 derece
                speed = Random.nextFloat() * 0.15f + 0.1f,
                offset = i / shootingStarCount.toFloat()
            )
        }
    }

    // Zaman akışı (saniye)
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                time = (now - start) / 1_000_000_000f
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawStars(stars, time * speedMultiplier)
        drawShootingStars(shooting, time * speedMultiplier)
    }
}

private fun DrawScope.drawStars(stars: List<Star>, t: Float) {
    stars.forEach { s ->
        // Yavaşça aşağı kayar; ekrandan çıkınca başa döner
        val y = ((s.y + t * s.speed) % 1f) * size.height
        // Hafif yatay salınım
        val x = ((s.x + sin(t * 0.3f + s.x * 6f) * s.drift + 1f) % 1f) * size.width
        val twinkle = 0.85f + 0.15f * sin(t * 2f + s.x * 10f)

        drawCircle(
            color = Color.White.copy(alpha = (s.alpha * twinkle).coerceIn(0f, 1f)),
            radius = s.size,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawShootingStars(shooting: List<ShootingStar>, t: Float) {
    shooting.forEach { s ->
        // Her yıldızın döngüsü ~6 saniye; offset ile dağıtılır
        val cycle = ((t * s.speed + s.offset) % 1f)
        if (cycle > 0.35f) return@forEach  // sadece döngünün ilk %35'inde görünür

        val progress = cycle / 0.35f  // 0..1
        val xStart = s.startX * size.width
        val yStart = s.startY * size.height
        val distance = size.width * 0.7f
        val angleRad = Math.toRadians(s.angleDeg.toDouble())

        val cx = xStart + (cos(angleRad) * distance * progress).toFloat()
        val cy = yStart + (sin(angleRad) * distance * progress).toFloat()

        val lengthPx = s.length * size.width
        val dx = (cos(angleRad) * lengthPx).toFloat()
        val dy = (sin(angleRad) * lengthPx).toFloat()

        // İz: baştan sona solarak çizgi
        val alpha = ((1f - progress) * 0.9f).coerceIn(0f, 1f)
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0f),
                    Color.White.copy(alpha = alpha),
                    Color.White.copy(alpha = 0f)
                ),
                start = Offset(cx - dx, cy - dy),
                end = Offset(cx, cy)
            ),
            start = Offset(cx - dx, cy - dy),
            end = Offset(cx, cy),
            strokeWidth = 2f
        )

        // Baş noktası parlak
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = 2.5f,
            center = Offset(cx, cy)
        )
    }
}

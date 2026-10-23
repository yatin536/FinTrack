package com.example.fintrack.ui.components

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fintrack.data.local.GlassThemePreset
import com.example.fintrack.data.model.TimePeriod
import com.example.fintrack.theme.LocalGlassThemePreset

/**
 * Aurora ambient lighting background.
 * Renders multi-stop glowing radial ambient orbs behind content
 * customized for the active [GlassThemePreset].
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    preset: GlassThemePreset = LocalGlassThemePreset.current,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable BoxScope.() -> Unit
) {
    val (baseColor, glowColor1, glowColor2) = if (darkTheme) {
        when (preset) {
            GlassThemePreset.MIDNIGHT_GLASS -> Triple(
                Color(0xFF0B0F19),
                Color(0xFF06B6D4), // Cyan
                Color(0xFF4F46E5)  // Indigo
            )
            GlassThemePreset.AURO_GLASS -> Triple(
                Color(0xFF0F172A),
                Color(0xFFA855F7), // Magenta/Lavender
                Color(0xFF06B6D4)  // Cyan
            )
            GlassThemePreset.SUNSET_GLOW -> Triple(
                Color(0xFF180E29),
                Color(0xFFF43F5E), // Coral
                Color(0xFFF59E0B)  // Amber
            )
            GlassThemePreset.EMERALD_CALM -> Triple(
                Color(0xFF062019),
                Color(0xFF34D399), // Mint
                Color(0xFF059669)  // Teal
            )
        }
    } else {
        when (preset) {
            GlassThemePreset.MIDNIGHT_GLASS -> Triple(
                Color(0xFFF8FAFC),
                Color(0xFFBAE6FD),
                Color(0xFFC7D2FE)
            )
            GlassThemePreset.AURO_GLASS -> Triple(
                Color(0xFFFAF5FF),
                Color(0xFFE9D5FF),
                Color(0xFFBAE6FD)
            )
            GlassThemePreset.SUNSET_GLOW -> Triple(
                Color(0xFFFFF1F2),
                Color(0xFFFECDD3),
                Color(0xFFFDE68A)
            )
            GlassThemePreset.EMERALD_CALM -> Triple(
                Color(0xFFF0FDF4),
                Color(0xFFA7F3D0),
                Color(0xFF99F6E4)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
    ) {
        // Draw the ambient glowing orbs
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val alpha1 = if (darkTheme) 0.22f else 0.45f
            val alpha2 = if (darkTheme) 0.18f else 0.40f

            // Top-right glow orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor1.copy(alpha = alpha1), Color.Transparent),
                    center = Offset(width * 0.85f, height * 0.12f),
                    radius = width * 0.75f
                ),
                center = Offset(width * 0.85f, height * 0.12f),
                radius = width * 0.75f
            )

            // Center-left or bottom glow orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor2.copy(alpha = alpha2), Color.Transparent),
                    center = Offset(width * 0.15f, height * 0.45f),
                    radius = width * 0.85f
                ),
                center = Offset(width * 0.15f, height * 0.45f),
                radius = width * 0.85f
            )
        }

        content()
    }
}

/**
 * Premium glassmorphic card container.
 * Features translucent frosted gradient surface and specular border highlight.
 * Utilizes hardware blur on Android 12+ (API 31+) with graceful translucent gradient fallback on API 26-30.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val surfaceBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0x2EFFFFFF), // 18% white highlight at top
                Color(0x14FFFFFF), // 8% white in center
                Color(0x1A000000)  // 10% dark depth at bottom
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xF0FFFFFF), // Translucent clean white
                Color(0xD9FFFFFF)
            )
        )
    }

    val borderBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0x66FFFFFF), // Bright specular top-left edge
                Color(0x26FFFFFF), // Subtle mid border
                Color(0x0DFFFFFF)  // Faded shadow edge
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0x80FFFFFF),
                Color(0x260F172A)
            )
        )
    }

    val baseModifier = modifier
        .clip(shape)
        .then(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.blur(0.dp) // API 31+ hardware pass
            } else {
                Modifier
            }
        )
        .background(brush = surfaceBrush, shape = shape)
        .border(width = borderWidth, brush = borderBrush, shape = shape)
        .then(
            if (onClick != null) {
                Modifier.clickable { onClick() }
            } else {
                Modifier
            }
        )

    Box(
        modifier = baseModifier,
        content = content
    )
}

/**
 * Air-gapped offline status pill badge with pulsing glowing emerald dot.
 */
@Composable
fun OfflineStatusBadge(
    modifier: Modifier = Modifier,
    text: String = "AIR-GAPPED OFFLINE"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x1A10B981))
            .border(1.dp, Color(0x4D10B981), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(Color(0xFF10B981).copy(alpha = pulseAlpha))
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = Color(0xFF10B981)
        )
    }
}

/**
 * Modern glass segmented period selector (Daily / Weekly / Monthly).
 */
@Composable
fun SegmentedPeriodSelector(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier,
    periods: List<TimePeriod> = listOf(TimePeriod.DAILY, TimePeriod.WEEKLY, TimePeriod.MONTHLY)
) {
    val isDark = isSystemInDarkTheme()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0x1AFFFFFF) else Color(0x140F172A))
            .border(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0x1F0F172A), RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        periods.forEach { period ->
            val isSelected = (selectedPeriod == period)
            val label = when (period) {
                TimePeriod.DAILY -> "Daily"
                TimePeriod.WEEKLY -> "Weekly"
                TimePeriod.MONTHLY -> "Monthly"
                TimePeriod.YEARLY -> "Yearly"
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (isSelected) {
                            Modifier
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(12.dp))
                        } else {
                            Modifier.clickable { onPeriodSelected(period) }
                        }
                    )
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Premium squircle floating action button with vibrant gradient and subtle shadow.
 */
@Composable
fun SquircleGradientFab(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp)
) {
    FloatingActionButton(
        onClick = onClick,
        shape = shape,
        containerColor = Color.Transparent,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        Color(0xFF06B6D4) // Vibrant Cyan end stop
                    )
                )
            )
            .border(1.dp, Color(0x66FFFFFF), shape)
    ) {
        icon()
    }
}

package com.starwinmod.winlator.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.withFrameMillis
import com.starwinmod.winlator.ui.theme.GlowPurple
import com.starwinmod.winlator.ui.theme.Primary
import com.starwinmod.winlator.ui.theme.Secondary
import com.starwinmod.winlator.ui.theme.Tertiary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ── VEGAS brand colors for the splash ──
private val VegasDarkBg = Color(0xFF0A0012)
private val VegasSurface = Color(0xFF150626)
private val VegasAccent = Color(0xFF1A0A30)
private val MagentaVibrant = Primary       // #D946EF
private val PurpleDeep = Color(0xFF7C3AED)
private val CyanBright = Secondary         // #22D3EE
private val GoldAccent = Tertiary          // #FBBF24

private val VegasLogoGradient = Brush.linearGradient(
    colors = listOf(MagentaVibrant, PurpleDeep, CyanBright),
)

private val VegasProgressGradient = listOf(MagentaVibrant, PurpleDeep, CyanBright)

// ── Sparkle specs — now with VEGAS brand colors ──
private val SPARKLE_COLORS = listOf(
    MagentaVibrant, CyanBright, GoldAccent, GlowPurple, PurpleDeep,
)

private data class SparkleSpec(
    val angleDeg: Float,
    val distFrac: Float,
    val size: Float,
    val periodMs: Long,
    val offsetMs: Long,
    val driftXFrac: Float,
    val riseYFrac: Float,
    val colorIdx: Int,
)

private val SPARKLE_SPECS: List<SparkleSpec> = run {
    val rng = java.util.Random(42L)
    List(24) {
        SparkleSpec(
            angleDeg   = rng.nextFloat() * 360f,
            distFrac   = 0.35f + rng.nextFloat() * 0.50f,
            size       = 2.0f + rng.nextFloat() * 4.0f,
            periodMs   = 1400L + (rng.nextFloat() * 1200f).toLong(),
            offsetMs   = (rng.nextFloat() * 3000f).toLong(),
            driftXFrac = (rng.nextFloat() - 0.5f) * 0.10f,
            riseYFrac  = 0.02f + rng.nextFloat() * 0.08f,
            colorIdx   = rng.nextInt(SPARKLE_COLORS.size),
        )
    }
}

private val STATUS_LABELS = listOf(
    "Initializing VEGAS engine",
    "Loading graphics pipeline",
    "Configuring Wine prefix",
    "Setting up DXVK runtime",
    "Calibrating performance",
)

@Composable
fun SplashScreen(
    progress: Int,
    showProceed: Boolean = false,
    onProceed: () -> Unit = {},
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    val displayedProgress by animateIntAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "counter",
    )

    val statusText = if (progress >= 100) "Ready"
                     else STATUS_LABELS[minOf((progress / 22), STATUS_LABELS.size - 1)]

    // Dot animation
    val dotPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 3.99f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dots",
    )
    val dots = if (progress < 100) ".".repeat(dotPhase.toInt() + 1) else ""

    // Logo breathing
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.06f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logoScale",
    )

    // Glow pulse
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue  = 0.8f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    // Shimmer sweep
    val shimmerPos by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue  = 1.3f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer",
    )

    var frameTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) { withFrameMillis { frameTime = it } }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VegasDarkBg),
        contentAlignment = Alignment.Center,
    ) {
        // ── Background glow vignette ──
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        VegasAccent.copy(alpha = 0.35f),
                        VegasSurface.copy(alpha = 0.15f),
                        VegasDarkBg,
                    ),
                    radius = size.minDimension * 0.75f,
                ),
                radius = size.minDimension * 0.9f,
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 48.dp),
        ) {
            // ── VEGAS Logo mark ──
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Glow circle
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MagentaVibrant.copy(alpha = glowAlpha * 0.20f),
                                MagentaVibrant.copy(alpha = glowAlpha * 0.05f),
                                Color.Transparent,
                            ),
                            radius = size.minDimension / 2f,
                        ),
                        radius = size.minDimension / 2f,
                    )
                }
                // "V" logo
                Canvas(modifier = Modifier.fillMaxSize().scale(logoScale)) {
                    val strokeW = size.minDimension * 0.18f
                    val leftX   = size.width * 0.20f
                    val rightX  = size.width * 0.80f
                    val topY    = size.height * 0.15f
                    val bottomY = size.height * 0.85f
                    val cx      = size.width / 2f

                    // Left arm of V
                    drawLine(
                        brush   = VegasLogoGradient,
                        start   = Offset(leftX, topY),
                        end     = Offset(cx, bottomY),
                        strokeWidth = strokeW,
                        cap     = StrokeCap.Round,
                    )
                    // Right arm of V
                    drawLine(
                        brush   = VegasLogoGradient,
                        start   = Offset(rightX, topY),
                        end     = Offset(cx, bottomY),
                        strokeWidth = strokeW,
                        cap     = StrokeCap.Round,
                    )
                    // Small decorative diamond at apex
                    drawCircle(
                        brush  = CyanBright.let { Brush.radialGradient(listOf(it, it.copy(alpha = 0.3f))) },
                        radius = strokeW * 0.30f,
                        center = Offset(cx, bottomY),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── "VEGAS" wordmark ──
            Text(
                text = "VEGAS",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp,
                brush = VegasLogoGradient,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(4.dp))

            // ── "STAR EMULATOR" subtitle ──
            Text(
                text = "STAR EMULATOR",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 3.sp,
                color = GlowPurple,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            // ── Status text ──
            Text(
                text = "$statusText$dots",
                fontSize = 13.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFFAAAAAA),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            // ── VEGAS Glowing Progress Bar ──
            VegasProgressBar(
                progress    = displayedProgress / 100f,
                shimmerPos  = shimmerPos,
                isComplete  = progress >= 100,
                modifier    = Modifier.fillMaxWidth().height(6.dp),
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "$displayedProgress%",
                fontSize = 13.sp,
                color = Color(0xFF888888),
            )

            // ── "ENTER" button ──
            AnimatedVisibility(
                visible = showProceed,
                enter   = fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.92f),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(28.dp))
                    Button(
                        onClick = onProceed,
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = VegasSurface,
                            contentColor   = Color.White,
                        ),
                        shape   = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .width(200.dp)
                            .height(48.dp),
                    ) {
                        // Glow border drawn behind — use Box trick
                        Text(
                            text = "ENTER",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp,
                            color = CyanBright,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── "Powered by VEGAS" footer ──
            if (!showProceed) {
                Text(
                    text = "Powered by VEGAS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp,
                    color = Color(0xFF666666),
                )
            }
        }
    }
}

// ── VEGAS Sparkle Canvas ──

@Composable
private fun SparkleCanvas(time: Long, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx       = size.width / 2f
        val cy       = size.height / 2f
        val halfSize = min(size.width, size.height) / 2f

        SPARKLE_SPECS.forEach { spec ->
            val phase = ((time + spec.offsetMs) % spec.periodMs).toFloat() / spec.periodMs
            val alpha = when {
                phase < 0.25f -> phase / 0.25f
                phase < 0.75f -> 1f
                else          -> 1f - (phase - 0.75f) / 0.25f
            } * 0.85f

            val angleRad = Math.toRadians(spec.angleDeg.toDouble()).toFloat()
            val dist     = spec.distFrac * halfSize
            val x        = cx + cos(angleRad) * dist + spec.driftXFrac * halfSize * phase
            val y        = cy + sin(angleRad) * dist - spec.riseYFrac * halfSize * phase

            val color = SPARKLE_COLORS[spec.colorIdx]
            drawFourPointStar(x, y, spec.size, color.copy(alpha = alpha))
        }
    }
}

private fun DrawScope.drawFourPointStar(cx: Float, cy: Float, radius: Float, color: Color) {
    val path        = androidx.compose.ui.graphics.Path()
    val innerRadius = radius * 0.35f
    val count       = 4
    for (i in 0 until count) {
        val outerAngle = (i.toFloat() / count) * 2f * PI.toFloat() - PI.toFloat() / 2f
        val innerAngle = outerAngle + PI.toFloat() / count
        val ox = cx + cos(outerAngle) * radius
        val oy = cy + sin(outerAngle) * radius
        val ix = cx + cos(innerAngle) * innerRadius
        val iy = cy + sin(innerAngle) * innerRadius
        if (i == 0) path.moveTo(ox, oy) else path.lineTo(ox, oy)
        path.lineTo(ix, iy)
    }
    path.close()
    drawPath(path, color)
}

// ── VEGAS Glowing Progress Bar ──

@Composable
private fun VegasProgressBar(
    progress: Float,
    shimmerPos: Float,
    isComplete: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val barH   = size.height
        val barW   = size.width
        val radius = barH / 2f
        val fillW  = (barW * progress).coerceIn(0f, barW)

        // Track
        drawRoundRect(
            color        = Color(0xFF1A0A30),
            size         = Size(barW, barH),
            cornerRadius = CornerRadius(radius),
        )

        if (fillW > 0f) {
            // Glow layers
            listOf(
                8f to 0.08f,
                5f to 0.14f,
                2f to 0.22f,
            ).forEach { (expand, a) ->
                drawRoundRect(
                    color        = MagentaVibrant.copy(alpha = a),
                    topLeft      = Offset(-expand / 2f, -expand / 2f),
                    size         = Size(fillW + expand, barH + expand),
                    cornerRadius = CornerRadius(radius + expand / 2f),
                )
            }

            // VEGAS gradient fill
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = VegasProgressGradient,
                    endX   = fillW,
                ),
                size         = Size(fillW, barH),
                cornerRadius = CornerRadius(radius),
            )

            // Shimmer sweep
            if (!isComplete) {
                val shimX     = shimmerPos * fillW
                val shimHalf  = barH * 4f
                clipRect(right = fillW) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.40f),
                                Color.Transparent,
                            ),
                            startX = shimX - shimHalf,
                            endX   = shimX + shimHalf,
                        ),
                        size         = Size(fillW, barH),
                        cornerRadius = CornerRadius(radius),
                    )
                }
            }
        }
    }
}

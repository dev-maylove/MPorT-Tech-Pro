package com.mporttech.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mporttech.pro.ui.navigation.AppNavigation
import com.mporttech.pro.ui.theme.LocalThemeMode
import com.mporttech.pro.ui.theme.MPorTTechTheme
import com.mporttech.pro.ui.theme.ThemeMode
import com.mporttech.pro.ui.theme.rememberThemeModeState
import com.mporttech.pro.ui.i18n.LocalAppLanguage
import com.mporttech.pro.ui.i18n.loadSavedLanguage
import com.mporttech.pro.ui.i18n.rememberAppLanguageState
import com.mporttech.pro.ui.i18n.t
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = this
            val themeModeState = rememberThemeModeState(ThemeMode.DARK)
            val languageState = rememberAppLanguageState(loadSavedLanguage(context))
            // Read via `by` so MaterialTheme recomposes when Profile toggles dark mode
            val themeMode by themeModeState
            CompositionLocalProvider(
                LocalThemeMode provides themeModeState,
                LocalAppLanguage provides languageState
            ) {
                MPorTTechTheme(themeMode = themeMode) {
                    var showStartup by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) {
                        delay(2200)
                        showStartup = false
                    }
                    if (showStartup) PremiumStartupScreen() else AppNavigation()
                }
            }
        }
    }
}

@Composable
private fun PremiumStartupScreen() {
    // Performance: only 2 infinite animations (was 5) — less GPU overdraw
    val transition = rememberInfiniteTransition(label = "startup")
    val pulse by transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val ring by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "ring"
    )
    val ring2 = 0f // static — avoid second continuous rotation
    val glow = 0.55f // static soft glow — no infinite pulse cost
    val particle = 0f // particles drawn static offsets only

    var progress by remember { mutableFloatStateOf(0f) }
    var status by remember { mutableStateOf("BOOT SEQUENCE") }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500),
        label = "progress"
    )

    LaunchedEffect(Unit) {
        val steps = listOf(
            0.10f to "INITIALIZING CORE",
            0.26f to "LOADING NETWORK MODULES",
            0.44f to "PREPARING DIAGNOSTICS",
            0.62f to "SYNCING TECH TOOLS",
            0.80f to "OPTIMIZING INTERFACE",
            0.94f to "CALIBRATING SENSORS",
            1.0f to "SYSTEM READY"
        )
        for ((p, label) in steps) {
            status = label
            progress = p
            delay(280)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF01040A),
                        Color(0xFF06101F),
                        Color(0xFF0A1A30),
                        Color(0xFF02060F)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Ambient orbs + subtle particle field
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Primary glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x4421B6FF), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.36f),
                    radius = size.minDimension * 0.5f
                ),
                radius = size.minDimension * 0.5f,
                center = Offset(size.width * 0.5f, size.height * 0.36f)
            )
            // Secondary ambient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x22176BFF), Color.Transparent),
                    center = Offset(size.width * 0.72f, size.height * 0.72f),
                    radius = size.minDimension * 0.35f
                ),
                radius = size.minDimension * 0.35f,
                center = Offset(size.width * 0.72f, size.height * 0.72f)
            )
            // Static accent dots (no per-frame particle animation)
            val cx = size.width * 0.5f
            val cy = size.height * 0.38f
            for (i in 0 until 8) {
                val angle = (i * 45.0) * (Math.PI / 180.0)
                val dist = size.minDimension * (0.24f + (i % 3) * 0.03f)
                val px = cx + (dist * cos(angle)).toFloat()
                val py = cy + (dist * sin(angle)).toFloat()
                drawCircle(
                    color = Color(0xFF66E6FF).copy(alpha = 0.28f + (i % 3) * 0.08f),
                    radius = (1.5f + (i % 2)).dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
                // Outer reverse ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 2.dp.toPx()
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color.Transparent,
                                Color(0x3321B6FF),
                                Color(0x5566E6FF),
                                Color.Transparent
                            )
                        ),
                        startAngle = 40f,
                        sweepAngle = 200f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }

                // Main rotating premium ring
                Canvas(modifier = Modifier.fillMaxSize(0.92f).graphicsLayer { rotationZ = ring }) {
                    val stroke = 3.8.dp.toPx()
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFF21B6FF),
                                Color(0xFF66E6FF),
                                Color(0xFF00D9FF),
                                Color.Transparent
                            )
                        ),
                        startAngle = 0f,
                        sweepAngle = 290f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    val r = size.minDimension / 2f - stroke
                    for (i in 0 until 4) {
                        val a = Math.toRadians((i * 32.0))
                        val px = center.x + r * cos(a).toFloat()
                        val py = center.y + r * sin(a).toFloat()
                        drawCircle(
                            Color(0xFF66E6FF).copy(alpha = 0.9f),
                            radius = 2.8.dp.toPx(),
                            center = Offset(px, py)
                        )
                    }
                }

                // Soft glow halo
                Canvas(modifier = Modifier.size(220.dp).alpha(glow * 0.5f)) {
                    drawCircle(
                        color = Color(0xFF21B6FF).copy(alpha = 0.2f),
                        style = Stroke(width = 14.dp.toPx())
                    )
                }

                // Inner subtle ring
                Canvas(modifier = Modifier.size(180.dp).alpha(0.35f)) {
                    drawCircle(
                        color = Color(0xFF66E6FF).copy(alpha = 0.25f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                Image(
                    painter = painterResource(R.drawable.mport_tech_logo),
                    contentDescription = "MPorT Tech",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .graphicsLayer(
                            scaleX = pulse,
                            scaleY = pulse,
                            alpha = 0.97f + glow * 0.03f
                        )
                )
            }

            Spacer(Modifier.height(22.dp))
            Text(
                text = "MPorT TECH",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "NETWORK TECHNICIAN TOOLKIT",
                color = Color(0xFF8EC8F0),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.4.sp
            )
            Spacer(Modifier.height(22.dp))

            // Premium progress track
            Box(
                modifier = Modifier
                    .width(210.dp)
                    .height(6.dp)
                    .background(Color(0xFF0E1C30), RoundedCornerShape(10.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.coerceIn(0.02f, 1f))
                        .height(6.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF00D9FF),
                                    Color(0xFF21B6FF),
                                    Color(0xFF176BFF),
                                    Color(0xFF66E6FF)
                                )
                            ),
                            RoundedCornerShape(10.dp)
                        )
                )
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = status,
                color = Color(0xFF7BA8D4),
                fontSize = 10.sp,
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                color = Color(0xFF21B6FF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Footer brand line
        Text(
            text = "PROFESSIONAL  •  SECURE  •  FIELD-READY",
            color = Color(0xFF3A5570),
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        )
    }
}

package com.mporttech.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mporttech.pro.core.auth.SessionManager
import com.mporttech.pro.features.auth.LoginScreen
import com.mporttech.pro.ui.i18n.LocalAppLanguage
import com.mporttech.pro.ui.i18n.loadSavedLanguage
import com.mporttech.pro.ui.i18n.rememberAppLanguageState
import com.mporttech.pro.ui.navigation.AppNavigation
import com.mporttech.pro.ui.theme.LocalThemeMode
import com.mporttech.pro.ui.theme.MPorTTechTheme
import com.mporttech.pro.ui.theme.ThemeMode
import com.mporttech.pro.ui.theme.rememberThemeModeState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.cos
import kotlin.math.sin

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        val keepSplash = AtomicBoolean(true)
        splash.setKeepOnScreenCondition { keepSplash.get() }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SideEffect { keepSplash.set(false) }
            val context = this
            val themeModeState = rememberThemeModeState(ThemeMode.DARK)
            val languageState = rememberAppLanguageState(loadSavedLanguage(context))
            val themeMode by themeModeState
            CompositionLocalProvider(
                LocalThemeMode provides themeModeState,
                LocalAppLanguage provides languageState
            ) {
                MPorTTechTheme(themeMode = themeMode) {
                    var showStartup by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) {
                        delay(1600)
                        showStartup = false
                    }
                    var hasSession by remember {
                        mutableStateOf(SessionManager.hasSession(context))
                    }
                    when {
                        showStartup -> PremiumStartupScreen()
                        !hasSession -> LoginScreen(
                            onLoggedIn = { hasSession = true },
                            onContinueAsGuest = { hasSession = true }
                        )
                        else -> AppNavigation()
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumStartupScreen() {
    val transition = rememberInfiniteTransition(label = "splash")
    val ringAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )

    var logoVisible by remember { mutableStateOf(false) }
    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "logoAlpha"
    )
    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.92f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "logoScale"
    )

    var progress by remember { mutableFloatStateOf(0f) }
    var status by remember { mutableStateOf("Starting") }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "progress"
    )

    LaunchedEffect(Unit) {
        logoVisible = true
        val steps = listOf(
            0.2f to "Initializing",
            0.45f to "Loading",
            0.7f to "Preparing",
            0.9f to "Almost ready",
            1.0f to "Ready"
        )
        for ((p, label) in steps) {
            status = label
            progress = p
            delay(260)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .drawWithCache {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val radius = size.minDimension * 0.48f
                            val glowBrush = Brush.radialGradient(
                                colors = listOf(Color(0x3321B6FF), Color.Transparent),
                                center = Offset(cx, cy),
                                radius = radius
                            )
                            val trackStroke = Stroke(width = 1.5.dp.toPx())
                            val trackR = size.minDimension / 2f - 4.dp.toPx()
                            onDrawBehind {
                                drawCircle(
                                    brush = glowBrush,
                                    radius = radius,
                                    center = Offset(cx, cy)
                                )
                                drawCircle(
                                    color = Color(0xFF21B6FF).copy(alpha = 0.18f),
                                    radius = trackR,
                                    center = Offset(cx, cy),
                                    style = trackStroke
                                )
                            }
                        }
                )

                Canvas(
                    modifier = Modifier
                        .size(200.dp)
                        .rotate(ringAngle)
                ) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val stroke = 2.5.dp.toPx()
                    val r = size.minDimension / 2f - 4.dp.toPx()
                    drawArc(
                        color = Color(0xFF66E6FF).copy(alpha = 0.9f),
                        startAngle = 0f,
                        sweepAngle = 100f,
                        useCenter = false,
                        topLeft = Offset(cx - r, cy - r),
                        size = Size(r * 2f, r * 2f),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    val rad = Math.toRadians(100.0)
                    drawCircle(
                        color = Color(0xFF66E6FF),
                        radius = 3.dp.toPx(),
                        center = Offset(
                            cx + r * cos(rad).toFloat(),
                            cy + r * sin(rad).toFloat()
                        )
                    )
                }

                Image(
                    painter = painterResource(R.drawable.mport_tech_logo),
                    contentDescription = "MPorT Tech",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(132.dp)
                        .graphicsLayer {
                            alpha = logoAlpha
                            scaleX = logoScale
                            scaleY = logoScale
                        }
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "MPorT TECH",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "NETWORK TECHNICIAN TOOLKIT",
                color = Color(0xFF6AB0E0),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(3.dp)
                    .background(Color(0xFF141414), RoundedCornerShape(50))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.coerceIn(0.04f, 1f))
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF00E5FF), Color(0xFF2196F3))
                            ),
                            RoundedCornerShape(50)
                        )
                )
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = status.uppercase(),
                color = Color(0xFF7A8FA8),
                fontSize = 11.sp,
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

        Text(
            text = "PROFESSIONAL  ·  SECURE  ·  FIELD-READY",
            color = Color(0xFF3A4A5C),
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        )
    }
}

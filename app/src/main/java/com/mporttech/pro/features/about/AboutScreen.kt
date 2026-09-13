package com.mporttech.pro.features.about

import com.mporttech.pro.R
import com.mporttech.pro.ui.i18n.t

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Support
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


import androidx.compose.material.icons.filled.ArrowBack
import androidx.navigation.NavController
import com.mporttech.pro.ui.components.PremiumFeatureItem
import com.mporttech.pro.ui.components.PremiumInfoRow
import com.mporttech.pro.ui.components.PremiumSectionCard
import com.mporttech.pro.ui.theme.MPorTTheme



// ============================================================
// MPORT TECH - ABOUT SCREEN PREMIUM V2
// ============================================================

@Composable
fun AboutScreen(
    nav: NavController? = null,
    onBackClick: () -> Unit = {}
) {
    val goBack: () -> Unit = {
        if (nav != null) {
            if (nav.previousBackStackEntry != null) nav.popBackStack()
            else nav.navigate("profile") { launchSingleTop = true }
        } else {
            onBackClick()
        }
    }


    val context = LocalContext.current

    /*
     * ========================================================
     * CONTACT CONFIGURATION
     *
     * Replace these values with your real contact information.
     * ========================================================
     */

    val supportEmail = "YOUR_EMAIL@example.com"

    val supportUrl = "https://YOUR_SUPPORT_LINK.com"


    /*
     * ========================================================
     * APP VERSION
     * ========================================================
     */

    val appVersion = try {

        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            0
        )

        packageInfo.versionName ?: "1.0.0"

    } catch (e: PackageManager.NameNotFoundException) {

        "1.0.0"
    }


    /*
     * ========================================================
     * COLORS
     * ========================================================
     */

    val ext = MPorTTheme.colors
    val backgroundStart = ext.heroGradientStart
    val backgroundEnd = ext.heroGradientEnd
    val surfaceColor = ext.elevatedSurface
    val cyan = ext.cyan
    val blue = ext.blue
    val purple = ext.purple
    val primaryText = MaterialTheme.colorScheme.onBackground
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = ext.cardBorder
    val heartColor = ext.heart


    /*
     * ========================================================
     * ANIMATION
     * ========================================================
     */

    // Single infinite transition (was 2) — derive alpha from scale for less recomposition
    val infiniteTransition = rememberInfiniteTransition(label = "AboutGlow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowScale"
    )
    // Map scale 1..1.06 → alpha 0.3..0.55 without second animator
    val glowAlpha = 0.3f + (glowScale - 1f) / 0.06f * 0.25f


    val scrollState = rememberScrollState()


    /*
     * ========================================================
     * MAIN SCREEN
     * ========================================================
     */

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(

                Brush.verticalGradient(

                    colors = listOf(
                        backgroundStart,
                        backgroundEnd
                    )
                )
            )
    ) {


        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    horizontal = 20.dp,
                    vertical = 16.dp
                )
        ) {


            // =================================================
            // TOP BAR
            // =================================================

            Row(
                modifier = Modifier
                    .fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {


                Surface(

                    modifier = Modifier
                        .size(46.dp),

                    shape = CircleShape,

                    color = Color(0xFF111827),

                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = borderColor
                    )
                ) {

                    IconButton(
                        onClick = goBack
                    ) {

                        Icon(

                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,

                            contentDescription =
                                "Back",

                            tint = primaryText
                        )
                    }
                }


                Spacer(
                    modifier = Modifier.width(14.dp)
                )


                Column {

                    Text(

                        text = t("about.title"),

                        color = cyan,

                        fontSize = 11.sp,

                        fontWeight = FontWeight.Bold,

                        letterSpacing = 2.sp
                    )


                    Text(

                        text = "MPorT Tech",

                        color = primaryText,

                        fontSize = 22.sp,

                        fontWeight = FontWeight.Bold
                    )
                }
            }


            Spacer(
                modifier = Modifier.height(32.dp)
            )


            // =================================================
            // HERO SECTION
            // =================================================

            Box(
                modifier = Modifier
                    .fillMaxWidth(),

                contentAlignment =
                    Alignment.Center
            ) {


                /*
                 * Animated Glow
                 */

                Box(

                    modifier = Modifier
                        .size(145.dp)
                        .scale(glowScale)
                        .alpha(glowAlpha)
                        .clip(CircleShape)
                        .background(

                            Brush.radialGradient(

                                colors = listOf(

                                    cyan.copy(
                                        alpha = 0.45f
                                    ),

                                    blue.copy(
                                        alpha = 0.15f
                                    ),

                                    Color.Transparent
                                )
                            )
                        )
                )


                /*
                 * Logo Container
                 */

                Surface(

                    modifier = Modifier
                        .size(104.dp),

                    shape = CircleShape,

                    color = Color.Transparent,

                    border = androidx.compose.foundation.BorderStroke(

                        width = 1.5.dp,

                        brush = Brush.linearGradient(

                            colors = listOf(
                                cyan,
                                blue,
                                purple
                            )
                        )
                    )
                ) {

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(R.drawable.mport_tech_logo),
                            contentDescription = "MPorT Tech Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(88.dp)
                        )
                    }
                }
            }


            Spacer(
                modifier = Modifier.height(22.dp)
            )


            Text(

                text = "MPorT Tech",

                modifier = Modifier
                    .fillMaxWidth(),

                color = primaryText,

                textAlign = TextAlign.Center,

                fontSize = 32.sp,

                fontWeight = FontWeight.Bold
            )


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            Text(

                text =
                    "PROFESSIONAL NETWORK INTELLIGENCE",

                modifier = Modifier
                    .fillMaxWidth(),

                color = cyan,

                textAlign = TextAlign.Center,

                fontSize = 11.sp,

                fontWeight = FontWeight.Bold,

                letterSpacing = 1.5.sp
            )


            Spacer(
                modifier = Modifier.height(18.dp)
            )


            Text(

                text =
                    "Powerful tools. Intelligent insights. Professional results.",

                modifier = Modifier
                    .fillMaxWidth(),

                color = secondaryText,

                textAlign = TextAlign.Center,

                fontSize = 15.sp,

                lineHeight = 23.sp
            )


            Spacer(
                modifier = Modifier.height(36.dp)
            )


            // =================================================
            // ABOUT APPLICATION
            // =================================================

            PremiumSectionCard(

                title = "ABOUT THE PLATFORM",

                icon = Icons.Default.Info,

                accentColor = cyan

            ) {

                Text(

                    text =

                    "MPorT Tech is a professional network utility platform " +
                    "designed to support technicians, network administrators, " +
                    "and IT professionals in monitoring, managing, and analyzing " +
                    "network environments with greater efficiency.",

                    color = secondaryText,

                    fontSize = 14.sp,

                    lineHeight = 22.sp
                )


                Spacer(
                    modifier = Modifier.height(14.dp)
                )


                Text(

                    text =

                    "Built with a strong focus on performance, reliability, " +
                    "and simplicity, MPorT Tech brings essential network tools " +
                    "together in one modern and intuitive experience.",

                    color = secondaryText,

                    fontSize = 14.sp,

                    lineHeight = 22.sp
                )
            }


            Spacer(
                modifier = Modifier.height(18.dp)
            )


            // =================================================
            // CORE CAPABILITIES
            // =================================================

            PremiumSectionCard(

                title = "CORE CAPABILITIES",

                icon = Icons.Default.SettingsEthernet,

                accentColor = cyan

            ) {


                PremiumFeatureItem(
                    text = "Network Monitoring",
                    accentColor = cyan
                )

                PremiumFeatureItem(
                    text = "Network Diagnostics",
                    accentColor = cyan
                )

                PremiumFeatureItem(
                    text = "Connectivity Testing",
                    accentColor = cyan
                )

                PremiumFeatureItem(
                    text = "Ping & Latency Analysis",
                    accentColor = cyan
                )

                PremiumFeatureItem(
                    text = "Device Discovery",
                    accentColor = cyan
                )

                PremiumFeatureItem(
                    text = "WiFi Information",
                    accentColor = cyan
                )

                PremiumFeatureItem(
                    text = "IP & Network Utilities",
                    accentColor = cyan
                )

                PremiumFeatureItem(
                    text = "Network Statistics",
                    accentColor = cyan
                )

                PremiumFeatureItem(
                    text = "MikroTik Integration",
                    accentColor = cyan
                )

                PremiumFeatureItem(
                    text = "Professional Technician Tools",
                    accentColor = cyan
                )
            }


            Spacer(
                modifier = Modifier.height(18.dp)
            )


            // =================================================
            // OUR VISION
            // =================================================

            PremiumSectionCard(

                title = "OUR VISION",

                icon = Icons.Default.Code,

                accentColor = purple

            ) {


                Text(

                    text =
                        "Technology should make complex tasks simpler.",

                    color = primaryText,

                    fontSize = 16.sp,

                    fontWeight = FontWeight.SemiBold,

                    lineHeight = 24.sp
                )


                Spacer(
                    modifier = Modifier.height(14.dp)
                )


                Text(

                    text =

                    "MPorT Tech is built with the vision of creating a modern " +
                    "and reliable platform where network professionals can access " +
                    "essential tools through a clean, intuitive, and efficient interface.",

                    color = secondaryText,

                    fontSize = 14.sp,

                    lineHeight = 22.sp
                )


                Spacer(
                    modifier = Modifier.height(14.dp)
                )


                Text(

                    text =
                        "Built for professionals. Designed for performance.",

                    color = purple,

                    fontSize = 14.sp,

                    fontWeight = FontWeight.Bold
                )
            }


            Spacer(
                modifier = Modifier.height(18.dp)
            )


            // =================================================
            // DEVELOPER
            // =================================================

            PremiumSectionCard(

                title = "THE DEVELOPER",

                icon = Icons.Default.Code,

                accentColor = cyan

            ) {


                Text(

                    text = "DeV_MayLoVE",

                    color = primaryText,

                    fontSize = 24.sp,

                    fontWeight = FontWeight.Bold
                )


                Spacer(
                    modifier = Modifier.height(6.dp)
                )


                Text(

                    text =
                        "Independent Developer & Technology Enthusiast",

                    color = cyan,

                    fontSize = 13.sp,

                    fontWeight = FontWeight.Medium
                )


                Spacer(
                    modifier = Modifier.height(18.dp)
                )


                Text(

                    text =

                    "MPorT Tech is independently developed with a passion for " +
                    "technology, innovation, practical network utilities, and " +
                    "modern software experiences.",

                    color = secondaryText,

                    fontSize = 14.sp,

                    lineHeight = 22.sp
                )


                Spacer(
                    modifier = Modifier.height(18.dp)
                )


                Text(

                    text = "AREAS OF INTEREST",

                    color = Color(0xFF5EC8E8),

                    fontSize = 11.sp,

                    fontWeight = FontWeight.Bold,

                    letterSpacing = 1.sp
                )


                Spacer(
                    modifier = Modifier.height(10.dp)
                )


                PremiumFeatureItem(
                    text = "Android Development",
                    accentColor = cyan
                )

                PremiumFeatureItem(
                    text = "Network Engineering",
                    accentColor = cyan
                )

                PremiumFeatureItem(
                    text = "MikroTik Technologies",
                    accentColor = cyan
                )

                PremiumFeatureItem(
                    text = "WiFi Infrastructure",
                    accentColor = cyan
                )

                PremiumFeatureItem(
                    text = "Network Automation",
                    accentColor = cyan
                )

                PremiumFeatureItem(
                    text = "Modern UI & UX Design",
                    accentColor = cyan
                )
            }


            Spacer(
                modifier = Modifier.height(18.dp)
            )


            // =================================================
            // APPLICATION INFORMATION
            // =================================================

            PremiumSectionCard(

                title = "APPLICATION INFORMATION",

                icon = Icons.Default.Info,

                accentColor = blue

            ) {


                PremiumInfoRow(

                    label = "APPLICATION",

                    value = "MPorT Tech"
                )


                PremiumInfoRow(

                    label = "CATEGORY",

                    value = "Network Tools & Utilities"
                )


                PremiumInfoRow(

                    label = "PLATFORM",

                    value = "Android"
                )


                PremiumInfoRow(

                    label = "VERSION",

                    value = appVersion
                )


                PremiumInfoRow(

                    label = "DEVELOPER",

                    value = "DeV_MayLoVE"
                )


                PremiumInfoRow(

                    label = "TECHNOLOGY",

                    value = "Kotlin • Jetpack Compose"
                )
            }


            Spacer(
                modifier = Modifier.height(30.dp)
            )


            // =================================================
            // CONTACT & SUPPORT
            // =================================================

            Text(

                text = "CONNECT WITH US",

                color = primaryText,

                fontSize = 20.sp,

                fontWeight = FontWeight.Bold
            )


            Spacer(
                modifier = Modifier.height(6.dp)
            )


            Text(

                text =
                    "Your feedback helps shape the future of MPorT Tech.",

                color = secondaryText,

                fontSize = 14.sp
            )


            Spacer(
                modifier = Modifier.height(18.dp)
            )


            // =================================================
            // CONTACT BUTTON
            // =================================================

            Button(

                onClick = {

                    val emailIntent = Intent(
                        Intent.ACTION_SENDTO
                    ).apply {

                        data = Uri.parse(
                            "mailto:$supportEmail"
                        )

                        putExtra(

                            Intent.EXTRA_SUBJECT,

                            "MPorT Tech - Support Request"
                        )
                    }


                    try {

                        context.startActivity(emailIntent)

                    } catch (e: Exception) {

                        Toast.makeText(

                            context,

                            "No email application found.",

                            Toast.LENGTH_SHORT

                        ).show()
                    }
                },


                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),


                shape = RoundedCornerShape(18.dp),


                colors = ButtonDefaults.buttonColors(

                    containerColor = cyan,

                    contentColor = Color(0xFF020617)
                ),


                contentPadding = PaddingValues(
                    horizontal = 20.dp
                )
            ) {


                Icon(

                    imageVector = Icons.Default.Email,

                    contentDescription = null
                )


                Spacer(
                    modifier = Modifier.width(12.dp)
                )


                Column {

                    Text(

                        text = "CONTACT DEVELOPER",

                        fontWeight = FontWeight.Bold,

                        fontSize = 14.sp
                    )


                    Text(

                        text = "Support • Feedback • Bug Reports",

                        fontSize = 11.sp,

                        maxLines = 1,

                        overflow = TextOverflow.Ellipsis
                    )
                }
            }


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            // =================================================
            // SUPPORT BUTTON
            // =================================================

            Button(

                onClick = {

                    try {

                        val supportIntent = Intent(

                            Intent.ACTION_VIEW,

                            Uri.parse(supportUrl)
                        )


                        context.startActivity(
                            supportIntent
                        )

                    } catch (e: Exception) {

                        Toast.makeText(

                            context,

                            "Unable to open support page.",

                            Toast.LENGTH_SHORT

                        ).show()
                    }
                },


                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),


                shape = RoundedCornerShape(18.dp),


                colors = ButtonDefaults.buttonColors(
            contentColor = Color(0xFF000000),

                    containerColor = Color(0xFF151B27),

                    contentColor = primaryText
                ),


                border = androidx.compose.foundation.BorderStroke(

                    width = 1.dp,

                    brush = Brush.linearGradient(

                        colors = listOf(

                            heartColor.copy(
                                alpha = 0.8f
                            ),

                            purple.copy(
                                alpha = 0.8f
                            )
                        )
                    )
                ),


                contentPadding = PaddingValues(
                    horizontal = 20.dp
                )
            ) {


                Icon(

                    imageVector = Icons.Default.Favorite,

                    contentDescription = null,

                    tint = heartColor
                )


                Spacer(
                    modifier = Modifier.width(12.dp)
                )


                Column {

                    Text(

                        text = "SUPPORT DEVELOPMENT",

                        fontWeight = FontWeight.Bold,

                        fontSize = 14.sp
                    )


                    Text(

                        text =
                            "Help shape the future of MPorT Tech",

                        color = secondaryText,

                        fontSize = 11.sp
                    )
                }
            }


            Spacer(
                modifier = Modifier.height(30.dp)
            )


            // =================================================
            // RESPONSIBLE USE
            // =================================================

            PremiumSectionCard(

                title = "RESPONSIBLE USE",

                icon = Icons.Default.Security,

                accentColor = Color(0xFF39FF14)

            ) {


                Text(

                    text =

                    "MPorT Tech is designed for legitimate network monitoring, " +
                    "diagnostics, management, and analysis.",

                    color = secondaryText,

                    fontSize = 14.sp,

                    lineHeight = 22.sp
                )


                Spacer(
                    modifier = Modifier.height(14.dp)
                )


                Text(

                    text =

                    "Users are responsible for ensuring that the application is " +
                    "used only on networks, devices, and systems they own or are " +
                    "explicitly authorized to access.",

                    color = secondaryText,

                    fontSize = 14.sp,

                    lineHeight = 22.sp
                )


                Spacer(
                    modifier = Modifier.height(14.dp)
                )


                Text(

                    text =
                        "MPorT Tech promotes responsible and ethical use of technology.",

                    color = Color(0xFF39FF14),

                    fontSize = 13.sp,

                    fontWeight = FontWeight.Bold
                )
            }


            Spacer(
                modifier = Modifier.height(34.dp)
            )


            // =================================================
            // FOOTER
            // =================================================

            HorizontalDivider(
                color = borderColor
            )


            Spacer(
                modifier = Modifier.height(24.dp)
            )


            Icon(

                imageVector = Icons.Default.Favorite,

                contentDescription = null,

                tint = heartColor,

                modifier = Modifier
                    .fillMaxWidth()
                    .size(26.dp)
            )


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            Text(

                text = "Thank You",

                modifier = Modifier
                    .fillMaxWidth(),

                color = primaryText,

                textAlign = TextAlign.Center,

                fontSize = 20.sp,

                fontWeight = FontWeight.Bold
            )


            Spacer(
                modifier = Modifier.height(10.dp)
            )


            Text(

                text =

                "Thank you for choosing MPorT Tech. Your feedback, " +
                "support, suggestions, and bug reports help us continue " +
                "improving and building better tools for the network community.",

                modifier = Modifier
                    .fillMaxWidth(),

                color = secondaryText,

                textAlign = TextAlign.Center,

                fontSize = 13.sp,

                lineHeight = 21.sp
            )


            Spacer(
                modifier = Modifier.height(22.dp)
            )


            Text(

                text =
                    "Stay Connected. Stay Efficient. Stay Professional.",

                modifier = Modifier
                    .fillMaxWidth(),

                color = cyan,

                textAlign = TextAlign.Center,

                fontSize = 13.sp,

                fontWeight = FontWeight.Bold
            )


            Spacer(
                modifier = Modifier.height(30.dp)
            )


            Text(

                text = "MPorT Tech",

                modifier = Modifier
                    .fillMaxWidth(),

                color = primaryText,

                textAlign = TextAlign.Center,

                fontSize = 18.sp,

                fontWeight = FontWeight.Bold
            )


            Spacer(
                modifier = Modifier.height(6.dp)
            )


            Text(

                text =
                    "Professional Network Intelligence",

                modifier = Modifier
                    .fillMaxWidth(),

                color = cyan,

                textAlign = TextAlign.Center,

                fontSize = 11.sp,

                letterSpacing = 1.sp
            )


            Spacer(
                modifier = Modifier.height(18.dp)
            )


            Text(

                text =
                    "Built with passion by DeV_MayLoVE",

                modifier = Modifier
                    .fillMaxWidth(),

                color = secondaryText,

                textAlign = TextAlign.Center,

                fontSize = 12.sp
            )


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            Text(

                text =
                    "© 2026 DeV_MayLoVE • All Rights Reserved",

                modifier = Modifier
                    .fillMaxWidth(),

                color = Color(0xFF5EC8E8),

                textAlign = TextAlign.Center,

                fontSize = 11.sp
            )


            Spacer(
                modifier = Modifier.height(40.dp)
            )
        }
    }
}

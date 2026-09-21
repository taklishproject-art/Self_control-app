package com.example.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SoberingVersesProvider
import com.example.ui.theme.ShieldAlertRed
import com.example.ui.theme.ShieldGreenLight
import com.example.ui.theme.ShieldGreenPrimary
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import kotlinx.coroutines.delay

class SoberingBlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "Browser"
        val blockedTerm = intent.getStringExtra(EXTRA_BLOCKED_TERM) ?: "Adult Query"

        setContent {
            SoberingScreen(
                appName = appName,
                blockedTerm = blockedTerm,
                onReturnHome = {
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(homeIntent)
                    finish()
                }
            )
        }
    }

    companion object {
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_BLOCKED_TERM = "extra_blocked_term"
    }
}

@Composable
fun SoberingScreen(
    appName: String,
    blockedTerm: String,
    onReturnHome: () -> Unit
) {
    val verse = remember { SoberingVersesProvider.getRandomVerse() }
    var countdown by remember { mutableIntStateOf(10) }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alert_pulse"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Slate950
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF450A0A),
                            Slate950,
                            Color(0xFF0F172A)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Pulsing Warning Beacon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(90.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(ShieldAlertRed.copy(alpha = 0.2f))
                        .border(2.dp, ShieldAlertRed, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ShieldAlertRed,
                        modifier = Modifier.size(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ቆም በል! ራስህን አድን!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "በ $appName ላይ የተከለከለ ፍለጋ፦ \"$blockedTerm\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ShieldAlertRed,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(20.dp))

                // The Holy Scripture Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1917)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "የሕያው እግዚአብሔር ቃል (${verse.reference})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = verse.text,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 26.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = verse.soberingThought,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE2E8F0),
                                textAlign = TextAlign.Center,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Cool-down Reflection Box
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.8f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.SelfImprovement,
                            contentDescription = null,
                            tint = ShieldGreenLight,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ስሜትህን አቀዝቅዝ (ጥልቅ አየር ተንፍስ)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (countdown > 0)
                                    "ለ $countdown ሰከንዶች ቆም ብለህ አስብ..."
                                else
                                    "አእምሮህ ረጋ? አሁኑኑ ወደ ሰላማዊ ስክሪን ተመለስ!",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (countdown > 0) Color(0xFFF59E0B) else ShieldGreenLight,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Action Button: Immediate Escape to Home
                Button(
                    onClick = onReturnHome,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (countdown > 0) Color(0xFFB91C1C) else ShieldGreenPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("sobering_escape_button")
                ) {
                    Icon(Icons.Default.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (countdown > 0) "ወደ Home ተመለስ ($countdown)" else "ተመልሼ እራሴን እጠብቃለሁ (Escape)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

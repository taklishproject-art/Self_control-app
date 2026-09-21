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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.example.data.BlockPreferences
import com.example.service.SanctuaryEngine
import com.example.ui.theme.ShieldAlertRed
import com.example.ui.theme.ShieldGreenLight
import com.example.ui.theme.ShieldGreenPrimary
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import kotlinx.coroutines.delay

class SanctuaryLockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_NIGHT_LOCK

        setContent {
            val prefs = remember { BlockPreferences(applicationContext) }
            SanctuaryLockScreen(
                mode = mode,
                onMorningCompleted = {
                    prefs.markMorningSanctuaryCompletedToday()
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(homeIntent)
                    finish()
                },
                onExitToHome = {
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

    override fun onBackPressed() {
        // Prevent back navigation during night lock
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_NIGHT_LOCK
        if (mode == MODE_NIGHT_LOCK) {
            // Keep on lock screen
            return
        }
        super.onBackPressed()
    }

    companion object {
        const val EXTRA_MODE = "extra_sanctuary_mode"
        const val MODE_WARNING = "mode_warning"
        const val MODE_NIGHT_LOCK = "mode_night_lock"
        const val MODE_MORNING_GATE = "mode_morning_gate"
    }
}

@Composable
fun SanctuaryLockScreen(
    mode: String,
    onMorningCompleted: () -> Unit,
    onExitToHome: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sanctuary_pulse"
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
                        colors = when (mode) {
                            SanctuaryLockActivity.MODE_WARNING -> listOf(Color(0xFF78350F), Slate950, Color(0xFF1E1B4B))
                            SanctuaryLockActivity.MODE_MORNING_GATE -> listOf(Color(0xFF064E3B), Slate950, Color(0xFF0F172A))
                            else -> listOf(Color(0xFF020617), Color(0xFF0B1329), Slate950)
                        }
                    )
                )
                .padding(20.dp)
        ) {
            when (mode) {
                SanctuaryLockActivity.MODE_WARNING -> WarningPreLockContent(pulseScale, onExitToHome)
                SanctuaryLockActivity.MODE_MORNING_GATE -> MorningGateContent(onMorningCompleted)
                else -> NightLockContent(pulseScale)
            }
        }
    }
}

@Composable
private fun WarningPreLockContent(pulseScale: Float, onDismiss: () -> Unit) {
    var secondsLeft by remember { mutableIntStateOf(SanctuaryEngine.getSecondsUntilLockdown()) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft = SanctuaryEngine.getSecondsUntilLockdown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(90.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                .border(2.dp, Color(0xFFF59E0B), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "የማረፊያ ዝግጅት ሰዓት ደርሷል!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "የኢትዮጵያ ሰዓት፡ ${SanctuaryEngine.getEthiopianTimeString()}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFF59E0B),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.9f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "ከ 5:00 ጀምሮ ስልክህ ሙሉ በሙሉ ይቆለፋል",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "የቀሩ ደቂቃዎች/ሰከንዶች፦ ${secondsLeft / 60} ደቂቃ ከ ${secondsLeft % 60} ሰከንድ\n\nእባክህ አስፈላጊ መልዕክቶችን አሁን ጨራርሰህ ራስህን ለእረፍትና ለጸሎት አዘጋጅ። ልክ 5:00 ሲል ስልኩ ሙሉ በሙሉ ወደ ሌሊት ጽሞና ይገባል።",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("ተረድቻለሁ (አዘጋጃለሁ)", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun NightLockContent(pulseScale: Float) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(100.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                .border(2.dp, Color(0xFF38BDF8), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Bedtime,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "የሌሊት የጽሞና እና የእረፍት ሰዓት",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "🔒 ስልኩ እስከ ጠዋቱ 11:00 ድረስ በቋሚነት ተቆልፏል",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF38BDF8),
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF1E40AF)))
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "የማታ ቃል (መዝሙረ ዳዊት 4:8)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "«በሰላም እተኛለሁ አንቀላፋለሁም፤ አቤቱ፥ አንተ ብቻህን በእምነት አሳድረኸኛልና።»",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "ስልክህን አስቀምጠህ በሰላም እረፍ። ሌሊቱን እግዚአብሔር በአንተና በህይወትህ ላይ ጠባቂ ነው።",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "የኢንተርኔት፣ ብሮውዘር እና የቪዲዮ አፖች ዝግ ናቸው። ጠዋት 11:00 በጸሎትና ስፖርት እንገናኝ!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )
            }
        }
    }
}

@Composable
private fun MorningGateContent(onCompleted: () -> Unit) {
    var hasPrayed by remember { mutableStateOf(false) }
    var hasExercised by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(ShieldGreenPrimary.copy(alpha = 0.2f))
                .border(2.dp, ShieldGreenPrimary, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.SelfImprovement,
                contentDescription = null,
                tint = ShieldGreenLight,
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "እንኳን አደረሰህ! አዲስ ቀን፣ አዲስ ብርታት!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "ጠዋት 11:00 — የማለዳ ጸሎት እና የንቃት ስፖርት በር",
            style = MaterialTheme.typography.bodyMedium,
            color = ShieldGreenLight,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.4f)),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(listOf(ShieldGreenPrimary, ShieldGreenLight))
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "የማለዳ ምስጋና (ሰቆቃወ ኤርምያስ 3:22-23)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ShieldGreenLight
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "«ያልጠፋነው ከእግዚአብሔር ምሕረት የተነሣ ነው፤ ርኅራኄው አያልቅምና። ማለዳ ማለዳ አዲስ ነው፤ ታማኝነትህ ታላቅ ነው።»",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Checkbox Checklist before phone unlocks
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ስልኩን ከመክፈትህ በፊት የዛሬውን ስንቅ ያዝ፦",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = hasPrayed,
                        onCheckedChange = { hasPrayed = it },
                        colors = CheckboxDefaults.colors(checkedColor = ShieldGreenPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "1. የማለዳ ጸሎትና ምስጋና አድርሻለሁ (Prayer)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = if (hasPrayed) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = hasExercised,
                        onCheckedChange = { hasExercised = it },
                        colors = CheckboxDefaults.colors(checkedColor = ShieldGreenPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "2. የ 15 ደቂቃ የማለዳ ስፖርት/ማነቃቂያ ሰርቻለሁ (Fitness)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = if (hasExercised) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onCompleted,
            enabled = hasPrayed && hasExercised,
            colors = ButtonDefaults.buttonColors(
                containerColor = ShieldGreenPrimary,
                disabledContainerColor = Color(0xFF334155)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("morning_gate_unlock_button")
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (hasPrayed && hasExercised) "ተጠናቋል! ስልኩን ክፈት (Enter Day)" else "ሁለቱንም ፈጽመህ ምረጥ",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}

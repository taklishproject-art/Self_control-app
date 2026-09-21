package com.example.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle

import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BlockLog
import com.example.data.BlockPreferences
import com.example.data.ContentFilterEngine
import com.example.service.ContentBlockerAccessibilityService
import com.example.ui.theme.ShieldAlertRed
import com.example.ui.theme.ShieldGreenDark
import com.example.ui.theme.ShieldGreenLight
import com.example.ui.theme.ShieldGreenPrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.util.AccessibilityUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShieldScreen() {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val prefs = remember { BlockPreferences(context) }

    var isServiceEnabled by remember { mutableStateOf(AccessibilityUtils.isAccessibilityServiceEnabled(context)) }
    var isUnbreakableMode by remember { mutableStateOf(prefs.isUnbreakableModeEnabled) }
    var isProtectionOn by remember { mutableStateOf(prefs.isProtectionEnabled) }
    var isStrictMode by remember { mutableStateOf(prefs.isStrictMode) }
    var blockBrowsers by remember { mutableStateOf(prefs.blockBrowsers) }
    var blockSocial by remember { mutableStateOf(prefs.blockSocial) }
    var vibrationAlert by remember { mutableStateOf(prefs.vibrationAlert) }

    var totalBlocks by remember { mutableIntStateOf(prefs.getTotalBlocks()) }
    var todayBlocks by remember { mutableIntStateOf(prefs.getTodayBlocks()) }
    var streakDays by remember { mutableIntStateOf(prefs.getStreakDays()) }
    var blockedLogs by remember { mutableStateOf(prefs.getBlockedLogs()) }
    var customKeywords by remember { mutableStateOf(prefs.getCustomKeywords()) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinDialogAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showAddKeywordDialog by remember { mutableStateOf(false) }
    var testQueryText by remember { mutableStateOf("") }
    var liveAlertMessage by remember { mutableStateOf<String?>(null) }

    // Periodic check for accessibility status
    LaunchedEffect(Unit) {
        while (true) {
            isServiceEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(context)
            kotlinx.coroutines.delay(2000)
        }
    }

    // Broadcast receiver for live block events
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == ContentBlockerAccessibilityService.ACTION_CONTENT_BLOCKED) {
                    val app = intent.getStringExtra(ContentBlockerAccessibilityService.EXTRA_APP_NAME) ?: "መተግበሪያ"
                    val term = intent.getStringExtra(ContentBlockerAccessibilityService.EXTRA_BLOCKED_TERM) ?: ""
                    liveAlertMessage = "በ $app ላይ \"$term\" የተባለ ተገቢ ያልሆነ ይዘት ተገኝቶ ታግዷል!"
                    totalBlocks = prefs.getTotalBlocks()
                    todayBlocks = prefs.getTodayBlocks()
                    blockedLogs = prefs.getBlockedLogs()
                }
            }
        }
        val filter = IntentFilter(ContentBlockerAccessibilityService.ACTION_CONTENT_BLOCKED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "SafeGuard",
                            tint = if (isProtectionOn && isServiceEnabled) ShieldGreenPrimary else ShieldAlertRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SafeGuard",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "የይዘት መቆጣጠሪያ እና ጽኑ ጥበቃ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSetPinDialog = true },
                        modifier = Modifier.testTag("pin_lock_button")
                    ) {
                        Icon(
                            imageVector = if (prefs.pinCode.isNullOrEmpty()) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = "የደህንነት ቁልፍ (PIN)",
                            tint = if (prefs.pinCode.isNullOrEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else ShieldGreenPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Live Alert Banner if blocked
            liveAlertMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ShieldAlertRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = ShieldAlertRed)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { liveAlertMessage = null }) {
                            Icon(Icons.Default.Close, contentDescription = "ዝጋ", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Tab Bar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        focusManager.clearFocus()
                        selectedTab = 0
                    },
                    text = { Text("ዋና ጥበቃ", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        focusManager.clearFocus()
                        selectedTab = 1
                    },
                    text = { Text("መቆጣጠሪያ ህጎች", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = {
                        focusManager.clearFocus()
                        selectedTab = 2
                    },
                    text = { Text("የታገዱ (${totalBlocks})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = {
                        focusManager.clearFocus()
                        selectedTab = 3
                    },
                    text = { Text("ፈታሽ", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                )
            }

            when (selectedTab) {
                0 -> ProtectionTab(
                    isServiceEnabled = isServiceEnabled,
                    isProtectionOn = isProtectionOn,
                    totalBlocks = totalBlocks,
                    todayBlocks = todayBlocks,
                    streakDays = streakDays,
                    onTestScriptureScreen = {
                        val intent = Intent(context, SoberingBlockActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            putExtra(SoberingBlockActivity.EXTRA_APP_NAME, "Google Chrome")
                            putExtra(SoberingBlockActivity.EXTRA_BLOCKED_TERM, "xn..")
                        }
                        context.startActivity(intent)
                    },
                    onToggleProtection = { requestedState ->
                        if (isUnbreakableMode && !requestedState) {
                            // Cannot turn off in unbreakable mode!
                            return@ProtectionTab
                        }
                        if (!requestedState && !prefs.pinCode.isNullOrEmpty()) {
                            pinDialogAction = {
                                isProtectionOn = false
                                prefs.isProtectionEnabled = false
                            }
                            showPinDialog = true
                        } else {
                            isProtectionOn = requestedState
                            prefs.isProtectionEnabled = requestedState
                        }
                    },
                    onOpenAccessibilitySettings = {
                        AccessibilityUtils.openAccessibilitySettings(context)
                    }
                )
                1 -> RulesTab(
                    blockBrowsers = blockBrowsers,
                    blockSocial = blockSocial,
                    isStrictMode = isStrictMode,
                    vibrationAlert = vibrationAlert,
                    customKeywords = customKeywords,
                    onToggleBrowsers = {
                        blockBrowsers = it
                        prefs.blockBrowsers = it
                    },
                    onToggleSocial = {
                        blockSocial = it
                        prefs.blockSocial = it
                    },
                    onToggleStrict = {
                        isStrictMode = it
                        prefs.isStrictMode = it
                    },
                    onToggleVibration = {
                        vibrationAlert = it
                        prefs.vibrationAlert = it
                    },
                    onAddKeywordClick = { showAddKeywordDialog = true },
                    onRemoveKeyword = { kw ->
                        prefs.removeCustomKeyword(kw)
                        customKeywords = prefs.getCustomKeywords()
                    }
                )
                2 -> HistoryTab(
                    logs = blockedLogs,
                    onClearLogs = {
                        prefs.clearLogs()
                        blockedLogs = emptyList()
                        totalBlocks = 0
                        todayBlocks = 0
                    }
                )
                3 -> SimulatorTab(
                    testQueryText = testQueryText,
                    onQueryChange = { testQueryText = it },
                    customKeywords = customKeywords,
                    isStrictMode = isStrictMode,
                    onPreviewScripture = { term ->
                        val intent = Intent(context, SoberingBlockActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            putExtra(SoberingBlockActivity.EXTRA_APP_NAME, "Google Chrome")
                            putExtra(SoberingBlockActivity.EXTRA_BLOCKED_TERM, term)
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }

    // PIN Verification Dialog
    if (showPinDialog) {
        PinVerificationDialog(
            correctPin = prefs.pinCode ?: "",
            onSuccess = {
                showPinDialog = false
                pinDialogAction?.invoke()
                pinDialogAction = null
            },
            onDismiss = {
                showPinDialog = false
                pinDialogAction = null
            }
        )
    }

    // Set or Change PIN Dialog
    if (showSetPinDialog) {
        SetPinDialog(
            currentPin = prefs.pinCode,
            onSave = { newPin ->
                prefs.pinCode = newPin
                showSetPinDialog = false
            },
            onDismiss = { showSetPinDialog = false }
        )
    }

    // Add Custom Keyword Dialog
    if (showAddKeywordDialog) {
        AddKeywordDialog(
            onAdd = { kw ->
                if (kw.isNotBlank()) {
                    prefs.addCustomKeyword(kw)
                    customKeywords = prefs.getCustomKeywords()
                }
                showAddKeywordDialog = false
            },
            onDismiss = { showAddKeywordDialog = false }
        )
    }
}

@Composable
private fun ProtectionTab(
    isServiceEnabled: Boolean,
    isProtectionOn: Boolean,
    totalBlocks: Int,
    todayBlocks: Int,
    streakDays: Int,
    onTestScriptureScreen: () -> Unit,
    onToggleProtection: (Boolean) -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shield_pulse"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Shield Status Card
        item {
            val isFullyActive = isServiceEnabled && isProtectionOn
            val cardBg = if (isFullyActive) {
                Brush.verticalGradient(listOf(Color(0xFF064E3B), Color(0xFF0F172A)))
            } else if (!isServiceEnabled) {
                Brush.verticalGradient(listOf(Color(0xFF7F1D1D), Color(0xFF0F172A)))
            } else {
                Brush.verticalGradient(listOf(Slate800, Slate900))
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("master_shield_card"),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardBg)
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Shield Visual with Pulse
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(110.dp)
                                .scale(if (isFullyActive) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(
                                    if (isFullyActive) ShieldGreenPrimary.copy(alpha = 0.2f)
                                    else if (!isServiceEnabled) ShieldAlertRed.copy(alpha = 0.2f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (isFullyActive) ShieldGreenPrimary
                                    else if (!isServiceEnabled) ShieldAlertRed
                                    else Slate700,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isFullyActive) Icons.Default.Security
                                else if (!isServiceEnabled) Icons.Default.Warning
                                else Icons.Default.Block,
                                contentDescription = null,
                                tint = if (isFullyActive) ShieldGreenLight
                                else if (!isServiceEnabled) ShieldAlertRed
                                else Color.Gray,
                                modifier = Modifier.size(56.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (isFullyActive) "ጥበቃው ሙሉ በሙሉ ንቁ ነው"
                            else if (!isServiceEnabled) "የተደራሽነት ፍቃድ ያስፈልጋል!"
                            else "ጥበቃው ቆሟል",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isFullyActive) "Chrome, Phoenix, TikTok, Instagram እና Facebook ላይ የአዋቂዎች ይዘት በራስ-ሰር ይታገዳል።"
                            else if (!isServiceEnabled) "አፑ በስልክዎ ላይ ጎጂ ይዘቶችን ለማገድ የተደራሽነት (Accessibility) ፍቃድ ማግኘት አለበት።"
                            else "ጥበቃውን መልሰው ለማብራት ከስር ያለውን መቀየሪያ ያብሩ።",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "የይዘት መቆጣጠሪያ ጥበቃ",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "🔒 በቋሚነት የበራ (ማጥፋት አይቻልም)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ShieldGreenLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Switch(
                                checked = true,
                                onCheckedChange = null,
                                enabled = false,
                                colors = SwitchDefaults.colors(
                                    disabledCheckedThumbColor = Color.White,
                                    disabledCheckedTrackColor = ShieldGreenPrimary
                                ),
                                modifier = Modifier.testTag("protection_master_switch")
                            )
                        }
                    }
                }
            }
        }

        // Scripture Sobering Shield Feature Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B18)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "የመጽሐፍ ቅዱስ አስደንጋጭ ማስታወሻ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "በ Chrome፣ Phoenix ወይም በየትኛውም ቦታ «xn..» ወይም የአዋቂ ቃል ሲፈለግ፣ ገጹ ወዲያውኑ ተዘግቶ በደማቅ የመጽሐፍ ቅዱስ ጥቅስ እና ስሜትን በሚያቀዘቅዝ ማስታወሻ ይሸፈናል።",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE2E8F0)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onTestScriptureScreen,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ስክሪኑን አሁኑኑ ሞክር (Preview Warning Screen)", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Night Sanctuary & Morning Prayer Gate System Card (Background System)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1322)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF6366F1)))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "የሳተላይት/ኔትወርክ ሰዓት የሌሊት ጽሞና እና የማለዳ ጸሎት በር",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ይህ ስርዓት በስልኩ ሰዓት ላይ ሳይመሰረት በትክክለኛው የኢትዮጵያ ሰዓት (UTC+3) ከጀርባ ይሰራል፦\n• 4:55 — የማረፊያ ቅድመ-ዝግጅት ማስጠንቀቂያ ይሰጣል\n• 5:00 — ስልኩን ሙሉ በሙሉ በመቆለፍ ወደ ሌሊት ጽሞና ያስገባል\n• 11:00 — የማለዳ ጸሎትና ስፖርት ሳያደርጉ ስልኩ አይከፈትም",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ከጀርባ በቋሚነት የበራ (Locked ON) — ማጥፋት አይቻልም",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }


        // Accessibility Permission Setup Card (Prominent if not enabled)
        item {
            if (!isServiceEnabled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ShieldAlertRed.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = ShieldAlertRed)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "የተደራሽነት (Accessibility) ፍቃድ አሰጣጥ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ShieldAlertRed
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "አፑ በ Chrome፣ በ Phoenix ብሮውዘር ወይም በ TikTok/Instagram ላይ የአዋቂዎች ቪዲዮ ወይም ጽሁፍ ሲመጣ በራስ-ሰር ለመዝጋት የስልክዎ የተደራሽነት ፈቃድ ያስፈልገዋል፦",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            InstructionStep(number = "1", text = "ከታች ያለውን «የተደራሽነት ፍቃድ ክፈት» ይጫኑ።")
                            InstructionStep(number = "2", text = "«Installed apps» ወይም «የተጫኑ መተግበሪያዎች» የሚለውን ይምረጡ።")
                            InstructionStep(number = "3", text = "«SafeGuard» ን ፈልገው ያብሩት (Turn ON)።")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onOpenAccessibilitySettings,
                            colors = ButtonDefaults.buttonColors(containerColor = ShieldAlertRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("enable_accessibility_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("የተደራሽነት ፍቃድ ክፈት (Open Settings)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ShieldGreenDark.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ShieldGreenPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "የተደራሽነት ፍቃድ በትክክል ተሰጥቷል",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ShieldGreenPrimary
                            )
                            Text(
                                text = "ስርዓቱ በሁሉም ብሮውዘሮች እና አፖች ላይ ንቁ ጥበቃ እያደረገ ነው።",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Stats Row: Total Blocks, Today, Streak Days
        item {
            Text(
                text = "የእርስዎ ቁጥጥር ስታቲስቲክስ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "የታገዱ ድምር",
                    value = "$totalBlocks",
                    subtitle = "ሙከራዎች",
                    icon = Icons.Default.Block,
                    color = ShieldAlertRed,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "የዛሬ እገዳዎች",
                    value = "$todayBlocks",
                    subtitle = "ዛሬ የታገዱ",
                    icon = Icons.Default.NotificationsActive,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "የንጽህና ቀናት",
                    value = "$streakDays",
                    subtitle = "ቀጣይ ቀናት",
                    icon = Icons.Default.Speed,
                    color = ShieldGreenPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Motivational Quote
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💡 የዕለቱ ማበረታቻ",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "«ራስን መግዛት የሁሉም ታላላቅ ስኬቶች መጀመሪያ ነው። ትኩረትዎን እና አእምሮዎን በንጽህና ይጠብቁ።»",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
private fun RulesTab(
    blockBrowsers: Boolean,
    blockSocial: Boolean,
    isStrictMode: Boolean,
    vibrationAlert: Boolean,
    customKeywords: Set<String>,
    onToggleBrowsers: (Boolean) -> Unit,
    onToggleSocial: (Boolean) -> Unit,
    onToggleStrict: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onAddKeywordClick: () -> Unit,
    onRemoveKeyword: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "የሚጠበቁ መተግበሪያዎች እና መቆጣጠሪያዎች",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ShieldGreenPrimary.copy(alpha = 0.12f)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(ShieldGreenPrimary, ShieldGreenDark))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = ShieldGreenPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "የተጠቃሚውን ጽናትና መንፈሳዊ ደህንነት ለመጠበቅ ሁሉም የመቆጣጠሪያ ህጎች በቋሚነት የበሩ (Locked ON) ናቸው፤ ማጥፋት አይቻልም።",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Browsers control
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ብሮውዘሮች (Chrome, Phoenix...)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "በ Google Chrome፣ Phoenix Browser፣ Firefox ወዘተ ላይ የአዋቂ ጣቢያዎች እና ፍለጋዎች ወዲያውኑ ይታገዳሉ።",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = ShieldGreenPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "በቋሚነት የበራ (ማጥፋት አይቻልም)",
                                style = MaterialTheme.typography.labelSmall,
                                color = ShieldGreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Switch(
                        checked = true,
                        onCheckedChange = null,
                        enabled = false,
                        colors = SwitchDefaults.colors(
                            disabledCheckedThumbColor = Color.White,
                            disabledCheckedTrackColor = ShieldGreenPrimary
                        ),
                        modifier = Modifier.testTag("toggle_browsers_switch")
                    )
                }
            }
        }

        // Social media control
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ማህበራዊ ሚዲያዎች (TikTok, Instagram...)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "በ TikTok (For You page)፣ Instagram Reels፣ Facebook ላይ ተገቢ ያልሆኑ ቪዲዮዎችን እና ጽሁፎችን ይዘጋል።",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = ShieldGreenPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "በቋሚነት የበራ (ማጥፋት አይቻልም)",
                                style = MaterialTheme.typography.labelSmall,
                                color = ShieldGreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Switch(
                        checked = true,
                        onCheckedChange = null,
                        enabled = false,
                        colors = SwitchDefaults.colors(
                            disabledCheckedThumbColor = Color.White,
                            disabledCheckedTrackColor = ShieldGreenPrimary
                        ),
                        modifier = Modifier.testTag("toggle_social_switch")
                    )
                }
            }
        }

        // Strict mode control
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = ShieldAlertRed,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ጥብቅ ሁነታ (Strict Mode)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "አጠራጣሪ ቃል እንደተገኘ ገጹን በፍጥነት ዘግቶ ወደ Home ይመልሳል።",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = ShieldGreenPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "በቋሚነት የበራ (ማጥፋት አይቻልም)",
                                style = MaterialTheme.typography.labelSmall,
                                color = ShieldGreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Switch(
                        checked = true,
                        onCheckedChange = null,
                        enabled = false,
                        colors = SwitchDefaults.colors(
                            disabledCheckedThumbColor = Color.White,
                            disabledCheckedTrackColor = ShieldGreenPrimary
                        ),
                        modifier = Modifier.testTag("toggle_strict_switch")
                    )
                }
            }
        }

        // Vibration alert control
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Vibration,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "የንዝረት ማስጠንቀቂያ",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "ተገቢ ያልሆነ ይዘት ተገኝቶ ሲታገድ ስልክዎ በንዝረት ያሳውቃል።",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = ShieldGreenPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "በቋሚነት የበራ (ማጥፋት አይቻልም)",
                                style = MaterialTheme.typography.labelSmall,
                                color = ShieldGreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Switch(
                        checked = true,
                        onCheckedChange = null,
                        enabled = false,
                        colors = SwitchDefaults.colors(
                            disabledCheckedThumbColor = Color.White,
                            disabledCheckedTrackColor = ShieldGreenPrimary
                        ),
                        modifier = Modifier.testTag("toggle_vibration_switch")
                    )
                }
            }
        }

        // Custom Blocklist Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "የእርስዎ የተከለከሉ ቃላት እና ጣቢያዎች",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ሊታገዱ የሚፈልጓቸውን የተለዩ ቃላት ወይም የድረ-ገጽ ስሞች ያክሉ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onAddKeywordClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_keyword_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("አክል")
                }
            }
        }

        item {
            if (customKeywords.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ምንም የተጨመረ ቃል የለም። (ሲስተሙ በውስጡ 70+ የአዋቂ ድረ-ገጾችና ቃላትን በቋሚነት ያግዳል)",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    customKeywords.forEach { kw ->
                        FilterChip(
                            selected = true,
                            onClick = { onRemoveKeyword(kw) },
                            label = { Text(kw) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "አስወግድ",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(
    logs: List<BlockLog>,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "የታገዱ ይዘቶች ታሪክ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "በቅርብ ጊዜ በስልክዎ ላይ የተከለከሉ ሙከራዎች",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (logs.isNotEmpty()) {
                OutlinedButton(
                    onClick = onClearLogs,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("አጽዳ")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ShieldGreenPrimary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "ምንም የታገደ ሙከራ የለም!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ስልክዎ እና አሰሳዎ ንጹህ ነው። ማንኛውም ተገቢ ያልሆነ ይዘት ሲገኝ እዚህ ይመዘገባል።",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val dateFormat = remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(logs, key = { it.id }) { log ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(ShieldAlertRed.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    Icons.Default.Block,
                                    contentDescription = null,
                                    tint = ShieldAlertRed,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.appName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "የታገደበት ምክንያት፦ \"${log.blockedTerm}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ShieldAlertRed
                                )
                                Text(
                                    text = dateFormat.format(Date(log.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimulatorTab(
    testQueryText: String,
    onQueryChange: (String) -> Unit,
    customKeywords: Set<String>,
    isStrictMode: Boolean,
    onPreviewScripture: (String) -> Unit
) {
    val testMatch = remember(testQueryText, customKeywords, isStrictMode) {
        if (testQueryText.isBlank()) null
        else ContentFilterEngine.checkContent(testQueryText, customKeywords, isStrictMode)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "የጥበቃውን ብቃት እዚህ ይሞክሩ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ሲስተሙ እንደ «xn..»፣ «porn» ወይም ሌላ የአዋቂ ቃል እንዴት በቅጽበት እንደሚለይ ለመፈተሽ ከታች ጽፈው ይሞክሩ።",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
            value = testQueryText,
            onValueChange = onQueryChange,
            label = { Text("የሚፈተሸውን ቃል ወይም ድረ-ገጽ እዚህ ያስገቡ") },
            placeholder = { Text("ለምሳሌ፦ xn, adult, porn, xxx") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("test_query_input"),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text
            ),
            singleLine = true
        )

        // Result Card
        if (testQueryText.isNotBlank()) {
            if (testMatch != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ShieldAlertRed.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = ShieldAlertRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🚨 ወዲያውኑ ይታገዳል! (BLOCKED)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ShieldAlertRed
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "የተገኘው ቃል፦ «${testMatch.matchedTerm}»",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "ምድብ፦ ${testMatch.category}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "ማብራሪያ፦ ${testMatch.reasonAmharic}። ይህ በ Chrome ወይም በሌሎች አፖች ሲፈለግ ወዲያውኑ ይዘጋል።",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onPreviewScripture(testMatch.matchedTerm) },
                            colors = ButtonDefaults.buttonColors(containerColor = ShieldAlertRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("አስደንጋጩን የመጽሐፍ ቅዱስ ስክሪን ሞክር (Test Warning)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ShieldGreenDark.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ShieldGreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "✓ ደህንነቱ የተጠበቀ ይዘት (ALLOWED)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ShieldGreenPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ይህ ቃል ምንም አይነት የአዋቂ ወይም ተገቢ ያልሆነ ይዘት የለበትም።",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InstructionStep(number: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(ShieldAlertRed)
        ) {
            Text(
                text = number,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PinVerificationDialog(
    correctPin: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("የደህንነት ቁልፍ (PIN) ያስገቡ") },
        text = {
            Column {
                Text("ጥበቃውን ለማጥፋት ቀደም ሲል ያስቀመጡትን ባለ 4 አሃዝ የይለፍ ቃል ያስገቡ፦")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = { if (it.length <= 6) enteredPin = it },
                    singleLine = true,
                    isError = errorMsg != null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (enteredPin == correctPin) {
                        onSuccess()
                    } else {
                        errorMsg = "የተሳሳተ የይለፍ ቃል! እባክዎ ደግመው ይሞክሩ።"
                    }
                }
            ) {
                Text("አረጋግጥ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ሰርዝ")
            }
        }
    )
}

@Composable
private fun SetPinDialog(
    currentPin: String?,
    onSave: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var newPin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (currentPin.isNullOrEmpty()) "የራስ-መቆጣጠሪያ ቁልፍ (PIN) ያዘጋጁ" else "የደህንነት ቁልፍ አስተዳድር") },
        text = {
            Column {
                Text(
                    text = "በድካም ወቅት ጥበቃውን በቀላሉ እንዳያጠፉት ለመከላከል ባለ 4 አሃዝ የይለፍ ቃል ያስቀምጡ።"
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6) newPin = it },
                    placeholder = { Text("ለምሳሌ፦ 1234") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPin.length >= 4) {
                        onSave(newPin)
                    }
                },
                enabled = newPin.length >= 4
            ) {
                Text("አስቀምጥ")
            }
        },
        dismissButton = {
            Row {
                if (!currentPin.isNullOrEmpty()) {
                    TextButton(onClick = { onSave(null) }) {
                        Text("ቁልፉን አስወግድ", color = ShieldAlertRed)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("ሰርዝ")
                }
            }
        }
    )
}

@Composable
private fun AddKeywordDialog(
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var keyword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("የተከለከለ ቃል ወይም ጣቢያ አክል") },
        text = {
            Column {
                Text("ሊታገድ የሚገባውን ቃል፣ የጣቢያ አድራሻ ወይም ሐረግ ያስገቡ፦")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    placeholder = { Text("ለምሳሌ፦ badsite.com ወይም ቃል") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(keyword) },
                enabled = keyword.isNotBlank()
            ) {
                Text("አክል")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ሰርዝ")
            }
        }
    )
}

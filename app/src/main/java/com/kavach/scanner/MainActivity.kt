package com.kavach.scanner

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.textclassifier.TextClassifier
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kavach.core.model.InputType
import com.kavach.core.model.Verdict
import com.kavach.scanner.data.AccountItems
import com.kavach.scanner.scan.ScanUiState
import com.kavach.scanner.scan.ScanViewModel
import com.kavach.scanner.shield.MessageShieldService
import com.kavach.scanner.ui.components.pressable
import com.kavach.scanner.ui.screens.AccountsScreen
import com.kavach.scanner.ui.screens.AttacksScreen
import com.kavach.scanner.ui.screens.DeviceScreen
import com.kavach.scanner.ui.screens.GuideScreen
import com.kavach.scanner.ui.screens.HistoryScreen
import com.kavach.scanner.ui.screens.HomeActions
import com.kavach.scanner.ui.screens.HomeScreen
import com.kavach.scanner.ui.screens.HomeStats
import com.kavach.scanner.ui.screens.HubMode
import com.kavach.scanner.ui.screens.PasswordScreen
import com.kavach.scanner.ui.screens.PrivacyScreen
import com.kavach.scanner.ui.screens.QrScannerScreen
import com.kavach.scanner.ui.screens.ReportScreen
import com.kavach.scanner.ui.screens.ResultScreen
import com.kavach.scanner.ui.screens.ScanHubScreen
import com.kavach.scanner.ui.screens.SettingsScreen
import com.kavach.scanner.ui.screens.ShieldScreen
import com.kavach.scanner.ui.theme.K
import com.kavach.scanner.ui.theme.KavachTheme
import com.kavach.scanner.ui.theme.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: ScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Hide app contents from screenshots of the recents screen (scanned messages can be sensitive). MASVS-PLATFORM-3
        if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(false)
        setContent { KavachApp(vm) }
        if (savedInstanceState == null) handleShare(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    /** Text or images shared from WhatsApp, Messages, Gallery etc. are treated as untrusted input and only analysed. */
    private fun handleShare(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("image/") == true) {
                    val uri: Uri? = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    else @Suppress("DEPRECATION") intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    uri?.let { vm.scanImage(it) }
                } else {
                    val text = listOfNotNull(intent.getStringExtra(Intent.EXTRA_SUBJECT), intent.getStringExtra(Intent.EXTRA_TEXT)).joinToString("\n")
                    if (text.isNotBlank()) vm.scan(text.take(10_000), InputType.URL)
                }
            }
            Intent.ACTION_VIEW -> {
                val url = intent.dataString
                if (url != null && (url.startsWith("http://", true) || url.startsWith("https://", true))) vm.scanLink(url.take(4_000))
            }
            Intent.ACTION_PROCESS_TEXT -> {
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()?.takeIf { it.isNotBlank() }?.let { vm.scan(it.take(10_000), InputType.URL) }
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val leftTabs = listOf(Tab("home", "Home", Icons.Filled.Shield), Tab("history", "History", Icons.Filled.History))
private val rightTabs = listOf(Tab("guide", "Learn", Icons.AutoMirrored.Filled.MenuBook), Tab("settings", "Settings", Icons.Filled.Settings))
private val tabRoutes = (leftTabs + rightTabs).map { it.route } + "scan"

@Composable
internal fun KavachBottomBar(route: String?, onTab: (String) -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(top = 30.dp).fillMaxWidth().shadow(16.dp, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).background(K.surface)
                .navigationBarsPadding().height(66.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leftTabs.forEach { BarItem(it, route == it.route, Modifier.weight(1f)) { onTab(it.route) } }
            Spacer(Modifier.weight(1.1f))
            rightTabs.forEach { BarItem(it, route == it.route, Modifier.weight(1f)) { onTab(it.route) } }
        }
        // Center scan button floats above the bar, with a slow radar sweep.
        val t = rememberInfiniteTransition(label = "fab")
        val sweep by t.animateFloat(0f, 360f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "sweep")
        val glow by t.animateFloat(0.25f, 0.6f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "glow")
        val orange = K.orange
        val bar = K.surface
        Column(Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(70.dp).drawBehind {
                    drawCircle(orange.copy(alpha = glow), radius = size.minDimension / 2)
                    drawCircle(bar, radius = size.minDimension / 2 - 4.dp.toPx())
                }.padding(7.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFFFF8A50), orange, Color(0xFFD84315))))
                    .pressable(onClick = { onTab("scan") }),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(56.dp)) {
                    rotate(sweep) {
                        drawArc(
                            Brush.sweepGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.55f))),
                            0f, 90f, useCenter = true,
                        )
                    }
                }
                Icon(Icons.Filled.Radar, "Scan", tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Text("Scan", color = if (route == "scan") orange else K.text, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
private fun BarItem(t: Tab, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c by animateColorAsState(if (selected) K.orange else K.textDim, label = "tabc")
    val w by animateDpAsState(if (selected) 22.dp else 0.dp, label = "tabw")
    Column(modifier.clip(RoundedCornerShape(16.dp)).pressable(onClick = onClick).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(t.icon, t.label, tint = c, modifier = Modifier.size(24.dp))
        Text(t.label, color = c, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        Box(Modifier.padding(top = 3.dp).width(w).height(3.dp).clip(RoundedCornerShape(2.dp)).background(c))
    }
}

/** Opens a checked link in a real browser, never back in Kavach (Kavach may be chosen as the link handler). */
private fun openInBrowser(ctx: Context, url: String) {
    val view = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
    val pm = ctx.packageManager
    val candidates = runCatching { pm.queryIntentActivities(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE), 0) }
        .getOrDefault(emptyList()).map { it.activityInfo.packageName }.filter { it != ctx.packageName }.distinct()
    val preferred = runCatching { pm.resolveActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")), PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName }.getOrNull()
    val target = preferred?.takeIf { it in candidates } ?: candidates.firstOrNull()
    if (target != null) view.setPackage(target)
    runCatching { ctx.startActivity(view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** Android 12+: tells whether the clipboard holds a link WITHOUT reading it (no privacy toast). */
private fun clipLooksLikeLink(ctx: Context): Long? {
    if (Build.VERSION.SDK_INT < 31) return null
    val cm = ctx.getSystemService(ClipboardManager::class.java) ?: return null
    val d = runCatching { cm.primaryClipDescription }.getOrNull() ?: return null
    val score = runCatching { d.getConfidenceScore(TextClassifier.TYPE_URL) }.getOrDefault(0f)
    return if (score >= 0.5f) d.timestamp else null
}

private fun readClip(ctx: Context): String? =
    ctx.getSystemService(ClipboardManager::class.java)?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(ctx)?.toString()

@Composable
fun KavachApp(vm: ScanViewModel) {
    val nav = rememberNavController()
    val state by vm.state.collectAsStateWithLifecycle()
    val theme by vm.theme.collectAsStateWithLifecycle()
    val history by vm.historyItems.collectAsStateWithLifecycle()
    val device by vm.device.collectAsStateWithLifecycle()
    val deviceRunning by vm.deviceRunning.collectAsStateWithLifecycle()
    val privacy by vm.privacy.collectAsStateWithLifecycle()
    val privacyRunning by vm.privacyRunning.collectAsStateWithLifecycle()
    val breach by vm.breach.collectAsStateWithLifecycle()
    val breachRunning by vm.breachRunning.collectAsStateWithLifecycle()
    val pickScreenshot = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.scanImage(uri)
    }
    val openScreenshot = { pickScreenshot.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    LaunchedEffect(Unit) {
        // A scan shared from another app may have started before this screen existed.
        if (vm.state.value !is ScanUiState.Idle && nav.currentDestination?.route != "result") nav.navigate("result")
        vm.openResult.collect {
            if (nav.currentDestination?.route != "result") {
                nav.navigate("result") { popUpTo("home"); launchSingleTop = true }
            }
        }
    }

    fun go(r: String) = nav.navigate(r) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true }

    KavachTheme(theme) {
        val dark = K.isDark
        Scaffold(
            containerColor = K.bg,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = { if (route in tabRoutes) KavachBottomBar(route) { go(it) } },
        ) { inner ->
            NavHost(
                nav, startDestination = "home", modifier = Modifier.padding(inner),
                enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 8 } },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(260)) { it / 8 } },
            ) {
                composable("home") {
                    val ctx = LocalContext.current
                    var shieldOn by remember { mutableStateOf(false) }
                    var accountsPct by remember { mutableIntStateOf(0) }
                    var clipStamp by remember { mutableStateOf<Long?>(null) }
                    var handledStamp by rememberSaveable { mutableLongStateOf(0L) }
                    val scope = rememberCoroutineScope()
                    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                        shieldOn = vm.settings.messageShield && MessageShieldService.hasAccess(ctx)
                        accountsPct = vm.settings.accountChecklist.count { id -> AccountItems.all.any { it.id == id } } * 100 / AccountItems.all.size
                        // Clipboard is only readable once the window has focus.
                        scope.launch { delay(400); clipStamp = clipLooksLikeLink(ctx) }
                    }
                    HomeScreen(
                        stats = HomeStats(
                            history.size,
                            history.count { it.verdict == Verdict.DANGEROUS },
                            history.count { it.verdict == Verdict.SUSPICIOUS },
                            device?.score,
                            shieldOn,
                            accountsPct,
                            copiedLink = clipStamp != null && clipStamp != handledStamp,
                        ),
                        isDark = dark,
                        a = HomeActions(
                            onToggleTheme = { vm.setTheme(if (dark) ThemeMode.LIGHT else ThemeMode.DARK) },
                            onScanHub = { go("scan") },
                            onPasteScan = {
                                handledStamp = clipStamp ?: 0L
                                readClip(ctx)?.takeIf { it.isNotBlank() }?.let { vm.scan(it.take(10_000), InputType.URL) }
                            },
                            onLink = { go("scan") },
                            onQr = { nav.navigate("qr") },
                            onScreenshot = openScreenshot,
                            onCaller = { nav.navigate("caller") },
                            onDevice = { nav.navigate("device") },
                            onPrivacy = { nav.navigate("privacy") },
                            onPassword = { nav.navigate("password") },
                            onReport = { nav.navigate("report") },
                            onGuide = { go("guide") },
                            onHistory = { go("history") },
                            onShield = { nav.navigate("shield") },
                            onAccounts = { nav.navigate("accounts") },
                            onAttacks = { nav.navigate("attacks") },
                        ),
                    )
                }
                composable("scan") {
                    ScanHubScreen(HubMode.TEXT, onScanText = { vm.scan(it, InputType.URL) }, onScanCaller = { vm.scan(it, InputType.PHONE) },
                        onQr = { nav.navigate("qr") }, onScreenshot = openScreenshot, onBack = null)
                }
                composable("caller") {
                    ScanHubScreen(HubMode.CALLER, onScanText = { vm.scan(it, InputType.URL) }, onScanCaller = { vm.scan(it, InputType.PHONE) },
                        onQr = { nav.navigate("qr") }, onScreenshot = openScreenshot, onBack = { nav.popBackStack() })
                }
                composable("privacy") { PrivacyScreen(privacy, privacyRunning, onRun = { vm.runPrivacyAudit() }, onBack = { nav.popBackStack() }) }
                composable("qr") {
                    QrScannerScreen(onResult = { vm.scan(it, InputType.QR) }, onBack = { nav.popBackStack() })
                }
                composable("result") {
                    val ctxR = LocalContext.current
                    val linkToOpen by vm.linkToOpen.collectAsStateWithLifecycle()
                    ResultScreen(
                        state = state,
                        onBack = { vm.reset(); nav.popBackStack("home", inclusive = false) },
                        onScanAgain = { vm.reset(); nav.popBackStack("home", inclusive = false); go("scan") },
                        onReport = { nav.navigate("report") },
                        openUrl = linkToOpen,
                        onOpenUrl = { openInBrowser(ctxR, it) },
                    )
                }
                composable("device") { DeviceScreen(device, deviceRunning, onRun = { vm.runDeviceCheck() }, onBack = { nav.popBackStack() }) }
                composable("password") {
                    PasswordScreen(breach, breachRunning, onBreachCheck = { vm.checkBreach(it) }, onReset = { vm.resetBreach() }, onBack = { nav.popBackStack() })
                }
                composable("report") {
                    ReportScreen(prefill = (state as? ScanUiState.Done)?.report?.input, onBack = { nav.popBackStack() })
                }
                composable("history") {
                    HistoryScreen(history, vm.settings.saveHistory, onRescan = {
                        vm.scan(it.input, if (it.type == InputType.SCREENSHOT) InputType.MESSAGE else it.type)
                    }, onClear = { vm.clearHistory() })
                }
                composable("guide") { GuideScreen(onBack = null, onAttacks = { nav.navigate("attacks") }) }
                composable("shield") { ShieldScreen(vm.settings, onBack = { nav.popBackStack() }) }
                composable("accounts") { AccountsScreen(vm.settings, onPassword = { nav.navigate("password") }, onBack = { nav.popBackStack() }) }
                composable("attacks") { AttacksScreen(onBack = { nav.popBackStack() }) }
                composable("settings") { SettingsScreen(vm.settings, theme, onTheme = { vm.setTheme(it) }, onClearHistory = { vm.clearHistory() }) }
            }
        }
    }
}

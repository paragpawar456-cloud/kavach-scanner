package com.kavach.scanner

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kavach.core.model.InputType
import com.kavach.scanner.scan.ScanUiState
import com.kavach.scanner.scan.ScanViewModel
import com.kavach.scanner.ui.screens.AccountsScreen
import com.kavach.scanner.ui.screens.AttacksScreen
import com.kavach.scanner.ui.screens.DeviceScreen
import com.kavach.scanner.ui.screens.ShieldScreen
import com.kavach.scanner.ui.screens.GuideScreen
import com.kavach.scanner.ui.screens.HistoryScreen
import com.kavach.scanner.ui.screens.HomeStats
import com.kavach.scanner.ui.screens.PasswordScreen
import com.kavach.scanner.ui.screens.ReportScreen
import com.kavach.scanner.ui.screens.HomeScreen
import com.kavach.scanner.ui.screens.QrScannerScreen
import com.kavach.scanner.ui.screens.ResultScreen
import com.kavach.scanner.ui.screens.SettingsScreen
import com.kavach.scanner.ui.theme.K
import com.kavach.scanner.ui.theme.KavachTheme
import com.kavach.scanner.ui.theme.ThemeMode

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
            Intent.ACTION_PROCESS_TEXT -> {
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()?.takeIf { it.isNotBlank() }?.let { vm.scan(it.take(10_000), InputType.URL) }
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Filled.Shield),
    Tab("history", "History", Icons.Filled.History),
    Tab("guide", "Learn", Icons.AutoMirrored.Filled.MenuBook),
    Tab("settings", "Settings", Icons.Filled.Settings),
)

@Composable
fun KavachApp(vm: ScanViewModel) {
    val nav = rememberNavController()
    val state by vm.state.collectAsStateWithLifecycle()
    val theme by vm.theme.collectAsStateWithLifecycle()
    val history by vm.historyItems.collectAsStateWithLifecycle()
    val device by vm.device.collectAsStateWithLifecycle()
    val deviceRunning by vm.deviceRunning.collectAsStateWithLifecycle()
    val breach by vm.breach.collectAsStateWithLifecycle()
    val breachRunning by vm.breachRunning.collectAsStateWithLifecycle()
    val pickScreenshot = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.scanImage(uri)
    }
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

    KavachTheme(theme) {
        val dark = K.isDark
        Scaffold(
            containerColor = K.bg,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (tabs.any { it.route == route }) {
                    NavigationBar(containerColor = K.surface, tonalElevation = 0.dp) {
                        tabs.forEach { t ->
                            NavigationBarItem(
                                selected = route == t.route,
                                onClick = {
                                    nav.navigate(t.route) {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(t.icon, t.label) },
                                label = { Text(t.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White, indicatorColor = K.orange, selectedTextColor = K.orange,
                                    unselectedIconColor = K.textDim, unselectedTextColor = K.textDim,
                                ),
                            )
                        }
                    }
                }
            },
        ) { inner ->
            NavHost(
                nav, startDestination = "home", modifier = Modifier.padding(inner),
                enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 8 } },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(260)) { it / 8 } },
            ) {
                composable("home") {
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    var shieldOn by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        shieldOn = vm.settings.messageShield && com.kavach.scanner.shield.MessageShieldService.hasAccess(ctx)
                    }
                    val st = remember(history) { history.size to history }
                    HomeScreen(
                        stats = HomeStats(
                            st.first,
                            history.count { it.verdict == com.kavach.core.model.Verdict.DANGEROUS },
                            history.count { it.verdict == com.kavach.core.model.Verdict.SUSPICIOUS },
                            device?.score,
                            shieldOn,
                        ),
                        isDark = dark,
                        onToggleTheme = { vm.setTheme(if (dark) ThemeMode.LIGHT else ThemeMode.DARK) },
                        onScanText = { vm.scan(it, InputType.URL) },
                        onQr = { nav.navigate("qr") },
                        onScreenshot = { pickScreenshot.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onDevice = { nav.navigate("device") },
                        onPassword = { nav.navigate("password") },
                        onReport = { nav.navigate("report") },
                        onGuide = { nav.navigate("guide") { launchSingleTop = true } },
                        onHistory = { nav.navigate("history") { launchSingleTop = true } },
                        onShield = { nav.navigate("shield") },
                        onAccounts = { nav.navigate("accounts") },
                        onAttacks = { nav.navigate("attacks") },
                    )
                }
                composable("qr") {
                    QrScannerScreen(onResult = { vm.scan(it, InputType.QR) }, onBack = { nav.popBackStack() })
                }
                composable("result") {
                    ResultScreen(
                        state = state,
                        onBack = { vm.reset(); nav.popBackStack("home", inclusive = false) },
                        onScanAgain = { vm.reset(); nav.popBackStack("home", inclusive = false) },
                        onReport = { nav.navigate("report") },
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
                    HistoryScreen(history, vm.settings.saveHistory, onRescan = { vm.scan(it.input, if (it.type == InputType.QR) InputType.QR else InputType.URL) }, onClear = { vm.clearHistory() })
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

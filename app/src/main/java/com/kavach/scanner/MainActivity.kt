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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kavach.core.model.InputType
import com.kavach.scanner.scan.ScanUiState
import com.kavach.scanner.scan.ScanViewModel
import com.kavach.scanner.ui.screens.GuideScreen
import com.kavach.scanner.ui.screens.HomeScreen
import com.kavach.scanner.ui.screens.QrScannerScreen
import com.kavach.scanner.ui.screens.ResultScreen
import com.kavach.scanner.ui.screens.SettingsScreen
import com.kavach.scanner.ui.theme.KavachTheme

class MainActivity : ComponentActivity() {

    private val vm: ScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Hide app contents from screenshots of the recents screen (scanned messages can be sensitive). MASVS-PLATFORM-3
        if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(false)
        setContent { KavachTheme { KavachApp(vm) } }
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

@Composable
fun KavachApp(vm: ScanViewModel) {
    val nav = rememberNavController()
    val state by vm.state.collectAsStateWithLifecycle()
    val pickScreenshot = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.scanImage(uri)
    }

    LaunchedEffect(Unit) {
        // A scan shared from another app may have started before this screen existed.
        if (vm.state.value !is ScanUiState.Idle && nav.currentDestination?.route != "result") nav.navigate("result")
        vm.openResult.collect {
            if (nav.currentDestination?.route != "result") {
                nav.navigate("result") { popUpTo("home"); launchSingleTop = true }
            }
        }
    }

    NavHost(nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onScanText = { vm.scan(it, InputType.URL) },
                onQr = { nav.navigate("qr") },
                onScreenshot = { pickScreenshot.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onGuide = { nav.navigate("guide") },
                onSettings = { nav.navigate("settings") },
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
            )
        }
        composable("settings") { SettingsScreen(vm.settings, onBack = { nav.popBackStack() }) }
        composable("guide") { GuideScreen(onBack = { nav.popBackStack() }) }
    }
}

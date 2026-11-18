package com.kavach.scanner

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.kavach.core.CallerCheck
import com.kavach.core.RiskScorer
import com.kavach.core.analyzers.MessageAnalyzer
import com.kavach.core.analyzers.UrlHeuristics
import com.kavach.core.model.CheckStatus
import com.kavach.core.model.InputType
import com.kavach.core.util.DomainUtils
import com.kavach.scanner.scan.ScanUiState
import com.kavach.scanner.ui.screens.GuideScreen
import com.kavach.scanner.ui.screens.HomeScreen
import com.kavach.scanner.ui.screens.HomeStats
import com.kavach.scanner.ui.screens.ResultScreen
import com.kavach.scanner.ui.theme.KavachTheme
import com.kavach.scanner.ui.theme.ThemeMode
import org.junit.Rule
import org.junit.Test

/** Renders key screens to PNG (no device needed) so the design can be reviewed. Run: ./gradlew :app:recordPaparazziDebug */
class ScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, theme = "android:Theme.Material.Light.NoActionBar", maxPercentDifference = 1.0)

    private fun shot(dark: Boolean = false, content: @Composable () -> Unit) =
        paparazzi.snapshot { KavachTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) { content() } }

    private val dangerous by lazy {
        val url = "http://sbi-kyc-update.xyz/login"
        val f = MessageAnalyzer.analyze("Dear customer your SBI account will be blocked today. Update KYC immediately $url") +
            UrlHeuristics.analyze(DomainUtils.parse(url)!!)
        RiskScorer.report(url, InputType.MESSAGE, f, listOf(url), listOf(CheckStatus("Address & look-alike analysis", true, "6 signals"), CheckStatus("VirusTotal", false, "Add an API key in Settings")))
    }

    @Test fun home() = shot { HomeUnderTest() }
    @Test fun homeDark() = shot(dark = true) { HomeUnderTest() }
    @Test fun resultDangerous() = shot { ResultScreen(ScanUiState.Done(dangerous), {}, {}, {}) }
    @Test fun resultCaller() = shot { ResultScreen(ScanUiState.Done(CallerCheck.analyze("1600123456")), {}, {}, {}) }
    @Test fun learn() = shot { GuideScreen(onBack = null) }

    @Composable
    private fun HomeUnderTest() = HomeScreen(
        stats = HomeStats(12, 3, 2, 82, true), isDark = false, onToggleTheme = {}, onScanText = {}, onQr = {}, onScreenshot = {},
        onDevice = {}, onPassword = {}, onReport = {}, onGuide = {}, onHistory = {}, onShield = {}, onAccounts = {}, onAttacks = {},
    )
}

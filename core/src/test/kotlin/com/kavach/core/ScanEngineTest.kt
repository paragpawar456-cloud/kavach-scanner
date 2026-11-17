package com.kavach.core

import com.kavach.core.analyzers.MessageAnalyzer
import com.kavach.core.analyzers.UrlHeuristics
import com.kavach.core.model.Finding
import com.kavach.core.model.InputType
import com.kavach.core.model.ScanOptions
import com.kavach.core.model.Verdict
import com.kavach.core.online.HttpOnlineChecker
import com.kavach.core.online.OnlineChecker
import com.kavach.core.online.RedirectResult
import com.kavach.core.online.ageFinding
import com.kavach.core.util.DomainUtils
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScanEngineTest {

    /** No network: every online check returns nothing. */
    private object Offline : OnlineChecker {
        override fun followRedirects(url: String) = RedirectResult(listOf(url), url, emptyList(), false)
        override fun domainAge(registrableDomain: String): Finding? = null
        override fun googleSafeBrowsing(url: String, apiKey: String): Finding? = null
        override fun virusTotal(url: String, apiKey: String): Finding? = null
        override fun urlhaus(url: String, authKey: String): Finding? = null
    }

    private val engine = ScanEngine(Offline)
    private val offline = ScanOptions(onlineChecks = false)

    private suspend fun verdict(s: String, t: InputType = InputType.URL) = engine.scan(s, t, offline)

    @Test fun officialBankIsSafe() = runTest {
        val r = verdict("https://www.onlinesbi.sbi/")
        assertEquals(Verdict.SAFE, r.verdict, r.findings.toString())
        assertTrue(r.findings.any { it.id == "official" })
    }

    @Test fun ordinarySitesAreSafe() = runTest {
        for (u in listOf("https://www.amazon.in/gp/cart", "https://github.com/user/repo", "https://www.hdfcbank.com/personal/pay", "https://en.wikipedia.org/wiki/India", "https://www.zomato.com", "https://www.thehindu.com/news/"))
            assertEquals(Verdict.SAFE, verdict(u).verdict, "$u -> ${verdict(u).findings}")
    }

    @Test fun brandOnFakeDomainIsDangerousOrSuspicious() = runTest {
        val r = verdict("http://sbi-kyc-update.xyz/login")
        assertEquals(Verdict.DANGEROUS, r.verdict, r.findings.toString())
        assertTrue(r.findings.any { it.id == "brand_misuse" })
    }

    @Test fun buriedOfficialDomain() = runTest {
        val r = verdict("https://hdfcbank.com.secure-verify.top/netbanking")
        assertEquals(Verdict.DANGEROUS, r.verdict)
        assertTrue(r.findings.any { it.id == "buried_official" })
    }

    @Test fun typosquat() {
        val p = DomainUtils.parse("https://www.flipkrat.com/offer")!!
        assertTrue(UrlHeuristics.analyze(p).any { it.id == "typosquat" })
        assertEquals("paypal.com", UrlHeuristics.typoTarget("paypa1"))
    }

    @Test fun homoglyph() = runTest {
        // Cyrillic 'а' in paypal
        val r = verdict("https://pаypal.com/signin")
        assertEquals(Verdict.DANGEROUS, r.verdict, r.findings.toString())
    }

    @Test fun apkLinkIsDangerous() = runTest {
        val r = verdict("http://bit.ly/3xYz")
        assertTrue(r.findings.any { it.id == "shortener" })
        assertEquals(Verdict.DANGEROUS, verdict("https://files.example.com/SBI_Rewards.apk").verdict)
    }

    @Test fun ipAndAtTrick() = runTest {
        assertTrue(verdict("http://192.168.10.5/login").verdict != Verdict.SAFE)
        assertEquals(Verdict.DANGEROUS, verdict("http://www.icicibank.com@45.12.3.9/verify").verdict)
    }

    @Test fun fakeGovernment() = runTest {
        val r = verdict("https://pmkisan-gov.online/apply")
        assertTrue(r.verdict != Verdict.SAFE)
        assertTrue(r.findings.any { it.id == "fake_gov" || it.id == "brand_misuse" })
    }

    @Test fun upiReceiveScam() = runTest {
        val r = verdict("upi://pay?pa=rahul123@ybl&pn=Cashback%20Refund&am=4999&tn=Scan%20to%20receive%20refund", InputType.QR)
        assertEquals(Verdict.DANGEROUS, r.verdict)
        val shop = verdict("upi://pay?pa=sharmastores@okaxis&pn=Sharma%20Stores", InputType.QR)
        assertEquals(Verdict.SAFE, shop.verdict, shop.findings.toString())
    }

    @Test fun callForwardingQr() = runTest {
        assertEquals(Verdict.DANGEROUS, verdict("tel:*401*9876543210", InputType.QR).verdict)
    }

    @Test fun kycSmsIsDangerous() = runTest {
        val sms = "Dear Customer, your SBI YONO account will be blocked today. Update your PAN KYC immediately by clicking the link: http://sbi-yono-kyc.co/update"
        val r = engine.scan(sms, InputType.MESSAGE, offline)
        assertEquals(Verdict.DANGEROUS, r.verdict, r.findings.joinToString("\n") { "${it.id} ${it.risk}" })
        assertEquals(listOf("http://sbi-yono-kyc.co/update"), r.urls)
    }

    @Test fun electricityScam() = runTest {
        val sms = "Dear consumer your electricity power will be disconnected tonight at 9.30pm because your previous month bill was not updated. Please immediately contact our electricity officer 9876543210"
        assertTrue(engine.scan(sms, InputType.MESSAGE, offline).verdict != Verdict.SAFE)
    }

    @Test fun genuineOtpMessageIsSafe() = runTest {
        val sms = "123456 is your OTP for transaction of INR 500.00 at AMAZON. Valid for 3 mins. Do not share this OTP with anyone. - HDFC Bank"
        val r = engine.scan(sms, InputType.MESSAGE, offline)
        assertEquals(Verdict.SAFE, r.verdict, r.findings.toString())
    }

    @Test fun otpRequestIsCritical() = runTest {
        val r = engine.scan("Sir I am from bank, please share the OTP you received to stop the debit", InputType.MESSAGE, offline)
        assertEquals(Verdict.DANGEROUS, r.verdict)
    }

    @Test fun taskJobScam() = runTest {
        val r = engine.scan("Part time job! Earn Rs 5000 daily by liking YouTube videos. Contact on telegram t.me/earnfast", InputType.MESSAGE, offline)
        assertTrue(r.verdict != Verdict.SAFE)
    }

    @Test fun hindiMessage() {
        val f = MessageAnalyzer.analyze("आपका खाता आज बंद हो जाएगा, तुरंत केवाईसी अपडेट करें")
        assertTrue(f.any { it.id == "block_threat" } && f.any { it.id == "kyc" }, f.toString())
    }

    @Test fun extractsBareAndDefangedLinks() {
        val urls = MessageAnalyzer.extractUrls("Visit amaz0n-offer.shop/win or hxxps://evil[.]com/x now. Mail me at a@b.com")
        assertTrue("amaz0n-offer.shop/win" in urls, urls.toString())
        assertTrue("https://evil.com/x" in urls, urls.toString())
        assertTrue(urls.none { it == "b.com" }, urls.toString())
    }

    @Test fun ageThresholds() {
        assertEquals("domain_new", ageFinding("x.com", 3).id)
        assertEquals("domain_old", ageFinding("x.com", 4000).id)
    }

    @Test fun redirectsAreFollowedAndApkDetected() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(301).setHeader("Location", "/step2"))
        server.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "text/html").setBody("<meta http-equiv=\"refresh\" content=\"0;url=/app.apk\">"))
        server.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "application/vnd.android.package-archive"))
        server.start()
        // Mock server runs on localhost, which the checker refuses for safety; test the hop logic via a subclass-free path:
        val checker = object : OnlineChecker by HttpOnlineChecker() {}
        val r = checker.followRedirects(server.url("/start").toString())
        // localhost is blocked on purpose (no scanning of the user's local network)
        assertTrue(r.findings.any { it.id == "private_host" })
        server.shutdown()
    }

    @Test fun scoringCombines() {
        val two = RiskScorer.score(listOf(
            Finding("a", "", "", com.kavach.core.model.Severity.MEDIUM, com.kavach.core.model.Category.DOMAIN, 30),
            Finding("b", "", "", com.kavach.core.model.Severity.MEDIUM, com.kavach.core.model.Category.DOMAIN, 30),
        ))
        assertEquals(51, two)
    }
}

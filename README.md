# Kavach – Link, QR & Message Scam Scanner (Android)

**Scan before you click.** Kavach checks links, QR codes, SMS/WhatsApp messages and screenshots for phishing,
malware and financial-fraud risk, and gives a clear verdict — **Looks Safe / Suspicious / Dangerous** — with a
0-100 risk score and plain-language reasons.

## What it checks (multiple independent layers)

| Layer | What it looks for | Needs internet? |
|---|---|---|
| Address analysis | Raw IP links, the `@` trick, unusual ports, plain http, long/encoded links, open redirects | No |
| Look-alike detection | Bank/brand names on fake domains (`sbi-kyc-update.xyz`), official domain hidden inside another (`hdfcbank.com.verify.top`), typo-domains (`flipkrat.com`), fake letters from other alphabets (`pаypal.com` with Cyrillic а), fake government sites | No |
| Cheap / hidden hosting | URL shorteners, free website builders, TLDs often abused by scammers | No |
| Downloads | `.apk` and other program files (fake bank/challan/PM-Kisan apps) | No |
| Message wording | KYC/PAN/account-block threats, lottery/KBC, electricity disconnection, "digital arrest"/CBI/customs, delivery problems, part-time job tasks, investment groups, loan apps, AnyDesk/TeamViewer requests, OTP requests, "money sent by mistake" – English, Hinglish and Hindi | No |
| UPI / QR payment | "Scan to receive refund" scams, payee posing as RBI/police, unknown UPI handles, pre-filled amounts | No |
| Special QR codes | Call-forwarding codes (`*401*`, `**21*`), USSD, SMS-sending, open Wi-Fi, `javascript:`/`data:` payloads | No |
| Live inspection (optional) | Expands short links and redirects, checks the HTTPS certificate, spots pages asking for passwords/card/PIN, detects APK downloads. No JavaScript, no cookies | Yes |
| Domain age (optional) | Domains registered days ago (RDAP – the modern WHOIS) | Yes |
| Threat databases (optional, your own free keys) | Google Safe Browsing, VirusTotal, URLhaus (abuse.ch) | Yes |

Signals are combined into one score (independent risks combine like probabilities), and any critical signal
(e.g. Google lists it as phishing, or it downloads an APK) makes the result **Dangerous**.

### Other features
- **QR scanner** (CameraX + on-device ML Kit). The code is analysed, never opened automatically. Can also read a QR from a gallery image.
- **Screenshot check**: pick a screenshot of an SMS/WhatsApp message; text (English + Hindi/Devanagari) and any QR codes in it are read on the phone and checked.
- **Share to Kavach**: in WhatsApp/Messages long-press a message → Share → *Check with Kavach*. Or select any text → *Check with Kavach*.
- **Help built in**: one tap to call **1930**, report at **cybercrime.gov.in**, or report the number on **Sanchar Saathi – Chakshu**.
- **Safety guide**: common Indian scams, golden rules, where to report.

## Standards & guidance followed
- **India**: CERT-In advisories (smishing, malicious APKs), IT Act 2000 (s.43, 66C, 66D), DPDP Act 2023 (data minimisation, purpose limitation – the app collects nothing), RBI BE(A)WARE booklet and digital payment security directions, NPCI UPI safety guidance, I4C/cybercrime.gov.in advisories, DoT Sanchar Saathi.
- **Global**: OWASP MASVS v2 / Mobile Top 10 (encrypted key storage in Android Keystore, minimal permissions, backups disabled, no WebView, recents-screen hiding), NIST CSF 2.0 & SP 800-52/63 (layered defence, TLS validation), ISO/IEC 27001:2022 principles (least privilege, secure defaults), Unicode TR39 (homoglyphs).

## Privacy
Only two permissions: **Internet** (optional checks) and **Camera** (QR screen only). No account, ads, analytics or
history. Screenshots never leave the phone. With online checks on, the link's domain goes to rdap.org, the link is
fetched (like a browser without JavaScript), and — only if you added keys — the link is sent to those providers.
Everything online can be switched off in Settings.

---

## How to get the app on your phone

### Option 1 – Let GitHub build it for you (no software to install)
1. Put this project in a GitHub repository (Claude can push it for you – just create an empty repository and share its name).
2. Open the repository on github.com → **Actions** tab → **Build APK**. A build starts automatically on every update (about 5–8 minutes). You can also press **Run workflow**.
3. When it shows a green tick, open the run and download **Kavach-APK** at the bottom. Unzip it to get `app-debug.apk`.
4. Copy the APK to your phone (WhatsApp it to yourself, Google Drive, or USB) and tap it.
5. Android will ask to **allow installing from this source** – allow it for that app, then tap **Install**.

### Option 2 – Build on your computer with Android Studio
1. Install **Android Studio** (free) from developer.android.com/studio. Accept the default setup; it downloads the Android SDK.
2. Unzip this project. In Android Studio choose **File → Open** and select the `kavach-scanner` folder. Wait until "Gradle sync" finishes (first time can take 5–10 minutes).
3. Connect your phone with a USB cable. On the phone: **Settings → About phone → tap "Build number" 7 times**, then **Settings → Developer options → USB debugging: ON**.
4. Press the green ▶ **Run** button. The app installs and opens on your phone.
5. To get an APK file instead: **Build → Build App Bundle(s)/APK(s) → Build APK(s)** → click **locate**. The file is `app/build/outputs/apk/debug/app-debug.apk`.

### Optional: add free threat-database keys (much stronger checks)
Open Kavach → ⚙ Settings, paste any of:
- **Google Safe Browsing** – console.cloud.google.com → create project → enable "Safe Browsing API" → Credentials → Create API key.
- **VirusTotal** – sign up at virustotal.com → your profile → API key (free: 4 lookups/minute).
- **URLhaus** – sign in at auth.abuse.ch → copy your Auth-Key.

Keys are saved encrypted on the phone only.

## For developers
- Kotlin 2.1, Jetpack Compose + Material 3, AGP 8.7, minSdk 26, targetSdk 35, JDK 17.
- `core/` – pure Kotlin scan engine (no Android), unit-tested: `./gradlew :core:test`.
- `app/` – Android UI, CameraX, ML Kit (barcode, text Latin + Devanagari), EncryptedSharedPreferences.
- Extend brand/official-domain lists in `core/.../util/KnownData.kt`, message patterns in `MessageAnalyzer.kt`.
- The release build is signed with the debug key for easy sideloading; create your own keystore before publishing on Play Store.

*Kavach is an awareness and risk-assessment tool, not a guarantee. It is not affiliated with any bank or government agency.*

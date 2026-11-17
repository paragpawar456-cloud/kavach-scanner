package com.kavach.scanner.data

data class AccountItem(val id: String, val title: String, val why: String, val link: String?)

object AccountItems {
    val all = listOf(
        AccountItem("wa2fa", "WhatsApp two-step verification", "Stops anyone registering your WhatsApp on another phone. WhatsApp → Settings → Account → Two-step verification.", null),
        AccountItem("g2fa", "Google 2-Step Verification", "Protects Gmail, Google Pay, Drive and Play Store.", "https://myaccount.google.com/signinoptions/two-step-verification"),
        AccountItem("gcheck", "Google Security Checkup", "Remove old devices and unknown apps with access to your account.", "https://myaccount.google.com/security-checkup"),
        AccountItem("aadhaar", "Lock Aadhaar biometrics", "Stops AePS withdrawals with cloned fingerprints.", "https://myaadhaar.uidai.gov.in/"),
        AccountItem("tafcop", "Check SIMs on your name", "Find and report SIM cards someone took on your ID (Sanchar Saathi – TAFCOP).", "https://tafcop.sancharsaathi.gov.in/"),
        AccountItem("ceir", "Know how to block a lost phone", "CEIR blocks a stolen phone on all networks.", "https://www.ceir.gov.in/"),
        AccountItem("findmy", "Turn on Find My Device", "Locate, lock or erase a lost phone.", "https://www.google.com/android/find"),
        AccountItem("upilimit", "Set UPI and card limits", "Lower your daily UPI limit and turn off international/online use on cards you don't use (in your bank app).", null),
        AccountItem("simpin", "Set a SIM PIN", "A stolen SIM can't be used in another phone to receive your OTPs.", null),
        AccountItem("unknownapps", "Block 'Install unknown apps'", "Turn it off for WhatsApp, Chrome and file managers so APKs can't install.", "settings:unknown"),
        AccountItem("playprotect", "Play Protect is on", "Play Store → profile → Play Protect → Scan.", null),
        AccountItem("passwords", "Unique passwords for email & banking", "Use the Password check tool and a password manager.", "kavach:password"),
        AccountItem("backup", "Back up photos & contacts", "Protects you from ransomware and phone loss.", null),
        AccountItem("family", "Teach family the golden rules", "Most victims are parents and grandparents. Share the Learn tab with them.", null),
    )
}

package com.kavach.scanner.data

import java.util.Calendar

object Tips {
    val all = listOf(
        "Your bank will never ask for your OTP, UPI PIN, CVV or password. Not by call, SMS, WhatsApp or e-mail.",
        "You never need to scan a QR code or enter your UPI PIN to RECEIVE money.",
        "Never install .apk files sent on WhatsApp. Fake 'RTO challan' and 'wedding card' APKs steal OTPs.",
        "There is no 'digital arrest' in Indian law. Police and CBI never question people on video calls.",
        "Lost money? Call 1930 within the first hour: the 'golden hour' gives the best chance to freeze it.",
        "Search customer-care numbers only on the official website. Fake numbers are planted on Google and Maps.",
        "Turn on two-step verification in WhatsApp: Settings → Account → Two-step verification.",
        "Never dial codes like *401* or **21* on a caller's request. They forward your calls and OTPs.",
        "'Part-time job, earn ₹5000 daily by liking videos' always ends with you paying money.",
        "Check the sender ID: genuine bank SMS come from IDs like AX-HDFCBK, not from 10-digit mobile numbers.",
        "Keep your phone updated: security patches close holes that malware uses.",
        "Lock your Aadhaar biometrics at myaadhaar.uidai.gov.in to stop AePS fraud.",
        "Check how many SIMs are registered on your Aadhaar at sancharsaathi.gov.in (TAFCOP).",
        "Use a different password for every important account, or a password manager.",
        "Before investing, check the advisor is SEBI-registered. Guaranteed high returns are always a scam.",
        "Cover your PIN at ATMs and shops; skimmers and hidden cameras are common.",
        "Don't charge your phone at public USB ports. Use your own adapter (juice-jacking).",
        "Avoid net banking on free public Wi-Fi.",
        "If someone asks you to install AnyDesk or TeamViewer, hang up. It gives full control of your phone.",
        "A small amount 'credited by mistake' followed by a request to return more is a scam. Check your bank app.",
    )

    fun today(): String = all[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % all.size]
}

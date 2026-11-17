package com.kavach.core.util

/**
 * Reference lists used by the offline heuristics. These are small, curated and easy to extend.
 * Sources of the scam themes: CERT-In advisories, RBI "BE(A)WARE" booklet on frauds,
 * I4C / cybercrime.gov.in public alerts, NPCI UPI safety guidance.
 */
object KnownData {

    /** Brand keyword -> official registrable domains. Brand words found on any other domain are a red flag. */
    val brands: Map<String, Set<String>> = mapOf(
        // Indian banks & payments
        "sbi" to setOf("sbi.co.in", "onlinesbi.sbi", "sbi.bank.in", "sbicard.com", "yonosbi.com", "onlinesbi.com"),
        "onlinesbi" to setOf("onlinesbi.sbi", "onlinesbi.com", "sbi.co.in"),
        "yono" to setOf("yonosbi.com", "sbi.co.in", "onlinesbi.sbi"),
        "hdfc" to setOf("hdfcbank.com", "hdfc.com", "hdfclife.com", "hdfcergo.com", "hdfc.bank.in", "hdfcsec.com"),
        "hdfcbank" to setOf("hdfcbank.com", "hdfc.bank.in"),
        "icici" to setOf("icicibank.com", "icicidirect.com", "iciciprulife.com", "icicilombard.com", "icici.bank.in"),
        "icicibank" to setOf("icicibank.com", "icici.bank.in"),
        "axisbank" to setOf("axisbank.com", "axis.bank.in"),
        "kotak" to setOf("kotak.com", "kotaksecurities.com", "kotak.bank.in"),
        "pnb" to setOf("pnbindia.in", "pnb.bank.in"),
        "bankofbaroda" to setOf("bankofbaroda.in", "bankofbaroda.com", "bobcard.co.in"),
        "canarabank" to setOf("canarabank.com", "canarabank.in"),
        "unionbank" to setOf("unionbankofindia.co.in"),
        "indusind" to setOf("indusind.com"),
        "yesbank" to setOf("yesbank.in"),
        "idfc" to setOf("idfcfirstbank.com"),
        "rbi" to setOf("rbi.org.in"),
        "npci" to setOf("npci.org.in"),
        "bhim" to setOf("bhimupi.org.in", "npci.org.in"),
        "paytm" to setOf("paytm.com", "paytm.in", "paytmbank.com", "paytmmoney.com"),
        "phonepe" to setOf("phonepe.com"),
        "gpay" to setOf("google.com", "pay.google.com"),
        "mobikwik" to setOf("mobikwik.com"),
        "cred" to setOf("cred.club"),
        // Government
        "incometax" to setOf("incometax.gov.in", "incometaxindia.gov.in"),
        "uidai" to setOf("uidai.gov.in"),
        "aadhaar" to setOf("uidai.gov.in", "myaadhaar.uidai.gov.in"),
        "epfo" to setOf("epfindia.gov.in", "epfo.gov.in"),
        "irctc" to setOf("irctc.co.in", "irctc.com"),
        "indiapost" to setOf("indiapost.gov.in"),
        "digilocker" to setOf("digilocker.gov.in"),
        "gst" to setOf("gst.gov.in"),
        "parivahan" to setOf("parivahan.gov.in"),
        "echallan" to setOf("parivahan.gov.in"),
        "fastag" to setOf("npci.org.in", "ihmcl.co.in"),
        "cowin" to setOf("cowin.gov.in"),
        "pmkisan" to setOf("pmkisan.gov.in"),
        // Shopping, telecom, utilities
        "amazon" to setOf("amazon.in", "amazon.com", "amazon.co.uk", "amazonpay.in", "amazon.de"),
        "flipkart" to setOf("flipkart.com"),
        "myntra" to setOf("myntra.com"),
        "meesho" to setOf("meesho.com"),
        "jio" to setOf("jio.com", "ril.com"),
        "airtel" to setOf("airtel.in", "airtel.com"),
        "bsnl" to setOf("bsnl.co.in", "bsnl.in"),
        "vodafone" to setOf("myvi.in", "vodafone.com"),
        "tatapower" to setOf("tatapower.com", "tatapower-ddl.com"),
        "bescom" to setOf("bescom.co.in"),
        "mahadiscom" to setOf("mahadiscom.in"),
        "adani" to setOf("adanielectricity.com", "adani.com"),
        "bluedart" to setOf("bluedart.com"),
        "fedex" to setOf("fedex.com"),
        "dhl" to setOf("dhl.com", "dhl.co.in"),
        "delhivery" to setOf("delhivery.com"),
        // Global
        "google" to setOf("google.com", "google.co.in", "youtube.com", "goo.gl", "g.co"),
        "gmail" to setOf("google.com", "gmail.com"),
        "apple" to setOf("apple.com", "icloud.com"),
        "icloud" to setOf("icloud.com", "apple.com"),
        "microsoft" to setOf("microsoft.com", "live.com", "office.com", "microsoftonline.com"),
        "outlook" to setOf("outlook.com", "live.com", "office.com", "microsoft.com"),
        "whatsapp" to setOf("whatsapp.com", "whatsapp.net", "wa.me"),
        "facebook" to setOf("facebook.com", "fb.com", "fb.me"),
        "instagram" to setOf("instagram.com"),
        "netflix" to setOf("netflix.com"),
        "paypal" to setOf("paypal.com", "paypal.me"),
        "binance" to setOf("binance.com"),
        "telegram" to setOf("telegram.org", "t.me"),
        "linkedin" to setOf("linkedin.com", "lnkd.in"),
    )

    /** Every official domain flattened, used for typo-squat comparisons. */
    val officialDomains: Set<String> by lazy { brands.values.flatten().toSet() }

    /** Domains that are very widely used and well established (not a guarantee of safety: content on them can be abused). */
    val wellKnownDomains: Set<String> = officialDomains + setOf(
        "wikipedia.org", "github.com", "gov.in", "nic.in", "cert-in.org.in", "cybercrime.gov.in",
        "sancharsaathi.gov.in", "x.com", "twitter.com", "reddit.com", "zoho.com", "swiggy.com", "zomato.com",
    )

    val urlShorteners: Set<String> = setOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd", "buff.ly", "cutt.ly", "rb.gy", "rebrand.ly",
        "shorturl.at", "tiny.cc", "t.ly", "bl.ink", "s.id", "v.gd", "shorte.st", "adf.ly", "bit.do", "soo.gd",
        "clck.ru", "qr.ae", "lnkd.in", "trib.al", "dlvr.it", "u.to", "x.co", "short.gy", "surl.li", "urlz.fr",
        "wa.link", "linktr.ee", "tinu.be", "1url.cz", "gg.gg", "chilp.it", "shrtco.de", "inx.lv", "kutt.it",
    )

    /** TLDs frequently abused for phishing (cheap/free registration); seen in Spamhaus/Interisle TLD reports. */
    val riskyTlds: Set<String> = setOf(
        "xyz", "top", "club", "tk", "ml", "ga", "cf", "gq", "buzz", "icu", "cyou", "rest", "sbs", "click",
        "link", "work", "support", "online", "site", "live", "shop", "store", "fun", "monster", "quest",
        "bond", "cfd", "lol", "mom", "beauty", "hair", "skin", "boats", "autos", "makeup", "zip", "mov",
        "country", "kim", "loan", "win", "bid", "review", "stream", "racing", "download", "party", "gdn",
        "men", "date", "faith", "accountant", "cricket", "science", "trade", "webcam", "pw", "cc", "su",
        "today", "vip", "info", "asia", "biz",
    )

    /** Second-level labels under which a registrable domain sits one level deeper (simplified public-suffix list). */
    val multiPartSuffixes: Set<String> = setOf(
        "co.in", "net.in", "org.in", "gov.in", "nic.in", "ac.in", "edu.in", "res.in", "firm.in", "gen.in", "ind.in",
        "bank.in", "co.uk", "org.uk", "gov.uk", "ac.uk", "com.au", "net.au", "org.au", "co.nz", "co.jp", "co.za",
        "com.br", "com.cn", "com.sg", "com.my", "co.id", "com.pk", "com.bd", "com.np", "co.ke", "com.ng",
        "github.io", "blogspot.com", "web.app", "firebaseapp.com", "netlify.app", "vercel.app", "pages.dev",
        "herokuapp.com", "glitch.me", "000webhostapp.com", "weebly.com", "wixsite.com", "repl.co", "ngrok.io",
        "ngrok-free.app", "trycloudflare.com", "workers.dev", "azurewebsites.net", "appspot.com", "onrender.com",
        "duckdns.org", "sites.google.com", "framer.app", "godaddysites.com", "square.site", "webflow.io",
    )

    /** Free hosting / tunnelling platforms where anyone can publish a page; phishing kits love them. */
    val freeHosting: Set<String> = setOf(
        "github.io", "blogspot.com", "web.app", "firebaseapp.com", "netlify.app", "vercel.app", "pages.dev",
        "herokuapp.com", "glitch.me", "000webhostapp.com", "weebly.com", "wixsite.com", "repl.co", "ngrok.io",
        "ngrok-free.app", "trycloudflare.com", "workers.dev", "azurewebsites.net", "appspot.com", "onrender.com",
        "duckdns.org", "sites.google.com", "framer.app", "godaddysites.com", "square.site", "webflow.io",
        "forms.gle", "docs.google.com", "ipfs.io", "dweb.link", "r2.dev",
    )

    /** Words in a domain or path that phishing pages use to look official. */
    val phishWords: Set<String> = setOf(
        "login", "signin", "verify", "verification", "kyc", "ekyc", "update", "secure", "account", "banking",
        "netbanking", "reward", "rewards", "refund", "otp", "wallet", "unlock", "suspend", "blocked", "confirm",
        "support", "helpdesk", "customercare", "claim", "bonus", "cashback", "prize", "lottery", "gift",
        "free", "offer", "pan", "aadhaar", "aadhar", "challan", "redeem", "points", "billing", "invoice",
        "password", "recover", "reactivate", "validate", "authentication", "webscr", "dispute",
    )

    /** Extensions that install software or run code. APK files are the #1 vector in Indian SMS/WhatsApp fraud. */
    val dangerousExtensions: Set<String> = setOf(
        "apk", "xapk", "apks", "exe", "msi", "scr", "bat", "cmd", "com", "pif", "vbs", "js", "jar", "ps1",
        "hta", "dll", "dmg", "pkg", "iso", "img", "lnk", "reg", "wsf",
    )

    val archiveExtensions: Set<String> = setOf("zip", "rar", "7z", "tar", "gz")

    /** Remote-access apps that fraudsters ask victims to install (RBI & CERT-In warnings). */
    val remoteAccessApps: List<String> = listOf(
        "anydesk", "teamviewer", "quicksupport", "quick support", "rustdesk", "airdroid", "ammyy", "supremo",
        "alpemix", "rsupport", "mobizen",
    )

    /** Well-known Indian UPI handles (after '@'). Unknown handles are not proof of fraud but worth a note. */
    val upiHandles: Set<String> = setOf(
        "ybl", "ibl", "axl", "okaxis", "okhdfcbank", "okicici", "oksbi", "paytm", "ptyes", "ptaxis", "pthdfc",
        "ptsbi", "upi", "apl", "yapl", "rapl", "abfspay", "axisbank", "icici", "hdfcbank", "sbi", "kotak",
        "kmbl", "indus", "freecharge", "ikwik", "jupiteraxis", "fbl", "federal", "yesbank", "yesbankltd",
        "idfcfirst", "idfcbank", "pnb", "barodampay", "boi", "cnrb", "unionbank", "uboi", "aubank", "airtel",
        "airtelpaymentsbank", "jio", "slice", "naviaxis", "waicici", "waaxis", "wahdfcbank", "wasbi", "postbank",
        "dbs", "citi", "hsbc", "sc", "rbl", "equitas", "dlb", "kvb", "tjsb", "timecosmos", "superyes", "pingpay",
        "amazonpay", "apay", "axisb", "okbizaxis", "yescred", "goaxb", "tapicici", "mbk", "ezeepay", "hdfcbankjd",
    )
}

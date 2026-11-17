package com.kavach.core.analyzers

import com.kavach.core.model.Category
import com.kavach.core.model.Finding
import com.kavach.core.model.Severity
import com.kavach.core.util.KnownData

/**
 * Reads SMS / WhatsApp / email text (typed, shared or read from a screenshot) and looks for the
 * wording of known Indian and global fraud patterns. English, Hinglish and common Hindi phrases.
 */
object MessageAnalyzer {

    private data class Pattern(
        val id: String,
        val title: String,
        val detail: String,
        val severity: Severity,
        val risk: Int,
        val regex: Regex,
        val reference: String? = null,
        val category: Category = Category.CONTENT,
    )

    private fun rx(s: String) = Regex(s, setOf(RegexOption.IGNORE_CASE))

    private const val RBI = "RBI BE(A)WARE booklet on financial frauds"
    private const val I4C = "I4C / cybercrime.gov.in public advisories"
    private const val CERTIN = "CERT-In advisories on smishing"

    private val patterns = listOf(
        Pattern(
            "urgency", "Creates panic or urgency",
            "Scammers rush you so you don't stop to think. Genuine organisations give you time and official channels.",
            Severity.MEDIUM, 15,
            rx("""\b(urgent(ly)?|immediately|right away|asap|within \d+\s?(hours?|hrs?|minutes?|mins?|days?)|today itself|last (date|chance|warning|reminder)|expir(e|es|ed|ing) (today|soon|tonight)|tonight|final notice|turant|jaldi|abhi turant)\b|तुरंत|अभी|आज ही"""),
            CERTIN,
        ),
        Pattern(
            "block_threat", "Threatens to block your account, card or SIM",
            "Messages saying your account/SIM/card \"will be blocked\" unless you click or call are the most common Indian bank-fraud opener.",
            Severity.HIGH, 30,
            rx("""\b(account|a/c|acct|card|sim|number|kyc|pan|wallet|yono|netbanking|upi)\b.{0,40}\b(block|blocked|suspend|suspended|deactivat\w*|clos(e|ed|ing)|freez\w*|band ho|expire[ds]?|disabled|on hold)|(खाता|अकाउंट|सिम|कार्ड).{0,25}(बंद|ब्लॉक|निलंबित)"""),
            RBI,
        ),
        Pattern(
            "kyc", "Asks you to update KYC / PAN / Aadhaar via a link",
            "RBI warns: banks never send links or ask you to update KYC through SMS/WhatsApp. Visit your branch or the official app.",
            Severity.HIGH, 35,
            rx("""\b(e-?kyc|re-?kyc|kyc)\b|update (your )?(pan|aadhaa?r)|(pan|aadhaa?r).{0,20}(link|updat|verif)\w*|केवाईसी"""),
            "RBI caution on KYC update frauds",
        ),
        Pattern(
            "lottery", "Promises a prize, lottery or lucky draw",
            "You cannot win a lottery you never entered. KBC/lottery/lucky-draw messages always end with a \"processing fee\".",
            Severity.HIGH, 35,
            rx("""\b(lottery|lucky draw|jackpot|kbc|kaun banega|you (have )?won|winner|prize money|won a|inaam|bumper)\b|congratulations.{0,60}\b(won|selected|winner|reward|prize)|इनाम|लॉटरी|विजेता|जीत"""),
            RBI,
        ),
        Pattern(
            "refund_reward", "Offers refund, cashback or reward points",
            "\"Redeem your reward points\" and \"income tax refund\" links are used to steal card details and OTPs.",
            Severity.MEDIUM, 22,
            rx("""\b(reward points?|redeem\w*|cashback|cash back|refund|tax refund|points? (will )?expire)\b"""),
            CERTIN,
        ),
        Pattern(
            "electricity", "Electricity bill disconnection threat",
            "\"Your electricity will be disconnected tonight, call this officer\" is a known scam. Power companies don't message from personal numbers.",
            Severity.HIGH, 40,
            rx("""(electricity|bijli|power|light)\b.{0,60}\b(disconnect\w*|cut|kat jayegi|band)|(bill).{0,40}(not (been )?updated|unpaid|pending|due).{0,60}(disconnect|tonight|cut)|बिजली.{0,30}(कट|काट)"""),
            I4C,
        ),
        Pattern(
            "digital_arrest", "Police / CBI / customs / courier threat (\"digital arrest\")",
            "There is no such thing as \"digital arrest\". Police, CBI, ED, NCB, customs or TRAI never threaten arrest over calls/video and never ask for money.",
            Severity.HIGH, 45,
            rx("""\b(digital arrest|cbi|ncb|narcotics|customs (department|duty|officer)|money laundering|arrest warrant|cyber cell|enforcement directorate|ed officer|illegal (items|drugs|parcel)|drugs? (found|in your parcel))\b|(parcel|courier|package|fedex).{0,50}(seized|illegal|drugs|passport)|trai.{0,40}(disconnect|block|suspend)"""),
            "I4C advisory on 'digital arrest' scams",
        ),
        Pattern(
            "delivery", "Fake delivery / parcel problem",
            "\"Your parcel is on hold, update address and pay ₹25\" links are used to steal card details (India Post / courier smishing).",
            Severity.HIGH, 30,
            rx("""(delivery|parcel|package|shipment|consignment|courier).{0,50}(failed|on hold|pending|incomplete|undeliver\w*|update (your )?address|re-?schedul\w*|returned)"""),
            CERTIN,
        ),
        Pattern(
            "job_task", "Part-time job / easy-money task",
            "\"Earn ₹5,000 daily by liking videos or rating hotels\" leads to prepaid \"tasks\" where you lose larger deposits.",
            Severity.HIGH, 40,
            rx("""\b(part[- ]?time (job|work)|work from home|earn (rs\.?|₹|inr)?\s?\d[\d,]*|daily (income|earning|salary|payment)|per day earning|like (and )?subscribe|youtube (like|task|video)|(hotel|google|movie) (review|rating)s?|prepaid task|task.{0,20}(earn|commission)|ghar baithe|salary \d+.{0,10}(daily|per day))\b"""),
            I4C,
        ),
        Pattern(
            "investment", "Too-good-to-be-true investment",
            "Guaranteed or very high returns, VIP stock-tip groups and crypto \"trading\" apps are the largest source of money lost to cybercrime in India.",
            Severity.HIGH, 40,
            rx("""\b(guaranteed (return|profit|income)|double (your )?money|\d{1,3}\s?% (daily|weekly|monthly|per day|per week|per month)|ipo allotment|stock (tips|market tips|advisor)|vip (group|trading)|crypto.{0,30}(profit|invest|return)|trading (group|app|platform).{0,40}(profit|return)|high returns?)\b"""),
            "SEBI investor alerts / I4C",
        ),
        Pattern(
            "loan", "Instant / pre-approved loan offer",
            "Unregistered instant-loan apps harass borrowers and misuse contacts and photos. Check the lender is on RBI's list.",
            Severity.MEDIUM, 25,
            rx("""\b(instant loan|pre-?approved (personal )?loan|loan (is )?(approved|sanctioned)|without (cibil|documents|credit check)|low interest loan)\b"""),
            "RBI caution on unauthorised digital lending",
        ),
        Pattern(
            "remote_access", "Asks you to install a screen-sharing app",
            "AnyDesk, TeamViewer and similar apps let the caller see and control your phone, including OTPs and banking apps.",
            Severity.CRITICAL, 70,
            rx("""\b(${KnownData.remoteAccessApps.joinToString("|") { Regex.escape(it) }}|screen ?shar(e|ing))\b"""),
            RBI,
        ),
        Pattern(
            "apk_install", "Asks you to install an app file (APK)",
            "Fake \"bank\", \"challan\", \"PM Kisan\", \"wedding invitation\" APKs sent on WhatsApp take over phones and read OTPs. Only install apps from Play Store.",
            Severity.CRITICAL, 75,
            rx("""\.apk\b|\bapk (file|download)|download (the |this )?apk|install (the |this )?(app|application|file) (from|via|using) (this|the) link"""),
            "CERT-In advisory on malicious APKs",
            Category.DOWNLOAD,
        ),
        Pattern(
            "sensitive_info", "Asks for card, PIN, Aadhaar or password details",
            "No bank, government department or company will ask for CVV, PIN, OTP, passwords or full card number by message.",
            Severity.HIGH, 40,
            rx("""(enter|submit|share|send|update|verify|provide|confirm|fill)\w*.{0,30}\b(cvv|card number|card details|expiry date|atm pin|upi pin|mpin|net ?banking (password|details)|login (id|details)|aadhaa?r (number|details))\b"""),
            RBI,
        ),
        Pattern(
            "wrong_transfer", "\"Money sent by mistake, please return it\"",
            "Fraudsters send a fake credit SMS (or a tiny real amount) and ask you to \"return\" more. Check your bank app, not the SMS.",
            Severity.HIGH, 35,
            rx("""(sent|transferred|credited|paid).{0,30}(by mistake|wrongly|accidentally|galti se)|(please|kindly) (return|refund|send back) (the |my )?(money|amount)"""),
            I4C,
        ),
        Pattern(
            "freebie", "Free gift / recharge / government scheme offer",
            "\"Free recharge\", \"free laptop under PM Yojana\" or \"free gift\" links collect your data or install malware.",
            Severity.MEDIUM, 25,
            rx("""\bfree (recharge|data|gift|laptop|scooty|mobile|iphone|smartphone|ration)|(pm|pradhan mantri).{0,30}(yojana|scheme).{0,50}(free|₹|rs\.?|apply|register)"""),
            "PIB Fact Check / I4C",
        ),
        Pattern(
            "card_upgrade", "Credit card limit / upgrade / activation",
            "Calls and links about raising card limit or \"activating\" card benefits are used to capture OTP and card details.",
            Severity.MEDIUM, 18,
            rx("""(credit )?card.{0,25}(limit (increase|upgrade|enhance)|upgrade|activate|lifetime free|annual (fee|charges) waive)"""),
            RBI,
        ),
        Pattern(
            "click_link", "Pushes you to click a link",
            "Legitimate alerts tell you what happened; scams push you to tap a link.",
            Severity.LOW, 8,
            rx("""\b(click|tap|visit|open) (on )?(the |this |below |given )?(link|here|url)|link (par|pe) click|नीचे दिए (गए )?लिंक"""),
        ),
        Pattern(
            "callback", "Asks you to call or WhatsApp a number",
            "Never call back numbers given in a suspicious message. Use the number on the official website or the back of your card.",
            Severity.LOW, 10,
            rx("""\b(call|contact|whatsapp|reach)\b.{0,20}(us |our |this |the )?(officer|executive|customer care|helpline|number|on)\b.{0,25}(\+?91[\s-]?)?[6-9]\d{9}"""),
            RBI,
        ),
        Pattern(
            "generic_greeting", "Generic greeting (\"Dear customer\")",
            "Your real bank usually knows your name.",
            Severity.LOW, 4,
            rx("""\b(dear (customer|user|sir/madam|valued customer|account holder|winner))\b"""),
        ),
    )

    private val otpRequest = rx("""\b(share|tell|send|forward|provide|give|bata\w*|bhej\w*)\b.{0,25}\b(otp|one time password|verification code|pin|cvv)\b|\b(otp|code)\b.{0,25}\b(share|bata\w*|bhej\w*|forward)\b|(ओटीपी|OTP).{0,20}(बताएं|बताइए|शेयर|भेजें)""")
    private val negation = rx("""(do not|don't|dont|never|not to|mat|na kare|kabhi|nahi)\W*(\w+\W+){0,4}$""")

    fun analyze(text: String): List<Finding> {
        if (text.isBlank()) return emptyList()
        val clean = text.replace(' ', ' ')
        val out = mutableListOf<Finding>()
        for (p in patterns) {
            val m = p.regex.find(clean) ?: continue
            val quote = m.value.trim().take(60)
            out += Finding(p.id, p.title, "${p.detail}\nFound: \"$quote\"", p.severity, p.category, p.risk, p.reference)
        }
        // OTP requests, ignoring the standard "do not share OTP" warnings that genuine banks include.
        val askedForOtp = otpRequest.findAll(clean).any { m ->
            val before = clean.substring(maxOf(0, m.range.first - 30), m.range.first)
            !negation.containsMatchIn(before) && !negation.containsMatchIn(m.value.substringBefore("otp", m.value))
        }
        if (askedForOtp) {
            out += Finding(
                "otp_request", "Asks you to share an OTP / PIN",
                "An OTP is a key to your account. No bank, delivery agent, police or company will ever ask for it. Sharing it = giving away your money.",
                Severity.CRITICAL, Category.CONTENT, 80, RBI,
            )
        }
        // Many independent scam themes together is itself a strong signal.
        val themes = out.count { it.severity >= Severity.MEDIUM }
        if (themes >= 3) {
            out += Finding(
                "combo", "Several scam techniques used together",
                "This message combines $themes different pressure tactics. Genuine messages almost never do this.",
                Severity.HIGH, Category.CONTENT, 30,
            )
        }
        return out
    }

    // ---------------- extraction ----------------

    private val tldPart = "com|in|net|org|co|io|me|info|biz|app|xyz|top|club|online|site|live|shop|store|link|click|icu|buzz|tk|ml|ga|cf|gq|ly|gl|gd|at|to|cc|ru|cn|pw|sbs|cyou|rest|vip|today|work|support|ee|ai|bank|gov|edu|us|uk|de|fun|win|loan|asia|bond|cfd|lol|mobi|page|dev|tech|website|space|cloud|pro|one|ink|im|li|ws|su|id|ac|is|cx"

    private val urlRegex = Regex(
        """(?i)\b((?:https?://|hxxps?://|www\.)[^\s<>"'`]+|(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+(?:$tldPart)\b(?:[/?#][^\s<>"'`]*)?)""",
    )
    private val upiRegex = Regex("""(?i)\b(upi://pay\?[^\s]+|[a-z0-9._\-]{2,64}@[a-z]{2,20})\b(?![.\w]*\.[a-z]{2,})""")
    private val phoneRegex = Regex("""(?<!\d)(\+?91[\s-]?)?[6-9]\d{4}[\s-]?\d{5}(?!\d)""")

    /** Pulls out every link in a text, including bare ones like "sbi-kyc.xyz/login" and defanged "hxxp". */
    fun extractUrls(text: String): List<String> {
        val fixed = text.replace("[.]", ".").replace("(.)", ".").replace("[:]", ":")
        return urlRegex.findAll(fixed).map {
            it.value.trimEnd('.', ',', ';', ':', ')', ']', '!', '?', '\'', '"')
                .replace(Regex("^hxxp", RegexOption.IGNORE_CASE), "http")
        }.filter { u ->
            // ignore e-mail addresses ("name@domain.com") – the part after @ would be picked up as a domain
            val idx = fixed.indexOf(u)
            !(idx > 0 && fixed[idx - 1] == '@')
        }.distinct().toList()
    }

    fun extractUpiIds(text: String): List<String> = upiRegex.findAll(text).map { it.value }
        .filter { it.startsWith("upi://", true) || it.substringAfter('@').lowercase() in KnownData.upiHandles }
        .distinct().toList()

    fun extractPhones(text: String): List<String> = phoneRegex.findAll(text).map { it.value.trim() }.distinct().toList()
}

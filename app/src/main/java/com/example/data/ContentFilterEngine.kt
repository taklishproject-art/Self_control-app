package com.example.data

import java.util.Calendar
import java.util.Locale

data class FilterMatch(
    val matchedTerm: String,
    val category: String,
    val reasonAmharic: String
)

object ContentFilterEngine {

    // Known Browser packages
    val BROWSER_PACKAGES = setOf(
        "com.android.chrome",
        "com.transsion.phoenix", // Phoenix Browser
        "org.mozilla.firefox",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.sec.android.app.sbrowser",
        "com.brave.browser",
        "com.microsoft.emmx",
        "com.UCMobile.intl",
        "com.duckduckgo.mobile.android"
    )

    // Known Social Media packages
    val SOCIAL_PACKAGES = setOf(
        "com.zhiliaoapp.musically",          // TikTok
        "com.ss.android.ugc.trill",           // TikTok Lite
        "com.instagram.android",             // Instagram
        "com.facebook.katana",               // Facebook
        "com.facebook.lite",                 // Facebook Lite
        "com.google.android.youtube",        // YouTube
        "com.google.android.apps.youtube.mango", // YouTube Go
        "com.twitter.android",               // X / Twitter
        "com.reddit.frontpage",              // Reddit
        "com.snapchat.android",              // Snapchat
        "org.telegram.messenger",            // Telegram
        "org.thunderdog.challegram",         // Telegram X
        "com.pinterest"                      // Pinterest
    )

    // Known VPN / Tunnel Bypass packages
    val VPN_PACKAGES = setOf(
        "com.nordvpn.android",
        "com.expressvpn.vpn",
        "hotspotshield.android.vpn",
        "free.vpn.unblock.proxy.turbovpn",
        "com.tunnelbear.android",
        "org.torproject.torbrowser",
        "org.torproject.android",
        "com.cloudflare.onedotonedotonedotone",
        "com.windscribe.vpn",
        "com.psiphon3.subscription",
        "com.wireguard.android",
        "ch.protonvpn.android",
        "com.vpn.powervpn",
        "supervpn.vpn.free.proxy"
    )

    // Common adult domains & hosts
    val ADULT_DOMAINS = listOf(
        "pornhub", "xvideos", "xnxx", "redtube", "chaturbate",
        "youporn", "tube8", "spankbang", "beeg", "xhamster",
        "stripchat", "bongacams", "livejasmin", "onlyfans", "brazzers",
        "eporner", "tnaflix", "cam4", "rule34", "hentai",
        "motherless", "hqporner", "daftsex", "vporn", "nudevista",
        "adultfriendfinder", "fapello", "thothub", "erome", "coomer.party",
        "kemono.party", "lustcinema", "javhd", "fetlife", "freeones"
    )

    // Primary explicit keywords & search terms
    val ADULT_KEYWORDS = listOf(
        "porn", "xxx", "xnxx", "xvideos", "pornhub",
        "nude", "nudes", "naked girl", "naked woman", "naked video",
        "sex video", "sexy video", "hardcore sex", "hot sex", "blowjob",
        "deepthroat", "pussy", "dildo", "masturbat", "orgasm",
        "gangbang", "creampie", "milf", "threesome", "camgirl",
        "webcam sex", "erotic video", "nsfw 18+", "18+ video", "18+ adult",
        "adult film", "adult video", "adult content", "hot clip 18+",
        "onlyfans leak", "sex tape", "hot webcam", "sex chat",
        "hentai", "ecchi", "eroge", "shemale", "bdsm"
    )

    // Leet-speak / character substitute lookup table
    private val LEET_REPLACEMENTS = mapOf(
        '0' to 'o',
        '@' to 'a',
        '4' to 'a',
        '1' to 'i',
        '!' to 'i',
        '|' to 'i',
        '3' to 'e',
        '5' to 's',
        '$' to 's',
        '7' to 't',
        '+' to 't',
        '8' to 'b'
    )

    fun getAppDisplayName(packageName: String): String {
        return when {
            packageName.contains("chrome") -> "Google Chrome"
            packageName.contains("phoenix") -> "Phoenix Browser"
            packageName.contains("musically") || packageName.contains("tiktok") -> "TikTok"
            packageName.contains("instagram") -> "Instagram"
            packageName.contains("facebook") -> "Facebook"
            packageName.contains("youtube") -> "YouTube"
            packageName.contains("telegram") || packageName.contains("challegram") -> "Telegram"
            packageName.contains("twitter") -> "X (Twitter)"
            packageName.contains("reddit") -> "Reddit"
            packageName.contains("snapchat") -> "Snapchat"
            packageName.contains("firefox") -> "Firefox"
            packageName.contains("opera") -> "Opera"
            packageName.contains("sbrowser") -> "Samsung Internet"
            packageName.contains("brave") -> "Brave Browser"
            packageName.contains("vpn") -> "VPN Bypass App"
            else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * ENGINE 3: Midnight Vulnerability Guard
     * Detects if current time falls in high-risk night hours (11:00 PM - 5:30 AM).
     */
    fun isMidnightVulnerabilityWindow(): Boolean {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        // High risk: 23:00 (11:00 PM) to 05:30 (5:30 AM)
        return hour >= 23 || hour < 5 || (hour == 5 && minute <= 30)
    }

    /**
     * ENGINE 5: Leet-speak & Evasion Decryption Engine
     * Converts obfuscated text (e.g. "p.0.r.n", "s3x", "x.x.x", "p_u_s_s_y") into normalized plain text.
     */
    fun normalizeLeetAndSeparators(raw: String): String {
        val lower = raw.lowercase(Locale.ROOT)
        // First, replace known leet substitution characters
        val sb = StringBuilder(lower.length)
        for (ch in lower) {
            val decoded = LEET_REPLACEMENTS[ch] ?: ch
            sb.append(decoded)
        }
        val leetDecoded = sb.toString()

        // Also remove punctuation separators between adjacent characters (e.g. "p.o.r.n" or "s-e-x")
        val collapsed = leetDecoded.replace(Regex("[._\\-\\s]+"), "")
        return "$leetDecoded $collapsed"
    }

    fun isMonitoredPackage(
        packageName: String,
        blockBrowsers: Boolean,
        blockSocial: Boolean
    ): Boolean {
        val lower = packageName.lowercase(Locale.ROOT)
        if (VPN_PACKAGES.contains(lower) || lower.contains("vpn") || lower.contains("proxy") || lower.contains("tunnel")) {
            return true
        }
        if (blockBrowsers && (BROWSER_PACKAGES.contains(lower) || lower.contains("browser") || lower.contains("chrome"))) {
            return true
        }
        if (blockSocial && (SOCIAL_PACKAGES.contains(lower) || lower.contains("tiktok") || lower.contains("instagram") || lower.contains("facebook"))) {
            return true
        }
        return false
    }

    /**
     * Checks text or URL content against built-in rules, custom user keywords,
     * normalized leet-speak decryption, and midnight vulnerability guard.
     */
    fun checkContent(
        text: String,
        customKeywords: Set<String>,
        strictMode: Boolean = true
    ): FilterMatch? {
        if (text.isBlank()) return null
        val normalized = text.lowercase(Locale.ROOT)
        val decodedCandidates = normalizeLeetAndSeparators(text)

        // 1. Check custom user blocklist first
        for (custom in customKeywords) {
            val trimmed = custom.trim().lowercase(Locale.ROOT)
            if (trimmed.isNotEmpty() && (normalized.contains(trimmed) || decodedCandidates.contains(trimmed))) {
                return FilterMatch(
                    matchedTerm = trimmed,
                    category = "የተጠቃሚ እገዳ (Custom)",
                    reasonAmharic = "በእርስዎ በተመዘገበው የተከለከለ ቃል ምክንያት ታግዷል"
                )
            }
        }

        // 2. Check adult domains (require whole token / host match or surrounded by punctuation/slashes)
        for (domain in ADULT_DOMAINS) {
            // Check if domain is part of URL or a standalone word (not a prefix of a word like 'portugal' if domain is 'pornhub')
            val domainRegex = Regex("(?:^|[^a-zA-Z0-9])${Regex.escape(domain)}(?:$|[^a-zA-Z0-9])")
            if (domainRegex.containsMatchIn(normalized) || domainRegex.containsMatchIn(decodedCandidates)) {
                return FilterMatch(
                    matchedTerm = domain,
                    category = "የአዋቂዎች ድረ-ገጽ (Adult Website)",
                    reasonAmharic = "የአዋቂዎች ድረ-ገጽ በመሆኑ በ SafeGuard ታግዷል"
                )
            }
        }

        // 3. Check explicit adult keywords with strict word-boundary matching
        // Tokenize text by all non-alphanumeric separators
        val tokens = normalized.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }.toSet()
        val decodedTokens = decodedCandidates.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }.toSet()

        for (kw in ADULT_KEYWORDS) {
            // Multi-word keywords (e.g. "naked girl", "sex video") vs single word (e.g. "porn", "hentai", "nude")
            if (kw.contains(" ")) {
                val kwRegex = Regex("(?:^|[^a-zA-Z0-9])${Regex.escape(kw)}(?:$|[^a-zA-Z0-9])")
                if (kwRegex.containsMatchIn(normalized) || kwRegex.containsMatchIn(decodedCandidates)) {
                    return FilterMatch(
                        matchedTerm = kw,
                        category = "ተገቢ ያልሆነ ይዘት (Explicit Content)",
                        reasonAmharic = "ተገቢ ያልሆነ የአዋቂ ይዘት ቃል በመገኘቱ ታግዷል"
                    )
                }
            } else {
                // Single word keywords MUST match complete individual word tokens
                // This prevents prefixes like "por" from triggering "porn", or "por" matching "portugal"
                if (tokens.contains(kw) || decodedTokens.contains(kw)) {
                    return FilterMatch(
                        matchedTerm = kw,
                        category = "ተገቢ ያልሆነ ቃል (Explicit Keyword)",
                        reasonAmharic = "ተገቢ ያልሆነ የአዋቂ ይዘት ቃል በመገኘቱ ታግዷል"
                    )
                }
            }
        }

        // ENGINE 3 Check: In Midnight Vulnerability Window, trigger on soft/provocative terms with word boundary
        if (isMidnightVulnerabilityWindow()) {
            val midnightSensitiveTerms = listOf("sexy", "hot girl", "dating 18", "escort", "erotic", "nude", "bikini dance")
            for (term in midnightSensitiveTerms) {
                val termRegex = Regex("(?:^|[^a-zA-Z0-9])${Regex.escape(term)}(?:$|[^a-zA-Z0-9])")
                if (termRegex.containsMatchIn(normalized) || termRegex.containsMatchIn(decodedCandidates)) {
                    return FilterMatch(
                        matchedTerm = term,
                        category = "የሌሊት ንቃት ጋሻ (Midnight Vulnerability Guard)",
                        reasonAmharic = "በሌሊት የንቃት ሰዓት ጥበቃ ጥንቃቄ ምክንያት ታግዷል"
                    )
                }
            }
        }

        return null
    }
}

package com.buwin.tiktokvideodownload.data.config

/**
 * Centralized application & network endpoints configuration.
 * Avoids hardcoding URLs and parameters across services and UI components.
 */
object AppConfig {

    // ── TikTok Service Endpoints ──
    const val TIKTOK_API_BASE_URL = "https://www.tikwm.com"
    const val TIKTOK_API_ENDPOINT = "$TIKTOK_API_BASE_URL/api/"

    // ── Facebook Service Endpoints ──
    const val FACEBOOK_API_BASE_URL = "https://facebook.bdbots.org"
    const val FACEBOOK_API_ENDPOINT = "$FACEBOOK_API_BASE_URL/dl"

    // ── Web Referers ──
    const val REFERER_TIKTOK = "$TIKTOK_API_BASE_URL/"
    const val REFERER_FACEBOOK = "https://www.facebook.com/"

    // ── User Agents ──
    const val USER_AGENT_MOBILE = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    const val USER_AGENT_DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    // ── Network Timeouts ──
    const val NETWORK_TIMEOUT_SECONDS = 30L

    // ── App Info ──
    const val APP_VERSION = "1.0.0"
    const val APP_BUILD = "1"
    const val APP_VERSION_DISPLAY = "$APP_VERSION (Build $APP_BUILD)"
}

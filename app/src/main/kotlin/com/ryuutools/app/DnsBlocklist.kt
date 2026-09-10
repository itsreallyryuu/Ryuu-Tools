package com.ryuutools.app

object DnsBlocklist {

    private val blockedDomains = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "google-analytics.com", "adservice.google.com", "ads.google.com",
        "adnxs.com", "connect.facebook.net",
        "amazon-adsystem.com", "scorecardresearch.com", "outbrain.com",
        "taboola.com", "criteo.com", "mopub.com", "chartboost.com",
        "unityads.unity3d.com", "applovin.com", "adcolony.com",
        "startapp.com", "ironsrc.com", "vungle.com", "smaato.com",
        "pubmatic.com", "rubiconproject.com", "openx.net", "bidswitch.net",
        "adform.net", "flurry.com", "mixpanel.com", "appsflyer.com"
    )

    fun isBlocked(domain: String?): Boolean {
        if (domain.isNullOrBlank()) return false
        val lower = domain.lowercase().trimEnd('.')
        return blockedDomains.any { lower == it || lower.endsWith(".$it") }
    }
}
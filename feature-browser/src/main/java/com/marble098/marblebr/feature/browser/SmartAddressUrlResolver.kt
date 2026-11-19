package com.marble098.marblebr.feature.browser

import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * پردازشگر نوار آدرس هوشمند (Smart Address Bar):
 * تشخیص خودکار آدرس وب، دامنه، IP یا عبارت جستجو.
 */
@Singleton
class SmartAddressUrlResolver @Inject constructor() {

    fun resolveInputToUrl(
        rawInput: String,
        searchTemplate: String = DEFAULT_SEARCH_TEMPLATE,
    ): String {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return "https://www.google.com"

        if (trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("about:", ignoreCase = true)
        ) {
            return trimmed
        }

        val looksLikeDomainOrIp = !trimmed.contains(" ") &&
            (DOMAIN_REGEX.matches(trimmed) || IPV4_REGEX.matches(trimmed) || trimmed.startsWith("localhost"))

        return if (looksLikeDomainOrIp) {
            "https://$trimmed"
        } else {
            val encoded = URLEncoder.encode(trimmed, Charsets.UTF_8.name())
            if (searchTemplate.contains("%s")) {
                searchTemplate.replace("%s", encoded)
            } else {
                "$DEFAULT_SEARCH_TEMPLATE$encoded"
            }
        }
    }

    companion object {
        const val DEFAULT_SEARCH_TEMPLATE = "https://www.google.com/search?q=%s"
        private val DOMAIN_REGEX = Regex("""^([a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}(/.*)?$""")
        private val IPV4_REGEX = Regex("""^(\d{1,3}\.){3}\d{1,3}(:\d+)?(/.*)?$""")
    }
}

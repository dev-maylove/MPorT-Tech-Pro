package com.mporttech.pro.core.security

import com.mporttech.pro.core.common.Constants
import okhttp3.CertificatePinner
import java.net.URI

/**
 * OkHttp Certificate Pinning (SPKI SHA-256).
 *
 * **How to obtain pins** (leaf + at least one backup / intermediate):
 * ```
 * ./scripts/fetch-cert-pins.sh api.mandalanet.id
 * ```
 * Or manually:
 * ```
 * openssl s_client -connect HOST:443 -servername HOST </dev/null 2>/dev/null \
 *   | openssl x509 -pubkey -noout \
 *   | openssl pkey -pubin -outform der \
 *   | openssl dgst -sha256 -binary | base64
 * ```
 * Format expected by OkHttp: `sha256/<base64>`
 *
 * Pin **both** the current leaf certificate and a backup (next leaf or intermediate)
 * so certificate rotation does not break the app.
 *
 * Enable with BuildConfig / Gradle:
 * `-PenableCertPinning=true` and non-empty [HOST_PINS].
 */
object CertificatePinning {

    /**
     * Host → list of SPKI pins (`sha256/...`).
     *
     * Fill via: `./scripts/fetch-cert-pins.sh api.mandalanet.id`
     * Then enable release pinning: `-PenableCertPinning=true`
     *
     * Pinning activates only when **all** of the following are true:
     * 1. BuildConfig.ENABLE_CERT_PINNING == true
     * 2. At least one host has a real `sha256/...` pin (not a placeholder)
     *
     * Empty list = pinning stays off (safe default for debug / pre-prod).
     */
    private val HOST_PINS: Map<String, List<String>> = mapOf(
        "api.mandalanet.id" to listOf(
            // Paste leaf + backup pins from scripts/fetch-cert-pins.sh, e.g.:
            // "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            // "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=",
        )
    )

    private fun isRealPin(pin: String): Boolean =
        pin.startsWith("sha256/") &&
            pin.length > 20 &&
            !pin.contains("PIN_HERE") &&
            !pin.contains("AAAA") &&
            !pin.contains("BBBB")

    /** True when flag is on and at least one host has a real pin. */
    fun isConfigured(): Boolean {
        if (!Constants.ENABLE_CERT_PINNING) return false
        return HOST_PINS.values.any { pins -> pins.any(::isRealPin) }
    }

    /**
     * Builds [CertificatePinner] for [baseUrl] host + any extra configured hosts.
     * Returns null if pinning should not be applied (flag off or no pins).
     */
    fun buildPinnerOrNull(baseUrl: String = Constants.API_BASE_URL): CertificatePinner? {
        if (!isConfigured()) return null

        val primaryHost = hostOf(baseUrl) ?: "api.mandalanet.id"
        val builder = CertificatePinner.Builder()
        var added = 0

        fun addHost(host: String) {
            val pins = HOST_PINS[host].orEmpty().filter(::isRealPin)
            if (pins.isEmpty()) return
            // OkHttp vararg overload
            builder.add(host, *pins.toTypedArray())
            added += pins.size
        }

        addHost(primaryHost)
        // Also pin any other explicitly listed hosts (CDN, auth subdomain, …)
        HOST_PINS.keys.filter { it != primaryHost }.forEach { addHost(it) }

        if (added == 0) return null
        return builder.build()
    }

    private fun hostOf(url: String): String? = try {
        URI(url).host?.lowercase()?.trim().takeUnless { it.isNullOrBlank() }
    } catch (_: Exception) {
        null
    }
}

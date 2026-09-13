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

    /** Master switch — keep false until production pins are filled and tested. */
    private const val PINNING_TEMPORARILY_DISABLED = true


    /**
     * Host → list of SPKI pins (`sha256/...`).
     *
     * Replace placeholder values with output of `scripts/fetch-cert-pins.sh`
     * before setting `ENABLE_CERT_PINNING=true` on release builds.
     *
     * Example after fetching real pins:
     * ```
     * "api.mandalanet.id" to listOf(
     *     "sha256/AbCdEf...=",  // leaf
     *     "sha256/XyZ...=",     // intermediate / backup
     * )
     * ```
     */
    private val HOST_PINS: Map<String, List<String>> = mapOf(
        // Production API host — pins MUST be real SPKI hashes before enabling.
        "api.mandalanet.id" to listOf(
            // TODO: paste leaf pin from scripts/fetch-cert-pins.sh
            // "sha256/LEAF_PIN_HERE=",
            // TODO: paste intermediate or backup leaf pin
            // "sha256/BACKUP_PIN_HERE=",
        )
    )

    /** True when flag is on and at least one host has a real pin. */
    fun isConfigured(): Boolean {
        if (PINNING_TEMPORARILY_DISABLED || !Constants.ENABLE_CERT_PINNING) return false
        return HOST_PINS.values.any { pins ->
            pins.any { it.startsWith("sha256/") && !it.contains("PIN_HERE") && it.length > 20 }
        }
    }

    /**
     * Builds [CertificatePinner] for [baseUrl] host + any extra configured hosts.
     * Returns null if pinning should not be applied (flag off or no pins).
     */
    fun buildPinnerOrNull(baseUrl: String = Constants.API_BASE_URL): CertificatePinner? {
        if (PINNING_TEMPORARILY_DISABLED || !Constants.ENABLE_CERT_PINNING) return null

        val primaryHost = hostOf(baseUrl) ?: "api.mandalanet.id"
        val builder = CertificatePinner.Builder()
        var added = 0

        fun addHost(host: String) {
            val pins = HOST_PINS[host].orEmpty().filter { pin ->
                pin.startsWith("sha256/") && pin.length > 20 && !pin.contains("PIN_HERE") &&
                    !pin.contains("AAAA")
            }
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

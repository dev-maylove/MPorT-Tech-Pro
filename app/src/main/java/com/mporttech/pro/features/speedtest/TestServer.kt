package com.mporttech.pro.features.speedtest

/**
 * Port of MPorT-Tes-Speed [TestServer] catalog.
 * Resources: lib/features/speed_test/models/test_server.dart
 */
data class TestServer(
    val id: String,
    val name: String,
    val host: String,
    val location: String,
    val country: String = "ID",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distanceKm: Int? = null,
    val isDefault: Boolean = false,
    val scheme: String = "http",
    val downloadPath: String = "/speedtest/download",
    val uploadPath: String = "/speedtest/upload.php",
    val pingPath: String = "/speedtest/latency.txt",
    val latencyMs: Double? = null,
    val network: String = "",
    val sponsor: String = ""
) {
    val baseUrl: String get() = "$scheme://$host"

    /** UI label: server name only */
    val displayName: String get() = name.ifBlank { sponsor }.ifBlank { host.substringBefore(":") }

    /** UI subtitle: location + optional latency — no full ookla URL */
    val displaySubtitle: String
        get() {
            val loc = location.ifBlank { country }
            val lat = latencyMs?.let { " · ${it.toInt()} ms" } ?: ""
            return if (loc.isNotBlank()) "$loc$lat" else host.substringBefore(":") + lat
        }

    /**
     * Apply this server without resolving DNS on the UI thread.
     *
     * Keeping the original hostname also lets HTTP virtual hosts route the
     * request normally. DNS is then performed by the network stack when the
     * actual request runs on Dispatchers.IO.
     */
    fun applyToConfig() {
        val normalizedHost = host.trim()
        val hostOnly = normalizedHost.substringBefore(":").trim()
        val portPart = normalizedHost.substringAfter(":", missingDelimiterValue = "").ifBlank { null }
        val isIp = hostOnly.matches(Regex("""^\d{1,3}(\.\d{1,3}){3}$"""))
        val isHaansiro = isIp && (hostOnly == "165.99.194.173" || name.contains("Haan", ignoreCase = true))

        // Most catalog entries are Ookla-hosted: HTTP on the branded hostname
        // redirects to https://server-{id}.prod.hosts.ooklaserver.net:8080.
        // Android HttpURLConnection does NOT follow HTTP→HTTPS redirects, so
        // downloads hang / return zero bytes. Prefer the final Ookla HTTPS origin.
        val numericId = id.takeIf { it.all { c -> c.isDigit() } }
        val useOoklaHost = numericId != null && !isHaansiro && !isIp

        val finalScheme: String
        val finalHostPort: String
        val originalHostname: String

        when {
            isHaansiro -> {
                finalScheme = scheme
                finalHostPort = normalizedHost
                originalHostname = "ookla.haansiro.net"
            }
            useOoklaHost -> {
                finalScheme = "https"
                finalHostPort = "server-$numericId.prod.hosts.ooklaserver.net:8080"
                originalHostname = "server-$numericId.prod.hosts.ooklaserver.net"
            }
            else -> {
                finalScheme = scheme
                finalHostPort = normalizedHost
                originalHostname = hostOnly
            }
        }

        ServerConfig.baseUrl = "$finalScheme://$finalHostPort"
        ServerConfig.originalHostname = originalHostname
        ServerConfig.downloadPath = downloadPath
        ServerConfig.uploadPath = uploadPath
        ServerConfig.pingPath = pingPath
        ServerConfig.serverName = name.ifBlank { originalHostname }
    }

    fun copyLatency(ms: Double) = copy(latencyMs = ms)

    companion object {
        /**
         * Resolve host or host:port → ip:port (IPv4 preferred).
         * Keeps the original port when present; defaults to 8080 for bare hostnames
         * that look like Ookla-style endpoints (common catalog pattern).
         */
        fun resolveHostToIp(hostPort: String): String {
            val raw = hostPort.trim()
            if (raw.isEmpty()) return raw
            val host: String
            val port: String?
            when {
                raw.startsWith("[") -> {
                    val end = raw.indexOf(']')
                    host = if (end > 0) raw.substring(1, end) else raw
                    port = raw.substring(end + 1).removePrefix(":").ifBlank { null }
                }
                raw.count { it == ':' } == 1 -> {
                    host = raw.substringBefore(":")
                    port = raw.substringAfter(":").ifBlank { null }
                }
                else -> {
                    host = raw
                    port = null
                }
            }
            if (host.isEmpty()) return raw
            val resolvedIp: String = if (host.matches(Regex("""^\d{1,3}(\.\d{1,3}){3}$"""))) {
                host
            } else {
                try {
                    val all = java.net.InetAddress.getAllByName(host)
                    all.firstOrNull { it is java.net.Inet4Address }?.hostAddress
                        ?: all.firstOrNull()?.hostAddress
                        ?: host
                } catch (_: Exception) {
                    host
                }
            }
            val finalPort = port ?: "8080"
            return "$resolvedIp:$finalPort"
        }

        fun haansiro() = TestServer(
            id = "75224",
            name = "HaanSirO Network",
            host = "165.99.194.173:8080",  // ookla.haansiro.net → IP:8080
            location = "Pati",
            latitude = -6.7487,
            longitude = 111.0379,
            distanceKm = 7,
            isDefault = true,
            network = "HaaNSirO",
            sponsor = "HaanSirO Network"
        )

        fun haansiroLegacy() = TestServer(
            id = "haansiro-legacy",
            name = "HaaNSirO (Legacy)",
            host = "speed2.haansiro.id",
            location = "Indonesia",
            latitude = -6.2088,
            longitude = 106.8456,
            scheme = "https",
            downloadPath = "/downloading",
            uploadPath = "/upload",
            pingPath = "/",
            network = "HaaNSirO",
            sponsor = "HaaNSirO"
        )

        private fun ookla(
            id: String,
            sponsor: String,
            host: String,
            location: String,
            lat: Double,
            lon: Double,
            distance: Int
        ) = TestServer(
            id = id,
            name = sponsor,
            host = host,
            location = location,
            latitude = lat,
            longitude = lon,
            distanceKm = distance,
            network = sponsor,
            sponsor = sponsor
        )

        /** Full catalog from MPorT-Tes-Speed resources. */
        fun catalog(): List<TestServer> = listOf(
            haansiro(),
            ookla("57184", "JSN Jaringanku", "speedtest-pati.jsn.net.id:8080", "Pati", -6.7487, 111.0379, 7),
            ookla("70985", "Global Media Data Prima", "speedtest-pati.gmdp.net.id:8080", "Pati", -6.7487, 111.0379, 7),
            ookla("69733", "ARRAB NETWORK", "speedtest-pati.arrab.id:8080", "Pati", -6.7487, 111.0379, 7),
            ookla("68681", "PT Jaringan Internet Tayu", "pati.tayu.my.id:8080", "Pati", -6.7487, 111.0379, 7),
            ookla("65329", "PerkasaNetwork", "speedtest.perkasa.net.id:8080", "Pati", -6.7487, 111.0379, 7),
            ookla("70300", "PusatNet", "pati.pusatnet.id:8080", "Pati", -6.7487, 111.0379, 7),
            ookla("70063", "PT BERKAH MEDIA KUSUMA VISION", "speedtest.bmkv.net:8080", "Pati", -6.7487, 111.0379, 7),
            ookla("72845", "PT STAR NUSANTARA NETWORK", "speedtest.starnus.net:8080", "Kab. Pati", -6.7450, 111.0460, 7),
            ookla("69130", "PT Menara Digital Salama", "speedtest-kudus.menaracloud.com:8080", "Kudus", -6.8075, 110.8427, 12),
            ookla("68392", "GM.NET", "speedtest.gmnet.id:8080", "Kudus", -6.8075, 110.8427, 12),
            ookla("68628", "ISKNET", "speedtest.isknet.id:8080", "Kudus", -6.8075, 110.8427, 12),
            ookla("73027", "PT Merdeka Telekomunikasi Center", "speedtest-kudus.merdeka.net.id:8080", "Kudus", -6.8075, 110.8427, 12),
            ookla("16143", "AN . NET", "kudus.an-group.my.id:8080", "Kudus", -6.8073, 110.8414, 12),
            ookla("71558", "Eratel", "speedtest-kudus.eratelindo.id:8080", "Kudus", -6.8073, 110.8414, 12),
            ookla("74426", "PT. Nesta Indo Media Kudus", "speedtest.infinityteknik.net:8080", "Kudus", -6.8073, 110.8414, 12),
            ookla("72982", "PT Merdeka Telekomunikasi Center", "speedtest-jepara.merdeka.net.id:8080", "Jepara", -6.5805, 110.6790, 21),
            ookla("67856", "Fahasa Net", "speed.fahasatridata.co.id:8080", "Jepara", -6.5805, 110.6790, 21),
            ookla("67570", "Internet Coffee", "jepara.netcoffee.my.id:8080", "Jepara", -6.5805, 110.6790, 21),
            ookla("72342", "UNISNU JEPARA", "speedtest.unisnu.ac.id:8080", "Jepara", -6.5805, 110.6790, 21),
            ookla("71935", "Lintas Home", "jepara.lintasjepara.my.id:8080", "Jepara", -6.5923, 110.6729, 21),
            ookla("69798", "DEMAK CENTRAL DATA ONLINE", "speedtest-demak.demak.online:8080", "Demak", -6.8906, 110.6434, 26),
            ookla("69928", "PT Menara Digital Salama", "speedtest-demak.menaracloud.com:8080", "Demak", -6.8906, 110.6434, 26),
            ookla("71987", "PT AYODYA DATA INTERNUSA", "speedtest.adinetwork.info:8080", "Rembang", -6.7094, 111.3413, 26),
            ookla("73323", "PT Merdeka Telekomunikasi Center", "speedtest-demak.merdeka.net.id:8080", "Demak", -6.8922, 110.6378, 26),
            ookla("63932", "PT Lintas Data Prima", "speeddmk.ldp.net.id:8080", "Demak", -6.8922, 110.6378, 26),
            ookla("60447", "Linkbit Inovasi Teknologi", "krry.speedtest.linkbit.net.id:8080", "Godong", -7.0279, 110.7748, 27),
            ookla("14443", "RECONET", "speedtest.reconet.co.id:8080", "Purwodadi", -7.1151, 110.9715, 30),
            ookla("70992", "High Speed Connection Network", "speedtest.hscnet.id:8080", "Purwodadi", -7.1151, 110.9715, 30),
            haansiroLegacy()
        )
    }
}

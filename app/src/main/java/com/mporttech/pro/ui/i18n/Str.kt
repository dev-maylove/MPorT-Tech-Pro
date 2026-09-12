package com.mporttech.pro.ui.i18n

/**
 * Central bilingual catalog (ID / EN).
 * Keys are stable; missing keys fall back to the key itself.
 */
object Str {
    private val id = mapOf(
        // Nav
        "nav.home" to "Beranda",
        "nav.network" to "Jaringan",
        "nav.tools" to "Alat",
        "nav.alerts" to "Peringatan",
        "nav.profile" to "Profil",

        // Common
        "common.back" to "Kembali",
        "common.refresh" to "Muat ulang",
        "common.scan" to "Pindai",
        "common.cancel" to "Batal",
        "common.save" to "Simpan",
        "common.close" to "Tutup",
        "common.share" to "Bagikan",
        "common.preview" to "Pratinjau",
        "common.online" to "Online",
        "common.offline" to "Offline",
        "common.loading" to "Memuat…",
        "common.error" to "Terjadi kesalahan",
        "common.ok" to "OK",
        "common.yes" to "Ya",
        "common.no" to "Tidak",
        "common.search" to "Cari",
        "common.settings" to "Pengaturan",
        "common.about" to "Tentang",
        "common.language" to "Bahasa",
        "common.theme" to "Tema",
        "common.dark_mode" to "Mode gelap",
        "common.notifications" to "Notifikasi",

        // Screens
        "screen.dashboard" to "Dasbor",
        "screen.network_monitor" to "Monitor Jaringan",
        "screen.device_manager" to "Manajer Perangkat",
        "screen.device_detail" to "Detail Perangkat",
        "screen.wifi" to "Analyzer WiFi",
        "screen.wifi_tools" to "Alat WiFi",
        "screen.speedtest" to "Uji Kecepatan",
        "screen.scanner" to "Pemindai Jaringan",
        "screen.diagnostic" to "Diagnostik",
        "screen.customers" to "Pelanggan",
        "screen.tickets" to "Tiket",
        "screen.alerts" to "Peringatan",
        "screen.alert_detail" to "Detail Peringatan",
        "screen.jobs" to "Pekerjaan",
        "screen.reports" to "Laporan",
        "screen.profile" to "Profil & Pengaturan",
        "screen.mikrotik" to "MikroTik / RouterOS",
        "screen.activity" to "Aktivitas",
        "screen.settings" to "Pengaturan",
        "screen.tools" to "Alat Teknisi",
        "screen.ping" to "Ping",
        "screen.traceroute" to "Traceroute",
        "screen.dns" to "DNS Lookup",
        "screen.port" to "Port Checker",
        "screen.about" to "Tentang",

        // Dashboard
        "dash.network_health" to "Kesehatan Jaringan",
        "dash.bandwidth" to "Penggunaan Bandwidth",
        "dash.quick_access" to "Akses Cepat",
        "dash.download" to "Unduh (RX)",
        "dash.upload" to "Unggah (TX)",
        "dash.live_traffic" to "Sampel TrafficStats (~1d)",
        "dash.subtitle" to "Kecerdasan Jaringan & Alat Teknisi",

        // Network
        "net.link_online" to "Tautan · Online",
        "net.link_offline" to "Tautan · Offline",
        "net.traffic_live" to "Lalu lintas (Mbps, langsung)",
        "net.interfaces" to "Antarmuka jaringan",
        "net.scan_lan" to "Pindai LAN",
        "net.gateway" to "Gateway",
        "net.authorize_scan" to "Saya berwenang memindai LAN privat (RFC1918)",
        "net.authorize_first" to "Centang otorisasi terlebih dahulu",
        "net.no_devices" to "Belum ada hasil. Gunakan Pindai LAN pada jaringan yang Anda kelola.",
        "net.probe_ready" to "Siap probe",
        "net.open_ports" to "Port terbuka",
        "net.no_ports" to "Tidak ada port manajemen terbuka (atau difilter firewall)",

        // Alerts
        "alert.no_data" to "Tidak ada koneksi data",
        "alert.gw_down" to "Gateway tidak merespons",
        "alert.gw_slow" to "Latency gateway tinggi",
        "alert.cellular" to "Menggunakan seluler",
        "alert.ssid_missing" to "SSID tidak terbaca",
        "alert.all_ok" to "Semua nominal",
        "alert.live_source" to "Langsung dari status perangkat (bukan server cloud)",
        "alert.refresh" to "MUAT ULANG STATUS",

        // Jobs
        "jobs.intro" to "Work order lapangan terintegrasi dengan modul Tiket lokal (Room).",
        "jobs.flow" to "Alur teknisi",

        // MikroTik
        "mt.probe_title" to "Probe RouterOS (tanpa menyimpan password)",
        "mt.probe_hint" to "Aplikasi tidak menyematkan kredensial. Uji port API/Winbox/WebFig ke host yang Anda kelola.",
        "mt.host" to "Host / IP router",
        "mt.check" to "PROBE PORT ROUTEROS",
        "mt.checklist" to "Checklist lapangan",

        // Profile
        "profile.edit" to "Edit Profil",
        "profile.security" to "Keamanan",
        "profile.server" to "Server API",
        "profile.language" to "Bahasa aplikasi",
        "profile.language_id" to "Indonesia",
        "profile.language_en" to "English",
        "profile.role" to "Teknisi Lapangan",

        // Settings
        "settings.app" to "Aplikasi",
        "settings.privacy" to "Privasi & keamanan",
        "settings.privacy_body" to "• Tidak ada kredensial MikroTik di APK\n• Scan LAN membutuhkan konfirmasi otorisasi\n• Data pelanggan/tiket hanya di database lokal",

        // Startup
        "startup.tagline" to "Alat teknisi jaringan",
        "startup.loading" to "Menyiapkan modul…",

        // Customers / tickets
        "customers.empty" to "Belum ada pelanggan",
        "customers.search" to "Cari nama, telepon, alamat…",
        "tickets.empty" to "Belum ada tiket",
        "diagnostic.empty" to "Belum ada hasil. Jalankan ping ke host yang diizinkan.",
        "diagnostic.run" to "Jalankan ping",
    )

    private val en = mapOf(
        "nav.home" to "Home",
        "nav.network" to "Network",
        "nav.tools" to "Tools",
        "nav.alerts" to "Alerts",
        "nav.profile" to "Profile",

        "common.back" to "Back",
        "common.refresh" to "Refresh",
        "common.scan" to "Scan",
        "common.cancel" to "Cancel",
        "common.save" to "Save",
        "common.close" to "Close",
        "common.share" to "Share",
        "common.preview" to "Preview",
        "common.online" to "Online",
        "common.offline" to "Offline",
        "common.loading" to "Loading…",
        "common.error" to "Something went wrong",
        "common.ok" to "OK",
        "common.yes" to "Yes",
        "common.no" to "No",
        "common.search" to "Search",
        "common.settings" to "Settings",
        "common.about" to "About",
        "common.language" to "Language",
        "common.theme" to "Theme",
        "common.dark_mode" to "Dark mode",
        "common.notifications" to "Notifications",

        "screen.dashboard" to "Dashboard",
        "screen.network_monitor" to "Network Monitor",
        "screen.device_manager" to "Device Manager",
        "screen.device_detail" to "Device Detail",
        "screen.wifi" to "WiFi Analyzer",
        "screen.wifi_tools" to "WiFi Tools",
        "screen.speedtest" to "Speed Test",
        "screen.scanner" to "Network Scanner",
        "screen.diagnostic" to "Diagnostic",
        "screen.customers" to "Customers",
        "screen.tickets" to "Tickets",
        "screen.alerts" to "Alerts",
        "screen.alert_detail" to "Alert Detail",
        "screen.jobs" to "Jobs",
        "screen.reports" to "Reports",
        "screen.profile" to "Profile & Settings",
        "screen.mikrotik" to "MikroTik / RouterOS",
        "screen.activity" to "Activity",
        "screen.settings" to "Settings",
        "screen.tools" to "Technician Tools",
        "screen.ping" to "Ping",
        "screen.traceroute" to "Traceroute",
        "screen.dns" to "DNS Lookup",
        "screen.port" to "Port Checker",
        "screen.about" to "About",

        "dash.network_health" to "Network Health",
        "dash.bandwidth" to "Bandwidth Usage",
        "dash.quick_access" to "Quick Access",
        "dash.download" to "Download (RX)",
        "dash.upload" to "Upload (TX)",
        "dash.live_traffic" to "Live TrafficStats sample (~1s)",
        "dash.subtitle" to "Network Intelligence & Technician Tools",

        "net.link_online" to "Link · Online",
        "net.link_offline" to "Link · Offline",
        "net.traffic_live" to "Traffic (Mbps, live)",
        "net.interfaces" to "Network interfaces",
        "net.scan_lan" to "Scan LAN",
        "net.gateway" to "Gateway",
        "net.authorize_scan" to "I am authorized to scan private LAN (RFC1918)",
        "net.authorize_first" to "Please confirm authorization first",
        "net.no_devices" to "No results yet. Run Scan LAN on a network you manage.",
        "net.probe_ready" to "Ready to probe",
        "net.open_ports" to "Open ports",
        "net.no_ports" to "No management ports open (or filtered by firewall)",

        "alert.no_data" to "No data connection",
        "alert.gw_down" to "Gateway not responding",
        "alert.gw_slow" to "High gateway latency",
        "alert.cellular" to "On cellular data",
        "alert.ssid_missing" to "SSID not readable",
        "alert.all_ok" to "All clear",
        "alert.live_source" to "Live from device status (not cloud)",
        "alert.refresh" to "REFRESH STATUS",

        "jobs.intro" to "Field work orders integrated with local Tickets module (Room).",
        "jobs.flow" to "Technician flow",

        "mt.probe_title" to "RouterOS probe (no password stored)",
        "mt.probe_hint" to "The app does not embed credentials. Probe API/Winbox/WebFig ports on hosts you manage.",
        "mt.host" to "Router host / IP",
        "mt.check" to "PROBE ROUTEROS PORTS",
        "mt.checklist" to "Field checklist",

        "profile.edit" to "Edit Profile",
        "profile.security" to "Security",
        "profile.server" to "API Server",
        "profile.language" to "App language",
        "profile.language_id" to "Indonesia",
        "profile.language_en" to "English",
        "profile.role" to "Field Technician",

        "settings.app" to "Application",
        "settings.privacy" to "Privacy & security",
        "settings.privacy_body" to "• No MikroTik credentials in the APK\n• LAN scan requires authorization\n• Customer/ticket data stays on-device",

        "startup.tagline" to "Network technician toolkit",
        "startup.loading" to "Preparing modules…",

        "customers.empty" to "No customers yet",
        "customers.search" to "Search name, phone, address…",
        "tickets.empty" to "No tickets yet",
        "diagnostic.empty" to "No results yet. Ping an authorized host.",
        "diagnostic.run" to "Run ping",
    )

    fun get(key: String, lang: AppLanguage): String {
        val table = if (lang == AppLanguage.ENGLISH) en else id
        return table[key] ?: en[key] ?: id[key] ?: key
    }
}

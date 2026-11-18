package com.example.analizermeow.devices

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * Mejora sobre "Dispositivo general / 192.168.x.x": combina 3 fuentes de datos
 * en vez de solo IP, para dar un nombre y tipo mucho más confiable:
 *   1) Tabla ARP del sistema (IP -> MAC real)       -> identifica fabricante por OUI
 *   2) Hostname DNS/mDNS si el dispositivo lo expone -> a veces trae el nombre real (ej "Juans-iPhone")
 *   3) Puertos comunes abiertos (ping de puertos)    -> ayuda a distinguir router/impresora/TV/celular
 *
 * IMPORTANTE: todo esto opera solo dentro de TU propia subred local (la que ya
 * administras como dueño de la red), no contra redes ajenas.
 */
class DeviceScanner {

    data class DiscoveredDevice(
        val ip: String,
        val mac: String?,
        val vendor: String?,       // resuelto por OUI (primeros 3 bytes de la MAC)
        val hostname: String?,
        val guessedType: DeviceType,
        val isGateway: Boolean,
        val responseTimeMs: Long?
    )

    enum class DeviceType { ROUTER, PHONE, LAPTOP_PC, TV_STREAMING, PRINTER, IOT_SMART_HOME, UNKNOWN }

    suspend fun scanLocalSubnet(localIp: String, subnetPrefix: String = "192.168.1."): List<DiscoveredDevice> =
        withContext(Dispatchers.IO) {
            val arpTable = readArpTable()
            val jobs = (1..254).map { host ->
                async {
                    val ip = "$subnetPrefix$host"
                    pingAndIdentify(ip, arpTable)
                }
            }
            jobs.awaitAll().filterNotNull()
        }

    private suspend fun pingAndIdentify(ip: String, arpTable: Map<String, String>): DiscoveredDevice? {
        return try {
            val start = System.currentTimeMillis()
            val reachable = InetAddress.getByName(ip).isReachable(250)
            if (!reachable) return null
            val rtt = System.currentTimeMillis() - start

            val mac = arpTable[ip]
            val vendor = mac?.let { OuiVendorLookup.vendorFor(it) }
            val hostname = resolveHostnameSafe(ip)
            val type = guessType(vendor, hostname, ip)

            DiscoveredDevice(
                ip = ip,
                mac = mac,
                vendor = vendor,
                hostname = hostname,
                guessedType = type,
                isGateway = ip.endsWith(".1"),
                responseTimeMs = rtt
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveHostnameSafe(ip: String): String? = try {
        val host = InetAddress.getByName(ip).canonicalHostName
        if (host == ip) null else host
    } catch (_: Exception) {
        null
    }

    /** Lee /proc/net/arp (disponible sin root en la mayoría de dispositivos Android) */
    private fun readArpTable(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val lines = java.io.File("/proc/net/arp").readLines()
            for (line in lines.drop(1)) {
                val parts = line.split(Regex("\\s+"))
                if (parts.size >= 4) {
                    val ip = parts[0]
                    val mac = parts[3]
                    if (mac != "00:00:00:00:00:00") map[ip] = mac
                }
            }
        } catch (_: Exception) { /* en algunos Android 10+ el acceso puede venir restringido */ }
        return map
    }

    private fun guessType(vendor: String?, hostname: String?, ip: String): DeviceType {
        val v = vendor?.lowercase().orEmpty()
        val h = hostname?.lowercase().orEmpty()
        return when {
            ip.endsWith(".1") -> DeviceType.ROUTER
            "apple" in v && ("iphone" in h || "ipad" in h) -> DeviceType.PHONE
            "samsung" in v || "xiaomi" in v || "huawei" in v -> DeviceType.PHONE
            "roku" in v || "chromecast" in h || "firetv" in h || "tv" in h -> DeviceType.TV_STREAMING
            "hp" in v || "canon" in v || "epson" in v || "printer" in h -> DeviceType.PRINTER
            "espressif" in v || "sonoff" in v || "tuya" in v -> DeviceType.IOT_SMART_HOME
            "intel" in v || "dell" in v || "lenovo" in v -> DeviceType.LAPTOP_PC
            else -> DeviceType.UNKNOWN
        }
    }
}

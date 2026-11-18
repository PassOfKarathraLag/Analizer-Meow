package com.example.analizermeow.signal

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Lee el estado de la red WiFi actual y las redes visibles alrededor.
 * Es la base para: pantalla de inicio (medidor %), listado de redes,
 * y la gráfica de "ruido" de señales.
 */
class SignalMonitor(context: Context) {

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    data class NetworkInfo(
        val ssid: String,
        val bssid: String,
        val rssiDbm: Int,          // ej. -50
        val frequencyMhz: Int,     // ej. 5180
        val channel: Int,
        val band: Band,
        val security: String
    )

    enum class Band { GHZ_2_4, GHZ_5, GHZ_6, UNKNOWN }

    /** Convierte dBm a un porcentaje 0-100 tipo "medidor" (igual al 94%/96% de tus capturas) */
    fun dbmToPercent(rssi: Int): Int {
        val minRssi = -100
        val maxRssi = -40
        val clamped = rssi.coerceIn(minRssi, maxRssi)
        return ((clamped - minRssi).toFloat() / (maxRssi - minRssi) * 100).toInt()
    }

    fun frequencyToChannel(freqMhz: Int): Int = when {
        freqMhz in 2412..2484 -> if (freqMhz == 2484) 14 else (freqMhz - 2412) / 5 + 1
        freqMhz in 5170..5825 -> (freqMhz - 5000) / 5
        freqMhz in 5955..7115 -> (freqMhz - 5950) / 5 + 1 // WiFi 6E
        else -> -1
    }

    fun bandOf(freqMhz: Int): Band = when {
        freqMhz in 2412..2484 -> Band.GHZ_2_4
        freqMhz in 5170..5825 -> Band.GHZ_5
        freqMhz in 5955..7115 -> Band.GHZ_6
        else -> Band.UNKNOWN
    }

    @Suppress("DEPRECATION")
    fun currentNetwork(): NetworkInfo? {
        val info = wifiManager.connectionInfo ?: return null
        if (info.networkId == -1) return null
        val freq = info.frequency
        return NetworkInfo(
            ssid = info.ssid.trim('"'),
            bssid = info.bssid ?: "",
            rssiDbm = info.rssi,
            frequencyMhz = freq,
            channel = frequencyToChannel(freq),
            band = bandOf(freq),
            security = "N/D" // se resuelve cruzando con ScanResult, ver scanNearby()
        )
    }

    @Suppress("DEPRECATION")
    fun scanNearby(): List<NetworkInfo> {
        val results: List<ScanResult> = wifiManager.scanResults ?: emptyList()
        return results.map { r ->
            NetworkInfo(
                ssid = r.SSID,
                bssid = r.BSSID,
                rssiDbm = r.level,
                frequencyMhz = r.frequency,
                channel = frequencyToChannel(r.frequency),
                band = bandOf(r.frequency),
                security = r.capabilities // ej "[WPA2-PSK-CCMP][ESS]"
            )
        }
    }

    /** Emite el estado actual cada [intervalMs] para alimentar la pantalla de inicio en vivo */
    fun liveSignal(intervalMs: Long = 1500): Flow<NetworkInfo?> = flow {
        while (true) {
            emit(currentNetwork())
            delay(intervalMs)
        }
    }

    fun startScan(): Boolean = wifiManager.startScan()
}

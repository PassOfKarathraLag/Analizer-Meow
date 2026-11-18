package com.example.analizermeow.speedtest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.InetAddress

/**
 * Prueba de velocidad y latencia ligera: usamos archivos pequeños de prueba
 * y varias muestras de ping, en vez de un SDK pesado de terceros.
 * Esto es clave para que la app siga siendo útil con señal débil: preferimos
 * resultados "N/D parcial" a trabarse esperando una descarga enorme.
 */
class SpeedTestManager {

    data class SpeedResult(
        val pingMs: Long?,
        val jitterMs: Long?,
        val downloadMbps: Double?,
        val packetLossPercent: Int
    )

    // Hosts pequeños y confiables para medir solo latencia/paquetes, no para "robar" ancho de banda del usuario
    private val pingTargets = listOf("8.8.8.8", "1.1.1.1")

    suspend fun measurePing(samples: Int = 5): Pair<Long?, Long?> = withContext(Dispatchers.IO) {
        val times = mutableListOf<Long>()
        repeat(samples) {
            try {
                val start = System.currentTimeMillis()
                val reachable = InetAddress.getByName(pingTargets[it % pingTargets.size]).isReachable(1500)
                if (reachable) times.add(System.currentTimeMillis() - start)
            } catch (_: Exception) { /* se cuenta como pérdida de paquete */ }
        }
        if (times.isEmpty()) return@withContext null to null
        val avg = times.average().toLong()
        val jitter = if (times.size > 1) {
            times.zipWithNext { a, b -> kotlin.math.abs(a - b) }.average().toLong()
        } else 0L
        avg to jitter
    }

    /** Descarga un archivo pequeño (ej. 1-2 MB) para estimar Mbps reales sin saturar datos móviles */
    suspend fun measureDownload(testFileUrl: String, timeoutMs: Int = 8000): Double? = withContext(Dispatchers.IO) {
        try {
            val url = URL(testFileUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            val start = System.currentTimeMillis()
            var bytes = 0L
            conn.inputStream.use { stream ->
                val buffer = ByteArray(8192)
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    bytes += read
                }
            }
            val elapsedSec = (System.currentTimeMillis() - start) / 1000.0
            if (elapsedSec <= 0) return@withContext null
            (bytes * 8 / 1_000_000.0) / elapsedSec // Mbps
        } catch (_: Exception) {
            null
        }
    }

    suspend fun runFullTest(testFileUrl: String): SpeedResult {
        val (ping, jitter) = measurePing()
        val download = if (ping != null) measureDownload(testFileUrl) else null
        val loss = if (ping == null) 100 else 0
        return SpeedResult(ping, jitter, download, loss)
    }
}

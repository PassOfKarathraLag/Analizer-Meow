package com.example.analizermeow.channels

import com.example.analizermeow.signal.SignalMonitor

/**
 * Reproduce la pestaña de "estrellas y recomendación" que viste en WiFi Analyzer,
 * pero con la lógica explícita (no es magia): penaliza canales con más redes
 * vecinas y con señales más fuertes encima, porque eso es lo que realmente
 * causa interferencia/lentitud.
 */
class ChannelAnalyzer {

    data class ChannelScore(
        val channel: Int,
        val band: SignalMonitor.Band,
        val stars: Int,          // 0..10, igual que tus capturas
        val overlappingNetworks: Int
    )

    data class ChannelReport(
        val currentChannel: Int,
        val currentStars: Int,
        val recommended: List<Int>,
        val allScores: List<ChannelScore>
    )

    // Canales 2.4GHz solo tienen 3 no solapados reales: 1, 6, 11
    private val channels24 = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)
    private val channels5 = listOf(36, 40, 44, 48, 52, 56, 60, 64, 100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 149, 153, 157, 161, 165)

    fun analyze(
        networks: List<SignalMonitor.NetworkInfo>,
        band: SignalMonitor.Band,
        currentChannel: Int
    ): ChannelReport {
        val universe = if (band == SignalMonitor.Band.GHZ_2_4) channels24 else channels5
        val scores = universe.map { ch ->
            val interferers = networks.filter { net ->
                net.band == band && isOverlapping(ch, net.channel, band)
            }
            // señal más fuerte (RSSI más alto, menos negativo) interfiere más
            var penalty = 0
            for (net in interferers) {
                penalty += when {
                    net.rssiDbm > -50 -> 3
                    net.rssiDbm > -70 -> 2
                    else -> 1
                }
            }
            val stars = (10 - penalty).coerceIn(0, 10)
            ChannelScore(ch, band, stars, interferers.size)
        }

        val maxStars = scores.maxOf { it.stars }
        val recommended = scores.filter { it.stars == maxStars }.map { it.channel }
        val current = scores.find { it.channel == currentChannel }?.stars ?: 0

        return ChannelReport(currentChannel, current, recommended, scores)
    }

    private fun isOverlapping(a: Int, b: Int, band: SignalMonitor.Band): Boolean {
        if (band == SignalMonitor.Band.GHZ_2_4) {
            // En 2.4GHz los canales se solapan si están a menos de 5 de distancia
            return kotlin.math.abs(a - b) < 5
        }
        // En 5GHz cada canal declarado ya es independiente (20MHz), salvo canales adyacentes en 40/80MHz;
        // para una v1 simple tratamos solo el mismo canal como solapado.
        return a == b
    }
}

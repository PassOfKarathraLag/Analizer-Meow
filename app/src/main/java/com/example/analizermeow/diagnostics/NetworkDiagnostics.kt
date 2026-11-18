package com.example.analizermeow.diagnostics

import com.example.analizermeow.channels.ChannelAnalyzer
import com.example.analizermeow.speedtest.SpeedTestManager

/**
 * Sección "Posibles fallas": cruza señal, canal, ping y velocidad para dar
 * un diagnóstico legible, no solo números sueltos.
 */
class NetworkDiagnostics {

    enum class Cause {
        SIGNAL_WEAK, CHANNEL_CONGESTED, ISP_LATENCY, ROUTER_OVERLOAD, UNKNOWN_INTERMITTENT
    }

    data class Diagnosis(
        val cause: Cause,
        val title: String,
        val explanation: String,
        val suggestion: String
    )

    fun diagnose(
        rssiDbm: Int,
        channelStars: Int,
        speed: SpeedTestManager.SpeedResult,
        reconnectsLastHour: Int
    ): List<Diagnosis> {
        val results = mutableListOf<Diagnosis>()

        if (rssiDbm < -75) {
            results.add(
                Diagnosis(
                    Cause.SIGNAL_WEAK,
                    "Señal débil por distancia u obstáculos",
                    "Tu teléfono recibe la señal del router muy baja ($rssiDbm dBm). " +
                        "Esto suele deberse a paredes, distancia o el material de construcción, no al proveedor.",
                    "Acércate al router o considera un repetidor/mesh en esa zona."
                )
            )
        }

        if (channelStars <= 4) {
            results.add(
                Diagnosis(
                    Cause.CHANNEL_CONGESTED,
                    "Canal saturado por redes vecinas",
                    "Hay muchas redes de vecinos usando el mismo canal o uno muy cercano, " +
                        "lo que genera interferencia (esto es típico en edificios y zonas densas).",
                    "Cambia el canal del router al recomendado en la pestaña de canales."
                )
            )
        }

        val ping = speed.pingMs
        if (ping != null && ping > 150) {
            results.add(
                Diagnosis(
                    Cause.ISP_LATENCY,
                    "Latencia alta hacia internet",
                    "La señal WiFi local está bien, pero la respuesta hacia internet tarda " +
                        "${ping}ms. Esto generalmente es del proveedor de internet (ISP) o de la hora pico, " +
                        "no del router.",
                    "Si pasa a ciertas horas del día, es probable saturación del ISP; repórtalo a tu proveedor."
                )
            )
        }

        if (reconnectsLastHour >= 3) {
            results.add(
                Diagnosis(
                    Cause.ROUTER_OVERLOAD,
                    "El router se está reiniciando o sobrecargando",
                    "Se detectaron $reconnectsLastHour reconexiones en la última hora. " +
                        "Esto suele pasar cuando el router lleva mucho tiempo encendido sin reiniciarse, " +
                        "o tiene demasiados dispositivos conectados.",
                    "Reinicia el router y revisa cuántos dispositivos están conectados realmente."
                )
            )
        }

        if (results.isEmpty()) {
            results.add(
                Diagnosis(
                    Cause.UNKNOWN_INTERMITTENT,
                    "Todo parece normal por ahora",
                    "Señal, canal y latencia están dentro de rangos saludables en este momento.",
                    "Si el problema es intermitente, vuelve a correr el diagnóstico cuando ocurra la falla."
                )
            )
        }

        return results
    }
}

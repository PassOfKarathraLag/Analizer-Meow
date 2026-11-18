package com.example.analizermeow.security

/**
 * "Auditor de seguridad de MI red" — pensado para clases de seguridad informática.
 *
 * A propósito, este módulo NO intenta obtener ni adivinar contraseñas de
 * ninguna red (propia ni ajena). En vez de eso evalúa, con datos públicos
 * del propio escaneo, qué tan vulnerable es la configuración ACTUAL,
 * exactamente lo que se usa para enseñar "por qué esto es inseguro":
 *
 *  - Tipo de cifrado (WEP / WPA / WPA2 / WPA3)
 *  - Si WPS está anunciado como activo (vector de ataque conocido y documentado)
 *  - Si el SSID sigue siendo el de fábrica (indica que probablemente la
 *    contraseña también sigue siendo la de fábrica)
 *  - Fuerza estimada del nombre de red como señal indirecta de qué tan
 *    personalizada está la configuración
 */
class SecurityChecker {

    enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

    data class SecurityFinding(
        val risk: RiskLevel,
        val title: String,
        val detail: String,
        val howToFix: String
    )

    fun audit(ssid: String, capabilities: String): List<SecurityFinding> {
        val findings = mutableListOf<SecurityFinding>()

        when {
            "WEP" in capabilities -> findings.add(
                SecurityFinding(
                    RiskLevel.CRITICAL,
                    "Cifrado WEP detectado",
                    "WEP es un protocolo obsoleto, roto públicamente desde hace más de 15 años. " +
                        "Cualquier herramienta gratuita puede romperlo en minutos.",
                    "Entra a la configuración del router y cambia a WPA2 o, si está disponible, WPA3."
                )
            )
            "WPA3" in capabilities -> findings.add(
                SecurityFinding(RiskLevel.LOW, "Cifrado WPA3", "Estás usando el estándar más seguro disponible hoy.", "Sin acción necesaria.")
            )
            "WPA2" in capabilities -> findings.add(
                SecurityFinding(RiskLevel.LOW, "Cifrado WPA2", "Nivel de cifrado aceptable y ampliamente usado.", "Considera migrar a WPA3 si tu router lo soporta.")
            )
            "WPA" in capabilities -> findings.add(
                SecurityFinding(RiskLevel.MEDIUM, "Cifrado WPA (sin el 2)", "WPA original es más débil que WPA2/3.", "Cambia el modo de seguridad a WPA2-PSK (AES) en el router.")
            )
            else -> findings.add(
                SecurityFinding(RiskLevel.CRITICAL, "Red sin cifrado o desconocido", "No se detectó cifrado reconocible.", "Configura al menos WPA2-PSK cuanto antes.")
            )
        }

        if ("WPS" in capabilities) {
            findings.add(
                SecurityFinding(
                    RiskLevel.HIGH,
                    "WPS activo",
                    "WPS (el botón de conexión rápida) tiene una vulnerabilidad conocida por PIN " +
                        "que permite ataques de fuerza bruta en pocas horas con herramientas públicas.",
                    "Desactiva WPS en la configuración del router si no lo usas."
                )
            )
        }

        if (isFactoryDefaultSsid(ssid)) {
            findings.add(
                SecurityFinding(
                    RiskLevel.MEDIUM,
                    "Nombre de red de fábrica",
                    "El SSID '$ssid' parece ser el que trae el router de fábrica, lo cual es una " +
                        "señal fuerte de que la contraseña y el usuario admin también siguen siendo los de fábrica.",
                    "Cambia el nombre de la red y, sobre todo, la contraseña del administrador del router."
                )
            )
        }

        return findings
    }

    private fun isFactoryDefaultSsid(ssid: String): Boolean {
        val patterns = listOf("INFINITUM", "TOTALPLAY", "MEGACABLE", "TP-LINK_", "dlink", "HUAWEI-")
        return patterns.any { ssid.contains(it, ignoreCase = true) }
    }
}

package com.example.analizermeow.devices

/**
 * Tabla OUI (primeros 3 bytes de la MAC -> fabricante). Esta es información
 * pública y oficial (IEEE la publica), no es un dato sensible ni de terceros.
 * Para producción real, se recomienda descargar la tabla completa de
 * https://standards-oui.ieee.org/oui/oui.txt y empaquetarla como asset.
 * Aquí va un subconjunto de ejemplo para que el proyecto compile y funcione.
 */
object OuiVendorLookup {

    private val table = mapOf(
        "3C:5A:B4" to "Google",
        "F4:F5:D8" to "Google",
        "A4:77:33" to "Apple",
        "AC:BC:32" to "Apple",
        "00:1A:11" to "Google",
        "38:94:ED" to "Huawei",
        "74:34:2B" to "Huawei", // coincide con el BSSID del router en tus capturas
        "D8:50:E6" to "Samsung",
        "8C:79:F5" to "Samsung",
        "FC:A1:3E" to "Xiaomi",
        "64:09:80" to "Xiaomi",
        "B0:A7:B9" to "Dell",
        "A8:5E:45" to "TP-Link",
        "50:C7:BF" to "TP-Link",
        "18:FE:34" to "Espressif", // IoT ESP8266/ESP32
        "AC:67:B2" to "Espressif",
        "F0:9E:4E" to "Roku"
    )

    fun vendorFor(mac: String): String? {
        if (mac.length < 8) return null
        val prefix = mac.substring(0, 8).uppercase()
        return table[prefix]
    }
}

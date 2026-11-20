package com.example.analizermeow.devices

/**
 * Tabla OUI (primeros 3 bytes de la MAC -> fabricante). Información pública,
 * tomada de los mismos registros que usan herramientas como nmap y los
 * buscadores de MAC (IEEE + bases comunitarias). No es exhaustiva —la lista
 * oficial completa tiene más de 50,000 entradas y pesaría varios MB—, pero
 * cubre con buena probabilidad lo que normalmente hay en una casa: celulares,
 * routers, consolas, asistentes y dispositivos de domótica.
 */
object OuiVendorLookup {

    private val table = mapOf(
        // --- Apple ---
        "00:03:93" to "Apple", "00:0A:27" to "Apple", "00:0A:95" to "Apple",
        "00:16:CB" to "Apple", "00:17:F2" to "Apple", "00:19:E3" to "Apple",
        "00:1B:63" to "Apple", "00:1D:4F" to "Apple", "00:1E:C2" to "Apple",
        "00:21:E9" to "Apple", "00:25:00" to "Apple", "00:26:BB" to "Apple",
        "04:48:9A" to "Apple", "0C:74:C2" to "Apple", "10:9A:DD" to "Apple",
        "18:AF:61" to "Apple", "28:37:37" to "Apple", "34:51:C9" to "Apple",
        "40:A6:D9" to "Apple", "54:9F:13" to "Apple", "5C:95:AE" to "Apple",
        "68:96:7B" to "Apple", "7C:D1:C3" to "Apple", "88:63:DF" to "Apple",
        "A4:B1:97" to "Apple", "AC:BC:32" to "Apple", "BC:67:78" to "Apple",
        "D0:23:DB" to "Apple", "F4:37:B7" to "Apple",

        // --- Samsung ---
        "00:07:AB" to "Samsung", "00:12:47" to "Samsung", "00:15:99" to "Samsung",
        "00:16:32" to "Samsung", "00:17:D5" to "Samsung", "00:1A:8A" to "Samsung",
        "00:1D:25" to "Samsung", "00:21:D1" to "Samsung", "00:23:39" to "Samsung",
        "08:08:C2" to "Samsung", "10:30:47" to "Samsung", "14:56:8E" to "Samsung",
        "18:3A:2D" to "Samsung", "1C:5A:3E" to "Samsung", "20:D5:BF" to "Samsung",
        "28:39:5E" to "Samsung", "34:14:5F" to "Samsung", "38:01:95" to "Samsung",
        "44:4E:1A" to "Samsung", "50:01:BB" to "Samsung", "5C:0A:5B" to "Samsung",
        "68:27:37" to "Samsung", "78:1F:DB" to "Samsung", "84:25:DB" to "Samsung",
        "9C:02:98" to "Samsung", "A0:60:90" to "Samsung", "B4:62:93" to "Samsung",
        "C4:73:1E" to "Samsung", "D0:66:7B" to "Samsung", "E4:12:1D" to "Samsung",

        // --- Huawei ---
        "00:E0:FC" to "Huawei", "00:9A:CD" to "Huawei", "0C:37:DC" to "Huawei",
        "0C:45:BA" to "Huawei", "24:DF:6A" to "Huawei", "24:DB:AC" to "Huawei",
        "28:31:52" to "Huawei", "48:AD:08" to "Huawei", "54:89:98" to "Huawei",
        "60:E7:01" to "Huawei", "64:A6:51" to "Huawei", "70:7B:E8" to "Huawei",
        "74:88:2A" to "Huawei", "74:34:2B" to "Huawei", "78:D7:52" to "Huawei",
        "80:38:BC" to "Huawei", "80:B6:86" to "Huawei", "AC:85:3D" to "Huawei",
        "B4:15:13" to "Huawei", "BC:76:70" to "Huawei", "CC:A2:23" to "Huawei",
        "D4:40:F0" to "Huawei", "DC:D2:FC" to "Huawei", "E0:24:7F" to "Huawei",
        "E8:08:8B" to "Huawei", "E8:CD:2D" to "Huawei", "EC:23:3D" to "Huawei",

        // --- Xiaomi ---
        "00:9E:C8" to "Xiaomi", "00:C3:0A" to "Xiaomi", "08:1C:6E" to "Xiaomi",
        "08:25:25" to "Xiaomi", "0C:F3:46" to "Xiaomi", "10:2A:B3" to "Xiaomi",
        "14:F6:5A" to "Xiaomi", "18:01:F1" to "Xiaomi", "24:D3:37" to "Xiaomi",
        "28:16:7F" to "Xiaomi", "2C:FE:4F" to "Xiaomi", "30:50:CE" to "Xiaomi",
        "50:4F:3B" to "Xiaomi", "68:AB:BC" to "Xiaomi", "68:B8:BB" to "Xiaomi",

        // --- Google / Nest ---
        "48:5D:60" to "Google", "48:D6:D5" to "Google", "54:60:9A" to "Google",
        "5C:4A:1F" to "Google", "7C:2E:BD" to "Google", "84:D6:11" to "Google",
        "94:EB:2C" to "Google", "A4:77:0D" to "Google", "B4:F5:A4" to "Google",
        "D4:A9:32" to "Google", "F4:F5:E8" to "Google", "F4:F5:DB" to "Google",
        "F8:FF:0B" to "Google", "6C:3C:7C" to "Google", "64:20:01" to "Google",
        "18:B4:30" to "Nest (Google)", "64:16:66" to "Nest (Google)",

        // --- Microsoft ---
        "00:03:FF" to "Microsoft", "00:12:5A" to "Microsoft", "00:15:5D" to "Microsoft",
        "00:17:FA" to "Microsoft", "00:22:48" to "Microsoft", "00:25:AE" to "Microsoft",
        "28:18:78" to "Microsoft", "3C:83:75" to "Microsoft", "48:50:73" to "Microsoft",
        "74:E2:8C" to "Microsoft", "9C:6C:15" to "Microsoft", "B8:4F:D5" to "Microsoft",
        "C8:3F:26" to "Microsoft",

        // --- Sony / PlayStation ---
        "00:01:4A" to "Sony", "00:13:A9" to "Sony", "00:1A:80" to "Sony",
        "00:1D:BA" to "Sony", "00:24:BE" to "Sony", "10:4F:A8" to "Sony",
        "30:F9:ED" to "Sony", "3C:07:71" to "Sony", "54:42:49" to "Sony",
        "78:84:3C" to "Sony", "AC:9B:0A" to "Sony", "D8:D4:3C" to "Sony",
        "F0:BF:97" to "Sony",
        "00:13:15" to "PlayStation", "00:19:C5" to "PlayStation",
        "00:1F:A7" to "PlayStation", "A8:E3:EE" to "PlayStation",
        "BC:60:A7" to "PlayStation", "F8:D0:AC" to "PlayStation",

        // --- HTC ---
        "00:09:2D" to "HTC", "00:23:76" to "HTC", "00:EE:BD" to "HTC",
        "04:C2:3E" to "HTC", "18:87:96" to "HTC", "2C:8A:72" to "HTC",
        "80:7A:BF" to "HTC", "90:21:55" to "HTC", "A8:26:D9" to "HTC",
        "D4:20:6D" to "HTC", "F8:DB:7F" to "HTC",

        // --- Routers / redes (TP-Link, D-Link, Netgear) ---
        "00:0A:EB" to "TP-Link", "00:14:78" to "TP-Link", "00:19:E0" to "TP-Link",
        "00:1D:0F" to "TP-Link", "00:27:19" to "TP-Link", "08:57:00" to "TP-Link",
        "0C:4B:54" to "TP-Link", "10:FE:ED" to "TP-Link", "14:75:90" to "TP-Link",
        "14:CC:20" to "TP-Link", "14:CF:92" to "TP-Link", "14:D8:64" to "TP-Link",
        "14:E6:E4" to "TP-Link", "64:56:01" to "TP-Link", "8C:21:0A" to "TP-Link",
        "8C:90:2D" to "TP-Link", "90:9A:4A" to "TP-Link", "A0:F3:C1" to "TP-Link",
        "BC:46:99" to "TP-Link", "E0:05:C5" to "TP-Link", "EC:17:2F" to "TP-Link",
        "EC:88:8F" to "TP-Link",
        "00:05:5D" to "D-Link", "1C:7E:E5" to "D-Link", "28:10:7B" to "D-Link",
        "90:94:E4" to "D-Link", "C4:A8:1D" to "D-Link",
        "00:09:5B" to "Netgear", "00:0F:B5" to "Netgear", "00:14:6C" to "Netgear",
        "00:18:4D" to "Netgear", "00:1B:2F" to "Netgear", "00:1E:2A" to "Netgear",
        "00:22:3F" to "Netgear", "08:02:8E" to "Netgear", "10:DA:43" to "Netgear",
        "20:4E:7F" to "Netgear", "28:C6:8E" to "Netgear", "2C:30:33" to "Netgear",
        "30:46:9A" to "Netgear", "44:94:FC" to "Netgear", "4C:60:DE" to "Netgear",
        "6C:B0:CE" to "Netgear", "74:44:01" to "Netgear", "9C:3D:CF" to "Netgear",
        "A0:40:A0" to "Netgear", "A4:2B:8C" to "Netgear", "C0:3F:0E" to "Netgear",
        "E0:46:9A" to "Netgear", "E8:FC:AF" to "Netgear",

        // --- Computadoras (Dell, Intel, Toshiba, Buffalo) ---
        "00:06:5B" to "Dell", "00:08:74" to "Dell", "00:0B:DB" to "Dell",
        "00:0D:56" to "Dell", "00:13:72" to "Dell", "00:14:22" to "Dell",
        "00:18:8B" to "Dell", "00:1A:A0" to "Dell", "00:1C:23" to "Dell",
        "00:1E:4F" to "Dell", "00:21:70" to "Dell", "10:98:36" to "Dell",
        "18:03:73" to "Dell", "24:B6:FD" to "Dell", "34:17:EB" to "Dell",
        "44:A8:42" to "Dell", "50:9A:4C" to "Dell", "74:86:7A" to "Dell",
        "78:2B:CB" to "Dell", "A4:BA:DB" to "Dell", "B8:CA:3A" to "Dell",
        "D0:67:E5" to "Dell", "F8:B1:56" to "Dell",
        "00:02:B3" to "Intel", "00:03:47" to "Intel", "00:07:E9" to "Intel",
        "00:0C:F1" to "Intel", "00:0E:0C" to "Intel", "00:0E:35" to "Intel",
        "00:11:11" to "Intel", "00:11:75" to "Intel", "00:20:7B" to "Intel",
        "00:50:F1" to "Intel", "00:AA:01" to "Intel", "00:D0:B7" to "Intel",
        "00:00:39" to "Toshiba", "00:0E:7B" to "Toshiba", "00:15:B7" to "Toshiba",
        "00:1C:7E" to "Toshiba", "00:23:18" to "Toshiba", "88:73:84" to "Toshiba",
        "B8:6B:23" to "Toshiba", "E8:9D:87" to "Toshiba", "EC:21:E5" to "Toshiba",
        "F4:64:5D" to "Toshiba",
        "00:07:40" to "Buffalo", "00:0D:0B" to "Buffalo", "00:16:01" to "Buffalo",
        "00:1D:73" to "Buffalo", "00:24:A5" to "Buffalo", "10:6F:3F" to "Buffalo",
        "34:3D:C4" to "Buffalo", "4C:E6:76" to "Buffalo", "74:03:BD" to "Buffalo",
        "84:AF:EC" to "Buffalo", "B0:C7:45" to "Buffalo",

        // --- Domótica / IoT (lo típico en una casa) ---
        "B8:27:EB" to "Raspberry Pi", "DC:A6:32" to "Raspberry Pi", "E4:5F:01" to "Raspberry Pi",
        "44:65:0D" to "Amazon (Echo/Fire TV)", "FC:65:DE" to "Amazon (Echo/Fire TV)",
        "68:37:E9" to "Amazon (Echo/Fire TV)",
        "00:17:88" to "Philips Hue", "EC:B5:FA" to "Philips Hue",
        "D0:73:D5" to "LIFX",
        "DC:1E:D5" to "Espressif (chip IoT ESP32/8266)", "08:3A:F2" to "Espressif (chip IoT ESP32/8266)",
        "08:D1:F9" to "Espressif (chip IoT ESP32/8266)", "08:B6:1F" to "Espressif (chip IoT ESP32/8266)",
        "0C:8B:95" to "Espressif (chip IoT ESP32/8266)", "34:B7:DA" to "Espressif (chip IoT ESP32/8266)",
        "3C:61:05" to "Espressif (chip IoT ESP32/8266)", "3C:84:27" to "Espressif (chip IoT ESP32/8266)",
        "54:5A:A6" to "Espressif (chip IoT ESP32/8266)", "58:CF:79" to "Espressif (chip IoT ESP32/8266)",
        "60:01:94" to "Espressif (chip IoT ESP32/8266)", "90:97:D5" to "Espressif (chip IoT ESP32/8266)",
        "94:B5:55" to "Espressif (chip IoT ESP32/8266)", "98:F4:AB" to "Espressif (chip IoT ESP32/8266)",
        "C8:2E:18" to "Espressif (chip IoT ESP32/8266)", "D4:8A:FC" to "Espressif (chip IoT ESP32/8266)",
        "E8:68:E7" to "Espressif (chip IoT ESP32/8266)",

        // --- Virtualización (por si pruebas en un emulador/VM) ---
        "00:05:69" to "VMware", "00:0C:29" to "VMware", "00:1C:14" to "VMware", "00:50:56" to "VMware"
    )

    fun vendorFor(mac: String): String? {
        if (mac.length < 8) return null
        val prefix = mac.substring(0, 8).uppercase()
        return table[prefix]
    }
}

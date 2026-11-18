package com.example.analizermeow.router

/**
 * Igual que viste en las capturas: una tabla pública de credenciales de
 * fábrica por marca/modelo, para ayudar al DUEÑO del router a entrar a su
 * propio panel de administración cuando no recuerda el usuario/clave.
 * Esta información es pública y la publican los propios fabricantes en sus manuales.
 */
object RouterConfigHelper {

    data class DefaultCredential(val brand: String, val model: String, val user: String, val password: String)

    private val defaults = listOf(
        DefaultCredential("TP-Link", "Archer serie", "admin", "admin"),
        DefaultCredential("TP-Link", "TD-W serie", "admin", "admin"),
        DefaultCredential("Huawei", "HG8245", "root", "admin"),
        DefaultCredential("2Wire", "general", "(sin login)", "(sin login)"),
        DefaultCredential("D-Link", "DIR series", "admin", "(vacío)"),
        DefaultCredential("Netgear", "Nighthawk", "admin", "password")
    )

    fun search(query: String): List<DefaultCredential> =
        defaults.filter { it.brand.contains(query, true) || it.model.contains(query, true) }

    /** Abre el panel del router del usuario en su propio WebView/navegador */
    fun adminPanelUrl(gatewayIp: String): String = "http://$gatewayIp/"
}

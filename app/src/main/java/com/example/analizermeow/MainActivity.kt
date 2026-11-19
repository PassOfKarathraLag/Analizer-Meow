package com.example.analizermeow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.analizermeow.channels.ChannelAnalyzer
import com.example.analizermeow.databinding.ActivityMainBinding
import com.example.analizermeow.devices.DeviceScanner
import com.example.analizermeow.diagnostics.NetworkDiagnostics
import com.example.analizermeow.security.SecurityChecker
import com.example.analizermeow.signal.SignalMonitor
import com.example.analizermeow.speedtest.SpeedTestManager
import kotlinx.coroutines.launch

/**
 * Punto de entrada. Conecta las 4 pestañas (Inicio / Canales / WiFi / Optimizar)
 * con la lógica real de cada módulo. Por ahora el resultado se muestra como
 * texto simple en pantalla (placeholder_title / placeholder_subtitle);
 * eso es lo que irás reemplazando por tarjetas y gráficas más adelante.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE
    )

    private lateinit var signalMonitor: SignalMonitor
    private val channelAnalyzer = ChannelAnalyzer()
    private val speedTest = SpeedTestManager()
    private val deviceScanner = DeviceScanner()
    private val diagnostics = NetworkDiagnostics()
    private val securityChecker = SecurityChecker()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        signalMonitor = SignalMonitor(this)

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadHomeTab()
                R.id.nav_channels -> loadChannelsTab()
                R.id.nav_wifi -> loadWifiTab()
                R.id.nav_optimize -> loadOptimizeTab()
            }
            true
        }

        ensurePermissions()
    }

    private fun ensurePermissions() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1001)
        } else {
            loadHomeTab()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                loadHomeTab()
            } else {
                showText(
                    "Faltan permisos",
                    "Analizer Meow necesita el permiso de ubicación para poder ver las redes WiFi cercanas (es un requisito de Android, no se usa para rastrear tu ubicación)."
                )
            }
        }
    }

    private fun showText(title: String, subtitle: String) {
        binding.placeholderTitle.text = title
        binding.placeholderSubtitle.text = subtitle
    }

    /** Pestaña 1: Inicio — medidor de % de señal */
    private fun loadHomeTab() {
        showText("Analizer Meow", "Cargando señal...")
        lifecycleScope.launch {
            signalMonitor.startScan()
            val network = signalMonitor.currentNetwork()
            if (network == null) {
                showText("Sin conexión WiFi", "Conéctate a una red WiFi y vuelve a abrir esta pestaña.")
                return@launch
            }
            val percent = signalMonitor.dbmToPercent(network.rssiDbm)
            showText(
                "$percent%",
                "Conectado a \"${network.ssid}\"\nSeñal: ${network.rssiDbm} dBm · Canal ${network.channel}"
            )
        }
    }

    /** Pestaña 2: Canales — calificación por estrellas y canal recomendado */
    private fun loadChannelsTab() {
        showText("Canales", "Analizando canales cercanos...")
        lifecycleScope.launch {
            val network = signalMonitor.currentNetwork()
            if (network == null) {
                showText("Sin conexión WiFi", "Conéctate a una red WiFi para analizar canales.")
                return@launch
            }
            val nearby = signalMonitor.scanNearby()
            val report = channelAnalyzer.analyze(nearby, network.band, network.channel)
            showText(
                "Canal actual: ${report.currentChannel} (${report.currentStars}/10 ★)",
                "Recomendado: ${report.recommended.joinToString(", ")}"
            )
        }
    }

    /** Pestaña 3: WiFi — redes vecinas detectadas */
    private fun loadWifiTab() {
        showText("Redes cercanas", "Buscando redes WiFi...")
        val nearby = signalMonitor.scanNearby()
        if (nearby.isEmpty()) {
            showText("Redes cercanas", "No se detectaron redes. Intenta de nuevo en unos segundos.")
            return
        }
        val resumen = nearby
            .sortedByDescending { it.rssiDbm }
            .take(6)
            .joinToString("\n") { "${it.ssid.ifBlank { "(oculta)" }}: ${it.rssiDbm} dBm" }
        showText("${nearby.size} redes detectadas", resumen)
    }

    /** Pestaña 4: Optimizar — seguridad de tu red + diagnóstico + dispositivos conectados */
    private fun loadOptimizeTab() {
        showText("Optimizar", "Revisando tu red...")
        lifecycleScope.launch {
            val network = signalMonitor.currentNetwork()
            if (network == null) {
                showText("Sin conexión WiFi", "Conéctate a una red WiFi para revisar su seguridad.")
                return@launch
            }

            val nearby = signalMonitor.scanNearby()
            val nearbyMatch = nearby.find { it.bssid == network.bssid }
            val findings = securityChecker.audit(network.ssid, nearbyMatch?.security ?: "")

            val speed = speedTest.runFullTest("https://speed.hetzner.de/100KB.bin")
            val report = channelAnalyzer.analyze(nearby, network.band, network.channel)
            val diag = diagnostics.diagnose(
                rssiDbm = network.rssiDbm,
                channelStars = report.currentStars,
                speed = speed,
                reconnectsLastHour = 0
            )

            val myIp = signalMonitor.deviceIp()
            val deviceCount = if (myIp != null) deviceScanner.scanLocalSubnet(myIp).size else 0

            val resumen = buildString {
                append("Seguridad: ${findings.firstOrNull()?.title ?: "OK"}\n")
                append("Posible falla: ${diag.firstOrNull()?.title ?: "Ninguna detectada"}\n")
                append("Dispositivos conectados: $deviceCount")
            }
            showText("Resultado del análisis", resumen)
        }
    }
}

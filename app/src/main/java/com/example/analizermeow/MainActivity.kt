package com.example.analizermeow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.analizermeow.channels.ChannelAnalyzer
import com.example.analizermeow.devices.DeviceScanner
import com.example.analizermeow.diagnostics.NetworkDiagnostics
import com.example.analizermeow.security.SecurityChecker
import com.example.analizermeow.signal.SignalMonitor
import com.example.analizermeow.speedtest.SpeedTestManager
import kotlinx.coroutines.launch

/**
 * Punto de entrada. Aquí solo se conecta la lógica de cada módulo con las
 * 4 secciones equivalentes a tus capturas:
 *   Inicio (medidor %) | Canales (gráfica + estrellas) | WiFi (redes vecinas) | Optimizar (seguridad + fallas)
 *
 * El layout XML (activity_main.xml con Bottom/Tab navigation) se arma en
 * Android Studio con el editor visual; aquí dejamos la lógica para que no
 * dependas de generar XML completo a mano.
 */
class MainActivity : AppCompatActivity() {

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
        setContentView(R.layout.activity_main)

        signalMonitor = SignalMonitor(this)
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
        if (requestCode == 1001) loadHomeTab()
    }

    /** Pestaña 1: Inicio — medidor de % de señal en vivo */
    private fun loadHomeTab() {
        lifecycleScope.launch {
            signalMonitor.startScan()
            val network = signalMonitor.currentNetwork() ?: return@launch
            val percent = signalMonitor.dbmToPercent(network.rssiDbm)
            // TODO: bind a tu vista circular (ej. un CircularProgressIndicator de Material)
            // binding.signalPercent.text = "$percent%"
        }
    }

    /** Pestaña 2: Canales — igual a tus capturas "Mega-5G-48B7" con estrellas */
    private fun loadChannelsTab() {
        lifecycleScope.launch {
            val network = signalMonitor.currentNetwork() ?: return@launch
            val nearby = signalMonitor.scanNearby()
            val report = channelAnalyzer.analyze(nearby, network.band, network.channel)
            // TODO: pintar report.allScores como lista de estrellas, igual a las capturas
        }
    }

    /** Pestaña 3: WiFi — lista de redes vecinas + gráfica tipo "montañas" */
    private fun loadWifiTab() {
        val nearby = signalMonitor.scanNearby()
        // TODO: alimentar MPAndroidChart con (frecuencia, rssi) por cada red para la curva tipo campana
    }

    /** Pestaña 4 (la de estrella en tus capturas): Optimizar — seguridad + diagnóstico + dispositivos */
    private fun loadOptimizeTab() {
        lifecycleScope.launch {
            val network = signalMonitor.currentNetwork() ?: return@launch

            // Auditoría de seguridad de TU red (no de redes ajenas)
            val nearbyMatch = signalMonitor.scanNearby().find { it.bssid == network.bssid }
            val findings = securityChecker.audit(network.ssid, nearbyMatch?.security ?: "")

            // Diagnóstico de "posibles fallas"
            val speed = speedTest.runFullTest("https://speed.hetzner.de/100KB.bin")
            val diag = diagnostics.diagnose(
                rssiDbm = network.rssiDbm,
                channelStars = channelAnalyzer.analyze(signalMonitor.scanNearby(), network.band, network.channel).currentStars,
                speed = speed,
                reconnectsLastHour = 0 // TODO: contar desconexiones reales con un BroadcastReceiver de WifiManager
            )

            // Dispositivos conectados, identificados con OUI + hostname
            val devices = deviceScanner.scanLocalSubnet(localIp = network.bssid)

            // TODO: pintar findings, diag y devices en RecyclerViews de la pestaña "Optimizar"
        }
    }
}

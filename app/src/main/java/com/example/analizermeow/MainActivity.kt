package com.example.analizermeow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.analizermeow.channels.ChannelAnalyzer
import com.example.analizermeow.databinding.ActivityMainBinding
import com.example.analizermeow.databinding.FragmentChannelsBinding
import com.example.analizermeow.databinding.FragmentHomeBinding
import com.example.analizermeow.databinding.FragmentOptimizeBinding
import com.example.analizermeow.databinding.FragmentWifiBinding
import com.example.analizermeow.devices.DeviceScanner
import com.example.analizermeow.diagnostics.NetworkDiagnostics
import com.example.analizermeow.security.SecurityChecker
import com.example.analizermeow.signal.SignalMonitor
import com.example.analizermeow.speedtest.SpeedTestManager
import com.example.analizermeow.ui.ChannelAdapter
import com.example.analizermeow.ui.NetworkAdapter
import kotlinx.coroutines.launch

/**
 * Punto de entrada. Cada pestaña infla su propio layout (fragment_home,
 * fragment_channels, fragment_wifi, fragment_optimize) dentro de
 * content_container, con una transición de cruce (fundido) al cambiar.
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
            if (allGranted) loadHomeTab() else showPermissionWarning()
        }
    }

    private fun showPermissionWarning() {
        val view = FragmentHomeBinding.inflate(layoutInflater, binding.contentContainer, false)
        view.homeSsid.text = "Faltan permisos"
        view.homeDetail.text =
            "Analizer Meow necesita el permiso de ubicación para ver redes WiFi cercanas (requisito de Android, no se usa para rastrearte)."
        swapContent(view.root)
    }

    /** Cambia lo que se ve en content_container con una transición de fundido/movimiento */
    private fun swapContent(view: View) {
        TransitionManager.beginDelayedTransition(binding.contentContainer, AutoTransition())
        binding.contentContainer.removeAllViews()
        binding.contentContainer.addView(view)
    }

    /** Pestaña 1: Inicio — medidor circular animado */
    private fun loadHomeTab() {
        val view = FragmentHomeBinding.inflate(layoutInflater, binding.contentContainer, false)
        swapContent(view.root)
        lifecycleScope.launch {
            signalMonitor.startScan()
            val network = signalMonitor.currentNetwork()
            if (network == null) {
                view.homeSsid.text = "Sin conexión WiFi"
                view.homeDetail.text = "Conéctate a una red WiFi y vuelve a abrir esta pestaña."
                return@launch
            }
            val percent = signalMonitor.dbmToPercent(network.rssiDbm)
            view.signalGauge.setProgressCompat(percent, true)
            view.signalPercent.text = "$percent%"
            view.homeSsid.text = network.ssid.ifBlank { "(red oculta)" }
            view.homeDetail.text = "Señal: ${network.rssiDbm} dBm · Canal ${network.channel}"
        }
    }

    /** Pestaña 2: Canales — lista con estrellas */
    private fun loadChannelsTab() {
        val view = FragmentChannelsBinding.inflate(layoutInflater, binding.contentContainer, false)
        swapContent(view.root)
        view.channelsList.layoutManager = LinearLayoutManager(this)
        lifecycleScope.launch {
            val network = signalMonitor.currentNetwork()
            if (network == null) {
                view.channelsCurrent.text = "Sin conexión WiFi"
                view.channelsRecommended.text = "Conéctate a una red WiFi para analizar canales."
                return@launch
            }
            val nearby = signalMonitor.scanNearby()
            val report = channelAnalyzer.analyze(nearby, network.band, network.channel)
            view.channelsCurrent.text = "Canal actual: ${report.currentChannel} (${report.currentStars}/10 ★)"
            view.channelsRecommended.text = "Recomendado: ${report.recommended.joinToString(", ")}"
            view.channelsList.adapter = ChannelAdapter(report.allScores, report.currentChannel, report.recommended)
        }
    }

    /** Pestaña 3: WiFi — lista de redes con barra de señal */
    private fun loadWifiTab() {
        val view = FragmentWifiBinding.inflate(layoutInflater, binding.contentContainer, false)
        swapContent(view.root)
        view.wifiList.layoutManager = LinearLayoutManager(this)
        val nearby = signalMonitor.scanNearby().sortedByDescending { it.rssiDbm }
        view.wifiHeader.text = if (nearby.isEmpty()) "Sin redes detectadas" else "${nearby.size} redes detectadas"
        view.wifiList.adapter = NetworkAdapter(nearby) { rssi -> signalMonitor.dbmToPercent(rssi) }
    }

    /** Pestaña 4: Optimizar — tarjetas de seguridad, fallas y dispositivos */
    private fun loadOptimizeTab() {
        val view = FragmentOptimizeBinding.inflate(layoutInflater, binding.contentContainer, false)
        swapContent(view.root)
        lifecycleScope.launch {
            val network = signalMonitor.currentNetwork()
            if (network == null) {
                view.optimizeSecurity.text = "Conéctate a una red WiFi para revisar su seguridad."
                view.optimizeDiagnosis.text = "—"
                view.optimizeDevices.text = "—"
                return@launch
            }

            val nearby = signalMonitor.scanNearby()
            val nearbyMatch = nearby.find { it.bssid == network.bssid }
            val findings = securityChecker.audit(network.ssid, nearbyMatch?.security ?: "")
            view.optimizeSecurity.text = findings.firstOrNull()?.let { "${it.title}\n${it.detail}" } ?: "Todo en orden."

            val speed = speedTest.runFullTest("https://speed.hetzner.de/100KB.bin")
            val report = channelAnalyzer.analyze(nearby, network.band, network.channel)
            val diag = diagnostics.diagnose(
                rssiDbm = network.rssiDbm,
                channelStars = report.currentStars,
                speed = speed,
                reconnectsLastHour = 0
            )
            view.optimizeDiagnosis.text =
                diag.firstOrNull()?.let { "${it.title}\n${it.explanation}" } ?: "Ninguna detectada."

            val myIp = signalMonitor.deviceIp()
            val deviceCount = if (myIp != null) deviceScanner.scanLocalSubnet(myIp).size else 0
            view.optimizeDevices.text = "$deviceCount dispositivos en tu red."
        }
    }
}

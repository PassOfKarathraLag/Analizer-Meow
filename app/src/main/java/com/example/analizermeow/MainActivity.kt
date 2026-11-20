package com.example.analizermeow

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import com.example.analizermeow.router.RouterConfigHelper
import com.example.analizermeow.security.SecurityChecker
import com.example.analizermeow.signal.SignalMonitor
import com.example.analizermeow.speedtest.SpeedTestManager
import com.example.analizermeow.ui.ChannelAdapter
import com.example.analizermeow.ui.DeviceAdapter
import com.example.analizermeow.ui.NetworkAdapter
import com.example.analizermeow.ui.NoiseChartHelper
import kotlinx.coroutines.launch

/**
 * Punto de entrada. Cada pestaña infla su propio layout dentro de
 * content_container, con una transición de fundido al cambiar.
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

    private fun openRouterConfig() {
        val ip = signalMonitor.deviceIp() ?: return
        val gateway = ip.substringBeforeLast(".") + ".1"
        val url = RouterConfigHelper.adminPanelUrl(gateway)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            // Sin navegador disponible; no hacemos nada más.
        }
    }

    /** Pestaña 1: Inicio — medidor circular, prueba de velocidad y accesos rápidos */
    private fun loadHomeTab() {
        val view = FragmentHomeBinding.inflate(layoutInflater, binding.contentContainer, false)
        swapContent(view.root)

        view.btnGoSecurity.setOnClickListener { binding.bottomNav.selectedItemId = R.id.nav_optimize }
        view.btnRouterConfig.setOnClickListener { openRouterConfig() }
        view.btnSpeedTest.setOnClickListener {
            view.speedResult.text = "Probando..."
            lifecycleScope.launch {
                val result = speedTest.runFullTest("https://speed.hetzner.de/100KB.bin")
                view.speedResult.text = if (result.pingMs != null) {
                    val download = result.downloadMbps?.let { " · Descarga: %.1f Mbps".format(it) } ?: ""
                    "Ping: ${result.pingMs} ms$download"
                } else {
                    "No se pudo medir (revisa tu conexión a internet)"
                }
            }
        }

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

    /** Pestaña 2: Canales — estrellas, con selector 2.4G/5G */
    private fun loadChannelsTab() {
        val view = FragmentChannelsBinding.inflate(layoutInflater, binding.contentContainer, false)
        swapContent(view.root)
        view.channelsList.layoutManager = LinearLayoutManager(this)

        val network = signalMonitor.currentNetwork()
        if (network == null) {
            view.channelsCurrent.text = "Sin conexión WiFi"
            view.channelsRecommended.text = "Conéctate a una red WiFi para analizar canales."
            return
        }

        val nearby = signalMonitor.scanNearby()

        fun render(band: SignalMonitor.Band) {
            val isBand24 = band == SignalMonitor.Band.GHZ_2_4
            view.channelsTab24.setTextColor(
                ContextCompat.getColor(this, if (isBand24) R.color.meow_text_primary else R.color.meow_text_secondary)
            )
            view.channelsTab5.setTextColor(
                ContextCompat.getColor(this, if (isBand24) R.color.meow_text_secondary else R.color.meow_text_primary)
            )

            val channelForBand = if (band == network.band) network.channel else -1
            val report = channelAnalyzer.analyze(nearby, band, channelForBand)
            view.channelsCurrent.text = if (channelForBand != -1) {
                "Canal actual: ${report.currentChannel} (${report.currentStars}/10 ★)"
            } else {
                "Viendo banda ${if (isBand24) "2.4G" else "5G"}"
            }
            view.channelsRecommended.text = "Recomendado: ${report.recommended.joinToString(", ")}"
            view.channelsList.adapter = ChannelAdapter(report.allScores, channelForBand, report.recommended)
        }

        view.channelsTab24.setOnClickListener { render(SignalMonitor.Band.GHZ_2_4) }
        view.channelsTab5.setOnClickListener { render(SignalMonitor.Band.GHZ_5) }

        render(network.band)
    }

    /** Pestaña 3: WiFi — gráfica de colinas + lista, con selector 2.4G/5G */
    private fun loadWifiTab() {
        val view = FragmentWifiBinding.inflate(layoutInflater, binding.contentContainer, false)
        swapContent(view.root)
        view.wifiList.layoutManager = LinearLayoutManager(this)

        val nearby = signalMonitor.scanNearby()
        val currentNet = signalMonitor.currentNetwork()
        val initialBand = currentNet?.band ?: SignalMonitor.Band.GHZ_2_4

        fun render(band: SignalMonitor.Band) {
            val isBand24 = band == SignalMonitor.Band.GHZ_2_4
            view.wifiTab24.setTextColor(
                ContextCompat.getColor(this, if (isBand24) R.color.meow_text_primary else R.color.meow_text_secondary)
            )
            view.wifiTab5.setTextColor(
                ContextCompat.getColor(this, if (isBand24) R.color.meow_text_secondary else R.color.meow_text_primary)
            )

            val filtered = nearby.filter { it.band == band }.sortedByDescending { it.rssiDbm }
            view.wifiHeader.text = if (filtered.isEmpty()) "Sin redes en esta banda" else "${filtered.size} redes detectadas"
            view.wifiList.adapter = NetworkAdapter(filtered) { rssi -> signalMonitor.dbmToPercent(rssi) }
            NoiseChartHelper.render(view.wifiChart, nearby, band, currentNet?.bssid)
        }

        view.wifiTab24.setOnClickListener { render(SignalMonitor.Band.GHZ_2_4) }
        view.wifiTab5.setOnClickListener { render(SignalMonitor.Band.GHZ_5) }

        render(initialBand)
    }

    /** Pestaña 4: Optimizar — todas las fallas de seguridad/diagnóstico + lista real de dispositivos */
    private fun loadOptimizeTab() {
        val view = FragmentOptimizeBinding.inflate(layoutInflater, binding.contentContainer, false)
        swapContent(view.root)
        view.optimizeDevicesList.layoutManager = LinearLayoutManager(this)
        view.optimizeDevicesList.isNestedScrollingEnabled = false

        lifecycleScope.launch {
            val network = signalMonitor.currentNetwork()
            if (network == null) {
                view.optimizeSecurity.text = "Conéctate a una red WiFi para revisar su seguridad."
                view.optimizeDiagnosis.text = "—"
                view.optimizeDevicesStatus.text = "—"
                return@launch
            }

            val nearby = signalMonitor.scanNearby()
            val nearbyMatch = nearby.find { it.bssid == network.bssid }
            val findings = securityChecker.audit(network.ssid, nearbyMatch?.security ?: "")
            view.optimizeSecurity.text = if (findings.isEmpty()) {
                "Todo en orden."
            } else {
                findings.joinToString("\n\n") { "${it.title}\n${it.detail}\n${it.howToFix}" }
            }

            val speed = speedTest.runFullTest("https://speed.hetzner.de/100KB.bin")
            val report = channelAnalyzer.analyze(nearby, network.band, network.channel)
            val diag = diagnostics.diagnose(
                rssiDbm = network.rssiDbm,
                channelStars = report.currentStars,
                speed = speed,
                reconnectsLastHour = 0
            )
            view.optimizeDiagnosis.text = diag.joinToString("\n\n") { "${it.title}\n${it.explanation}\n${it.suggestion}" }

            view.optimizeDevicesStatus.text = "Buscando dispositivos en tu red..."
            val myIp = signalMonitor.deviceIp()
            val devices = if (myIp != null) deviceScanner.scanLocalSubnet(myIp) else emptyList()
            view.optimizeDevicesStatus.text = "${devices.size} dispositivos encontrados"
            view.optimizeDevicesList.adapter = DeviceAdapter(devices.sortedBy { it.ip })
        }
    }
}

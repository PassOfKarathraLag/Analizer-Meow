package com.example.analizermeow.ui

import androidx.core.content.ContextCompat
import com.example.analizermeow.R
import com.example.analizermeow.signal.SignalMonitor
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

/**
 * Dibuja la "gráfica de ruido" tipo WiFi Analyzer: una colina por cada red,
 * centrada en su canal, cuya altura es su intensidad de señal. No es una
 * réplica exacta del original (esas curvas se calculan distinto), pero
 * transmite la misma idea de un vistazo: entre más colinas se amontonen
 * cerca de la tuya, más interferencia hay en ese canal.
 */
object NoiseChartHelper {

    fun render(
        chart: LineChart,
        networks: List<SignalMonitor.NetworkInfo>,
        band: SignalMonitor.Band,
        highlightBssid: String?
    ) {
        val context = chart.context
        val accent = ContextCompat.getColor(context, R.color.meow_accent)
        val secondary = ContextCompat.getColor(context, R.color.meow_accent_dim)
        val textColor = ContextCompat.getColor(context, R.color.meow_text_secondary)

        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(false)
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)
        chart.axisRight.isEnabled = false

        val filtered = networks.filter { it.band == band }

        if (filtered.isEmpty()) {
            chart.clear()
            chart.setNoDataTextColor(textColor)
            chart.setNoDataText("Sin redes detectadas en esta banda")
            chart.invalidate()
            return
        }

        val dataSets = filtered.map { net ->
            val x = net.channel.toFloat()
            val baseline = -100f
            val shoulder = (net.rssiDbm - 25).toFloat()
            val entries = listOf(
                Entry(x - 2.5f, baseline),
                Entry(x - 1f, shoulder),
                Entry(x, net.rssiDbm.toFloat()),
                Entry(x + 1f, shoulder),
                Entry(x + 2.5f, baseline)
            )
            val isMine = net.bssid == highlightBssid
            val color = if (isMine) accent else secondary
            val label = net.ssid.ifBlank { "(oculta)" }

            val dataSet = LineDataSet(entries, label)
            dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
            dataSet.setDrawFilled(true)
            dataSet.fillColor = color
            dataSet.fillAlpha = if (isMine) 160 else 90
            dataSet.setColor(color)
            dataSet.lineWidth = 2f
            dataSet.setDrawCircles(false)
            dataSet.setDrawValues(false)
            dataSet
        }

        chart.data = LineData(dataSets)

        val left = chart.axisLeft
        left.axisMinimum = -100f
        left.axisMaximum = -20f
        left.textColor = textColor
        left.setDrawGridLines(true)
        left.gridColor = secondary

        val bottom = chart.xAxis
        bottom.position = XAxis.XAxisPosition.BOTTOM
        bottom.textColor = textColor
        bottom.setDrawGridLines(false)
        bottom.granularity = 1f

        chart.invalidate()
    }
}

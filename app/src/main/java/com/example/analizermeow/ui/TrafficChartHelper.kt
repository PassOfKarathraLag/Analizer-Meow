package com.example.analizermeow.ui

import androidx.core.content.ContextCompat
import com.example.analizermeow.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

/**
 * Grafica en vivo de descarga/subida, parecida a la que trae la pantalla de
 * inicio del WiFi Analyzer original debajo del medidor. Se alimenta con
 * TrafficStats de Android (contador nativo de bytes), no con un servidor
 * externo, así que funciona aunque no haya internet (solo tráfico=0).
 */
object TrafficChartHelper {

    fun setup(chart: LineChart) {
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(false)
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)
        chart.setDrawBorders(false)
        chart.axisRight.isEnabled = false
        chart.axisLeft.setDrawLabels(false)
        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.axisMinimum = 0f
        chart.xAxis.setDrawLabels(false)
        chart.xAxis.setDrawGridLines(false)
    }

    fun update(chart: LineChart, download: List<Entry>, upload: List<Entry>) {
        val context = chart.context
        val downColor = ContextCompat.getColor(context, R.color.meow_accent)
        val upColor = ContextCompat.getColor(context, R.color.meow_upload)

        val downSet = LineDataSet(download.toList(), "Descargar")
        downSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        downSet.setColor(downColor)
        downSet.setDrawFilled(true)
        downSet.fillColor = downColor
        downSet.fillAlpha = 60
        downSet.lineWidth = 2f
        downSet.setDrawCircles(false)
        downSet.setDrawValues(false)

        val upSet = LineDataSet(upload.toList(), "Subir")
        upSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        upSet.setColor(upColor)
        upSet.setDrawFilled(false)
        upSet.lineWidth = 2f
        upSet.setDrawCircles(false)
        upSet.setDrawValues(false)

        chart.data = LineData(listOf(downSet, upSet))
        chart.invalidate()
    }
}

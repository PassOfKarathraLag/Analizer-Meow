package com.example.analizermeow.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.analizermeow.R
import com.example.analizermeow.signal.SignalMonitor
import com.google.android.material.progressindicator.LinearProgressIndicator

/**
 * Pinta cada red cercana con su nombre, su dBm, y una barra que se anima
 * rellenándose hasta el % de señal (como las barras teal del original).
 */
class NetworkAdapter(
    private val items: List<SignalMonitor.NetworkInfo>,
    private val toPercent: (Int) -> Int
) : RecyclerView.Adapter<NetworkAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.item_network_name)
        val dbm: TextView = view.findViewById(R.id.item_network_dbm)
        val bar: LinearProgressIndicator = view.findViewById(R.id.item_network_bar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_network, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val net = items[position]
        holder.name.text = net.ssid.ifBlank { "(oculta)" }
        holder.dbm.text = "${net.rssiDbm} dBm"
        holder.bar.progress = 0
        holder.bar.setProgressCompat(toPercent(net.rssiDbm), true)
    }
}

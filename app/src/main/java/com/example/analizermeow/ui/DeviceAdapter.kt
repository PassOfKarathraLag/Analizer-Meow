package com.example.analizermeow.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.analizermeow.R
import com.example.analizermeow.devices.DeviceScanner

/**
 * Pinta cada dispositivo encontrado con su fabricante (si se pudo
 * identificar por MAC) o su hostname, en vez del ambiguo
 * "Dispositivo general / 192.168.x.x" del original.
 */
class DeviceAdapter(
    private val items: List<DeviceScanner.DiscoveredDevice>
) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.item_device_name)
        val ip: TextView = view.findViewById(R.id.item_device_ip)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = items[position]
        val vendorLabel = device.vendor
            ?: device.guessedType.name.lowercase().replaceFirstChar { it.uppercase() }
        val hostLabel = device.hostname?.let { " · $it" } ?: ""
        holder.name.text = if (device.isGateway) "Router ($vendorLabel)" else "$vendorLabel$hostLabel"
        holder.ip.text = device.ip
    }
}

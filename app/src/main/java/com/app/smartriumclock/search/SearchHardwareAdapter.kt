package com.app.smartriumclock.search

import android.bluetooth.BluetoothDevice
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.app.smartriumclock.R
import kotlinx.android.synthetic.main.hardware_list_item.view.*

class SearchHardwareAdapter(
        private val hardwareActivity: SearchHardwareActivity,
        private val items: HashMap<String, BluetoothDevice>) :
        RecyclerView.Adapter<SearchHardwareAdapter.ViewHolder>() {

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(p0.context).inflate(R.layout.hardware_list_item, p0, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        p0.bind(items.values.toTypedArray()[p1])
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private var bluetoothDevice: BluetoothDevice? = null

        init {
            itemView.setOnClickListener {
                val device = bluetoothDevice
                if (device != null) hardwareActivity.onRequestConnectDevice(device)
            }
        }

        fun bind(bluetoothDevice: BluetoothDevice) {
            this.bluetoothDevice = bluetoothDevice

            val bluetoothText = String.format("%s", bluetoothDevice.name)
            itemView.hardwareListItemTitle.text = bluetoothText



        }
    }

}

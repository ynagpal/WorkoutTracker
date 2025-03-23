package com.example.healthtracker

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class DeviceListActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private val deviceList = mutableListOf<BluetoothDevice>()
    private val deviceNames = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private var bluetoothAdapter: BluetoothAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listView = ListView(this)
        setContentView(listView)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNames)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val device = deviceList[position]
            val resultIntent = Intent().apply {
                putExtra("device_address", device.address)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        startBluetoothScan()
    }

    private fun startBluetoothScan() {
        if (bluetoothAdapter?.isEnabled != true) {
            Toast.makeText(this, "Enable Bluetooth first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                val device = result?.device
                if (device != null && device.address !in deviceList.map { it.address }) {
                    deviceList.add(device)
                    deviceNames.add("${device.name ?: "Unknown"} - ${device.address}")
                    adapter.notifyDataSetChanged()
                }
            }
        }

        scanner.startScan(callback)
        Toast.makeText(this, "Scanning devices...", Toast.LENGTH_SHORT).show()

        listView.postDelayed({
            scanner.stopScan(callback)
            Toast.makeText(this, "Scan complete", Toast.LENGTH_SHORT).show()
        }, 8000)
    }
}

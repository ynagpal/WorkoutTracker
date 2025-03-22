package com.example.healthtracker

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SpO2Activity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var spO2DataText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spo2)

        spO2DataText = findViewById(R.id.spO2DataText)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        scanForSpO2Devices()
    }

    private fun scanForSpO2Devices() {
        if (bluetoothAdapter?.isEnabled != true) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        val scanner: BluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                val device = result?.device
                if (device?.name?.contains("SpO2") == true) {
                    scanner.stopScan(this)
                    spO2DataText.text = "Connected to ${device.name} for SpO2 tracking"
                }
            }
        }

        scanner.startScan(callback)
        Toast.makeText(this, "Scanning for SpO2 devices...", Toast.LENGTH_SHORT).show()
    }
}
package com.example.healthtracker

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SleepActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var sleepDataText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sleep)

        sleepDataText = findViewById(R.id.sleepDataText)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        scanForSleepDevices()
    }

    private fun scanForSleepDevices() {
        if (bluetoothAdapter?.isEnabled != true) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        val scanner: BluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                val device = result?.device
                if (device?.name?.contains("Sleep") == true) {
                    scanner.stopScan(this)
                    sleepDataText.text = "Connected to ${device.name} for sleep tracking"
                }
            }
        }

        scanner.startScan(callback)
        Toast.makeText(this, "Scanning for sleep devices...", Toast.LENGTH_SHORT).show()
    }
}
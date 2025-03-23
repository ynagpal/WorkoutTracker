package com.example.healthtracker

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SleepActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var sleepDataText: TextView

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            scanForSleepDevices()
        } else {
            Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sleep)

        sleepDataText = findViewById(R.id.sleepDataText)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        checkBluetoothPermissions()
    }

    private fun checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val requiredPermissions = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
            val missingPermissions = requiredPermissions.any {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missingPermissions) {
                permissionLauncher.launch(requiredPermissions)
            } else {
                scanForSleepDevices()
            }
        } else {
            scanForSleepDevices()
        }
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
                if (device?.name?.contains("Sleep", true) == true) {
                    scanner.stopScan(this)
                    sleepDataText.text = "Connected to ${device.name} for sleep tracking"
                }
            }
        }

        scanner.startScan(callback)
        Toast.makeText(this, "Scanning for sleep devices...", Toast.LENGTH_SHORT).show()
    }
}

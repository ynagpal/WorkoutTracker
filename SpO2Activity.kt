package com.example.healthtracker

import android.bluetooth.*
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class SpO2Activity : AppCompatActivity() {
    private lateinit var spO2Text: TextView
    private var bluetoothGatt: BluetoothGatt? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spo2)

        spO2Text = findViewById(R.id.spO2Text)

        scanForBluetoothDevices()
    }

    private fun scanForBluetoothDevices() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter?.isEnabled == true) {
            val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
            val scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    super.onScanResult(callbackType, result)
                    val device = result.device
                    if (device.name != null && device.name.contains("Ring")) { 
                        bluetoothLeScanner?.stopScan(this)
                        connectToDevice(device)
                    }
                }
            }
            bluetoothLeScanner.startScan(scanCallback)
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> gatt?.discoverServices()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                val service = gatt?.getService(UUID.fromString("00001822-0000-1000-8000-00805F9B34FB"))
                val characteristic = service?.getCharacteristic(UUID.fromString("00002A5F-0000-1000-8000-00805F9B34FB"))
                gatt?.setCharacteristicNotification(characteristic, true)
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                val spo2 = characteristic?.value?.get(1)?.toInt() ?: 95
                runOnUiThread {
                    spO2Text.text = "SpO2: $spo2%"
                }
            }
        })
    }
}

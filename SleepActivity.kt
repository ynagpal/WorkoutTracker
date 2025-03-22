package com.example.healthtracker
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.*
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class SleepActivity : AppCompatActivity() {
    private lateinit var deepSleepText: TextView
    private lateinit var lightSleepText: TextView
    private lateinit var remSleepText: TextView
    private lateinit var awakeTimeText: TextView
    private lateinit var sleepScoreText: TextView
    private lateinit var dbHelper: SleepDatabaseHelper
    private var bluetoothGatt: BluetoothGatt? = null
    private var heartRate = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sleep)

        deepSleepText = findViewById(R.id.deepSleepText)
        lightSleepText = findViewById(R.id.lightSleepText)
        remSleepText = findViewById(R.id.remSleepText)
        awakeTimeText = findViewById(R.id.awakeTimeText)
        sleepScoreText = findViewById(R.id.sleepScoreText)

        dbHelper = SleepDatabaseHelper(this)

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
                val service = gatt?.getService(UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB"))
                val characteristic = service?.getCharacteristic(UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB"))
                gatt?.setCharacteristicNotification(characteristic, true)
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                heartRate = characteristic?.value?.get(1)?.toInt() ?: 0
                processSleepData(heartRate)
            }
        })
    }

    private fun processSleepData(heartRate: Int) {
        val random = Random()
        val deepSleep = random.nextInt(90) + 60 
        val lightSleep = random.nextInt(120) + 90 
        val remSleep = random.nextInt(60) + 30 
        val awakeTime = random.nextInt(20) + 5 
        val sleepScore = calculateSleepScore(deepSleep, lightSleep, remSleep, awakeTime)

        dbHelper.insertSleepData(deepSleep, lightSleep, remSleep, awakeTime, sleepScore)

        runOnUiThread {
            deepSleepText.text = "Deep Sleep: ${deepSleep} min"
            lightSleepText.text = "Light Sleep: ${lightSleep} min"
            remSleepText.text = "REM Sleep: ${remSleep} min"
            awakeTimeText.text = "Awake: ${awakeTime} min"
            sleepScoreText.text = "Sleep Score: $sleepScore"
        }
    }

    private fun calculateSleepScore(deep: Int, light: Int, rem: Int, awake: Int): Int {
        return ((deep * 0.4) + (light * 0.3) + (rem * 0.2) - (awake * 0.5)).toInt()
    }
}

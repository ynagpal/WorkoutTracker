package com.example.healthtracker

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var heartRate = 0
    private var steps = 0
    private var caloriesBurned = 0.0
    private var workoutDuration = 0
    private var startTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var heartRateText: TextView
    private lateinit var stepsText: TextView
    private lateinit var caloriesText: TextView
    private lateinit var heartRateGraph: ImageView
    private lateinit var startWorkoutButton: Button
    private lateinit var historyButton: Button
    private lateinit var sleepButton: Button
    private lateinit var spO2Button: Button
    private lateinit var darkModeToggle: Switch
    private lateinit var viewLogsButton: Button

    private var isConnectedToDevice = false
    private val bleDevices = mutableListOf<BluetoothDevice>()
    private lateinit var bleScanner: BluetoothLeScanner
    private var scanCallback: ScanCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            Logger.log(this, "CRASH", throwable.stackTraceToString())
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        heartRateText = findViewById(R.id.heartRateText)
        stepsText = findViewById(R.id.stepsText)
        caloriesText = findViewById(R.id.caloriesText)
        heartRateGraph = findViewById(R.id.heartRateGraph)
        startWorkoutButton = findViewById(R.id.startWorkoutButton)
        historyButton = findViewById(R.id.historyButton)
        sleepButton = findViewById(R.id.sleepButton)
        spO2Button = findViewById(R.id.spO2Button)
        darkModeToggle = findViewById(R.id.darkModeToggle)
        viewLogsButton = findViewById(R.id.viewLogsButton)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bleScanner = bluetoothAdapter?.bluetoothLeScanner ?: return

        darkModeToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            recreate()
        }

        startWorkoutButton.setOnClickListener { if (isConnectedToDevice) startWorkout() else showToast("Connect to a device first") }
        historyButton.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        sleepButton.setOnClickListener { startActivity(Intent(this, SleepActivity::class.java)) }
        spO2Button.setOnClickListener { startActivity(Intent(this, SpO2Activity::class.java)) }
        viewLogsButton.setOnClickListener { startActivity(Intent(this, LogViewerActivity::class.java)) }

        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse)
        heartRateGraph.startAnimation(pulseAnimation)

        scanForBluetoothDevices()
    }

    private fun scanForBluetoothDevices() {
        if (bluetoothAdapter?.isEnabled != true) {
            showToast("Please enable Bluetooth")
            return
        }

        bleDevices.clear()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.device?.let { device ->
                    if (!bleDevices.contains(device) && !device.name.isNullOrBlank()) {
                        bleDevices.add(device)
                    }
                }
            }
        }

        bleScanner.startScan(scanCallback)
        handler.postDelayed({
            bleScanner.stopScan(scanCallback)
            showDeviceSelectionDialog()
        }, 5000) // scan for 5 seconds
        showToast("Scanning for devices...")
    }

    private fun showDeviceSelectionDialog() {
        if (bleDevices.isEmpty()) {
            showToast("No BLE devices found")
            return
        }

        val deviceNames = bleDevices.map { it.name ?: "Unnamed Device" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select Device")
            .setItems(deviceNames) { _, which ->
                val selectedDevice = bleDevices[which]
                connectToDevice(selectedDevice)
            }
            .setCancelable(false)
            .show()
    }

    private fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        runOnUiThread {
                            showToast("Connected to ${device.name}")
                            isConnectedToDevice = true
                        }
                        gatt?.discoverServices()
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        runOnUiThread {
                            showToast("Disconnected from ${device.name}")
                            isConnectedToDevice = false
                        }
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                val service = gatt?.getService(UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB"))
                val characteristic = service?.getCharacteristic(UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB"))
                if (characteristic != null) {
                    gatt.setCharacteristicNotification(characteristic, true)
                }
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                val value = characteristic?.value ?: return
                val flag = value.getOrNull(0)?.toInt() ?: return
                heartRate = if (flag and 0x01 != 0) {
                    (value.getOrNull(1)?.toInt() ?: 0 and 0xFF) or
                            ((value.getOrNull(2)?.toInt() ?: 0 and 0xFF) shl 8)
                } else {
                    value.getOrNull(1)?.toInt() ?: 0 and 0xFF
                }

                runOnUiThread { updateWorkout() }
            }
        })
    }

    private fun startWorkout() {
        startTime = System.currentTimeMillis()
        handler.postDelayed(object : Runnable {
            override fun run() {
                updateWorkout()
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun updateWorkout() {
        workoutDuration = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        steps += (heartRate / 3)
        caloriesBurned = calculateCalories(70, 25, heartRate, workoutDuration)

        heartRateText.text = "❤️ Heart Rate: $heartRate BPM"
        stepsText.text = "🚶 Steps Taken: $steps"
        caloriesText.text = "🔥 Calories Burned: ${caloriesBurned.roundToInt()} kcal"
    }

    private fun calculateCalories(weight: Int, age: Int, heartRate: Int, duration: Int): Double {
        return if (duration > 0)
            ((age * 0.2017) + (weight * 0.09036) + (heartRate * 0.6309) - 55.0969) * (duration / 4.184)
        else 0.0
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

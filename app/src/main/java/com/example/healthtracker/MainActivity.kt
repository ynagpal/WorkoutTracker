package com.example.healthtracker

import android.app.AlertDialog
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import java.util.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var isDeviceConnected = false
    private var heartRate = 0
    private var steps = 0
    private var caloriesBurned = 0.0
    private var workoutDuration = 0
    private var startTime: Long = 0
    private val handler = Handler()
    private var isWorkoutRunning = false

    private lateinit var heartRateText: TextView
    private lateinit var stepsText: TextView
    private lateinit var caloriesText: TextView
    private lateinit var heartRateGraph: ImageView
    private lateinit var startWorkoutButton: Button
    private lateinit var stopWorkoutButton: Button
    private lateinit var connectBluetoothButton: Button
    private lateinit var historyButton: Button
    private lateinit var sleepButton: Button
    private lateinit var spO2Button: Button
    private lateinit var darkModeToggle: Switch
    private lateinit var viewLogsButton: Button

    private val scanResults = mutableListOf<BluetoothDevice>()
    private var scanCallback: ScanCallback? = null
    private var classicReceiver: BroadcastReceiver? = null

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
        stopWorkoutButton = findViewById(R.id.stopWorkoutButton)
        connectBluetoothButton = findViewById(R.id.connectBluetoothButton)
        historyButton = findViewById(R.id.historyButton)
        sleepButton = findViewById(R.id.sleepButton)
        spO2Button = findViewById(R.id.spO2Button)
        darkModeToggle = findViewById(R.id.darkModeToggle)
        viewLogsButton = findViewById(R.id.viewLogsButton)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        stopWorkoutButton.isEnabled = false

        connectBluetoothButton.setOnClickListener { scanForBluetoothDevices() }

        startWorkoutButton.setOnClickListener {
            if (!isDeviceConnected) {
                Toast.makeText(this, "Please connect a Bluetooth device first.", Toast.LENGTH_SHORT).show()
            } else {
                startWorkout()
            }
        }

        stopWorkoutButton.setOnClickListener {
            stopWorkout()
        }

        historyButton.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        sleepButton.setOnClickListener { startActivity(Intent(this, SleepActivity::class.java)) }
        spO2Button.setOnClickListener { startActivity(Intent(this, SpO2Activity::class.java)) }
        viewLogsButton.setOnClickListener { startActivity(Intent(this, LogViewerActivity::class.java)) }

        darkModeToggle.setOnCheckedChangeListener { _, isChecked ->
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse)
        heartRateGraph.startAnimation(pulseAnimation)
    }

    private fun startWorkout() {
        if (isWorkoutRunning) return
        isWorkoutRunning = true
        startWorkoutButton.isEnabled = false
        stopWorkoutButton.isEnabled = true

        startTime = System.currentTimeMillis()
        handler.post(workoutRunnable)
    }

    private fun stopWorkout() {
        isWorkoutRunning = false
        handler.removeCallbacks(workoutRunnable)
        startWorkoutButton.isEnabled = true
        stopWorkoutButton.isEnabled = false
        Toast.makeText(this, "Workout stopped", Toast.LENGTH_SHORT).show()
    }

    private val workoutRunnable = object : Runnable {
        override fun run() {
            if (isWorkoutRunning) {
                updateWorkout()
                handler.postDelayed(this, 1000)
            }
        }
    }

    private fun updateWorkout() {
        workoutDuration = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        steps += (heartRate / 3)
        caloriesBurned = calculateCalories(70, 25, heartRate, workoutDuration)

        runOnUiThread {
            heartRateText.text = "❤️ Heart Rate: $heartRate BPM"
            stepsText.text = "🚶 Steps Taken: $steps"
            caloriesText.text = "🔥 Calories Burned: ${caloriesBurned.roundToInt()} kcal"
        }
    }

    private fun calculateCalories(weight: Int, age: Int, heartRate: Int, duration: Int): Double {
        return if (duration > 0)
            ((age * 0.2017) + (weight * 0.09036) + (heartRate * 0.6309) - 55.0969) * (duration / 4.184)
        else 0.0
    }

    private fun scanForBluetoothDevices() {
        scanResults.clear()
        val bleScanner = bluetoothAdapter?.bluetoothLeScanner ?: return

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.device?.let { device ->
                    if (!scanResults.contains(device) && device.name != null) {
                        scanResults.add(device)
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Toast.makeText(this@MainActivity, "BLE scan failed: $errorCode", Toast.LENGTH_SHORT).show()
            }
        }

        bleScanner.startScan(scanCallback)
        Toast.makeText(this, "Scanning BLE devices...", Toast.LENGTH_SHORT).show()

        handler.postDelayed({
            bleScanner.stopScan(scanCallback)
            if (scanResults.isEmpty()) {
                startClassicBluetoothDiscovery()
            } else {
                showDeviceSelectionDialog()
            }
        }, 5000)
    }

    private fun startClassicBluetoothDiscovery() {
        Toast.makeText(this, "Scanning classic devices...", Toast.LENGTH_SHORT).show()
        classicReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        if (device?.name != null && !scanResults.contains(device)) {
                            scanResults.add(device)
                        }
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        unregisterReceiver(this)
                        showDeviceSelectionDialog()
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

        registerReceiver(classicReceiver, filter)
        bluetoothAdapter?.startDiscovery()
    }

    private fun showDeviceSelectionDialog() {
        if (scanResults.isEmpty()) {
            Toast.makeText(this, "No devices found", Toast.LENGTH_SHORT).show()
            return
        }

        val deviceNames = scanResults.map { it.name + "\n" + it.address }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Bluetooth Device")
            .setItems(deviceNames) { _, which ->
                connectToDevice(scanResults[which])
            }
            .show()
    }

    private fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    isDeviceConnected = true
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
                    }
                    gatt?.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    isDeviceConnected = false
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Disconnected from ${device.name}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                val service = gatt?.getService(UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb"))
                val characteristic = service?.getCharacteristic(UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb"))
                if (characteristic != null) {
                    gatt.setCharacteristicNotification(characteristic, true)
                    val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
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
}

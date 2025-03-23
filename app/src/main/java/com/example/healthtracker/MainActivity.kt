package com.example.healthtracker

import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import kotlin.math.roundToInt
import java.util.*

class MainActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var heartRate = 0
    private var steps = 0
    private var caloriesBurned = 0.0
    private var workoutDuration = 0
    private var startTime: Long = 0
    private val handler = Handler()

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

    private var isBluetoothConnected = false
    private val REQUEST_DEVICE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            Logger.log(this, "CRASH", throwable.stackTraceToString())
        }

        setContentView(R.layout.activity_main)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

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

        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse)
        heartRateGraph.startAnimation(pulseAnimation)

        // Toggle dark mode
        darkModeToggle.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        startWorkoutButton.setOnClickListener {
            val intent = Intent(this, DeviceListActivity::class.java)
            startActivityForResult(intent, REQUEST_DEVICE)
        }

        historyButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        sleepButton.setOnClickListener {
            startActivity(Intent(this, SleepActivity::class.java))
        }

        spO2Button.setOnClickListener {
            startActivity(Intent(this, SpO2Activity::class.java))
        }

        viewLogsButton.setOnClickListener {
            startActivity(Intent(this, LogViewerActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_DEVICE && resultCode == Activity.RESULT_OK) {
            val address = data?.getStringExtra("device_address")
            val device = bluetoothAdapter?.getRemoteDevice(address)
            if (device != null) {
                connectToDevice(device)
            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        isBluetoothConnected = true
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
                            startWorkout()
                        }
                        gatt?.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        isBluetoothConnected = false
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Disconnected from ${device.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt?.getService(UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB"))
                    val characteristic = service?.getCharacteristic(UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB"))
                    if (characteristic != null) {
                        gatt.setCharacteristicNotification(characteristic, true)
                    }
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
        if (!isBluetoothConnected) {
            Toast.makeText(this, "Please connect to a Bluetooth device first", Toast.LENGTH_SHORT).show()
            return
        }

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
}

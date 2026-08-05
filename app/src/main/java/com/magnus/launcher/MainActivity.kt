package com.magnus.launcher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private lateinit var mqttClient: MQTTClient
    private lateinit var btnDataPause: Button
    private lateinit var tvTime: TextView
    private lateinit var tvBattery: TextView
    private lateinit var ivBatteryIcon: TextView
    private var isRecording = true
    private val TAG = "MainActivity"
    private val handler = Handler(Looper.getMainLooper())
    private var isCharging = false
    private var animationRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        setContentView(R.layout.activity_main)

        try {
            mqttClient = MQTTClient(this)
            mqttClient.connect()

            tvTime = findViewById(R.id.tvTime)
            tvBattery = findViewById(R.id.tvBattery)
            ivBatteryIcon = findViewById(R.id.ivBatteryIcon)

            startTimeUpdate()
            registerBatteryReceiver()

            btnDataPause = findViewById(R.id.btnDataPause)
            btnDataPause.setBackgroundColor(Color.parseColor("#004400"))
            btnDataPause.setTextColor(Color.parseColor("#00FF00"))
            
            val updateGpsDisplay = object : Runnable {
                override fun run() {
                    val gpsData = mqttClient.getGpsData()
                    val displayText = if (isRecording) {
                        "📍 REC\n$gpsData"
                    } else {
                        "⏸️ PAUSE\n$gpsData"
                    }
                    btnDataPause.text = displayText
                    handler.postDelayed(this, 500)
                }
            }
            handler.post(updateGpsDisplay)
            
            btnDataPause.setOnClickListener { toggleDataPause() }

            findViewById<Button>(R.id.btnRaceChronoHeader).setOnClickListener {
                launchApp("com.racechrono.app", "com.racechrono.app.ui.MainActivity")
            }

            findViewById<Button>(R.id.btnRallyCallHeader).setOnClickListener {
                launchApp("io.tiste.RallyCall", "io.tiste.RallyCall.MainActivity")
            }

            findViewById<Button>(R.id.btnRecord).setOnClickListener {
                try {
                    startActivity(Intent(MediaStore.ACTION_VIDEO_CAPTURE))
                } catch (e: Exception) {
                    Log.e(TAG, "Camera error: ${e.message}")
                }
            }

            findViewById<Button>(R.id.btnBluetooth).setOnClickListener {
                startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
            }

            findViewById<Button>(R.id.btnSettings).setOnClickListener {
                startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
            }

            findViewById<Button>(R.id.btnReboot).setOnClickListener {
                Runtime.getRuntime().exec("su -c reboot")
            }

        } catch (e: Exception) {
            Log.e(TAG, "onCreate error: ${e.message}", e)
        }
    }

    private fun launchApp(packageName: String, activityName: String) {
        try {
            val intent = Intent()
            intent.component = ComponentName(packageName, activityName)
            startActivity(intent)
            Log.d(TAG, "Launched $packageName/$activityName")
        } catch (e: Exception) {
            Log.e(TAG, "Launch error: ${e.message}")
        }
    }

    private fun startTimeUpdate() {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val updateTime = object : Runnable {
            override fun run() {
                tvTime.text = timeFormat.format(Date())
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateTime)
    }

    private fun registerBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val batteryPct = (level * 100) / scale
                    
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    tvBattery.text = "$batteryPct%"

                    val color = when {
                        batteryPct >= 75 -> Color.parseColor("#00FF00")
                        batteryPct >= 50 -> Color.parseColor("#FFFF00")
                        batteryPct >= 25 -> Color.parseColor("#FF8800")
                        else -> Color.parseColor("#FF0000")
                    }
                    tvBattery.setTextColor(color)

                    if (isCharging && !animationRunning) {
                        startChargingAnimation(color)
                    } else if (!isCharging && animationRunning) {
                        animationRunning = false
                        ivBatteryIcon.text = "🔋"
                        ivBatteryIcon.setTextColor(color)
                    } else if (!isCharging) {
                        ivBatteryIcon.text = "🔋"
                        ivBatteryIcon.setTextColor(color)
                    }
                }
            }
        }, filter)
    }

    private fun startChargingAnimation(baseColor: Int) {
        animationRunning = true
        var opacity = 128
        val direction = 1

        val animation = object : Runnable {
            override fun run() {
                if (animationRunning) {
                    // Pulse l'opacité de la batterie (remplissage simulé)
                    val adjustedColor = Color.argb(
                        opacity,
                        Color.red(baseColor),
                        Color.green(baseColor),
                        Color.blue(baseColor)
                    )
                    
                    ivBatteryIcon.text = "🔋"
                    ivBatteryIcon.setTextColor(adjustedColor)
                    
                    opacity += (direction * 20)
                    if (opacity >= 255 || opacity <= 128) {
                        opacity = if (opacity >= 255) 128 else 255
                    }
                    
                    handler.postDelayed(this, 300)
                } else {
                    ivBatteryIcon.text = "🔋"
                    ivBatteryIcon.setTextColor(baseColor)
                }
            }
        }
        handler.post(animation)
    }

    private fun toggleDataPause() {
        isRecording = !isRecording
        if (isRecording) {
            btnDataPause.setBackgroundColor(Color.parseColor("#004400"))
            btnDataPause.setTextColor(Color.parseColor("#00FF00"))
            mqttClient.setPaused(false)
        } else {
            btnDataPause.setBackgroundColor(Color.parseColor("#440000"))
            btnDataPause.setTextColor(Color.parseColor("#FF0000"))
            mqttClient.setPaused(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttClient.disconnect()
        handler.removeCallbacksAndMessages(null)
    }
}

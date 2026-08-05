package com.magnus.launcher

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Button
import android.util.Log

class MainActivity : Activity() {
    private lateinit var mqttClient: MQTTClient
    private lateinit var btnDataPause: Button
    private var isRecording = true
    private val TAG = "MainActivity"
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            // Init MQTT
            mqttClient = MQTTClient(this)
            mqttClient.connect()

            // Data Pause Button - affiche GPS en temps réel
            btnDataPause = findViewById(R.id.btnDataPause)
            btnDataPause.setBackgroundColor(Color.parseColor("#004400"))
            btnDataPause.setTextColor(Color.parseColor("#00FF00"))
            
            // Update GPS display every 500ms
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
            
            btnDataPause.setOnClickListener {
                toggleDataPause()
            }

            // RaceChrono Header
            findViewById<Button>(R.id.btnRaceChronoHeader).setOnClickListener {
                startActivity(Intent("com.racechrono.app"))
            }

            // Rally Call Header
            findViewById<Button>(R.id.btnRallyCallHeader).setOnClickListener {
                startActivity(Intent("io.tiste.RallyCall"))
            }

            // Camera Button
            findViewById<Button>(R.id.btnRecord).setOnClickListener {
                try {
                    startActivity(Intent(MediaStore.ACTION_VIDEO_CAPTURE))
                } catch (e: Exception) {
                    Log.e(TAG, "Camera error: ${e.message}")
                }
            }

            // Bluetooth Button
            findViewById<Button>(R.id.btnBluetooth).setOnClickListener {
                startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
            }

            // Settings Button
            findViewById<Button>(R.id.btnSettings).setOnClickListener {
                startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
            }

            // Reboot Button
            findViewById<Button>(R.id.btnReboot).setOnClickListener {
                Runtime.getRuntime().exec("su -c reboot")
            }

        } catch (e: Exception) {
            Log.e(TAG, "onCreate error: ${e.message}", e)
        }
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

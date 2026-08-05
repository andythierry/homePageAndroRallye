package com.magnus.launcher

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.util.Log

class MainActivity : Activity() {
    private lateinit var mqttClient: MQTTClient
    private lateinit var btnDataPause: Button
    private var isRecording = true
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            // Init MQTT
            mqttClient = MQTTClient(this)
            mqttClient.connect()
            
            // Data Pause Button
            btnDataPause = findViewById(R.id.btnDataPause)
            btnDataPause.setBackgroundColor(Color.parseColor("#004400"))
            btnDataPause.setTextColor(Color.parseColor("#00FF00"))
            btnDataPause.setOnClickListener {
                toggleDataPause()
            }
            
            // GPS Button
            findViewById<Button>(R.id.btnGPS).setOnClickListener {
                val gpsData = mqttClient.getGpsData()
                btnDataPause.text = gpsData
            }
            
            // Rally Call Button
            findViewById<Button>(R.id.btnRallyCall).setOnClickListener {
                startActivity(Intent("io.tiste.RallyCall"))
            }
            
            // Settings Button
            findViewById<Button>(R.id.btnSettings).setOnClickListener {
                startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
            }
            
            // Record Button
            findViewById<Button>(R.id.btnRecord).setOnClickListener {
                Log.d(TAG, "Record clicked")
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
            btnDataPause.text = "⏺️ REC"
            mqttClient.setPaused(false)
        } else {
            btnDataPause.setBackgroundColor(Color.parseColor("#440000"))
            btnDataPause.setTextColor(Color.parseColor("#FF0000"))
            btnDataPause.text = "⏸️ PAUSE"
            mqttClient.setPaused(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttClient.disconnect()
    }
}

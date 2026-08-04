package com.magnus.launcher
import android.view.WindowManager
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Context
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var mqttClient: MQTTClient
    private lateinit var timeDisplay: TextView
    private lateinit var batteryDisplay: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateBattery()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Initialiser MQTT
        mqttClient = MQTTClient(this)
        mqttClient.connect()
        // Garder l'écran allumé
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Permettre l'ajustement de luminosité
        window.attributes.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        
        timeDisplay = findViewById(R.id.time_display)
        batteryDisplay = findViewById(R.id.battery_display)
        val btnGPS = findViewById<Button>(R.id.btn_gps)
        val btnBLE = findViewById<Button>(R.id.btn_ble)
        val btnRallyCall = findViewById<Button>(R.id.btn_rally_call)
        val btnSettings = findViewById<Button>(R.id.btn_settings)
        val btnRecord = findViewById<Button>(R.id.btn_shutdown)
        val btnReboot = findViewById<Button>(R.id.btn_reboot)
        
        btnGPS.setOnClickListener { launchApp("com.racechrono.app") }
        btnBLE.setOnClickListener { toggleBluetooth() }
        btnRallyCall.setOnClickListener { launchApp("io.tiste.RallyCall") }
        btnSettings.setOnClickListener { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) }
        btnRecord.setOnClickListener { recordVideo() }
        btnReboot.setOnClickListener { reboot() }
        
        updateTime()
        updateBattery()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }
    
    private fun recordVideo() {
    try {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(this, "Erreur caméra: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
    
    private fun updateTime() {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        timeDisplay.text = sdf.format(Date())
        handler.postDelayed({ updateTime() }, 1000)
    }
    
    private fun updateBattery() {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        
        val color = when {
            level > 50 -> "#4CAF50"
            level > 20 -> "#FFC107"
            else -> "#F44336"
        }
        
        batteryDisplay.setTextColor(android.graphics.Color.parseColor(color))
        batteryDisplay.text = "🔋 $level%"
    }
    
    private fun launchApp(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "App non trouvée: $packageName", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun toggleBluetooth() {
        try {
            startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur BLE", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun reboot() {
        try {
            Runtime.getRuntime().exec("su -c 'reboot'")
        } catch (e: Exception) {
            Toast.makeText(this, "Reboot requiert root", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        handler.removeCallbacksAndMessages(null)
        mqttClient.disconnect()
    }
}

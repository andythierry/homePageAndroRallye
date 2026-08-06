package com.magnus.launcher.settings
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Button
import androidx.core.content.ContextCompat
import com.magnus.launcher.R

class SettingsActivity : AppCompatActivity() {
    private lateinit var statusBluetooth: TextView
    private lateinit var statusBattery: TextView
    private lateinit var statusNetwork: TextView
    private lateinit var statusGPS: TextView
    private lateinit var statusSolution: TextView
    
    private lateinit var radioGroup: RadioGroup
    private lateinit var btnApply: Button
    private lateinit var btnBack: Button
    
    private var currentSolution = 0
    private var wakeLock: PowerManager.WakeLock? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        initViews()
        setupRadioGroup()
        setupUpdateStatus()
        registerBatteryReceiver()
        setupBackButton()
    }

    private fun setupBackButton() {
        btnBack = findViewById(R.id.btn_back)
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun initViews() {
        statusBluetooth = findViewById(R.id.status_bluetooth)
        statusBattery = findViewById(R.id.status_battery)
        statusNetwork = findViewById(R.id.status_network)
        statusGPS = findViewById(R.id.status_gps)
        statusSolution = findViewById(R.id.status_solution)
        
        radioGroup = findViewById(R.id.radio_group_solutions)
        btnApply = findViewById(R.id.btn_apply)
        
        btnApply.setOnClickListener { applySolution() }
        
        val prefs = getSharedPreferences("magnus_settings", Context.MODE_PRIVATE)
        currentSolution = prefs.getInt("gps_wake_solution", 2)
        
        radioGroup.check(
            when (currentSolution) {
                1 -> R.id.radio_disable_sleep
                2 -> R.id.radio_wakelock
                3 -> R.id.radio_foreground_service
                4 -> R.id.radio_alarm_manager
                else -> R.id.radio_wakelock
            }
        )
        
        if (!prefs.getBoolean("wakelock_initialized", false)) {
            enableWakeLock()
            prefs.edit()
                .putBoolean("wakelock_initialized", true)
                .putInt("gps_wake_solution", 2)
                .apply()
            showToast("✅ WakeLock activé par défaut")
        }
    }

    private fun setupRadioGroup() {
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            currentSolution = when (checkedId) {
                R.id.radio_disable_sleep -> 1
                R.id.radio_wakelock -> 2
                R.id.radio_foreground_service -> 3
                R.id.radio_alarm_manager -> 4
                else -> 0
            }
        }
    }

    private fun setupUpdateStatus() {
        updateStatus()
        handler.postDelayed(object : Runnable {
            override fun run() {
                updateStatus()
                handler.postDelayed(this, updateInterval)
            }
        }, updateInterval)
    }

    private fun updateStatus() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bluetoothStatus = if (bluetoothAdapter?.isEnabled == true) {
            "🔵 Bluetooth : ON"
        } else {
            "⚫ Bluetooth : OFF"
        }
        statusBluetooth.text = bluetoothStatus

        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = registerReceiver(null, intentFilter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        statusBattery.text = "🔋 Batterie : $level%"

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val networkStatus = if (networkInfo?.isConnected == true) {
            "📡 Réseau : ON (${networkInfo.typeName})"
        } else {
            "❌ Réseau : OFF"
        }
        statusNetwork.text = networkStatus

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsStatus = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            "📍 GPS : ON"
        } else {
            "❌ GPS : OFF"
        }
        statusGPS.text = gpsStatus

        val solutionName = when (currentSolution) {
            1 -> "🚫 Veille désactivée"
            2 -> "⚡ WakeLock (partiel)"
            3 -> "📲 Service au premier plan"
            4 -> "⏰ AlarmManager"
            else -> "❌ Aucune"
        }
        statusSolution.text = "Solution active : $solutionName"
    }

    private fun applySolution() {
        val prefs = getSharedPreferences("magnus_settings", Context.MODE_PRIVATE)
        prefs.edit().putInt("gps_wake_solution", currentSolution).apply()
        when (currentSolution) {
            1 -> disableSleep()
            2 -> enableWakeLock()
            3 -> startForegroundService()
            4 -> startAlarmManager()
        }
        showToast("Solution appliquée : $currentSolution")
    }

    private fun disableSleep() {
        val intent = Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS)
        startActivity(intent)
        showToast("Réglez 'Mise en veille' sur 15+ minutes ou 'Jamais'")
    }

    private fun enableWakeLock() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED) {
            showToast("Permission WAKE_LOCK requise")
            return
        }
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "magnus:GPSWakeLock")
        wakeLock?.acquire(60 * 60 * 1000L)
        showToast("✅ WakeLock activé (garde GPS + réseau actifs)")
    }

    private fun disableWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            wakeLock = null
            showToast("WakeLock désactivé")
        }
    }

    private fun startForegroundService() {
        val intent = Intent(this, GPSForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        showToast("✅ Service GPS au premier plan activé")
    }

    private fun stopForegroundService() {
        val intent = Intent(this, GPSForegroundService::class.java)
        stopService(intent)
        showToast("Service GPS arrêté")
    }

    private fun startAlarmManager() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, GPSAlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 30000, pending)
        showToast("✅ AlarmManager activé (réveille tous les 30s)")
    }

    private fun stopAlarmManager() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, GPSAlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pending)
        showToast("AlarmManager arrêté")
    }

    private fun registerBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(null, filter)
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}

class GPSForegroundService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 42
        private const val CHANNEL_ID = "magnus_gps_service"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(CHANNEL_ID, "GPS Tracking", android.app.NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification = androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🗺️ Trackeur Rallye")
            .setContentText("📍 GPS en suivi continu")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?) = null
}

class GPSAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextIntent = Intent(context, GPSAlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(context, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 30000, pending)
    }
}

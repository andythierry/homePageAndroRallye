#!/bin/bash
set -e

# ============================================================================
# MAGNUS Launcher - Ajouter SettingsActivity au repo Git
# ============================================================================

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  MAGNUS Launcher - Setup SettingsActivity                     ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}\n"

# ============================================================================
# Vérifier qu'on est dans le bon répertoire
# ============================================================================

if [ ! -f "app/build.gradle" ]; then
    echo -e "${RED}✗ Erreur : app/build.gradle non trouvé${NC}"
    echo -e "${YELLOW}Tu dois être dans le répertoire racine du projet Android${NC}"
    echo -e "  cd ~/magnus-launcher/homePageAndroRallye"
    exit 1
fi

echo -e "${GREEN}✓ Répertoire Android trouvé${NC}\n"

# ============================================================================
# 1. Créer la structure des dossiers
# ============================================================================

echo -e "${YELLOW}📁 Créant la structure des dossiers...${NC}"

KOTLIN_DIR="app/src/main/java/com/magnus/launcher/settings"
LAYOUT_DIR="app/src/main/res/layout"

mkdir -p "$KOTLIN_DIR"
mkdir -p "$LAYOUT_DIR"

echo -e "${GREEN}✓ Structure créée${NC}\n"

# ============================================================================
# 2. Créer SettingsActivity.kt
# ============================================================================

echo -e "${YELLOW}📝 Créant SettingsActivity.kt...${NC}"

cat > "$KOTLIN_DIR/SettingsActivity.kt" << 'KOTLIN_EOF'
package com.magnus.launcher.settings

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
import androidx.appcompat.app.AppCompatActivity
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
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WAKE_LOCK
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showToast("Permission WAKE_LOCK requise")
            return
        }

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "magnus:GPSWakeLock"
        )
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
        val pending = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 30000,
            pending
        )

        showToast("✅ AlarmManager activé (réveille tous les 30s)")
    }

    private fun stopAlarmManager() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, GPSAlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}

class GPSForegroundService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 42
        private const val CHANNEL_ID = "magnus_gps_service"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "GPS Tracking",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🗺️ Trackeur Rallye")
            .setContentText("📍 GPS en suivi continu")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
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
        val pending = PendingIntent.getBroadcast(
            context,
            0,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 30000,
            pending
        )
    }
}
KOTLIN_EOF

echo -e "${GREEN}✓ SettingsActivity.kt créé${NC}\n"

# ============================================================================
# 3. Créer activity_settings.xml
# ============================================================================

echo -e "${YELLOW}📝 Créant activity_settings.xml...${NC}"

cat > "$LAYOUT_DIR/activity_settings.xml" << 'XML_EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#000000"
    android:padding="16dp">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="🗺️ Trackeur Rallye - Paramètres GPS"
        android:textSize="28sp"
        android:textColor="#FFFFFF"
        android:textStyle="bold"
        android:textAlignment="center"
        android:layout_marginBottom="24dp" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:background="#1A1A1A"
        android:padding="12dp"
        android:layout_marginBottom="24dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="📊 État actuel"
            android:textSize="18sp"
            android:textColor="#00FF00"
            android:textStyle="bold"
            android:layout_marginBottom="12dp" />

        <TextView
            android:id="@+id/status_bluetooth"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="🔵 Bluetooth : ON"
            android:textSize="16sp"
            android:textColor="#FFFFFF"
            android:layout_marginBottom="8dp" />

        <TextView
            android:id="@+id/status_battery"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="🔋 Batterie : 85%"
            android:textSize="16sp"
            android:textColor="#FFFFFF"
            android:layout_marginBottom="8dp" />

        <TextView
            android:id="@+id/status_network"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="📡 Réseau : ON"
            android:textSize="16sp"
            android:textColor="#FFFFFF"
            android:layout_marginBottom="8dp" />

        <TextView
            android:id="@+id/status_gps"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="📍 GPS : ON"
            android:textSize="16sp"
            android:textColor="#FFFFFF"
            android:layout_marginBottom="8dp" />

        <TextView
            android:id="@+id/status_solution"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Solution active : Aucune"
            android:textSize="14sp"
            android:textColor="#FFAA00"
            android:layout_marginTop="8dp" />

    </LinearLayout>

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:layout_marginBottom="16dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="⚡ Solutions de veille GPS"
                android:textSize="18sp"
                android:textColor="#00FF00"
                android:textStyle="bold"
                android:layout_marginBottom="16dp" />

            <RadioGroup
                android:id="@+id/radio_group_solutions"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:background="#1A1A1A"
                    android:padding="12dp"
                    android:layout_marginBottom="12dp">

                    <RadioButton
                        android:id="@+id/radio_disable_sleep"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginEnd="12dp" />

                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:orientation="vertical">

                        <TextView
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:text="🚫 Désactiver la veille"
                            android:textSize="16sp"
                            android:textColor="#FFFFFF"
                            android:textStyle="bold" />

                        <TextView
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:text="Plus simple, mais consomme batterie"
                            android:textSize="12sp"
                            android:textColor="#AAAAAA"
                            android:layout_marginTop="4dp" />

                    </LinearLayout>

                </LinearLayout>

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:background="#1A1A1A"
                    android:padding="12dp"
                    android:layout_marginBottom="12dp">

                    <RadioButton
                        android:id="@+id/radio_wakelock"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginEnd="12dp"
                        android:checked="true" />

                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:orientation="vertical">

                        <TextView
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:text="⚡ WakeLock (Partiel)"
                            android:textSize="16sp"
                            android:textColor="#FFFFFF"
                            android:textStyle="bold" />

                        <TextView
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:text="Garde GPS + réseau actifs, bon compromis"
                            android:textSize="12sp"
                            android:textColor="#AAAAAA"
                            android:layout_marginTop="4dp" />

                    </LinearLayout>

                </LinearLayout>

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:background="#1A1A1A"
                    android:padding="12dp"
                    android:layout_marginBottom="12dp">

                    <RadioButton
                        android:id="@+id/radio_foreground_service"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginEnd="12dp" />

                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:orientation="vertical">

                        <TextView
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:text="📲 Service au premier plan"
                            android:textSize="16sp"
                            android:textColor="#FFFFFF"
                            android:textStyle="bold" />

                        <TextView
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:text="Le plus fiable, affiche notification"
                            android:textSize="12sp"
                            android:textColor="#AAAAAA"
                            android:layout_marginTop="4dp" />

                    </LinearLayout>

                </LinearLayout>

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:background="#1A1A1A"
                    android:padding="12dp"
                    android:layout_marginBottom="12dp">

                    <RadioButton
                        android:id="@+id/radio_alarm_manager"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginEnd="12dp" />

                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:orientation="vertical">

                        <TextView
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:text="⏰ AlarmManager"
                            android:textSize="16sp"
                            android:textColor="#FFFFFF"
                            android:textStyle="bold" />

                        <TextView
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:text="Réveille tous les 30s, très fiable"
                            android:textSize="12sp"
                            android:textColor="#AAAAAA"
                            android:layout_marginTop="4dp" />

                    </LinearLayout>

                </LinearLayout>

            </RadioGroup>

        </LinearLayout>

    </ScrollView>

    <Button
        android:id="@+id/btn_apply"
        android:layout_width="match_parent"
        android:layout_height="60dp"
        android:text="✅ APPLIQUER LA SOLUTION"
        android:textSize="18sp"
        android:textColor="#FFFFFF"
        android:textStyle="bold"
        android:background="#00AA00"
        android:layout_marginBottom="12dp" />

    <Button
        android:id="@+id/btn_back"
        android:layout_width="match_parent"
        android:layout_height="48dp"
        android:text="← RETOUR"
        android:textSize="16sp"
        android:textColor="#FFFFFF"
        android:background="#333333" />

</LinearLayout>
XML_EOF

echo -e "${GREEN}✓ activity_settings.xml créé${NC}\n"

# ============================================================================
# 4. Ajouter à Git
# ============================================================================

echo -e "${YELLOW}📦 Ajoutant les fichiers à Git...${NC}"

git add "$KOTLIN_DIR/SettingsActivity.kt"
git add "$LAYOUT_DIR/activity_settings.xml"

echo -e "${GREEN}✓ Fichiers ajoutés${NC}\n"

# ============================================================================
# 5. Commit
# ============================================================================

echo -e "${YELLOW}💾 Committing...${NC}"

git commit -m "Add: SettingsActivity - GPS wake lock management

- 4 sleep solutions for GPS tracking
- Real-time status display (Bluetooth, Battery, Network, GPS)
- RadioButton selection + Apply button
- WakeLock (default), Foreground Service, AlarmManager options
- SharedPreferences for persistent configuration
- Material Design layout (landscape)"

echo -e "${GREEN}✓ Commit effectué${NC}\n"

# ============================================================================
# 6. Push
# ============================================================================

echo -e "${YELLOW}🚀 Pushant vers GitHub...${NC}"

git push -u origin main

echo -e "${GREEN}✓ Push effectué${NC}\n"

# ============================================================================
# Résumé
# ============================================================================

echo -e "${GREEN}════════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✅ SettingsActivity ajouté et pushé avec succès !${NC}"
echo -e "${GREEN}════════════════════════════════════════════════════════════════${NC}\n"

echo -e "${YELLOW}📁 Fichiers créés :${NC}"
echo -e "  ✓ $KOTLIN_DIR/SettingsActivity.kt"
echo -e "  ✓ $LAYOUT_DIR/activity_settings.xml\n"

echo -e "${YELLOW}📋 Prochaines étapes :${NC}"
echo -e "  1. Modifier MainActivity.kt (bouton Settings)"
echo -e "  2. Ajouter permissions à AndroidManifest.xml"
echo -e "  3. Compiler et tester sur S8\n"

echo -e "${YELLOW}🔗 Vérifier sur GitHub :${NC}"
echo -e "  https://github.com/andythierry/homePageAndroRallye\n"

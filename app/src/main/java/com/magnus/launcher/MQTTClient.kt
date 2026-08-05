package com.magnus.launcher

import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import org.eclipse.paho.client.mqttv3.*

class MQTTClient(private val context: Context) {
    private var mqttClient: MqttClient? = null
    private val TAG = "MQTTClient"
    
    private val BROKER_URL = "tcp://161.97.83.80:1883"
    private val USERNAME = "Thierry974andy"
    private val PASSWORD = "mosquitto"
    private val CLIENT_ID = "S8_Tracker_${System.currentTimeMillis()}"
    
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var isPaused = false
    
    private var currentLat: Double = 0.0
    private var currentLon: Double = 0.0
    private var currentSpeed: Double = 0.0
    
    fun connect() {
        Thread {
            try {
                Log.d(TAG, "Connecting...")
                mqttClient = MqttClient(BROKER_URL, CLIENT_ID, null)
                val options = MqttConnectOptions()
                options.isCleanSession = true
                options.userName = USERNAME
                options.password = PASSWORD.toCharArray()
                options.isAutomaticReconnect = true
                mqttClient?.connect(options)
                Log.d(TAG, "Connected!")
                startLocationUpdates()
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}")
            }
        }.start()
    }
    
    private fun startLocationUpdates() {
        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val locationRequest = LocationRequest.Builder(1000L).setPriority(Priority.PRIORITY_HIGH_ACCURACY).build()
            
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    if (isPaused) return
                    for (location in locationResult.locations) {
                        currentLat = location.latitude
                        currentLon = location.longitude
                        currentSpeed = location.speed.toDouble()
                        
                        val lat = String.format("%.6f", currentLat).replace(",", ".")
                        val lon = String.format("%.6f", currentLon).replace(",", ".")
                        val speed = String.format("%.1f", currentSpeed).replace(",", ".")
                        
                        try {
                            mqttClient?.publish("rally/s8/gps/latitude", lat.toByteArray(), 1, true)
                            mqttClient?.publish("rally/s8/gps/longitude", lon.toByteArray(), 1, true)
                            mqttClient?.publish("rally/s8/gps/speed", speed.toByteArray(), 1, true)
                            Log.d(TAG, "GPS: $lat, $lon, $speed m/s")
                        } catch (e: Exception) {
                            Log.e(TAG, "Publish: ${e.message}")
                        }
                    }
                }
            }
            
            fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
            Log.d(TAG, "Location updates started")
        } catch (e: Exception) {
            Log.e(TAG, "Location error: ${e.message}")
        }
    }
    
    fun setPaused(paused: Boolean) {
        isPaused = paused
        try {
            val msg = MqttMessage(paused.toString().toByteArray())
            mqttClient?.publish("rally/s8/control/pause", msg)
            Log.d(TAG, "Pause sent: $paused")
        } catch (e: Exception) {
            Log.e(TAG, "Pause publish error: ${e.message}")
        }
    }
    
    fun getGpsData(): String {
        return "📍 ${String.format("%.6f", currentLat)}\n📍 ${String.format("%.6f", currentLon)}\n⚡ ${String.format("%.1f", currentSpeed)} m/s"
    }
    
    fun disconnect() {
        try {
            if (locationCallback != null) {
                fusedLocationClient?.removeLocationUpdates(locationCallback!!)
            }
            mqttClient?.disconnect()
            Log.d(TAG, "Disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error: ${e.message}")
        }
    }
}

package com.magnus.launcher

import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import org.eclipse.paho.client.mqttv3.*

class MQTTClient(private val context: Context) {
    private var client: MqttClient? = null
    private val TAG = "MQTTClient"
    private var fusedLocationClient: FusedLocationProviderClient? = null
    
    fun connect() {
        Thread {
            try {
                Log.d(TAG, "Connecting...")
                client = MqttClient("tcp://161.97.83.80:1883", "S8_${System.currentTimeMillis()}", null)
                val opts = MqttConnectOptions()
                opts.userName = "Thierry974andy"
                opts.password = "mosquitto".toCharArray()
                opts.isCleanSession = true
                client?.connect(opts)
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
            val locationRequest = LocationRequest.Builder(1000L)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()
            
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    for (location in locationResult.locations) {
                        val lat = String.format("%.6f", location.latitude)
                        val lon = String.format("%.6f", location.longitude)
                        val speed = String.format("%.1f", location.speed)
                        
                        try {
                            client?.publish("rally/s8/gps/latitude", lat.toByteArray(), 1, true)
                            client?.publish("rally/s8/gps/longitude", lon.toByteArray(), 1, true)
                            client?.publish("rally/s8/gps/speed", speed.toByteArray(), 1, true)
                            Log.d(TAG, "GPS: $lat, $lon, $speed m/s")
                        } catch (e: Exception) {
                            Log.e(TAG, "Publish: ${e.message}")
                        }
                    }
                }
            }
            
            fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            Log.d(TAG, "Location updates started")
        } catch (e: Exception) {
            Log.e(TAG, "Location error: ${e.message}")
        }
    }
    
    fun disconnect() {
        try {
            client?.disconnect()
            Log.d(TAG, "Disconnected")
        } catch (e: Exception) {}
    }
}

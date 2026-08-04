package com.magnus.launcher

import android.content.Context
import android.util.Log
import org.eclipse.paho.client.mqttv3.*

class MQTTClient(private val context: Context) {
    private var mqttClient: MqttClient? = null
    private val TAG = "MQTTClient"
    
    // Config Contabo
    private val BROKER_URL = "tcp://161.97.83.80:1883"
    private val USERNAME = "Thierry974andy*"
    private val PASSWORD = "mosquitto"
    private val CLIENT_ID = "S8_Tracker_${System.currentTimeMillis()}"
    
    // Topics
    private val TOPIC_GPS_LAT = "rally/s8/gps/latitude"
    private val TOPIC_GPS_LON = "rally/s8/gps/longitude"
    private val TOPIC_GPS_SPEED = "rally/s8/gps/speed"
    private val TOPIC_GPS_ACCURACY = "rally/s8/gps/accuracy"
    
    fun connect() {
        try {
            mqttClient = MqttClient(BROKER_URL, CLIENT_ID, null)
            val options = MqttConnectOptions()
            options.isCleanSession = true
            options.userName = USERNAME
            options.password = PASSWORD.toCharArray()
            options.isAutomaticReconnect = true
            options.connectionTimeout = 10
            options.keepAliveInterval = 60
            
            mqttClient?.connect(options)
            Log.d(TAG, "MQTT connected to $BROKER_URL")
        } catch (e: MqttException) {
            Log.e(TAG, "MQTT connection failed: ${e.message}")
        }
    }
    
    fun publishGPS(latitude: Double, longitude: Double, speed: Float, accuracy: Float) {
        try {
            if (mqttClient?.isConnected == true) {
                val latPayload = String.format("%.6f", latitude).toByteArray()
                val lonPayload = String.format("%.6f", longitude).toByteArray()
                val speedPayload = String.format("%.1f", speed).toByteArray()
                val accPayload = String.format("%.1f", accuracy).toByteArray()
                
                mqttClient?.publish(TOPIC_GPS_LAT, latPayload, 1, true)
                mqttClient?.publish(TOPIC_GPS_LON, lonPayload, 1, true)
                mqttClient?.publish(TOPIC_GPS_SPEED, speedPayload, 1, true)
                mqttClient?.publish(TOPIC_GPS_ACCURACY, accPayload, 1, true)
                
                Log.d(TAG, "GPS published: Lat=$latitude, Lon=$longitude, Speed=$speed")
            } else {
                Log.w(TAG, "MQTT not connected, reconnecting...")
                connect()
            }
        } catch (e: MqttException) {
            Log.e(TAG, "Publish failed: ${e.message}")
        }
    }
    
    fun disconnect() {
        try {
            mqttClient?.disconnect()
            mqttClient?.close()
            Log.d(TAG, "MQTT disconnected")
        } catch (e: MqttException) {
            Log.e(TAG, "Disconnect failed: ${e.message}")
        }
    }
    
    fun isConnected(): Boolean = mqttClient?.isConnected == true
}

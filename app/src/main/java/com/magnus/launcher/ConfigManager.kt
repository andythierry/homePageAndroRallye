package com.magnus.launcher

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class ConfigManager(private val context: Context) {
    private val TAG = "ConfigManager"
    private val config = mutableMapOf<String, String>()

    init {
        loadConfig()
    }

    private fun loadConfig() {
        try {
            val inputStream = context.assets.open("config.properties")
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line = line!!.trim()
                    if (line!!.isEmpty() || line!!.startsWith("#")) continue
                    
                    val parts = line!!.split("=", limit = 2)
                    if (parts.size == 2) {
                        config[parts[0].trim()] = parts[1].trim()
                    }
                }
            }
            Log.d(TAG, "Config chargée: ${config.keys.size} clés")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur config: ${e.message}")
        }
    }

    fun get(key: String, default: String = ""): String {
        return config[key] ?: default
    }

    fun getInt(key: String, default: Int = 0): Int {
        return config[key]?.toIntOrNull() ?: default
    }
}

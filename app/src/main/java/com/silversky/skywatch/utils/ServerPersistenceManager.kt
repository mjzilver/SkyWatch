package com.silversky.skywatch.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.silversky.skywatch.model.SavedServer
import java.io.File

class ServerPersistenceManager(private val context: Context) {
    private val gson = Gson()
    private val fileName = "saved_servers.json"
    
    fun saveServers(servers: List<SavedServer>) {
        try {
            val json = gson.toJson(servers)
            val file = File(context.filesDir, fileName)
            file.writeText(json)
            Log.d("ServerPersistence", "Saved ${servers.size} servers")
        } catch (e: Exception) {
            Log.e("ServerPersistence", "Failed to save servers", e)
        }
    }
    
    fun loadServers(): List<SavedServer> {
        try {
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                val json = file.readText()
                val type = object : TypeToken<List<SavedServer>>() {}.type
                val servers = gson.fromJson<List<SavedServer>>(json, type)
                Log.d("ServerPersistence", "Loaded ${servers?.size ?: 0} servers")
                return servers ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e("ServerPersistence", "Failed to load servers", e)
        }
        return emptyList()
    }
}
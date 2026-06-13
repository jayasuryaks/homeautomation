package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.Locale

class SafeHavenRepository(private val db: SafeHavenDatabase) {
    val allDevices: Flow<List<Device>> = db.deviceDao.getAllDevices()
    val allAlerts: Flow<List<SecurityAlert>> = db.alertDao.getAllAlerts()

    suspend fun insertDevice(device: Device) {
        db.deviceDao.insertDevice(device)
    }

    suspend fun deleteDevice(device: Device) {
        db.deviceDao.deleteDevice(device)
    }

    suspend fun updateDevice(device: Device) {
        db.deviceDao.updateDevice(device)
    }

    suspend fun togglePower(id: Int, isOn: Boolean, statusText: String) {
        db.deviceDao.togglePower(id, isOn, statusText)
    }

    suspend fun updateValue(id: Int, value: Float, statusText: String) {
        db.deviceDao.updateValue(id, value, statusText)
    }

    suspend fun insertAlert(alert: SecurityAlert) {
        db.alertDao.insertAlert(alert)
    }

    suspend fun markAlertRead(id: Int) {
        db.alertDao.markAsRead(id)
    }

    // ==========================================
    // Backend REST API Synchronization Methods
    // ==========================================

    suspend fun syncDevices(homeSlug: String): Boolean {
        return try {
            val response = RetrofitClient.apiService.getDevices(homeSlug)
            if (response.isSuccessful && response.body() != null) {
                val devices = response.body()!!
                db.deviceDao.deleteAll()
                for (device in devices) {
                    db.deviceDao.insertDevice(device)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun syncAlerts(homeSlug: String): Boolean {
        return try {
            val response = RetrofitClient.apiService.getAlerts(homeSlug)
            if (response.isSuccessful && response.body() != null) {
                val alerts = response.body()!!
                db.alertDao.deleteAll()
                for (alert in alerts) {
                    db.alertDao.insertAlert(alert)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun sendDeviceCommand(homeSlug: String, deviceId: Int, action: String, value: Float? = null): Boolean {
        return try {
            val payload = mutableMapOf<String, Any?>("action" to action)
            if (value != null) {
                payload["value"] = value
            }
            val response = RetrofitClient.apiService.sendCommand(homeSlug, deviceId, payload)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun createDeviceOnBackend(homeSlug: String, name: String, category: String, connectionType: String? = null): Boolean {
        return try {
            val slug = name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-')
            val icon = when (category.lowercase(Locale.ROOT)) {
                "cameras" -> "videocam"
                "sensors" -> "sensors"
                "smart locks", "locks" -> "lock"
                "lighting", "lights" -> "lightbulb"
                else -> "router"
            }
            val status = when (category.lowercase(Locale.ROOT)) {
                "cameras" -> if (connectionType != null) "Live • $connectionType HD" else "Live • HD"
                "sensors" -> "Monitoring"
                "smart locks", "locks" -> "Locked"
                "lighting", "lights" -> "Off"
                else -> "Connected"
            }
            val devCategory = when (category.lowercase(Locale.ROOT)) {
                "cameras", "sensors", "smart locks", "locks", "hubs & bridges" -> "Security"
                "lighting", "lights" -> "Lighting"
                else -> "Climate"
            }
            val deviceType = when (category.lowercase(Locale.ROOT)) {
                "cameras" -> "camera"
                "sensors" -> "motion_sensor"
                "smart locks", "locks" -> "lock"
                "lighting", "lights" -> "light"
                else -> "hub"
            }

            val payload = mapOf(
                "name" to name,
                "slug" to slug,
                "device_type" to deviceType,
                "category" to devCategory,
                "status_text" to status,
                "float_value" to 0.0,
                "icon_name" to icon,
                "is_on" to (category.lowercase(Locale.ROOT).contains("cam") || category.lowercase(Locale.ROOT).contains("lock")),
                "is_online" to true,
                "mqtt_topic" to "devices/$slug"
            )

            val response = RetrofitClient.apiService.createDevice(homeSlug, payload)
            if (response.isSuccessful && response.body() != null) {
                db.deviceDao.insertDevice(response.body()!!)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteDeviceOnBackend(homeSlug: String, deviceId: Int): Boolean {
        return try {
            val response = RetrofitClient.apiService.deleteDevice(homeSlug, deviceId)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun renameDeviceOnBackend(homeSlug: String, deviceId: Int, newName: String): Boolean {
        return try {
            val response = RetrofitClient.apiService.updateDevice(homeSlug, deviceId, mapOf("name" to newName))
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun markAlertReadOnBackend(homeSlug: String, alertId: Int): Boolean {
        return try {
            val response = RetrofitClient.apiService.markAlertRead(homeSlug, alertId)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

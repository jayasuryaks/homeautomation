package com.example.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity(tableName = "devices")
data class Device(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // "Security", "Climate", "Lighting"
    val statusText: String, // "Locked", "Unlocked", "Cooling to 72°F", "On", "Off"
    val floatValue: Float = 0f, // Brightness (e.g. 80.0f) or temperature (74.0f)
    val iconName: String, // "lock", "videocam", "thermostat", "lightbulb"
    val isOn: Boolean = false,
    val isOnline: Boolean = true
)

@Entity(tableName = "alerts")
data class SecurityAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val timestamp: String, // e.g. "14:22"
    val dateGroup: String, // "Today", "Yesterday"
    val level: String, // "Critical", "Warning", "Info"
    val tag: String, // "Entry Way", "Garage", "Front Door", or empty
    val isRead: Boolean = false
)

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY category ASC, id ASC")
    fun getAllDevices(): Flow<List<Device>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: Device)

    @Update
    suspend fun updateDevice(device: Device)

    @Delete
    suspend fun deleteDevice(device: Device)

    @Query("UPDATE devices SET isOn = :isOn, statusText = :statusText WHERE id = :id")
    suspend fun togglePower(id: Int, isOn: Boolean, statusText: String)

    @Query("UPDATE devices SET floatValue = :newValue, statusText = :statusText WHERE id = :id")
    suspend fun updateValue(id: Int, newValue: Float, statusText: String)

    @Query("DELETE FROM devices")
    suspend fun deleteAll()
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY id DESC")
    fun getAllAlerts(): Flow<List<SecurityAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: SecurityAlert)

    @Query("UPDATE alerts SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)

    @Query("DELETE FROM alerts")
    suspend fun deleteAll()
}

@Database(entities = [Device::class, SecurityAlert::class], version = 2, exportSchema = false)
abstract class SafeHavenDatabase : RoomDatabase() {
    abstract val deviceDao: DeviceDao
    abstract val alertDao: AlertDao

    companion object {
        @Volatile
        private var INSTANCE: SafeHavenDatabase? = null

        fun getDatabase(context: Context): SafeHavenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SafeHavenDatabase::class.java,
                    "safehaven_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate database with initial values matching prototype UI
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getDatabase(context)
                            prepopulateDevices(database.deviceDao)
                            prepopulateAlerts(database.alertDao)
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun prepopulateDevices(dao: DeviceDao) {
            dao.insertDevice(Device(name = "Front Door Lock", category = "Security", statusText = "Locked", iconName = "lock", isOn = true, isOnline = true))
            dao.insertDevice(Device(name = "Driveway Camera", category = "Security", statusText = "Live • HD", iconName = "videocam", isOn = true, isOnline = true))
            dao.insertDevice(Device(name = "Main Thermostat", category = "Climate", statusText = "Cooling to 72°F", floatValue = 74f, iconName = "thermostat", isOn = true, isOnline = true))
            dao.insertDevice(Device(name = "Living Room Lights", category = "Lighting", statusText = "On • 80% Brightness", floatValue = 80f, iconName = "lightbulb", isOn = true, isOnline = true))
            dao.insertDevice(Device(name = "Kitchen Pendant", category = "Lighting", statusText = "Offline", floatValue = 0f, iconName = "lightbulb", isOn = false, isOnline = false))
        }

        private suspend fun prepopulateAlerts(dao: AlertDao) {
            dao.insertAlert(SecurityAlert(
                title = "Intrusion Detected",
                message = "Front door sensor triggered while system was armed in 'Away' mode.",
                timestamp = "14:22",
                dateGroup = "Today",
                level = "Critical",
                tag = "Entry Way"
            ))
            dao.insertAlert(SecurityAlert(
                title = "System Update",
                message = "Firmware version 4.2.1 successfully installed on all hubs.",
                timestamp = "23:58",
                dateGroup = "Yesterday",
                level = "Info",
                tag = ""
            ))
            dao.insertAlert(SecurityAlert(
                title = "Unusual Activity",
                message = "Repeated motion detected near the garage door after sunset.",
                timestamp = "19:40",
                dateGroup = "Yesterday",
                level = "Warning",
                tag = "Garage"
            ))
            dao.insertAlert(SecurityAlert(
                title = "Person Spotted",
                message = "Delivery person detected at Front Porch camera.",
                timestamp = "16:15",
                dateGroup = "Yesterday",
                level = "Info",
                tag = "Front Door"
            ))
        }
    }
}

package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SafeHavenViewModel(application: Application) : AndroidViewModel(application) {
    private val database = SafeHavenDatabase.getDatabase(application)
    private val repository = SafeHavenRepository(database)

    // Sync state configuration
    var authToken: String? = null
    var homeSlug: String = "default"
    private var pollJob: kotlinx.coroutines.Job? = null

    // Reactive StateFlows from DB
    val devices: StateFlow<List<Device>> = repository.allDevices
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val alerts: StateFlow<List<SecurityAlert>> = repository.allAlerts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // User Profile state
    private val _userName = MutableStateFlow("Emma Johnson")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("emma.j@safehaven.tech")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userAddress = MutableStateFlow("1248 Sentinel Heights\nAustin, TX 78701")
    val userAddress: StateFlow<String> = _userAddress.asStateFlow()

    private val _userRole = MutableStateFlow("Home Owner")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val _userPhone = MutableStateFlow("+1 (555) 123-4567")
    val userPhone: StateFlow<String> = _userPhone.asStateFlow()

    // Notification Toggles
    private val _biometricsEnabled = MutableStateFlow(true)
    val biometricsEnabled: StateFlow<Boolean> = _biometricsEnabled.asStateFlow()

    private val _pushNotifications = MutableStateFlow(true)
    val pushNotifications: StateFlow<Boolean> = _pushNotifications.asStateFlow()

    private val _criticalAlerts = MutableStateFlow(true)
    val criticalAlerts: StateFlow<Boolean> = _criticalAlerts.asStateFlow()

    private val _motionDetection = MutableStateFlow(true)
    val motionDetection: StateFlow<Boolean> = _motionDetection.asStateFlow()

    private val _motionPushNotifications = MutableStateFlow(true)
    val motionPushNotifications: StateFlow<Boolean> = _motionPushNotifications.asStateFlow()

    private val _systemArmed = MutableStateFlow(true)
    val systemArmed: StateFlow<Boolean> = _systemArmed.asStateFlow()

    private val _armedMode = MutableStateFlow("Away") // "Home", "Away", "Night"
    val armedMode: StateFlow<String> = _armedMode.asStateFlow()

    // Temporary login state & authentication
    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _forgotPasswordSuccess = MutableStateFlow(false)
    val forgotPasswordSuccess: StateFlow<Boolean> = _forgotPasswordSuccess.asStateFlow()

    private val _forgotPasswordError = MutableStateFlow<String?>(null)
    val forgotPasswordError: StateFlow<String?> = _forgotPasswordError.asStateFlow()

    private val _isForgotPasswordLoading = MutableStateFlow(false)
    val isForgotPasswordLoading: StateFlow<Boolean> = _isForgotPasswordLoading.asStateFlow()

    // Temporary active camera selection
    private val _activeCameraIndex = MutableStateFlow(0)
    val activeCameraIndex: StateFlow<Int> = _activeCameraIndex.asStateFlow()

    private val _talkActive = MutableStateFlow(false)
    val talkActive: StateFlow<Boolean> = _talkActive.asStateFlow()

    private val _listenActive = MutableStateFlow(false)
    val listenActive: StateFlow<Boolean> = _listenActive.asStateFlow()

    init {
        // Automatically start polling when user logs in, stop when logged out
        viewModelScope.launch {
            isUserLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    startPolling()
                } else {
                    stopPolling()
                }
            }
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                syncWithBackend()
                kotlinx.coroutines.delay(3000)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private suspend fun syncWithBackend() {
        repository.syncDevices(homeSlug)
        repository.syncAlerts(homeSlug)
        try {
            val response = RetrofitClient.apiService.getSystemState(homeSlug)
            if (response.isSuccessful && response.body() != null) {
                val state = response.body()!!
                _systemArmed.value = state.isArmed
                _armedMode.value = state.armedMode
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectCamera(index: Int) {
        _activeCameraIndex.value = index
        _talkActive.value = false
        _listenActive.value = false
    }

    fun toggleTalk() {
        _talkActive.value = !_talkActive.value
    }

    fun toggleListen() {
        _listenActive.value = !_listenActive.value
    }

    suspend fun login(emailOrUsername: String, pass: String): Boolean {
        return try {
            val response = RetrofitClient.apiService.login(mapOf("username" to emailOrUsername, "password" to pass))
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                authToken = data["token"] as? String
                val fetchedUsername = data["username"] as? String ?: emailOrUsername

                if (authToken != null) {
                    val profileResponse = RetrofitClient.apiService.getProfile("Token $authToken")
                    if (profileResponse.isSuccessful && profileResponse.body() != null) {
                        val profileData = profileResponse.body()!!
                        _userEmail.value = profileData["email"] as? String ?: ""
                        _userName.value = profileData["username"] as? String ?: fetchedUsername
                        val defaultHome = profileData["default_home"] as? Map<*, *>
                        if (defaultHome != null) {
                            homeSlug = defaultHome["slug"] as? String ?: "default"
                            _userAddress.value = defaultHome["address"] as? String ?: ""
                        }
                    }
                }
                
                syncWithBackend()
                _isUserLoggedIn.value = true
                _loginError.value = null
                true
            } else {
                _loginError.value = "Invalid username or password."
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _loginError.value = "Server connection error."
            false
        }
    }

    suspend fun register(name: String, email: String, phone: String, pass: String): Boolean {
        return try {
            val response = RetrofitClient.apiService.register(
                mapOf("username" to name, "email" to email, "password" to pass)
            )
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                authToken = data["token"] as? String
                _userName.value = name
                _userEmail.value = email
                _userPhone.value = phone

                if (authToken != null) {
                    val profileResponse = RetrofitClient.apiService.getProfile("Token $authToken")
                    if (profileResponse.isSuccessful && profileResponse.body() != null) {
                        val profileData = profileResponse.body()!!
                        val defaultHome = profileData["default_home"] as? Map<*, *>
                        if (defaultHome != null) {
                            homeSlug = defaultHome["slug"] as? String ?: "default"
                            _userAddress.value = defaultHome["address"] as? String ?: ""
                        }
                    }
                }

                syncWithBackend()
                _isUserLoggedIn.value = true
                _loginError.value = null
                true
            } else {
                _loginError.value = "Registration failed. Username may already exist."
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _loginError.value = "Server connection error."
            false
        }
    }

    fun signOut() {
        authToken = null
        _isUserLoggedIn.value = false
    }

    fun clearLoginError() {
        _loginError.value = null
    }

    suspend fun sendPasswordResetLink(email: String): Boolean {
        _isForgotPasswordLoading.value = true
        _forgotPasswordError.value = null
        _forgotPasswordSuccess.value = false
        return try {
            val response = RetrofitClient.apiService.forgotPassword(mapOf("email" to email))
            _isForgotPasswordLoading.value = false
            if (response.isSuccessful && response.body() != null) {
                _forgotPasswordSuccess.value = true
                true
            } else {
                _forgotPasswordError.value = "User with this email or username does not exist."
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isForgotPasswordLoading.value = false
            _forgotPasswordError.value = "Server connection error."
            false
        }
    }

    fun clearForgotPasswordState() {
        _forgotPasswordSuccess.value = false
        _forgotPasswordError.value = null
        _isForgotPasswordLoading.value = false
    }

    // Setters
    fun setBiometrics(enabled: Boolean) {
        _biometricsEnabled.value = enabled
    }

    fun setPushNotifications(enabled: Boolean) {
        _pushNotifications.value = enabled
    }

    fun setCriticalAlerts(enabled: Boolean) {
        _criticalAlerts.value = enabled
    }

    fun setMotionDetection(enabled: Boolean) {
        _motionDetection.value = enabled
    }

    fun setMotionPushNotifications(enabled: Boolean) {
        _motionPushNotifications.value = enabled
    }

    fun setSystemArmed(armed: Boolean) {
        val mode = if (armed) "Away" else "Disarmed"
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.updateSystemState(
                    homeSlug,
                    mapOf("isArmed" to armed, "armedMode" to mode)
                )
                if (response.isSuccessful && response.body() != null) {
                    val state = response.body()!!
                    _systemArmed.value = state.isArmed
                    _armedMode.value = state.armedMode
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setArmedMode(mode: String) {
        val armed = (mode != "Disarmed")
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.updateSystemState(
                    homeSlug,
                    mapOf("isArmed" to armed, "armedMode" to mode)
                )
                if (response.isSuccessful && response.body() != null) {
                    val state = response.body()!!
                    _systemArmed.value = state.isArmed
                    _armedMode.value = state.armedMode
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateUserProfile(name: String, email: String, address: String, phone: String) {
        _userName.value = name
        _userEmail.value = email
        _userAddress.value = address
        _userPhone.value = phone
    }

    // Database updates for Devices
    fun toggleDevicePower(id: Int, currentIsOn: Boolean, currentName: String) {
        viewModelScope.launch {
            val targetIsOn = !currentIsOn
            val success = repository.sendDeviceCommand(homeSlug, id, "toggle")
            if (success) {
                val newText = if (targetIsOn) "On • 80% Brightness" else "Off"
                repository.togglePower(id, targetIsOn, newText)
            }
        }
    }

    fun toggleLock(id: Int, name: String, currentIsLocked: Boolean) {
        viewModelScope.launch {
            val targetIsLocked = !currentIsLocked
            val action = if (targetIsLocked) "lock" else "unlock"
            val success = repository.sendDeviceCommand(homeSlug, id, action)
            if (success) {
                val newText = if (targetIsLocked) "Locked" else "Unlocked"
                repository.togglePower(id, targetIsLocked, newText)

                // Add security alert via backend POST endpoint
                try {
                    RetrofitClient.apiService.createAlert(
                        homeSlug,
                        mapOf(
                            "title" to if (targetIsLocked) "Home Secured" else "Lock Released",
                            "message" to "$name was ${if (targetIsLocked) "manually locked" else "unlocked"} via command console.",
                            "level" to (if (targetIsLocked) "Info" else "Warning"),
                            "tag" to "Security"
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun updateThermostatTemp(id: Int, targetTemp: Float) {
        viewModelScope.launch {
            val success = repository.sendDeviceCommand(homeSlug, id, "set_temperature", targetTemp)
            if (success) {
                val formatted = "Cooling to ${targetTemp.toInt()}°F"
                repository.updateValue(id, targetTemp, formatted)
            }
        }
    }

    fun saveDevice(name: String, category: String, connectionType: String? = null) {
        viewModelScope.launch {
            repository.createDeviceOnBackend(homeSlug, name, category, connectionType)
        }
    }

    fun deleteDevice(device: Device) {
        viewModelScope.launch {
            val success = repository.deleteDeviceOnBackend(homeSlug, device.id)
            if (success) {
                repository.deleteDevice(device)
            }
        }
    }

    fun renameDevice(device: Device, newName: String) {
        viewModelScope.launch {
            val success = repository.renameDeviceOnBackend(homeSlug, device.id, newName)
            if (success) {
                val updated = device.copy(name = newName)
                repository.updateDevice(updated)
            }
        }
    }

    fun markAlertRead(id: Int) {
        viewModelScope.launch {
            val success = repository.markAlertReadOnBackend(homeSlug, id)
            if (success) {
                repository.markAlertRead(id)
            }
        }
    }

    fun triggerPanicAlert() {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.createAlert(
                    homeSlug,
                    mapOf(
                        "title" to "Emergency Panic Tripped",
                        "message" to "Manual panic signal dispatched to Austin Private Security from Emma's console.",
                        "level" to "Critical",
                        "tag" to "Console"
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addSecurityAlert(title: String, message: String, level: String, tag: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.createAlert(
                    homeSlug,
                    mapOf(
                        "title" to title,
                        "message" to message,
                        "level" to level,
                        "tag" to tag
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}


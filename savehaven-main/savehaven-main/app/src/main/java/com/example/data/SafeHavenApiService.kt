package com.example.data

import retrofit2.Response
import retrofit2.http.*

interface SafeHavenApiService {
    @POST("api/auth/login/")
    suspend fun login(@Body body: Map<String, String>): Response<Map<String, Any>>

    @POST("api/auth/register/")
    suspend fun register(@Body body: Map<String, String>): Response<Map<String, Any>>

    @POST("api/auth/forgot-password/")
    suspend fun forgotPassword(@Body body: Map<String, String>): Response<Map<String, Any>>

    @GET("api/auth/profile/")
    suspend fun getProfile(@Header("Authorization") token: String): Response<Map<String, Any>>

    @GET("api/homes/{home_slug}/devices/")
    suspend fun getDevices(@Path("home_slug") homeSlug: String): Response<List<Device>>

    @POST("api/homes/{home_slug}/devices/")
    suspend fun createDevice(@Path("home_slug") homeSlug: String, @Body body: Map<String, @JvmSuppressWildcards Any?>): Response<Device>

    @PATCH("api/homes/{home_slug}/devices/{device_id}/")
    suspend fun updateDevice(@Path("home_slug") homeSlug: String, @Path("device_id") deviceId: Int, @Body body: Map<String, @JvmSuppressWildcards Any?>): Response<Device>

    @DELETE("api/homes/{home_slug}/devices/{device_id}/")
    suspend fun deleteDevice(@Path("home_slug") homeSlug: String, @Path("device_id") deviceId: Int): Response<Unit>

    @POST("api/homes/{home_slug}/devices/{device_id}/command/")
    suspend fun sendCommand(@Path("home_slug") homeSlug: String, @Path("device_id") deviceId: Int, @Body body: Map<String, @JvmSuppressWildcards Any?>): Response<Map<String, Any>>

    @GET("api/homes/{home_slug}/alerts/")
    suspend fun getAlerts(@Path("home_slug") homeSlug: String): Response<List<SecurityAlert>>

    @POST("api/homes/{home_slug}/alerts/")
    suspend fun createAlert(@Path("home_slug") homeSlug: String, @Body body: Map<String, @JvmSuppressWildcards Any?>): Response<Map<String, Any>>

    @PATCH("api/homes/{home_slug}/alerts/{alert_id}/read/")
    suspend fun markAlertRead(@Path("home_slug") homeSlug: String, @Path("alert_id") alertId: Int, @Body body: Map<String, @JvmSuppressWildcards Any> = emptyMap()): Response<SecurityAlert>

    @GET("api/homes/{home_slug}/system/")
    suspend fun getSystemState(@Path("home_slug") homeSlug: String): Response<SystemStateResponse>

    @PATCH("api/homes/{home_slug}/system/")
    suspend fun updateSystemState(@Path("home_slug") homeSlug: String, @Body body: Map<String, @JvmSuppressWildcards Any?>): Response<SystemStateResponse>
}

data class SystemStateResponse(
    val isArmed: Boolean,
    val armedMode: String
)

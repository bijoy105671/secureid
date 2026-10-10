package com.example.data.model

data class LastKnownLocation(
    val latitude: Double = 23.8103, // Default Dhaka coordinates / GPS
    val longitude: Double = 90.4125,
    val locationName: String = "Gulshan 2, Dhaka, Bangladesh",
    val timestamp: Long = System.currentTimeMillis() - (5 * 60 * 1000), // 5 mins ago
    val batteryPercentage: Int = 84,
    val isCharging: Boolean = false,
    val networkName: String = "Grameenphone 4G LTE",
    val isDataConnected: Boolean = true,
    val isGpsActive: Boolean = true
)

data class SimStatus(
    val isInserted: Boolean = true,
    val operatorName: String = "Grameenphone (SIM 1)",
    val countryIso: String = "BD",
    val isAirplaneModeOn: Boolean = false,
    val isTrayEjected: Boolean = false,
    val lastChangedTimestamp: Long = System.currentTimeMillis()
)

data class PowerOffChallenge(
    val challengeId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val otp: String,
    val requestingDevice: String = "Primary Phone",
    val targetSecondaryPhone: String = "Samsung Galaxy S24 Ultra",
    val isApproved: Boolean = false
)

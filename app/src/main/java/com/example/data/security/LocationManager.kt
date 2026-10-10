package com.example.data.security

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.data.model.LastKnownLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class LocationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _movementStatusBn = MutableStateFlow("ডিভাইস স্থির রয়েছে")
    val movementStatusBn: StateFlow<String> = _movementStatusBn.asStateFlow()

    private val _movementStatusEn = MutableStateFlow("Device is Stationary")
    val movementStatusEn: StateFlow<String> = _movementStatusEn.asStateFlow()

    private val _currentSpeedKmh = MutableStateFlow(0f)
    val currentSpeedKmh: StateFlow<Float> = _currentSpeedKmh.asStateFlow()

    private val _totalDistanceTraveledMeters = MutableStateFlow(0f)
    val totalDistanceTraveledMeters: StateFlow<Float> = _totalDistanceTraveledMeters.asStateFlow()

    private val _movementTrail = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val movementTrail: StateFlow<List<Pair<Double, Double>>> = _movementTrail.asStateFlow()

    private var previousLocation: Location? = null
    private var locationCallback: LocationCallback? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun hasLocationPermission(): Boolean {
        val finePerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarsePerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return finePerm || coarsePerm
    }

    @SuppressLint("MissingPermission")
    fun fetchLastKnownLocation(
        onSuccess: (LastKnownLocation) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (!hasLocationPermission()) {
            onError("Location permission not granted")
            return
        }

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        scope.launch {
                            val addressName = resolveAddress(location.latitude, location.longitude)
                            val result = LastKnownLocation(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                locationName = addressName,
                                timestamp = System.currentTimeMillis(),
                                isDataConnected = true,
                                isGpsActive = true
                            )
                            onSuccess(result)
                        }
                    } else {
                        // Fallback to high accuracy request if last location is null
                        requestSingleFreshLocation(onSuccess, onError)
                    }
                }
                .addOnFailureListener { e ->
                    onError(e.message ?: "Failed to get last known location")
                }
        } catch (e: SecurityException) {
            onError(e.message ?: "Security exception")
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestSingleFreshLocation(
        onSuccess: (LastKnownLocation) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasLocationPermission()) {
            onError("Permission not granted")
            return
        }

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        scope.launch {
                            val addressName = resolveAddress(location.latitude, location.longitude)
                            val result = LastKnownLocation(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                locationName = addressName,
                                timestamp = System.currentTimeMillis(),
                                isDataConnected = true,
                                isGpsActive = true
                            )
                            onSuccess(result)
                        }
                    } else {
                        onError("Location is unavailable on device")
                    }
                }
                .addOnFailureListener { e ->
                    onError(e.message ?: "Failed fresh location request")
                }
        } catch (e: SecurityException) {
            onError(e.message ?: "Security exception")
        }
    }

    @SuppressLint("MissingPermission")
    fun startRealtimeMovementTracking(
        onUpdate: (LastKnownLocation, Float, String, String) -> Unit
    ) {
        if (!hasLocationPermission()) return
        if (_isTracking.value) return

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000L)
            .setMinUpdateIntervalMillis(2000L)
            .setMinUpdateDistanceMeters(1f) // Sensitive movement detection for anti-theft
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val latest = result.lastLocation ?: return

                // Calculate speed in km/h
                val speedKmh = if (latest.hasSpeed()) (latest.speed * 3.6f) else calculateSpeedFromDelta(latest)
                _currentSpeedKmh.value = speedKmh

                // Distance calculation
                previousLocation?.let { prev ->
                    val distance = prev.distanceTo(latest)
                    if (distance > 0.5f) {
                        _totalDistanceTraveledMeters.value += distance
                    }
                }
                previousLocation = latest

                // Movement state analysis
                val (statusBn, statusEn) = when {
                    speedKmh > 25f -> "যানবাহনে দ্রুত স্থানান্তরিত হচ্ছে! 🚨" to "In Transit / High-Speed Movement! 🚨"
                    speedKmh > 3f -> "ডিভাইস নিয়ে হাঁটা হচ্ছে (Movement Detected)" to "Walking / Moving"
                    _totalDistanceTraveledMeters.value > 5f -> "স্থান পরিবর্তন হয়েছে (Displaced)" to "Location Displaced"
                    else -> "ডিভাইস স্থির অবস্থায় রয়েছে (Stationary)" to "Stationary / Stationary Guard"
                }

                _movementStatusBn.value = statusBn
                _movementStatusEn.value = statusEn

                // Append breadcrumb
                val currentList = _movementTrail.value.toMutableList()
                currentList.add(Pair(latest.latitude, latest.longitude))
                if (currentList.size > 20) currentList.removeAt(0)
                _movementTrail.value = currentList

                scope.launch {
                    val addressName = resolveAddress(latest.latitude, latest.longitude)
                    val locObj = LastKnownLocation(
                        latitude = latest.latitude,
                        longitude = latest.longitude,
                        locationName = addressName,
                        timestamp = System.currentTimeMillis(),
                        isDataConnected = true,
                        isGpsActive = true
                    )
                    onUpdate(locObj, speedKmh, statusBn, statusEn)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            _isTracking.value = true
        } catch (e: SecurityException) {
            _isTracking.value = false
        }
    }

    fun stopRealtimeMovementTracking() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
        _isTracking.value = false
        _currentSpeedKmh.value = 0f
        previousLocation = null
        _movementStatusBn.value = "ট্র্যাকিং স্থগিত"
        _movementStatusEn.value = "Tracking Paused"
    }

    private fun calculateSpeedFromDelta(current: Location): Float {
        val prev = previousLocation ?: return 0f
        val timeDeltaSeconds = ((current.time - prev.time) / 1000f).coerceAtLeast(1f)
        val distanceMeters = prev.distanceTo(current)
        val speedMps = distanceMeters / timeDeltaSeconds
        return speedMps * 3.6f
    }

    private fun resolveAddress(lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Synchronous fallback for simple address lookup
                val list = geocoder.getFromLocation(lat, lng, 1)
                if (!list.isNullOrEmpty()) {
                    val addr = list[0]
                    val feature = addr.featureName ?: ""
                    val locality = addr.locality ?: addr.subAdminArea ?: ""
                    val country = addr.countryName ?: ""
                    listOf(feature, locality, country).filter { it.isNotBlank() }.joinToString(", ")
                } else {
                    "${String.format(Locale.US, "%.4f", lat)}° N, ${String.format(Locale.US, "%.4f", lng)}° E"
                }
            } else {
                @Suppress("DEPRECATION")
                val list = geocoder.getFromLocation(lat, lng, 1)
                if (!list.isNullOrEmpty()) {
                    val addr = list[0]
                    val feature = addr.featureName ?: ""
                    val locality = addr.locality ?: addr.subAdminArea ?: ""
                    val country = addr.countryName ?: ""
                    listOf(feature, locality, country).filter { it.isNotBlank() }.joinToString(", ")
                } else {
                    "${String.format(Locale.US, "%.4f", lat)}° N, ${String.format(Locale.US, "%.4f", lng)}° E"
                }
            }
        } catch (_: Exception) {
            "${String.format(Locale.US, "%.4f", lat)}° N, ${String.format(Locale.US, "%.4f", lng)}° E"
        }
    }
}

package com.example.data.security

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.model.LastKnownLocation
import com.example.data.model.LocationFixType
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

/**
 * Hardened LocationManager for SHIELD X Anti-Theft Telemetry.
 *
 * Implements:
 * - Real observation timestamps from Android Location object (location.time).
 * - Honest fix classification: LIVE_GPS vs CACHED_FUSED vs STALE_CACHED vs UNAVAILABLE.
 * - Real hardware and network status query (ConnectivityManager, BatteryManager).
 * - Never fabricates fake online connection or live satellite status.
 */
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

    private fun isNetworkConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isGpsHardwareEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager ?: return false
        return lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
    }

    private fun getBatteryInfo(): Pair<Int, Boolean> {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return Pair(pct, isCharging)
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

        val isNet = isNetworkConnected()
        val isGps = isGpsHardwareEnabled()
        val (batteryPct, isCharging) = getBatteryInfo()

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        scope.launch {
                            val now = System.currentTimeMillis()
                            val ageMs = now - location.time
                            val fixType = if (ageMs > 10 * 60 * 1000L) {
                                LocationFixType.STALE_CACHED
                            } else {
                                LocationFixType.CACHED_FUSED
                            }

                            val addressName = resolveAddress(location.latitude, location.longitude)
                            val result = LastKnownLocation(
                                hasFix = true,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                locationName = addressName,
                                accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
                                provider = location.provider,
                                fixType = fixType,
                                observationTimestamp = location.time,
                                batteryPercentage = batteryPct,
                                isCharging = isCharging,
                                networkName = if (isNet) "ইন্টারনেট সংযুক্ত (Online)" else "অফলাইন (Disconnected)",
                                isNetworkConnected = isNet,
                                isGpsHardwareEnabled = isGps
                            )
                            onSuccess(result)
                        }
                    } else {
                        // Request fresh high accuracy location
                        requestSingleFreshLocation(onSuccess, onError)
                    }
                }
                .addOnFailureListener { e ->
                    onError(e.message ?: "Failed to retrieve location")
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

        val isNet = isNetworkConnected()
        val isGps = isGpsHardwareEnabled()
        val (batteryPct, isCharging) = getBatteryInfo()

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        scope.launch {
                            val addressName = resolveAddress(location.latitude, location.longitude)
                            val result = LastKnownLocation(
                                hasFix = true,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                locationName = addressName,
                                accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
                                provider = location.provider ?: "gps",
                                fixType = LocationFixType.LIVE_GPS,
                                observationTimestamp = location.time,
                                batteryPercentage = batteryPct,
                                isCharging = isCharging,
                                networkName = if (isNet) "লাইভ ডাটা কানেক্টেড" else "অফলাইন",
                                isNetworkConnected = isNet,
                                isGpsHardwareEnabled = isGps
                            )
                            onSuccess(result)
                        }
                    } else {
                        onError("GPS location unavailable. Ensure GPS is turned on.")
                    }
                }
                .addOnFailureListener { e ->
                    onError(e.message ?: "Failed fresh location query")
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
            .setMinUpdateDistanceMeters(1f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val latest = result.lastLocation ?: return

                val speedKmh = if (latest.hasSpeed()) (latest.speed * 3.6f) else calculateSpeedFromDelta(latest)
                _currentSpeedKmh.value = speedKmh

                previousLocation?.let { prev ->
                    val distance = prev.distanceTo(latest)
                    if (distance > 0.5f) {
                        _totalDistanceTraveledMeters.value += distance
                    }
                }
                previousLocation = latest

                val (statusBn, statusEn) = when {
                    speedKmh > 25f -> "যানবাহনে দ্রুত স্থানান্তরিত হচ্ছে! 🚨" to "In Transit / High-Speed Movement! 🚨"
                    speedKmh > 3f -> "ডিভাইস নিয়ে হাঁটা হচ্ছে (Movement Detected)" to "Walking / Moving"
                    _totalDistanceTraveledMeters.value > 5f -> "স্থান পরিবর্তন হয়েছে (Displaced)" to "Location Displaced"
                    else -> "ডিভাইস স্থির অবস্থায় রয়েছে (Stationary)" to "Stationary / Stationary Guard"
                }

                _movementStatusBn.value = statusBn
                _movementStatusEn.value = statusEn

                val currentList = _movementTrail.value.toMutableList()
                currentList.add(Pair(latest.latitude, latest.longitude))
                if (currentList.size > 25) currentList.removeAt(0)
                _movementTrail.value = currentList

                scope.launch {
                    val isNet = isNetworkConnected()
                    val isGps = isGpsHardwareEnabled()
                    val (batteryPct, isCharging) = getBatteryInfo()
                    val addressName = resolveAddress(latest.latitude, latest.longitude)

                    val locObj = LastKnownLocation(
                        hasFix = true,
                        latitude = latest.latitude,
                        longitude = latest.longitude,
                        locationName = addressName,
                        accuracyMeters = if (latest.hasAccuracy()) latest.accuracy else null,
                        provider = latest.provider ?: "fused",
                        fixType = LocationFixType.LIVE_GPS,
                        observationTimestamp = latest.time,
                        batteryPercentage = batteryPct,
                        isCharging = isCharging,
                        networkName = if (isNet) "অনলাইন ট্র্যাকিং সক্রিয়" else "অফলাইন",
                        isNetworkConnected = isNet,
                        isGpsHardwareEnabled = isGps
                    )
                    onUpdate(locObj, speedKmh, statusBn, statusEn)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback as LocationCallback,
                context.mainLooper
            )
            _isTracking.value = true
        } catch (_: SecurityException) {}
    }

    fun stopRealtimeMovementTracking() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
        _isTracking.value = false
        previousLocation = null
    }

    private fun calculateSpeedFromDelta(current: Location): Float {
        val prev = previousLocation ?: return 0f
        val distanceMeters = prev.distanceTo(current)
        val timeDeltaSec = (current.time - prev.time) / 1000f
        if (timeDeltaSec <= 0f) return 0f
        val speedMps = distanceMeters / timeDeltaSec
        return (speedMps * 3.6f).coerceIn(0f, 200f)
    }

    private fun resolveAddress(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var addressResult = "GPS: ${String.format(Locale.US, "%.4f, %.4f", latitude, longitude)}"
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val a = addresses[0]
                    addressResult = listOfNotNull(a.subLocality, a.locality, a.countryName).joinToString(", ")
                }
                addressResult
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val a = addresses[0]
                    listOfNotNull(a.subLocality, a.locality, a.countryName).joinToString(", ")
                } else {
                    "GPS: ${String.format(Locale.US, "%.4f, %.4f", latitude, longitude)}"
                }
            }
        } catch (_: Exception) {
            "GPS: ${String.format(Locale.US, "%.4f, %.4f", latitude, longitude)}"
        }
    }
}

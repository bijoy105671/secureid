package com.example.data.model

enum class LocationFixType(val labelBn: String, val labelEn: String) {
    LIVE_GPS("লাইভ জিপিএস (সরাসরি উপগ্রহ)", "Live Satellite GPS"),
    CACHED_FUSED("ফিউজড নেটওয়ার্ক ক্যাশড", "Fused / Network Cached"),
    STALE_CACHED("পুরনো ক্যাশড রেকর্ড (Stale)", "Stale Cached Record"),
    UNAVAILABLE("লোকেশন অনুপলব্ধ (GPS বন্ধ)", "Location Unavailable")
}

data class LastKnownLocation(
    val hasFix: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String = "লোকেশন এখনও গ্রহণ করা হয়নি",
    val accuracyMeters: Float? = null,
    val provider: String? = null,
    val fixType: LocationFixType = LocationFixType.UNAVAILABLE,
    val observationTimestamp: Long? = null,
    val batteryPercentage: Int = -1,
    val isCharging: Boolean = false,
    val networkName: String = "অজানা নেটওয়ার্ক",
    val isNetworkConnected: Boolean = false,
    val isGpsHardwareEnabled: Boolean = false
)

data class SimStatus(
    val isInserted: Boolean = true,
    val simState: String = "READY",
    val operatorName: String = "টেলিকম নেটওয়ার্ক",
    val countryIso: String = "BD",
    val isAirplaneModeOn: Boolean = false,
    val isTrayEjected: Boolean = false,
    val hardwareTrayRemovalSupported: Boolean = false, // Documented platform limitation
    val lastChangedTimestamp: Long = System.currentTimeMillis()
)

data class PowerOffChallenge(
    val challengeId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val otp: String,
    val requestingDevice: String = "প্রাথমিক ডিভাইস",
    val targetSecondaryPhone: String = "নিবন্ধিত সেকেন্ডারি ফোন",
    val isApproved: Boolean = false,
    val expiryTimestamp: Long = System.currentTimeMillis() + 120_000L
)

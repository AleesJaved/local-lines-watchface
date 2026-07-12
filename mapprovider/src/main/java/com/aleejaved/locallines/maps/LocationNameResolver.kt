package com.aleejaved.locallines.maps

import android.content.Context
import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume

data class LocationNames(
    val number: String?,
    val road: String?,
    val town: String?,
    val city: String?,
    val country: String?,
)

class LocationNameResolver(context: Context) {
    private val geocoder = Geocoder(context)

    suspend fun resolve(latitude: Double, longitude: Double): LocationNames? {
        val systemResult = withTimeoutOrNull(GEOCODER_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        val address = addresses.firstOrNull()
                        continuation.resume(address?.toLocationNames())
                    }

                    override fun onError(errorMessage: String?) {
                        continuation.resume(null)
                    }
                })
            }
        }
        if (systemResult?.hasAnyValue() == true) return systemResult
        return resolveWithNominatim(latitude, longitude)
    }

    private suspend fun resolveWithNominatim(latitude: Double, longitude: Double): LocationNames? =
        withContext(Dispatchers.IO) {
            runCatching {
                val language = Locale.getDefault().toLanguageTag()
                val endpoint = "$NOMINATIM_REVERSE_URL?format=jsonv2&addressdetails=1&zoom=18" +
                    "&lat=$latitude&lon=$longitude&accept-language=$language"
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = NETWORK_TIMEOUT_MS
                    readTimeout = NETWORK_TIMEOUT_MS
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", USER_AGENT)
                }
                try {
                    if (connection.responseCode !in 200..299) return@runCatching null
                    connection.inputStream.bufferedReader().use { parseNominatim(it.readText()) }
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()
        }

    private fun Address.toLocationNames(): LocationNames {
        val city = locality.clean() ?: subAdminArea.clean() ?: adminArea.clean()
        val town = subLocality.clean() ?: locality.clean() ?: subAdminArea.clean()
        val road = thoroughfare.clean() ?: featureName.clean()
        return LocationNames(
            number = subThoroughfare.clean(),
            road = road,
            town = town,
            city = city,
            country = countryName.clean(),
        )
    }

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private fun LocationNames.hasAnyValue(): Boolean =
        number != null || road != null || town != null || city != null || country != null

    companion object {
        private const val GEOCODER_TIMEOUT_MS = 3_000L
        private const val NETWORK_TIMEOUT_MS = 6_000
        private const val NOMINATIM_REVERSE_URL = "https://nominatim.openstreetmap.org/reverse"
        private const val USER_AGENT =
            "LocalLinesWatchFace/1.0 (+https://github.com/AleesJaved/local-lines-watchface)"

        internal fun parseNominatim(json: String): LocationNames? {
            val address = JSONObject(json).optJSONObject("address") ?: return null
            fun first(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
                address.optString(key).trim().takeIf { it.isNotEmpty() }
            }

            val number = first("house_number")
            val road = first("road", "pedestrian", "footway")
            val town = first("town", "village", "suburb", "neighbourhood", "hamlet", "city_district")
            val city = first("city", "town", "municipality", "county", "state_district", "state")
            val country = first("country")
            return LocationNames(number, road, town, city, country).takeIf {
                it.number != null || it.road != null || it.town != null || it.city != null || it.country != null
            }
        }
    }
}

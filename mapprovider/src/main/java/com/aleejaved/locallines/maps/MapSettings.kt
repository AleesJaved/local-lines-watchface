package com.aleejaved.locallines.maps

import android.content.Context

class MapSettings(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    var refreshMode: RefreshMode
        get() = runCatching {
            RefreshMode.valueOf(preferences.getString(KEY_REFRESH_MODE, RefreshMode.BALANCED.name)!!)
        }.getOrDefault(RefreshMode.BALANCED)
        set(value) {
            preferences.edit().putString(KEY_REFRESH_MODE, value.name).apply()
        }

    var locationLabelMode: LocationLabelMode
        get() = runCatching {
            LocationLabelMode.valueOf(preferences.getString(KEY_LOCATION_LABEL_MODE, LocationLabelMode.NONE.name)!!)
        }.getOrDefault(LocationLabelMode.NONE)
        set(value) {
            preferences.edit().putString(KEY_LOCATION_LABEL_MODE, value.name).apply()
        }

    val streetLabel: String? get() = preferences.getString(KEY_STREET_LABEL, null)
    val townLabel: String? get() = preferences.getString(KEY_TOWN_LABEL, null)
    val cityLabel: String? get() = preferences.getString(KEY_CITY_LABEL, null)

    fun selectedLocationLabel(): String? = when (locationLabelMode) {
        LocationLabelMode.NONE -> null
        LocationLabelMode.STREET -> streetLabel ?: townLabel ?: cityLabel
        LocationLabelMode.TOWN -> townLabel ?: cityLabel
        LocationLabelMode.CITY -> cityLabel ?: townLabel
    }

    val lastLatitude: Double?
        get() = if (preferences.contains(KEY_LATITUDE)) {
            Double.fromBits(preferences.getLong(KEY_LATITUDE, 0L))
        } else null

    val lastLongitude: Double?
        get() = if (preferences.contains(KEY_LONGITUDE)) {
            Double.fromBits(preferences.getLong(KEY_LONGITUDE, 0L))
        } else null

    val lastUpdatedMillis: Long
        get() = preferences.getLong(KEY_UPDATED, 0L)

    fun recordSnapshot(latitude: Double, longitude: Double, updatedMillis: Long) {
        preferences.edit()
            .putLong(KEY_LATITUDE, latitude.toBits())
            .putLong(KEY_LONGITUDE, longitude.toBits())
            .putLong(KEY_UPDATED, updatedMillis)
            .apply()
    }

    fun recordAddress(street: String?, town: String?, city: String?) {
        preferences.edit()
            .putString(KEY_STREET_LABEL, street)
            .putString(KEY_TOWN_LABEL, town)
            .putString(KEY_CITY_LABEL, city)
            .apply()
    }

    companion object {
        private const val PREFERENCES = "local_lines_settings"
        private const val KEY_REFRESH_MODE = "refresh_mode"
        private const val KEY_LATITUDE = "last_latitude"
        private const val KEY_LONGITUDE = "last_longitude"
        private const val KEY_UPDATED = "last_updated"
        private const val KEY_LOCATION_LABEL_MODE = "location_label_mode"
        private const val KEY_STREET_LABEL = "street_label"
        private const val KEY_TOWN_LABEL = "town_label"
        private const val KEY_CITY_LABEL = "city_label"
    }
}

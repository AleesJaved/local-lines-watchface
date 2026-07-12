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
            LocationLabelMode.valueOf(preferences.getString(KEY_LOCATION_LABEL_MODE, LocationLabelMode.TOWN.name)!!)
        }.getOrDefault(LocationLabelMode.TOWN)
        set(value) {
            preferences.edit().putString(KEY_LOCATION_LABEL_MODE, value.name).apply()
        }

    var locationNumberEnabled: Boolean
        get() = preferences.getBoolean(KEY_SHOW_NUMBER, locationLabelMode == LocationLabelMode.STREET)
        set(value) = preferences.edit().putBoolean(KEY_SHOW_NUMBER, value).apply()

    var locationRoadEnabled: Boolean
        get() = preferences.getBoolean(KEY_SHOW_ROAD, locationLabelMode == LocationLabelMode.STREET)
        set(value) = preferences.edit().putBoolean(KEY_SHOW_ROAD, value).apply()

    var locationTownEnabled: Boolean
        get() = preferences.getBoolean(KEY_SHOW_TOWN, locationLabelMode == LocationLabelMode.TOWN)
        set(value) = preferences.edit().putBoolean(KEY_SHOW_TOWN, value).apply()

    var locationCityEnabled: Boolean
        get() = preferences.getBoolean(KEY_SHOW_CITY, locationLabelMode == LocationLabelMode.CITY)
        set(value) = preferences.edit().putBoolean(KEY_SHOW_CITY, value).apply()

    var locationCountryEnabled: Boolean
        get() = preferences.getBoolean(KEY_SHOW_COUNTRY, false)
        set(value) = preferences.edit().putBoolean(KEY_SHOW_COUNTRY, value).apply()

    val numberLabel: String? get() = preferences.getString(KEY_NUMBER_LABEL, null)
    val roadLabel: String? get() = preferences.getString(KEY_STREET_LABEL, null)
    val townLabel: String? get() = preferences.getString(KEY_TOWN_LABEL, null)
    val cityLabel: String? get() = preferences.getString(KEY_CITY_LABEL, null)
    val countryLabel: String? get() = preferences.getString(KEY_COUNTRY_LABEL, null)
    val lastLocationLabelUpdatedMillis: Long get() = preferences.getLong(KEY_LABEL_UPDATED, 0L)

    fun hasEnabledLocationParts(): Boolean =
        locationNumberEnabled || locationRoadEnabled || locationTownEnabled ||
            locationCityEnabled || locationCountryEnabled

    fun selectedLocationLabel(): String? {
        val numberAndRoad = listOfNotNull(
            numberLabel.takeIf { locationNumberEnabled },
            roadLabel.takeIf { locationRoadEnabled },
        ).joinToString(" ").takeIf { it.isNotBlank() }
        return listOfNotNull(
            numberAndRoad,
            townLabel.takeIf { locationTownEnabled },
            cityLabel.takeIf { locationCityEnabled },
            countryLabel.takeIf { locationCountryEnabled },
        ).distinct().joinToString(", ").takeIf { it.isNotBlank() }
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

    fun recordAddress(number: String?, road: String?, town: String?, city: String?, country: String?) {
        preferences.edit()
            .putString(KEY_NUMBER_LABEL, number)
            .putString(KEY_STREET_LABEL, road)
            .putString(KEY_TOWN_LABEL, town)
            .putString(KEY_CITY_LABEL, city)
            .putString(KEY_COUNTRY_LABEL, country)
            .putLong(KEY_LABEL_UPDATED, System.currentTimeMillis())
            .apply()
    }

    companion object {
        private const val PREFERENCES = "local_lines_settings"
        private const val KEY_REFRESH_MODE = "refresh_mode"
        private const val KEY_LATITUDE = "last_latitude"
        private const val KEY_LONGITUDE = "last_longitude"
        private const val KEY_UPDATED = "last_updated"
        private const val KEY_LOCATION_LABEL_MODE = "location_label_mode"
        private const val KEY_SHOW_NUMBER = "show_location_number"
        private const val KEY_SHOW_ROAD = "show_location_road"
        private const val KEY_SHOW_TOWN = "show_location_town"
        private const val KEY_SHOW_CITY = "show_location_city"
        private const val KEY_SHOW_COUNTRY = "show_location_country"
        private const val KEY_NUMBER_LABEL = "number_label"
        private const val KEY_STREET_LABEL = "street_label"
        private const val KEY_TOWN_LABEL = "town_label"
        private const val KEY_CITY_LABEL = "city_label"
        private const val KEY_COUNTRY_LABEL = "country_label"
        private const val KEY_LABEL_UPDATED = "label_updated"
    }
}

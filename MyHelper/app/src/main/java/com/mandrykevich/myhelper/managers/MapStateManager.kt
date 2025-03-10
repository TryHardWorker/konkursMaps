package com.mandrykevich.myhelper.managers

import android.content.Context
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition

class MapStateManager(private val context: Context) {

    private val PREFS_NAME = "MapPreferences"
    private val KEY_LATITUDE = "latitude"
    private val KEY_LONGITUDE = "longitude"
    private val KEY_ZOOM = "zoom"

    fun restoreMapState(mapView: com.yandex.mapkit.mapview.MapView): CameraPosition {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val latitude = sharedPreferences.getFloat(KEY_LATITUDE, Float.NaN)
        val longitude = sharedPreferences.getFloat(KEY_LONGITUDE, Float.NaN)
        val zoom = sharedPreferences.getFloat(KEY_ZOOM, Float.NaN)

        return if (latitude.isNaN() || longitude.isNaN() || zoom.isNaN()) {
            CameraPosition(Point(53.67527705696968, 23.828771603557005), 11f, 0.0f, 0.0f)
        } else {
            CameraPosition(Point(latitude.toDouble(), longitude.toDouble()), zoom, 0.0f, 0.0f)
        }
    }

    fun saveMapState(mapView: com.yandex.mapkit.mapview.MapView) {
        val currentPosition = mapView.map.cameraPosition
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.putFloat(KEY_LATITUDE, currentPosition.target.latitude.toFloat())
        editor.putFloat(KEY_LONGITUDE, currentPosition.target.longitude.toFloat())
        editor.putFloat(KEY_ZOOM, currentPosition.zoom.toFloat())
        editor.apply()
    }
}
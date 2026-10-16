package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
import java.util.Locale

@SuppressLint("MissingPermission")
suspend fun getCurrentLocationName(context: Context): String? {
    try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val location = fusedLocationClient.lastLocation.await()
        if (location != null) {
            val geocoder = Geocoder(context, Locale("tr", "TR"))
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                return buildString {
                    if (address.subAdminArea != null) append(address.subAdminArea).append(", ")
                    if (address.adminArea != null) append(address.adminArea).append(", ")
                    if (address.countryName != null) append(address.countryName)
                }.trimEnd(',', ' ')
            }
            return "${location.latitude}, ${location.longitude}"
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

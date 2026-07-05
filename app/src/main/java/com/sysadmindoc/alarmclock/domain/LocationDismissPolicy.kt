package com.sysadmindoc.alarmclock.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object LocationDismissPolicy {
    const val MIN_RADIUS_METERS = 25
    const val MAX_RADIUS_METERS = 5_000

    data class CheckResult(
        val distanceMeters: Float,
        val radiusMeters: Int,
        val outsideFence: Boolean
    )

    fun coerceRadius(radiusMeters: Int): Int =
        radiusMeters.coerceIn(MIN_RADIUS_METERS, MAX_RADIUS_METERS)

    fun hasTarget(latitude: Double, longitude: Double): Boolean {
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return false
        return latitude != 0.0 || longitude != 0.0
    }

    fun check(
        targetLatitude: Double,
        targetLongitude: Double,
        radiusMeters: Int,
        currentLatitude: Double,
        currentLongitude: Double
    ): CheckResult? {
        if (!hasTarget(targetLatitude, targetLongitude)) return null
        if (currentLatitude !in -90.0..90.0 || currentLongitude !in -180.0..180.0) return null
        val radius = coerceRadius(radiusMeters)
        val distance = distanceMeters(
            targetLatitude = targetLatitude,
            targetLongitude = targetLongitude,
            currentLatitude = currentLatitude,
            currentLongitude = currentLongitude
        )
        return CheckResult(
            distanceMeters = distance,
            radiusMeters = radius,
            outsideFence = distance >= radius
        )
    }

    fun distanceMeters(
        targetLatitude: Double,
        targetLongitude: Double,
        currentLatitude: Double,
        currentLongitude: Double
    ): Float {
        val earthRadiusMeters = 6_371_000.0
        val targetLat = Math.toRadians(targetLatitude)
        val currentLat = Math.toRadians(currentLatitude)
        val latDelta = Math.toRadians(currentLatitude - targetLatitude)
        val lngDelta = Math.toRadians(currentLongitude - targetLongitude)

        val a = sin(latDelta / 2).pow(2.0) +
            cos(targetLat) * cos(currentLat) * sin(lngDelta / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (earthRadiusMeters * c).toFloat()
    }
}

package com.nhathuy.nextmeet.model

import com.google.android.gms.maps.model.LatLng
import kotlin.math.sqrt

object LocationUtils {
    /**
     * Tính khoảng cách từ mot diem den mot truong da tuyen
     */
    fun distanceToPolyline(point: LatLng, polylinePoints: List<LatLng>): Double {
        var minDistance = Double.MAX_VALUE
        for (i in 0 until polylinePoints.size - 1) {
            val start = polylinePoints[i]
            val end = polylinePoints[i + 1]
            val distance = distanceToLineSegment(point, start, end)
            minDistance = minOf(minDistance, distance)
        }
        return minDistance
    }

    /**
     * Tính khoảng cách từ mot diem den mot doan thang
     */
    private fun distanceToLineSegment(
        point: LatLng,
        lineStart: LatLng,
        lineEnd: LatLng
    ): Double {
        val A = point.latitude - lineStart.latitude
        val B = point.longitude - lineStart.longitude
        val C = lineEnd.latitude - lineStart.latitude
        val D = lineEnd.longitude - lineStart.longitude

        val dot = A * C + B * D
        val lenSq = C * C + D * D

        val param = if (lenSq != 0.0) dot / lenSq else -1.0
        val xx: Double
        val yy: Double

        if (param < 0) {
            xx = lineStart.latitude
            yy = lineStart.longitude
        } else if (param > 1) {
            xx = lineEnd.latitude
            yy = lineEnd.longitude
        } else {
            xx = lineStart.latitude + param * C
            yy = lineStart.longitude + param * D
        }

        val dx = point.latitude - xx
        val dy = point.longitude - yy

        return sqrt(dx * dx + dy * dy) * 111320
    }
}
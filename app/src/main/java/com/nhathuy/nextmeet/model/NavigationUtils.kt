package com.nhathuy.nextmeet.model

import com.google.android.gms.maps.model.LatLng
import com.nhathuy.nextmeet.model.NavigationStep
import com.nhathuy.nextmeet.model.TransportMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object NavigationUtils {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    data class RouteResult(
        val duration: Int, // minutes
        val distanceMeters: Int,
        val encodedPolyline: String,
        val steps: List<NavigationStep>
    )

    suspend fun calculateRoute(
        origin: LatLng,
        destination: LatLng,
        transportMode: TransportMode,
        apiKey: String
    ): RouteResult? {
        return withContext(Dispatchers.IO) {
            try {
                val travelMode = when (transportMode) {
                    TransportMode.DRIVING -> "DRIVE"
                    TransportMode.WALKING -> "WALK"
                    TransportMode.TRANSIT -> "TRANSIT"
                }

                val requestBody = JSONObject().apply {
                    put("origin", JSONObject().apply {
                        put("location", JSONObject().apply {
                            put("latLng", JSONObject().apply {
                                put("latitude", origin.latitude)
                                put("longitude", origin.longitude)
                            })
                        })
                    })
                    put("destination", JSONObject().apply {
                        put("location", JSONObject().apply {
                            put("latLng", JSONObject().apply {
                                put("latitude", destination.latitude)
                                put("longitude", destination.longitude)
                            })
                        })
                    })
                    put("travelMode", travelMode)
                    put("computeAlternativeRoutes", false)
                    put("routeModifiers", JSONObject().apply {
                        put("avoidTolls", false)
                        put("avoidHighways", false)
                        put("avoidFerries", false)
                    })
                    put("languageCode", "vi")
                    put("units", "METRIC")
                }

                val request = Request.Builder()
                    .url("https://routes.googleapis.com/directions/v2:computeRoutes")
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Goog-Api-Key", apiKey)
                    .addHeader("X-Goog-FieldMask", "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline,routes.legs.steps")
                    .build()

                val response = okHttpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    responseBody?.let { parseRouteResponse(it) }
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun parseRouteResponse(responseBody: String): RouteResult? {
        try {
            val jsonResponse = JSONObject(responseBody)
            val routes = jsonResponse.getJSONArray("routes")

            if (routes.length() > 0) {
                val route = routes.getJSONObject(0)
                val duration = route.optString("duration", "0s")
                val distanceMeters = route.optInt("distanceMeters", 0)
                val encodedPolyline = route.getJSONObject("polyline").getString("encodedPolyline")

                val steps = mutableListOf<NavigationStep>()
                val legs = route.optJSONArray("legs")
                if (legs != null && legs.length() > 0) {
                    val leg = legs.getJSONObject(0)
                    val legSteps = leg.optJSONArray("steps")
                    if (legSteps != null) {
                        for (i in 0 until legSteps.length()) {
                            val step = legSteps.getJSONObject(i)
                            steps.add(parseNavigationStep(step))
                        }
                    }
                }

                return RouteResult(
                    duration = parseDurationToMinutes(duration),
                    distanceMeters = distanceMeters,
                    encodedPolyline = encodedPolyline,
                    steps = steps
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun parseNavigationStep(stepJson: JSONObject): NavigationStep {
        val navigationInstruction = stepJson.optJSONObject("navigationInstruction")
        val localizedValues = stepJson.optJSONObject("localizedValues")

        val instruction = navigationInstruction?.optString("instructions", "Tiếp tục") ?: "Tiếp tục"
        val distance = localizedValues?.optJSONObject("distance")?.optString("text", "") ?: ""

        // Parse start and end locations
        val startLocation = stepJson.optJSONObject("startLocation")?.optJSONObject("latLng")
        val endLocation = stepJson.optJSONObject("endLocation")?.optJSONObject("latLng")

        return NavigationStep(
            instruction = instruction,
            distance = distance,
            startLocation = LatLng(
                startLocation?.optDouble("latitude") ?: 0.0,
                startLocation?.optDouble("longitude") ?: 0.0
            ),
            endLocation = LatLng(
                endLocation?.optDouble("latitude") ?: 0.0,
                endLocation?.optDouble("longitude") ?: 0.0
            )
        )
    }

    private fun parseDurationToMinutes(duration: String): Int {
        return try {
            val seconds = duration.replace("s", "").toInt()
            (seconds / 60)
        } catch (e: Exception) {
            0
        }
    }

    fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0

            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1F) shl shift)
                shift += 5
            } while (b >= 0x20)

            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0

            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }

        return poly
    }
}
package com.nhathuy.nextmeet.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ActivityTurnByTurnNavigationBinding
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.LocationUtils
import com.nhathuy.nextmeet.model.NavigationStep
import com.nhathuy.nextmeet.model.NavigationUtils
import com.nhathuy.nextmeet.model.TransportMode
import com.nhathuy.nextmeet.resource.AppointmentUiState
import com.nhathuy.nextmeet.utils.Constant
import com.nhathuy.nextmeet.viewmodel.AppointmentPlusViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

@AndroidEntryPoint
class TurnByTurnNavigationActivity : AppCompatActivity(), OnMapReadyCallback,
    TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityTurnByTurnNavigationBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var textToSpeech: TextToSpeech

    private val appointmentViewModel: AppointmentPlusViewModel by viewModels()

    // Core navigation data
    private var appointment: AppointmentPlus? = null
    private var currentLocation: Location? = null
    private var navigationSteps: List<NavigationStep> = emptyList()
    private var currentStepIndex = 0
    private var isNavigationActive = false
    private var hasArrivedAtDestination = false

    // Enhanced route tracking
    private var routePolyline: Polyline? = null
    private var passedRoutePolyline: Polyline? = null
    private var destinationMarker: Marker? = null
    private var userLocationMarker: Marker? = null
    private var originalRoutePoints: List<LatLng> = emptyList()
    private var currentRoutePointIndex = 0

    // Off-route detection
    private var consecutiveOffRouteCount = 0
    private var lastRerouteTime = 0L
    private var isRerouting = false

    // Enhanced location tracking
    private var lastBearingUpdateLocation: Location? = null
    private var smoothedBearing: Float = 0f
    private var lastAccurateBearing: Float = 0f
    private var userLocationAccuracyCircle: Circle? = null
    private var isFirstLocationUpdate = true

    // Voice control
    private var isMuted = false

    private companion object {
        const val LOCATION_UPDATE_INTERVAL = 1000L // 1 giây
        const val FASTEST_UPDATE_INTERVAL = 500L // 0.5 giây
        const val STEP_COMPLETION_DISTANCE = 50f // 50 meters
        const val REROUTE_DISTANCE_THRESHOLD = 50f // 50 meters
        const val BEARING_UPDATE_THRESHOLD = 5f // 5 degrees
        const val MIN_REROUTE_INTERVAL = 30000L // 30 seconds
        const val OFF_ROUTE_COUNT_THRESHOLD = 3
        const val ROUTE_POINT_SEARCH_RADIUS = 100f
        const val MIN_ACCURACY_THRESHOLD = 20f // meters
        const val MIN_DISTANCE_FOR_BEARING_UPDATE = 5f // meters
        const val CAMERA_UPDATE_DISTANCE_THRESHOLD = 10f // meters
        const val CAMERA_UPDATE_BEARING_THRESHOLD = 15f // degrees
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTurnByTurnNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            enableEdgeToEdge()
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        // Keep screen on during navigation
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val appointmentId = intent.getIntExtra(Constant.EXTRA_APPOINTMENT_ID, -1)
        if (appointmentId == -1) {
            finish()
            return
        }

        initializeComponents()
        setupClickListeners()
        observeAppointment(appointmentId)
        setupMap()
    }

    private fun initializeComponents() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        textToSpeech = TextToSpeech(this, this)

        // Load voice state
        loadVoiceState()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    onLocationUpdate(location)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            btnStopNavigation.setOnClickListener { stopNavigation() }
            btnMute.setOnClickListener { toggleMute() }
            btnRecenter.setOnClickListener { recenterCamera() }

            // Long click để test voice
            btnMute.setOnLongClickListener {
                testVoice()
                true
            }
        }
    }

    private fun observeAppointment(appointmentId: Int) {
        lifecycleScope.launch {
            appointmentViewModel.appointmentUiState.collect { state ->
                when (state) {
                    is AppointmentUiState.AppointmentLoaded -> {
                        appointment = state.appointment
                        Log.d("TurnByTurnNavigationActivity", "Appointment loaded: ${appointment?.status}")
                        setupNavigationUI()
                    }
                    is AppointmentUiState.Error -> {
                        finish()
                    }
                    else -> {}
                }
            }
        }
        appointmentViewModel.getAppointmentById(appointmentId)
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as com.google.android.gms.maps.SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableLocationTracking()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                Constant.LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationTracking() {
        googleMap.isMyLocationEnabled = false

        val locationRequest = LocationRequest.create().apply {
            interval = LOCATION_UPDATE_INTERVAL
            fastestInterval = FASTEST_UPDATE_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 5f
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback,
            Looper.getMainLooper()
        )

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                currentLocation = it
                Log.d("Navigation", "Current location: ${it.latitude}, ${it.longitude}")
                startNavigation()
            } ?: run {
                Log.e("Navigation", "Unable to get current location")
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.apply {
            uiSettings.apply {
                isZoomControlsEnabled = false
                isCompassEnabled = false
                isMyLocationButtonEnabled = false
                isMapToolbarEnabled = false
            }
            mapType = GoogleMap.MAP_TYPE_NORMAL
        }
        checkLocationPermission()
    }

    private fun setupNavigationUI() {
        appointment?.let { appointment ->
            binding.apply {
                tvDestinationAddress.text = appointment.location
            }
        }
    }

    private fun startNavigation() {
        val currentLoc = currentLocation ?: return
        val destination = appointment ?: return

        isNavigationActive = true
        binding.navigationControls.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = NavigationUtils.calculateRoute(
                    LatLng(currentLoc.latitude, currentLoc.longitude),
                    LatLng(destination.latitude, destination.longitude),
                    TransportMode.DRIVING,
                    getString(R.string.google_map_api_key)
                )

                result?.let { routeResult ->
                    withContext(Dispatchers.Main) {
                        displayRoute(routeResult)
                        Log.d("Navigation", "Route calculated with ${routeResult.steps.size} steps")

                        if (routeResult.steps.isNotEmpty()) {
                            startTurnByTurnNavigation(routeResult.steps)
                            updateNavigationInfo(routeResult)
                            announceInstruction("Bắt đầu điều hướng")
                        } else {
                            Log.e("Navigation", "No navigation steps found")
                        }
                    }
                } ?: run {
                    Log.e("Navigation", "Failed to calculate route")
                }
            } catch (e: Exception) {
                Log.e("TurnByTurnNavigationActivity", "Error calculating route", e)
            }
        }
    }

    private fun updateNavigationInfo(routeResult: NavigationUtils.RouteResult) {
        binding.apply {
            appointment?.let { appt ->
                tvDestinationAddress.text = appt.location
            }

            mainInstructionPanel.visibility = View.VISIBLE

            if (routeResult.steps.isNotEmpty()) {
                displayCurrentStep(routeResult.steps[0])
            }
        }
    }

    private fun displayRoute(routeResult: NavigationUtils.RouteResult) {
        // Clear existing overlays
        clearMapOverlays()

        // Store original route points
        originalRoutePoints = NavigationUtils.decodePolyline(routeResult.encodedPolyline)
        currentRoutePointIndex = 0

        // Add destination marker
        appointment?.let { appt ->
            destinationMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(appt.latitude, appt.longitude))
                    .title(appt.title)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
        }

        // Draw route polyline
        routePolyline = googleMap.addPolyline(
            PolylineOptions()
                .addAll(originalRoutePoints)
                .width(12f)
                .color(ContextCompat.getColor(this, R.color.primary_color))
                .geodesic(true)
        )

        // Create user location marker
        currentLocation?.let { location ->
            createUserLocationMarker(location)
            updateNavigationCamera(location)
        }
    }

    private fun clearMapOverlays() {
        routePolyline?.remove()
        passedRoutePolyline?.remove()
        destinationMarker?.remove()
        userLocationMarker?.remove()
        userLocationAccuracyCircle?.remove()
    }

    private fun createUserLocationMarker(location: Location) {
        userLocationMarker?.remove()
        userLocationAccuracyCircle?.remove()

        val userPosition = LatLng(location.latitude, location.longitude)

        // Tạo vòng tròn độ chính xác
        if (location.hasAccuracy() && location.accuracy <= MIN_ACCURACY_THRESHOLD) {
            userLocationAccuracyCircle = googleMap.addCircle(
                CircleOptions()
                    .center(userPosition)
                    .radius(location.accuracy.toDouble())
                    .fillColor(ContextCompat.getColor(this, R.color.light_primary))
                    .strokeColor(ContextCompat.getColor(this, R.color.primary_color))
                    .strokeWidth(2f)
            )
        }

        // Tạo marker người dùng
        userLocationMarker = googleMap.addMarker(
            MarkerOptions()
                .position(userPosition)
                .icon(createUserLocationIcon())
                .anchor(0.5f, 0.5f)
                .rotation(if (location.hasBearing()) location.bearing else smoothedBearing)
                .flat(true)
                .zIndex(100f)
        )
    }

    private fun createUserLocationIcon(): BitmapDescriptor {
        val size = 60
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Shadow
        val shadowPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(80, 0, 0, 0)
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f + 1, size / 2f + 1, (size / 2f) - 2, shadowPaint)

        // White border
        val borderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 2, borderPaint)

        // Blue center
        val centerPaint = android.graphics.Paint().apply {
            color = ContextCompat.getColor(this@TurnByTurnNavigationActivity, R.color.primary_color)
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 6, centerPaint)

        // Direction arrow
        val arrowPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }

        val arrowPath = android.graphics.Path().apply {
            moveTo(size / 2f, size / 2f - 12)
            lineTo(size / 2f - 8, size / 2f + 8)
            lineTo(size / 2f - 3, size / 2f + 5)
            lineTo(size / 2f + 3, size / 2f + 5)
            lineTo(size / 2f + 8, size / 2f + 8)
            close()
        }
        canvas.drawPath(arrowPath, arrowPaint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun startTurnByTurnNavigation(steps: List<NavigationStep>) {
        navigationSteps = steps
        currentStepIndex = 0
        if (steps.isNotEmpty()) {
            displayCurrentStep(steps[0])
        }
    }

    private fun onLocationUpdate(location: Location) {
        if (!isNavigationActive) return

        // Filter inaccurate locations
        if (location.hasAccuracy() && location.accuracy > MIN_ACCURACY_THRESHOLD * 2) {
            Log.d("Navigation", "Location accuracy too low: ${location.accuracy}m")
            return
        }

        val previousLocation = currentLocation
        currentLocation = location

        // Update user location marker
        updateUserLocationMarker(location)

        // Update camera if needed
        if (isFirstLocationUpdate || shouldUpdateCamera(previousLocation, location)) {
            updateNavigationCamera(location)
            isFirstLocationUpdate = false
        }

        // Update passed route visualization
        updatePassedRoute(location)

        // Check step completion
        checkStepCompletion(location)

        // Update distance and time
        updateDistanceAndTime(location)

        // Check for reroute
        if (previousLocation != null) {
            checkForReroute(location)
        }
    }

    private fun shouldUpdateCamera(previousLocation: Location?, currentLocation: Location): Boolean {
        if (previousLocation == null) return true

        val distance = previousLocation.distanceTo(currentLocation)
        val bearingChange = if (previousLocation.hasBearing() && currentLocation.hasBearing()) {
            abs(shortestBearingDifference(previousLocation.bearing, currentLocation.bearing))
        } else 0f

        return distance > CAMERA_UPDATE_DISTANCE_THRESHOLD || bearingChange > CAMERA_UPDATE_BEARING_THRESHOLD
    }

    private fun updateUserLocationMarker(location: Location) {
        val userPosition = LatLng(location.latitude, location.longitude)

        // Update accuracy circle
        userLocationAccuracyCircle?.let { circle ->
            if (location.hasAccuracy() && location.accuracy <= MIN_ACCURACY_THRESHOLD) {
                circle.center = userPosition
                circle.radius = location.accuracy.toDouble()
            } else {
                circle.remove()
                userLocationAccuracyCircle = null
            }
        }

        userLocationMarker?.let { marker ->
            animateMarkerToPosition(marker, userPosition)
            updateMarkerBearing(marker, location)
        } ?: run {
            createUserLocationMarker(location)
        }
    }

    private fun animateMarkerToPosition(marker: Marker, newPosition: LatLng) {
        val startPosition = marker.position
        val handler = Handler(Looper.getMainLooper())
        val startTime = SystemClock.uptimeMillis()
        val duration = 1000L
        val interpolator = AccelerateDecelerateInterpolator()

        val animatePosition = object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - startTime
                val t = elapsed.toFloat() / duration
                val interpolatedTime = interpolator.getInterpolation(t.coerceIn(0f, 1f))

                val lat = startPosition.latitude + (newPosition.latitude - startPosition.latitude) * interpolatedTime
                val lng = startPosition.longitude + (newPosition.longitude - startPosition.longitude) * interpolatedTime

                marker.position = LatLng(lat, lng)

                if (t < 1.0) {
                    handler.postDelayed(this, 16)
                }
            }
        }
        handler.post(animatePosition)
    }

    private fun updateMarkerBearing(marker: Marker, location: Location) {
        val newBearing = when {
            location.hasBearing() && location.bearing != 0f -> location.bearing
            location.hasSpeed() && location.speed > 1f -> calculateBearingFromMovement(location)
            else -> smoothedBearing
        }

        // Handle bearing wrap-around
        if (abs(newBearing - smoothedBearing) > 180f) {
            smoothedBearing = if (newBearing > smoothedBearing) {
                smoothedBearing + 360f
            } else {
                smoothedBearing - 360f
            }
        }

        // Smooth interpolation
        val bearingDiff = newBearing - smoothedBearing
        smoothedBearing += bearingDiff * 0.3f
        smoothedBearing = (smoothedBearing + 360f) % 360f

        animateMarkerBearing(marker, smoothedBearing)
    }

    private fun calculateBearingFromMovement(currentLocation: Location): Float {
        lastBearingUpdateLocation?.let { lastLocation ->
            val distance = currentLocation.distanceTo(lastLocation)

            if (distance >= MIN_DISTANCE_FOR_BEARING_UPDATE) {
                val bearing = lastLocation.bearingTo(currentLocation)
                lastBearingUpdateLocation = currentLocation
                lastAccurateBearing = bearing
                return bearing
            }
        } ?: run {
            lastBearingUpdateLocation = currentLocation
        }

        return lastAccurateBearing
    }

    private fun animateMarkerBearing(marker: Marker, targetBearing: Float) {
        val startBearing = marker.rotation
        val bearingDiff = shortestBearingDifference(startBearing, targetBearing)

        if (abs(bearingDiff) < BEARING_UPDATE_THRESHOLD) return

        val handler = Handler(Looper.getMainLooper())
        val startTime = SystemClock.uptimeMillis()
        val duration = 300L

        val animateBearing = object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - startTime
                val t = (elapsed.toFloat() / duration).coerceIn(0f, 1f)

                val currentBearing = startBearing + bearingDiff * t
                marker.rotation = (currentBearing + 360f) % 360f

                if (t < 1.0) {
                    handler.postDelayed(this, 16)
                }
            }
        }
        handler.post(animateBearing)
    }

    private fun shortestBearingDifference(from: Float, to: Float): Float {
        var diff = to - from
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        return diff
    }

    private fun updateNavigationCamera(location: Location) {
        val targetPosition = LatLng(location.latitude, location.longitude)

        val newCameraPosition = CameraPosition.Builder()
            .target(targetPosition)
            .zoom(19f)
            .bearing(if (location.hasBearing()) location.bearing else smoothedBearing)
            .tilt(60f)
            .build()

        googleMap.animateCamera(
            CameraUpdateFactory.newCameraPosition(newCameraPosition),
            1500,
            object : GoogleMap.CancelableCallback {
                override fun onFinish() {}
                override fun onCancel() {}
            }
        )
    }

    private fun updatePassedRoute(currentLocation: Location) {
        if (originalRoutePoints.isEmpty()) return

        val userLatLng = LatLng(currentLocation.latitude, currentLocation.longitude)
        val closestPointIndex = findClosestRoutePointIndex(userLatLng)

        if (closestPointIndex > currentRoutePointIndex) {
            currentRoutePointIndex = closestPointIndex

            // Update passed route
            passedRoutePolyline?.remove()
            if (currentRoutePointIndex > 0) {
                val passedPoints = originalRoutePoints.subList(0, currentRoutePointIndex + 1)
                passedRoutePolyline = googleMap.addPolyline(
                    PolylineOptions()
                        .addAll(passedPoints)
                        .width(8f)
                        .color(ContextCompat.getColor(this, android.R.color.darker_gray))
                        .geodesic(true)
                )
            }

            // Update remaining route
            routePolyline?.remove()
            if (currentRoutePointIndex < originalRoutePoints.size - 1) {
                val remainingPoints = originalRoutePoints.subList(currentRoutePointIndex, originalRoutePoints.size)
                routePolyline = googleMap.addPolyline(
                    PolylineOptions()
                        .addAll(remainingPoints)
                        .width(12f)
                        .color(ContextCompat.getColor(this, R.color.color_blue))
                        .geodesic(true)
                )
            }
        }
    }

    private fun findClosestRoutePointIndex(userLocation: LatLng): Int {
        var closestIndex = currentRoutePointIndex
        var minDistance = Float.MAX_VALUE

        val searchStart = maxOf(0, currentRoutePointIndex - 10)
        val searchEnd = minOf(originalRoutePoints.size - 1, currentRoutePointIndex + 50)

        for (i in searchStart..searchEnd) {
            val distance = calculateDistance(userLocation, originalRoutePoints[i])
            if (distance < minDistance) {
                minDistance = distance
                closestIndex = i
            }
        }

        return closestIndex
    }

    private fun calculateDistance(point1: LatLng, point2: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude,
            results
        )
        return results[0]
    }

    private fun checkStepCompletion(currentLocation: Location) {
        if (currentStepIndex >= navigationSteps.size) return

        val currentStep = navigationSteps[currentStepIndex]
        val stepLocation = Location("step").apply {
            latitude = currentStep.endLocation.latitude
            longitude = currentStep.endLocation.longitude
        }

        val distanceToStep = currentLocation.distanceTo(stepLocation)

        if (distanceToStep <= STEP_COMPLETION_DISTANCE) {
            currentStepIndex++

            if (currentStepIndex < navigationSteps.size) {
                val nextStep = navigationSteps[currentStepIndex]
                displayCurrentStep(nextStep)
                announceInstruction(nextStep.instruction)
            } else {
                onNavigationCompleted()
            }
        }
    }

    private fun displayCurrentStep(step: NavigationStep) {
        binding.apply {
            tvCurrentInstruction.text = step.instruction
            tvStepDistance.text = step.distance
            ivDirectionIcon.setImageResource(step.getDirectionIcon())

            val nextStepIndex = currentStepIndex + 1
            if (nextStepIndex < navigationSteps.size) {
                val nextStep = navigationSteps[nextStepIndex]
                tvNextInstruction.text = "Sau đó: ${nextStep.instruction}"
                tvNextInstruction.visibility = View.VISIBLE
            } else {
                tvNextInstruction.visibility = View.GONE
            }
        }
    }

    private fun updateDistanceAndTime(currentLocation: Location) {
        appointment?.let { appt ->
            val destinationLocation = Location("destination").apply {
                latitude = appt.latitude
                longitude = appt.longitude
            }

            val remainingDistance = currentLocation.distanceTo(destinationLocation)
            // Update UI if needed
        }
    }

    private fun checkForReroute(currentLocation: Location) {
        if (isRerouting) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRerouteTime < MIN_REROUTE_INTERVAL) return

        val isOffRoute = isUserOffRoute(currentLocation)

        if (isOffRoute) {
            consecutiveOffRouteCount++
            Log.d("Navigation", "Off route count: $consecutiveOffRouteCount")

            if (consecutiveOffRouteCount >= OFF_ROUTE_COUNT_THRESHOLD) {
                Log.d("Navigation", "User is consistently off route, initiating reroute...")
                rerouteNavigation()
                consecutiveOffRouteCount = 0
                lastRerouteTime = currentTime
            }
        } else {
            consecutiveOffRouteCount = 0
        }
    }

    private fun isUserOffRoute(currentLocation: Location): Boolean {
        if (originalRoutePoints.isEmpty()) return false

        val userLatLng = LatLng(currentLocation.latitude, currentLocation.longitude)
        val distanceToRoute = LocationUtils.distanceToPolyline(userLatLng, originalRoutePoints)

        return distanceToRoute > REROUTE_DISTANCE_THRESHOLD
    }

    private fun rerouteNavigation() {
        val currentLoc = currentLocation ?: return
        val destination = appointment ?: return

        isRerouting = true

        lifecycleScope.launch {
            try {
                val result = NavigationUtils.calculateRoute(
                    LatLng(currentLoc.latitude, currentLoc.longitude),
                    LatLng(destination.latitude, destination.longitude),
                    TransportMode.DRIVING,
                    getString(R.string.google_map_api_key)
                )

                result?.let { routeResult ->
                    withContext(Dispatchers.Main) {
                        displayRoute(routeResult)
                        startTurnByTurnNavigation(routeResult.steps)
                        updateNavigationInfo(routeResult)
                        announceInstruction("Đang tính toán lại lộ trình")
                        isRerouting = false
                    }
                }
            } catch (e: Exception) {
                Log.e("Navigation", "Error rerouting", e)
                isRerouting = false
            }
        }
    }

    private fun onNavigationCompleted() {
        isNavigationActive = false
        hasArrivedAtDestination = true

        binding.apply {
            tvCurrentInstruction.text = "Bạn đã đến nơi"
            tvStepDistance.text = ""
            tvNextInstruction.visibility = View.GONE
            btnStopNavigation.text = "Hoàn thành"
        }

        announceInstruction("Bạn đã đến điểm hẹn")

        lifecycleScope.launch {
            appointment?.let { appt ->

                appointment?.let { appt ->
                    appointmentViewModel.checkAppointmentStatus(appt.id)
                }

//                appointmentViewModel.updateAppointmentBasedOnTime(appt.id)
//                delay(500)
//                appointmentViewModel.updateNavigationStatus(appt.id, true)
            }
        }
    }

    private fun recenterCamera() {
        currentLocation?.let { location ->
            updateNavigationCamera(location)
        }
    }

    // Voice control functions
    private fun toggleMute() {
        isMuted = !isMuted
        updateMuteButtonUI()
        saveVoiceState()

        // Announce state change
        if (isMuted) {
            // Phát thông báo cuối cùng trước khi tắt
            if (::textToSpeech.isInitialized) {
                textToSpeech.speak("Đã tắt tiếng", TextToSpeech.QUEUE_FLUSH, null, null)
            }
        } else {
            announceInstruction("Đã bật tiếng")
        }

        Log.d("Navigation", "Voice ${if (isMuted) "muted" else "unmuted"}")
    }

    private fun updateMuteButtonUI() {
        val iconRes = if (isMuted) {
            R.drawable.ic_volume_off
        } else {
            R.drawable.ic_volume
        }

        binding.btnMute.apply {
            icon = ContextCompat.getDrawable(this@TurnByTurnNavigationActivity, iconRes)
            tag = if (isMuted) "muted" else "unmuted"

            backgroundTintList = if (isMuted) {
                ContextCompat.getColorStateList(this@TurnByTurnNavigationActivity, R.color.red)
            } else {
                ContextCompat.getColorStateList(this@TurnByTurnNavigationActivity, R.color.white)
            }

            iconTint = if (isMuted) {
                ContextCompat.getColorStateList(this@TurnByTurnNavigationActivity, R.color.white)
            } else {
                ContextCompat.getColorStateList(this@TurnByTurnNavigationActivity, R.color.gray_dark)
            }
        }
    }

    private fun saveVoiceState() {
        val prefs = getSharedPreferences("navigation_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("is_muted", isMuted).apply()
    }

    private fun loadVoiceState() {
        val prefs = getSharedPreferences("navigation_prefs", MODE_PRIVATE)
        isMuted = prefs.getBoolean("is_muted", false)
        updateMuteButtonUI()
    }

    private fun testVoice() {
        if (ensureTTSReady()) {
            val wasTemporarilyMuted = isMuted
            isMuted = false // Temporarily unmute for test
            announceInstruction("Đây là kiểm tra âm thanh")
            isMuted = wasTemporarilyMuted // Restore original state
        }
    }

    private fun ensureTTSReady(): Boolean {
        if (!::textToSpeech.isInitialized) {
            Log.w("Navigation", "TTS not initialized, reinitializing...")
            textToSpeech = TextToSpeech(this, this)
            return false
        }
        return true
    }

    private fun announceInstruction(instruction: String) {
        if (::textToSpeech.isInitialized && !isMuted) {
            textToSpeech.stop()

            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }

            textToSpeech.speak(
                instruction,
                TextToSpeech.QUEUE_FLUSH,
                params,
                "navigation_instruction"
            )

            Log.d("Navigation", "Announcing: $instruction")
        } else {
            Log.d("Navigation", "Voice is muted or TTS not initialized")
        }
    }

    private fun stopNavigation() {
        isNavigationActive = false
        fusedLocationClient.removeLocationUpdates(locationCallback)

        val resultIntent = Intent().apply {
            putExtra("navigation_completed", hasArrivedAtDestination)
        }
        setResult(if (hasArrivedAtDestination) RESULT_OK else RESULT_CANCELED, resultIntent)

        finish()
    }

    // TTS Initialization
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale("vi", "VN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("Navigation", "Vietnamese TTS not supported, using English")
                textToSpeech.setLanguage(Locale.US)
            }

            textToSpeech.apply {
                setSpeechRate(1.0f)
                setPitch(1.0f)
            }

            Log.d("Navigation", "TTS initialized successfully")

            if (!isMuted && isNavigationActive) {
                announceInstruction("Hệ thống điều hướng đã sẵn sàng")
            }
        } else {
            Log.e("Navigation", "TTS initialization failed")
        }
    }

    // Lifecycle methods
    override fun onPause() {
        super.onPause()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!::textToSpeech.isInitialized) {
            textToSpeech = TextToSpeech(this, this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Cleanup map overlays
        clearMapOverlays()

        // Update appointment status
        appointment?.let { appt ->
            lifecycleScope.launch {
                appointmentViewModel.checkAppointmentStatus(appt.id)
            }
        }

        // Cleanup TTS
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }

        // Cleanup location updates
        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }

        // Clear screen flags
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constant.LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableLocationTracking()
                } else {
                    finish()
                }
            }
        }
    }
}
package com.nhathuy.nextmeet.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.nhathuy.nextmeet.viewmodel.AppointmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
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

    private var appointment: AppointmentPlus? = null
    private var currentLocation: Location? = null
    private var navigationSteps: List<NavigationStep> = emptyList()
    private var currentStepIndex = 0
    private var isNavigationActive = false

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

    private companion object {
        const val LOCATION_UPDATE_INTERVAL = 2000L // 2 seconds
        const val FASTEST_UPDATE_INTERVAL = 1000L // 1 second
        const val STEP_COMPLETION_DISTANCE = 50f // 50 meters
        const val REROUTE_DISTANCE_THRESHOLD = 50f // 50 meters (stricter for better detection)
        const val BEARING_UPDATE_THRESHOLD = 10f // 10 degrees
        const val MIN_REROUTE_INTERVAL = 30000L // 30 seconds between reroutes
        const val OFF_ROUTE_COUNT_THRESHOLD = 3 // Consecutive off-route detections
        const val ROUTE_POINT_SEARCH_RADIUS = 100f // meters
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTurnByTurnNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        }
    }

    private fun observeAppointment(appointmentId: Int) {
        lifecycleScope.launch {
            appointmentViewModel.appointmentUiState.collect { state ->
                when (state) {
                    is AppointmentUiState.AppointmentLoaded -> {
                        appointment = state.appointment
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
            topStatusBar.visibility = View.VISIBLE

            appointment?.let { appt ->
                tvDestinationName.text = appt.title
                tvAppointmentTime.text = formatAppointmentTime(appt)
                tvDestinationAddress.text = appt.location
            }

            val totalDistanceKm = routeResult.distanceMeters / 1000f
            val estimatedTimeMinutes = routeResult.duration

            tvRemainingTime.text = "${estimatedTimeMinutes}p"
            tvRemainingDistance.text = if (totalDistanceKm < 1) {
                "${routeResult.distanceMeters}m"
            } else {
                "${"%.1f".format(totalDistanceKm)}km"
            }

            val arrivalTimeMillis = System.currentTimeMillis() + (estimatedTimeMinutes * 60 * 1000)
            val arrivalTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            tvArrivalTime.text = arrivalTimeFormat.format(Date(arrivalTimeMillis))

            mainInstructionPanel.visibility = View.VISIBLE

            if (routeResult.steps.isNotEmpty()) {
                displayCurrentStep(routeResult.steps[0])
            }
        }
    }

    private fun displayRoute(routeResult: NavigationUtils.RouteResult) {
        // Clear existing overlays
        routePolyline?.remove()
        passedRoutePolyline?.remove()
        destinationMarker?.remove()
        userLocationMarker?.remove()

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
                .color(ContextCompat.getColor(this, R.color.color_blue))
                .geodesic(true)
        )

        // Create user location marker with custom icon
        currentLocation?.let { location ->
            createUserLocationMarker(location)

            val cameraPosition = CameraPosition.Builder()
                .target(LatLng(location.latitude, location.longitude))
                .zoom(18f)
                .bearing(location.bearing)
                .tilt(45f)
                .build()

            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }

    private fun createUserLocationMarker(location: Location) {
        userLocationMarker?.remove()

        // Create a custom marker with circle and direction arrow
        val userPosition = LatLng(location.latitude, location.longitude)

        userLocationMarker = googleMap.addMarker(
            MarkerOptions()
                .position(userPosition)
                .icon(createUserLocationIcon())
                .anchor(0.5f, 0.5f)
                .rotation(location.bearing)
                .flat(true)
        )
    }

    private fun createUserLocationIcon(): BitmapDescriptor {
        // Create a blue circle with white border for user location
        val size = 40
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // Draw white border
        val borderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, borderPaint)

        // Draw blue center
        val centerPaint = android.graphics.Paint().apply {
            color = ContextCompat.getColor(this@TurnByTurnNavigationActivity, R.color.color_blue)
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 3, centerPaint)

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

        val previousLocation = currentLocation
        currentLocation = location

        // Update user location marker
        updateUserLocationMarker(location)

        // Update camera
        updateNavigationCamera(location)

        // Update passed route visualization
        updatePassedRoute(location)

        // Check step completion
        checkStepCompletion(location)

        // Update distance and time
        updateDistancedAndTime(location)

        // Check for off-route and reroute if necessary
        if (previousLocation != null) {
            checkForReroute(location)
        }
    }

    private fun updateUserLocationMarker(location: Location) {
        userLocationMarker?.let { marker ->
            marker.position = LatLng(location.latitude, location.longitude)
            marker.rotation = location.bearing
        } ?: run {
            createUserLocationMarker(location)
        }
    }

    private fun updatePassedRoute(currentLocation: Location) {
        if (originalRoutePoints.isEmpty()) return

        val userLatLng = LatLng(currentLocation.latitude, currentLocation.longitude)
        val closestPointIndex = findClosestRoutePointIndex(userLatLng)

        if (closestPointIndex > currentRoutePointIndex) {
            currentRoutePointIndex = closestPointIndex

            // Update passed route polyline
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

        // Search within a reasonable range around current index
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

    private fun updateNavigationCamera(location: Location) {
        val cameraPosition = CameraPosition.Builder()
            .target(LatLng(location.latitude, location.longitude))
            .zoom(18f)
            .bearing(location.bearing)
            .tilt(45f)
            .build()

        googleMap.animateCamera(
            CameraUpdateFactory.newCameraPosition(cameraPosition),
            1000, null
        )
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

    private fun updateDistancedAndTime(currentLocation: Location) {
        appointment?.let { appt ->
            val destinationLocation = Location("destination").apply {
                latitude = appt.latitude
                longitude = appt.longitude
            }

            val remainingDistance = currentLocation.distanceTo(destinationLocation)
            val remainingDistanceKm = remainingDistance / 1000f

            binding.apply {
                tvRemainingDistance.text = if (remainingDistanceKm < 1) {
                    "${remainingDistance.toInt()}m"
                } else {
                    "${"%.1f".format(remainingDistanceKm)}km"
                }

                val estimatedTimeMinutes = (remainingDistanceKm / 0.5f).toInt()
                tvRemainingTime.text = "${estimatedTimeMinutes}p"

                val arrivalTime = System.currentTimeMillis() + (estimatedTimeMinutes * 60 * 1000)
                tvArrivalTime.text = SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(Date(arrivalTime))
            }
        }
    }

    private fun checkForReroute(currentLocation: Location) {
        if (isRerouting) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRerouteTime < MIN_REROUTE_INTERVAL) return

        // Check if user is off route
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

        binding.apply {
            tvCurrentInstruction.text = "Bạn đã đến nơi"
            tvStepDistance.text = ""
            tvNextInstruction.visibility = View.GONE
            btnStopNavigation.text = "Hoàn thành"

            announceInstruction("Bạn đã đến điểm hẹn")

            lifecycleScope.launch {
                appointment?.let { appt ->
                    // Update appointment status if needed
                }
            }
        }
    }

    private fun recenterCamera() {
        currentLocation?.let { location ->
            updateNavigationCamera(location)
        }
    }

    private fun toggleMute() {
        val iconRes = if (binding.btnMute.tag == "muted") {
            R.drawable.ic_volume
        } else {
            R.drawable.ic_volume_off
        }
        binding.btnMute.icon = ContextCompat.getDrawable(this, iconRes)
    }

    private fun stopNavigation() {
        isNavigationActive = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        finish()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale("vi", "VN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech.setLanguage(Locale.US)
            }
        }
    }

    private fun announceInstruction(instruction: String) {
        if (::textToSpeech.isInitialized) {
            textToSpeech.speak(instruction, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun formatAppointmentTime(appointment: AppointmentPlus): String {
        val startTime = SimpleDateFormat("HH:mm", Locale.getDefault())
            .format(Date(appointment.startDateTime))
        return "Cuộc hẹn lúc $startTime"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }

        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }

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
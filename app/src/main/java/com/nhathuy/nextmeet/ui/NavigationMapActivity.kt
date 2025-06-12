package com.nhathuy.nextmeet.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.adapter.RouteStepAdapter
import com.nhathuy.nextmeet.databinding.ActivityNavigationMapBinding
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.TransportMode
import com.nhathuy.nextmeet.resource.AppointmentUiState
import com.nhathuy.nextmeet.utils.Constant
import com.nhathuy.nextmeet.utils.Constant.LOCATION_PERMISSION_REQUEST_CODE
import com.nhathuy.nextmeet.viewmodel.AppointmentPlusViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.nhathuy.nextmeet.model.RouteStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class NavigationMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityNavigationMapBinding
    private lateinit var routeStepAdapter: RouteStepAdapter
    private var appointment: AppointmentPlus? = null

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var currentLocation: Location? = null
    private var selectedTransportMode = TransportMode.DRIVING
    private lateinit var okHttpClient: OkHttpClient

    private val appointmentViewModel: AppointmentPlusViewModel by viewModels()

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigationMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize OkHttpClient
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        // Initialize BottomSheetBehavior early
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        // Hide expandable content by default
        binding.expandableContent.visibility = View.GONE

        val appointmentId = intent.getIntExtra(Constant.EXTRA_APPOINTMENT_ID, -1)
        if (appointmentId == -1) {
            throw IllegalStateException("Appointment ID not found")
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        lifecycleScope.launch {
            appointmentViewModel.appointmentUiState.collect { state ->
                when (state) {
                    is AppointmentUiState.AppointmentLoaded -> {
                        appointment = state.appointment
                        setupUI()
                    }

                    is AppointmentUiState.Error -> {
                        // Handle error (show message, finish activity, etc.)
                        finish()
                    }

                    else -> {}
                }
            }
        }
        appointmentViewModel.getAppointmentById(appointmentId)

        setupMap()
        setupRecyclerView()
        setupClickListener()
    }

    // set up giao dien
    private fun setupUI() {
        appointment?.let { appt ->
            binding.apply {
                tvAppointmentTime.text = formatAppointmentTime(appointment!!)
                tvAppointmentTitle.text = appt.title
                tvDestinationAddress.text = appt.location
                Log.d("NavigationMapActivity", "Appointment: ${appt.location}")
            }
        }
    }

    // khoi tao map
    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    // khoi tao recycler view
    private fun setupRecyclerView() {
        routeStepAdapter = RouteStepAdapter()
        binding.rvRouteSteps.apply {
            adapter = routeStepAdapter
            layoutManager = LinearLayoutManager(this@NavigationMapActivity)
        }
    }

    // xu ly su kien click
    private fun setupClickListener() {
        binding.apply {
            // Set callback for Bottom Sheet
            bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            expandableContent.visibility = View.VISIBLE
                        }
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            expandableContent.visibility = View.GONE
                        }
                        else -> {}
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // Optional: animate expandableContent alpha if desired
                }
            })

            // Only expand/collapse when clicking the peek area, not the whole sheet
            peekContent.setOnClickListener {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                } else if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }

            // Mode chon phuong tien
            cardDriving.setOnClickListener { selectTransportMode(TransportMode.DRIVING) }
            cardWalking.setOnClickListener { selectTransportMode(TransportMode.WALKING) }
            cardTransit.setOnClickListener { selectTransportMode(TransportMode.TRANSIT) }

            //xu ly action button
            btnStartNavigation.setOnClickListener { startNavigation() }
            btnShareLocation.setOnClickListener { shareLocation() }

            //Fab
            fabMyLocation.setOnClickListener { moveToCurrentLocation() }
            fabClose.setOnClickListener { finish() }
        }
    }

    //format thoi gian cua cuoc hen
    private fun formatAppointmentTime(appointment: AppointmentPlus): String {
        val startTime = SimpleDateFormat("HH:mm", Locale.getDefault())
            .format(Date(appointment.startDateTime))
        val endTime = SimpleDateFormat("HH:mm", Locale.getDefault())
            .format(Date(appointment.endDateTime))
        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            .format(Date(appointment.startDateTime))
        return "$startTime - $endTime, $date"
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMyLocationButtonEnabled = false
        }
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        googleMap.isMyLocationEnabled = true
        getCurrentLocation()
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                currentLocation = it

                // lay vi tri hien tai cua nguoi dung
                val currentLatLng = LatLng(it.latitude, it.longitude)

                appointment?.let { appointment ->
                    //lay toa do diem hen cua contact
                    val destinationLatLng = LatLng(appointment.latitude, appointment.longitude)

                    //them market cho diem den
                    googleMap.addMarker(
                        MarkerOptions().position(destinationLatLng)
                            .title(appointment.title)
                            .snippet(appointment.location)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )

                    // tinh toan duong di
                    calculateRoute(currentLatLng, destinationLatLng)

                    // tạo một khoảng bao chứa cả điểm đêến và điểm đi
                    val boundsBuilder = LatLngBounds.Builder()
                    boundsBuilder.include(currentLatLng)
                    boundsBuilder.include(destinationLatLng)
                    val bounds = boundsBuilder.build()
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                }
            }
        }
    }

    // cho phuong tien di chuyen
    private fun selectTransportMode(mode: TransportMode) {
        selectedTransportMode = mode
        updateTransportModeUI(mode)

        val currentLatLng = currentLocation?.let { LatLng(it.latitude, it.longitude) }
        val destinationLatLng = appointment?.let { LatLng(it.latitude, it.longitude) }

        if (currentLatLng != null && destinationLatLng != null) {
            calculateRoute(currentLatLng, destinationLatLng)
        }
    }

    private fun updateTransportModeUI(mode: TransportMode) {
        binding.apply {
            // Reset tất cả về trạng thái không được chọn
            cardDriving.strokeWidth = 0
            cardWalking.strokeWidth = 0
            cardTransit.strokeWidth = 0

            // Highlight phương tiện được chọn
            when (mode) {
                TransportMode.DRIVING -> cardDriving.strokeWidth = 2
                TransportMode.WALKING -> cardWalking.strokeWidth = 2
                TransportMode.TRANSIT -> cardTransit.strokeWidth = 2
            }
        }
    }

    private fun calculateRoute(origin: LatLng, destination: LatLng) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = getRouteFromRoutesAPI(origin, destination, selectedTransportMode)
                withContext(Dispatchers.Main) {
                    if (result != null) {
                        displayRoute(result)
                        updateRouteInfo(result)
                    } else {
                        showError("Không thể tính toán tuyến đường")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Lỗi khi tính toán tuyến đường: ${e.message}")
                }
            }
        }
    }

    // Sử dụng Routes API mới
    private suspend fun getRouteFromRoutesAPI(
        origin: LatLng,
        destination: LatLng,
        transportMode: TransportMode
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
                    .addHeader("X-Goog-Api-Key", getString(R.string.google_map_api_key))
                    .addHeader("X-Goog-FieldMask", "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline,routes.legs.steps.navigationInstruction,routes.legs.steps.localizedValues,routes.legs.steps.polyline.encodedPolyline")
                    .build()

                val response = okHttpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        parseRouteResponse(responseBody)
                    } else null
                } else {
                    Log.e("NavigationMapActivity", "Routes API Error: ${response.code} - ${response.message}")
                    null
                }
            } catch (e: Exception) {
                Log.e("NavigationMapActivity", "Exception in getRouteFromRoutesAPI", e)
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

                // Parse steps for navigation instructions
                val steps = mutableListOf<RouteStep>()
                val legs = route.optJSONArray("legs")
                if (legs != null && legs.length() > 0) {
                    val leg = legs.getJSONObject(0)
                    val legSteps = leg.optJSONArray("steps")
                    if (legSteps != null) {
                        for (i in 0 until legSteps.length()) {
                            val step = legSteps.getJSONObject(i)
                            val navigationInstruction = step.optJSONObject("navigationInstruction")
                            val localizedValues = step.optJSONObject("localizedValues")

                            val instruction = navigationInstruction?.optString("instructions", "Tiếp tục") ?: "Tiếp tục"
                            val stepDistance = localizedValues?.optJSONObject("distance")?.optString("text", "") ?: ""
                            val stepDuration = localizedValues?.optJSONObject("staticDuration")?.optString("text", "") ?: ""

                            steps.add(RouteStep(
                                instruction = instruction,
                                distance = stepDistance,
                                duration = stepDuration,
                                iconResId = R.drawable.ic_straight
                            ))
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
            Log.e("NavigationMapActivity", "Error parsing route response", e)
        }
        return null
    }

    private fun parseDurationToMinutes(duration: String): Int {
        // Parse duration string like "1234s" to minutes
        return try {
            val seconds = duration.replace("s", "").toInt()
            (seconds / 60)
        } catch (e: Exception) {
            0
        }
    }

    // hien thị route
    private fun displayRoute(routeResult: RouteResult) {
        // Xóa các polyline trước đó
        googleMap.clear()

        // Thêm marker cho điểm đến
        appointment?.let {
            val destinationLatLng = LatLng(it.latitude, it.longitude)
            googleMap.addMarker(
                MarkerOptions()
                    .position(destinationLatLng)
                    .title(it.title)
                    .snippet(it.location)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
        }

        // Vẽ đường đi
        val path = decodePolyline(routeResult.encodedPolyline)
        val polylineOptions = PolylineOptions()
            .addAll(path)
            .width(8f)
            .color(ContextCompat.getColor(this, R.color.blue))
        googleMap.addPolyline(polylineOptions)

        // Cập nhật danh sách bước đi
        routeStepAdapter.updateSteps(routeResult.steps)
    }

    // cap nhat thong tin buoc di
    private fun updateRouteInfo(routeResult: RouteResult) {
        binding.apply {
            tvTravelTime.text = "${routeResult.duration} phút"
            tvDistance.text = "${String.format("%.1f", routeResult.distanceMeters / 1000.0)} km"

            // Tính toán thời gian đến nơi
            val arrivalTime = System.currentTimeMillis() + (routeResult.duration * 60 * 1000)
            tvArrivalTime.text = SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date(arrivalTime))

            // Hiển thị bottom sheet khi có thông tin route
            showRouteInfo()
        }
    }

    // Thêm phương thức để hiển thị thông tin route khi có dữ liệu
    private fun showRouteInfo() {
        // Khi có thông tin route, tự động mở rộng bottom sheet
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        // Đảm bảo expandable content hiển thị khi mở rộng
        binding.expandableContent.visibility = View.VISIBLE
    }

    /**
     * bat dau dieu huong
     */
    private fun startNavigation() {
        appointment?.let { appt ->
            // Start Google Maps navigation
            val uri = "google.navigation:q=${appt.latitude},${appt.longitude}&mode=${getNavigationMode()}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            intent.setPackage("com.google.android.apps.maps")

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                updateAppointmentNavigationStatus()
            } else {
                showError("Google Maps không được cài đặt")
            }
        }
    }

    // lay navigation mode
    private fun getNavigationMode(): String {
        return when (selectedTransportMode) {
            TransportMode.DRIVING -> "d"
            TransportMode.WALKING -> "w"
            TransportMode.TRANSIT -> "r"
        }
    }

    private fun updateAppointmentNavigationStatus() {
        lifecycleScope.launch(Dispatchers.IO) {
            // Update appointment to mark navigation as started
            // This would require an AppointmentRepository method
        }
    }

    //chia se vi tri location
    private fun shareLocation() {
        val shareText = appointment?.let {
            "Tôi đang đi đến: ${it.location}\n" +
                    "Cuộc hẹn: ${it.title}\n" +
                    "Thời gian: ${formatAppointmentTime(it)}\n" +
                    "Vị trí: https://maps.google.com/?q=${it.latitude},${it.longitude}"
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        startActivity(Intent.createChooser(shareIntent, "Chia sẻ vị trí"))
    }

    // di chuyen toi vi tri hien tai
    private fun moveToCurrentLocation() {
        currentLocation?.let { location ->
            val currentLatLng = LatLng(location.latitude, location.longitude)
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
        }
    }

    /**
     * giai ma chuoi polyline da duoc encode thanh danh sach cac diem latlng
     * dung thuat toan polyline encoding
     */
    private fun decodePolyline(encoded: String): List<LatLng> {
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

    //hien thi thong bao loi
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Toast.LENGTH_LONG).show()
        Log.d("NavigationMapActivity", message)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocation()
                } else {
                    showError("Cần quyền vị trí để sử dụng tính năng điều hướng")
                }
            }
        }
    }

    // Data class để lưu kết quả route
    data class RouteResult(
        val duration: Int, // minutes
        val distanceMeters: Int,
        val encodedPolyline: String,
        val steps: List<RouteStep>
    )
}
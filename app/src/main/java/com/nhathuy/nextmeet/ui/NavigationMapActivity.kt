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

        val appointmentId = intent.getIntExtra(Constant.EXTRA_APPOINTMENT_ID, -1)
        if (appointmentId == -1) {
            throw IllegalStateException("Appointment ID not found")
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize bottom sheet behavior
        setupBottomSheet()

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

    // Setup UI với thông tin appointment
    private fun setupUI() {
        appointment?.let { appt ->
            binding.apply {
                tvAppointmentTime.text = formatAppointmentTime(appt)
                tvAppointmentTitle.text = appt.title
                tvDestinationAddress.text = appt.location
                Log.d("NavigationMapActivity", "Appointment: ${appt.location}")
            }
        }

        // Mặc định chọn phương tiện lái xe
        updateTransportModeUI(TransportMode.DRIVING)
    }

    // Khởi tạo map
    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    // Khởi tạo recycler view
    private fun setupRecyclerView() {
        routeStepAdapter = RouteStepAdapter()
        binding.rvRouteSteps.apply {
            adapter = routeStepAdapter
            layoutManager = LinearLayoutManager(this@NavigationMapActivity)
        }
    }

    // Setup BottomSheet behavior theo layout design
    private fun setupBottomSheet() {
        try {
            bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)

            // Thiết lập behavior theo layout - peek height = 110dp, không hideable
            bottomSheetBehavior.apply {
                peekHeight = resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek_height) // 110dp
                isHideable = false
                isFitToContents = false // vi tri trung gian
                halfExpandedRatio = 0.4f // vi tri 40% chieu cao man hinh
                state = BottomSheetBehavior.STATE_COLLAPSED
            }

            // Ẩn expandable content khi collapsed
            binding.expandableContent.visibility = View.GONE

            // Setup callback để handle state changes
            bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            binding.expandableContent.visibility = View.VISIBLE
                        }
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            binding.expandableContent.visibility = View.GONE
                        }
                        BottomSheetBehavior.STATE_DRAGGING -> {
                            // User đang kéo bottom sheet
                        }
                        BottomSheetBehavior.STATE_SETTLING -> {
                            // Bottom sheet đang settle vào vị trí cuối
                        }
                        else -> {}
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // Có thể thêm animation cho expandable content dựa trên slideOffset
                    // slideOffset: 0 = collapsed, 1 = expanded
                    binding.expandableContent.alpha = slideOffset
                }
            })

        } catch (e: Exception) {
            Log.e("NavigationMapActivity", "Error setting up BottomSheet", e)
        }
    }

    // Xử lý sự kiện click
    private fun setupClickListener() {
        binding.apply {
            // Click vào peek content để expand/collapse bottom sheet
            peekContent.setOnClickListener {
                toggleBottomSheet()
            }

            // Chọn phương tiện di chuyển
            cardDriving.setOnClickListener { selectTransportMode(TransportMode.DRIVING) }
            cardWalking.setOnClickListener { selectTransportMode(TransportMode.WALKING) }
            cardTransit.setOnClickListener { selectTransportMode(TransportMode.TRANSIT) }

            // Action buttons
            btnStartNavigation.setOnClickListener { startNavigation() }
            buttonShare.setOnClickListener { shareLocation() }

            // FAB buttons - Fix tên theo layout
            buttonMyLocation.setOnClickListener { moveToCurrentLocation() }
            buttonClose.setOnClickListener { finish() }
        }
    }

    // Toggle bottom sheet state
    private fun toggleBottomSheet() {
        if (::bottomSheetBehavior.isInitialized) {
            when (bottomSheetBehavior.state) {
                BottomSheetBehavior.STATE_COLLAPSED -> {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
                BottomSheetBehavior.STATE_EXPANDED -> {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
                else -> {
                    // Nếu đang dragging hoặc settling, set về collapsed
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
        }
    }

    // Format thời gian của cuộc hẹn
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
            isZoomControlsEnabled = false // Tắt zoom controls vì có FAB buttons
            isCompassEnabled = false // Tắt compass vì có compass button riêng
            isMyLocationButtonEnabled = false // Tắt vì có FAB my location riêng
        }
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
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

                // Lấy vị trí hiện tại của người dùng
                val currentLatLng = LatLng(it.latitude, it.longitude)

                appointment?.let { appointment ->
                    // Lấy tọa độ điểm hẹn
                    val destinationLatLng = LatLng(appointment.latitude, appointment.longitude)

                    // Thêm marker cho điểm đến
                    googleMap.addMarker(
                        MarkerOptions().position(destinationLatLng)
                            .title(appointment.title)
                            .snippet(appointment.location)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )

                    // Tính toán đường đi
                    calculateRoute(currentLatLng, destinationLatLng)

                    // Tạo bounds chứa cả hai điểm
                    val boundsBuilder = LatLngBounds.Builder()
                    boundsBuilder.include(currentLatLng)
                    boundsBuilder.include(destinationLatLng)
                    val bounds = boundsBuilder.build()

//                    // Padding để tránh che bởi bottom sheet (110dp peek height)
//                    val padding = resources.getDimensionPixelSize(R.dimen.map_padding) // ~150dp
//                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                }
            }
        }
    }

    // Chọn phương tiện di chuyển
    private fun selectTransportMode(mode: TransportMode) {
        selectedTransportMode = mode
        updateTransportModeUI(mode)

        // Tính lại route với phương tiện mới
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

            // Highlight phương tiện được chọn với stroke color blue
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
                        // Tự động expand bottom sheet khi có thông tin route
                        expandBottomSheetWithRouteInfo()
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

    // Expand bottom sheet khi có thông tin route
    private fun expandBottomSheetWithRouteInfo() {
        if (::bottomSheetBehavior.isInitialized) {
            // Delay một chút để đảm bảo UI đã update
            binding.root.postDelayed({
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }, 300)
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
                                iconResId = getDirectionIcon(instruction)
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
    //them method de xu ly icon directions
    private fun getDirectionIcon(instruction:String):Int{
        return when {
            instruction.contains("rẽ trái", ignoreCase = true) ||
                    instruction.contains("turn left", ignoreCase = true) -> R.drawable.ic_turn_left
            instruction.contains("rẽ phải", ignoreCase = true) ||
                    instruction.contains("turn right", ignoreCase = true) -> R.drawable.ic_turn_right
            instruction.contains("nhẹ trái", ignoreCase = true) ||
                    instruction.contains("slight left", ignoreCase = true) -> R.drawable.ic_turn_slight_left
            instruction.contains("nhẹ phải", ignoreCase = true) ||
                    instruction.contains("slight right", ignoreCase = true) -> R.drawable.ic_turn_slight_right
            else -> R.drawable.ic_straight
        }
    }

    // Hiển thị route trên map
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
            .geodesic(true)
        googleMap.addPolyline(polylineOptions)

        // Cập nhật danh sách bước đi
        routeStepAdapter.updateSteps(routeResult.steps)
    }

    // Cập nhật thông tin route
    private fun updateRouteInfo(routeResult: RouteResult) {
        binding.apply {
            tvTravelTime.text = "${routeResult.duration} phút"
            tvDistance.text = "${String.format("%.1f", routeResult.distanceMeters / 1000.0)} km"

            // Tính toán thời gian đến nơi
            val arrivalTime = System.currentTimeMillis() + (routeResult.duration * 60 * 1000)
            tvArrivalTime.text = SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date(arrivalTime))
        }
    }

    // Bắt đầu điều hướng
    private fun startNavigation() {
        appointment?.let { appt ->
            // Start TurnByTurnNavigationActivity instead of Google Maps
            val intent = Intent(this, TurnByTurnNavigationActivity::class.java)
            intent.putExtra(Constant.EXTRA_APPOINTMENT_ID, appt.id)
            startActivity(intent)
            updateAppointmentNavigationStatus()
        }
    }

    private fun updateAppointmentNavigationStatus() {
        lifecycleScope.launch(Dispatchers.IO) {
            // Update appointment to mark navigation as started
            // This would require an AppointmentRepository method
        }
    }

    // Chia sẻ vị trí location
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

    // Di chuyển tới vị trí hiện tại
    private fun moveToCurrentLocation() {
        currentLocation?.let { location ->
            val currentLatLng = LatLng(location.latitude, location.longitude)
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
        } ?: run {
            showError("Không thể xác định vị trí hiện tại")
        }
    }

    /**
     * Giải mã chuỗi polyline đã được encode thành danh sách các điểm LatLng
     * sử dụng thuật toán polyline encoding
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

    // Hiển thị thông báo lỗi
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        Log.e("NavigationMapActivity", message)
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

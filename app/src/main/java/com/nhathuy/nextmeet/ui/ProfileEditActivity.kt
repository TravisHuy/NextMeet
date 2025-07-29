package com.nhathuy.nextmeet.ui

import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ActivityProfileEditBinding
import com.nhathuy.nextmeet.model.User
import com.nhathuy.nextmeet.model.ValidationResult
import com.nhathuy.nextmeet.resource.Resource
import com.nhathuy.nextmeet.ui.AddAppointmentActivity
import com.nhathuy.nextmeet.utils.Constant
import com.nhathuy.nextmeet.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class ProfileEditActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileEditBinding
    private val userViewModel: UserViewModel by viewModels()

    private var userId: Int = 0
    private var location: String? = null
    private var latitude: Double? = null
    private var longitude: Double? = null

    private var isLocationFromMap = false

    // Geocoding
    private var geocodingJob: Job? = null
    private var geocodingCache = mutableMapOf<String, Pair<Double?, Double?>>()

    // Activity Result Launcher
    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                val selectedAddress = data.getStringExtra(GoogleMapActivity.EXTRA_SELECTED_ADDRESS)
                val selectedLat = data.getDoubleExtra(GoogleMapActivity.EXTRA_SELECTED_LAT, 0.0)
                val selectedLng = data.getDoubleExtra(GoogleMapActivity.EXTRA_SELECTED_LNG, 0.0)

                isLocationFromMap = true

                location = selectedAddress
                latitude = selectedLat
                longitude = selectedLng

                if (!selectedAddress.isNullOrEmpty()) {
                    binding.etEditUserLocation.setText(location)
                    binding.tilEditUserLocation.helperText = getString(R.string.location_found_from_map)
                    Log.d(
                        "ProfileEditActivity",
                        "Selected from map: $selectedAddress at ($selectedLat, $selectedLng)"
                    )
                } else {
                    binding.etEditUserLocation.setText(getString(R.string.no_location_selected))
                    binding.tilEditUserLocation.helperText = getString(R.string.no_location_selected)
                    location = ""
                    latitude = null
                    longitude = null
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            enableEdgeToEdge()
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        initializeFromIntent()
        setupObservers()
        setupUI()
    }

    private fun initializeFromIntent() {
        userId = intent.getIntExtra(Constant.EXTRA_USER_ID, -1)
    }

    private fun setupObservers() {
        userViewModel.getCurrentUser().observe(this) { user ->
            user?.let {
                location = it.defaultAddress
                latitude = it.defaultLatitude
                longitude = it.defaultLongitude

                Log.d("ProfileEditActivity", "Current user id: ${it.id}")
                displayUserInfo(it)
            }
        }

        lifecycleScope.launch {
            userViewModel.userEditFormState.collect { validationResults ->
                displayValidationResults(validationResults)
            }
        }

        lifecycleScope.launch {
            userViewModel.updateState.collect { state ->
                state?.let {
                    when (it) {
                        is Resource.Loading -> {
                            showLoading(true)
                        }

                        is Resource.Success -> {
                            showLoading(false)
                            Toast.makeText(
                                this@ProfileEditActivity,
                                getString(R.string.update_success),
                                Toast.LENGTH_SHORT
                            ).show()
                            userViewModel.resetUpdateState()
                            delay(1000)
                            finish()
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        }

                        is Resource.Error -> {
                            showLoading(false)

                            Toast.makeText(
                                this@ProfileEditActivity,
                                getString(R.string.update_error,it.message),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun displayValidationResults(validationResults: Map<String, ValidationResult>) {
        validationResults["name"]?.let { result ->
            binding.tilEditUserFullName.error = if (result.isValid) null else result.errorMessage
        }
        validationResults["email"]?.let { result ->
            binding.tilEditUserEmail.error = if (result.isValid) null else result.errorMessage
        }
        validationResults["phone"]?.let { result ->
            binding.tilEditUserPhone.error = if (result.isValid) null else result.errorMessage
        }
    }

    private fun setupUI() {
        setupLocationInput()
        setupClickListeners()
    }

    //hiển thị thông tin trong edit user
    private fun displayUserInfo(user: User) {
        with(binding) {
            etEditUserFullName.setText(user.name)
            etEditUserPhone.setText(user.phone)
            etEditUserEmail.setText(user.email)
            etEditUserLocation.setText(user.defaultAddress)

            if (!user.defaultAddress.isNullOrEmpty()) {
                tilEditUserLocation.helperText = getString(R.string.location_current)
            } else {
                tilEditUserLocation.helperText =
                    getString(R.string.location_hint_default)
            }
        }
    }

    private fun setupLocationInput() {
        binding.tilEditUserLocation.apply {
            helperText = getString(R.string.location_hint_default)
            setEndIconDrawable(R.drawable.ic_geo)
            setEndIconContentDescription(getString(R.string.map_picker_description))

            setEndIconOnClickListener {
                val intent = Intent(this@ProfileEditActivity, GoogleMapActivity::class.java).apply {
                    if (latitude != null && longitude != null) {
                        putExtra(GoogleMapActivity.EXTRA_SELECTED_LAT, latitude)
                        putExtra(GoogleMapActivity.EXTRA_SELECTED_LNG, longitude)
                        putExtra(GoogleMapActivity.EXTRA_SELECTED_ADDRESS, location)
                    }
                }
                mapPickerLauncher.launch(intent)
            }
        }

        binding.etEditUserLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val manualLocation = s?.toString()?.trim()

                // Skip geocoding if location is from map selection
                if (isLocationFromMap) {
                    isLocationFromMap = false
                    return
                }

                geocodingJob?.cancel()

                if (!manualLocation.isNullOrEmpty() && manualLocation.length >= 3) {
                    location = manualLocation

                    geocodingJob = lifecycleScope.launch {
                        delay(800)
                        binding.tilEditUserLocation.helperText = getString(R.string.location_searching)

                        geocodeAddress(manualLocation) { lat, lng ->
                            if (lat != null && lng != null) {
                                latitude = lat
                                longitude = lng
                                binding.tilEditUserLocation.helperText = getString(R.string.location_found)
                                Log.d(
                                    "Geocoding",
                                    "Found coordinates: $lat, $lng for address: $manualLocation"
                                )
                            } else {
                                latitude = null
                                longitude = null
                                binding.tilEditUserLocation.helperText =
                                    getString(R.string.location_not_found)
                                Log.d(
                                    "Geocoding",
                                    "No coordinates found for address: $manualLocation"
                                )
                            }
                        }
                    }
                } else if (manualLocation.isNullOrEmpty()) {
                    location = null
                    latitude = null
                    longitude = null
                    binding.tilEditUserLocation.helperText =
                        getString(R.string.location_hint_default)
                    geocodingJob?.cancel()
                } else {
                    location = manualLocation
                    latitude = null
                    longitude = null
                    binding.tilEditUserLocation.helperText = getString(R.string.location_enter_more)
                }
            }
        })
    }

    private fun geocodeAddress(address: String, callback: (Double?, Double?) -> Unit) {
        if (address.isBlank() || address.length < 3) {
            callback(null, null)
            return
        }

        val cachedResult = geocodingCache[address]
        if (cachedResult != null) {
            callback(cachedResult.first, cachedResult.second)
            return
        }

        try {
            val geocoder = Geocoder(this, Locale.getDefault())

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocationName(address, 1) { addresses ->
                    handleGeocodingResult(address, addresses, callback)
                }
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocationName(address, 1)
                        launch(Dispatchers.Main) {
                            handleGeocodingResult(address, addresses, callback)
                        }
                    } catch (e: Exception) {
                        Log.e("Geocoding", "Error geocoding address: ${e.message}")
                        launch(Dispatchers.Main) {
                            callback(null, null)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Geocoding", "Geocoder error: ${e.message}")
            callback(null, null)
        }
    }

    private fun handleGeocodingResult(
        address: String,
        addresses: List<android.location.Address>?,
        callback: (Double?, Double?) -> Unit
    ) {
        if (!addresses.isNullOrEmpty()) {
            val location = addresses[0]
            val result = Pair(location.latitude, location.longitude)
            geocodingCache[address] = result
            callback(location.latitude, location.longitude)
        } else {
            geocodingCache[address] = Pair(null, null)
            callback(null, null)
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
        binding.btnCancel.setOnClickListener {
            onBackPressed()
        }
        binding.btnSave.setOnClickListener {
            showFormEditUserInfo()
        }
    }

    private fun showLoading(isLoading: Boolean) {
//        binding.editProfileProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showFormEditUserInfo() {
        val name = binding.etEditUserFullName.text.toString().trim()
        val phone = binding.etEditUserPhone.text.toString().trim()
        val email = binding.etEditUserEmail.text.toString().trim()
        val userLocation = location ?: ""

        val user = User(
            id = userId,
            name = name,
            phone = phone,
            email = email,
            defaultAddress = userLocation,
            defaultLatitude = latitude ?: 0.0,
            defaultLongitude = longitude ?: 0.0
        )

        userViewModel.validateAndUpdateUser(user)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
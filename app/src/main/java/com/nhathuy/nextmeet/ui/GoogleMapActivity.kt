package com.nhathuy.nextmeet.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ActivityGoogleMapBinding
import java.io.IOException
import java.util.Locale

class GoogleMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityGoogleMapBinding
    private lateinit var mMap: GoogleMap
    private lateinit var searchBar: SearchBar
    private lateinit var searchView: SearchView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var selectedLatLng: LatLng? = null
    private var selectAddress: String? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        const val EXTRA_SELECTED_ADDRESS = "selected_address"
        const val EXTRA_SELECTED_LAT = "selected_lat"
        const val EXTRA_SELECTED_LNG = "selected_lng"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoogleMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeMap()
        initializeFusedLocation()
        setupSearchView()
        setupSaveButton()
    }

    private fun initializeMap() {
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragmentContainer) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun initializeFusedLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setupSearchView() {
        searchView.setupWithSearchBar(searchBar)
        searchView.editText.setOnEditorActionListener { _, _, _ ->
            val query = searchView.text.toString()
            searchAddress(query)
            searchView.hide()
            true
        }
    }

    private fun searchAddress(locationName: String) {
        val geocoder = Geocoder(this, Locale.getDefault())

        // for android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocationName(locationName, 1) { address ->
                handleFoundAddress(address, locationName)
            }
        } else {
            try {
                val addressList = geocoder.getFromLocationName(locationName, 1)
                handleFoundAddress(addressList, locationName)
            } catch (e: IOException) {
                Toast.makeText(this, "Lỗi khi tìm kiếm địa chỉ", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun setupSaveButton() {
       binding.btnConfirmLocation.setOnClickListener{
           if(selectedLatLng!=null){
               val resultIntent = Intent().apply {
                   putExtra(EXTRA_SELECTED_ADDRESS,selectAddress)
                   putExtra(EXTRA_SELECTED_LAT,selectedLatLng!!.latitude)
                   putExtra(EXTRA_SELECTED_LNG,selectedLatLng!!.longitude)
               }
               setResult(RESULT_OK,resultIntent)
               finish()
           }
           else{
               Toast.makeText(this,getString(R.string.please_select_location),Toast.LENGTH_SHORT).show()
           }
       }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true

        mMap.setOnMapClickListener { latLng ->
            mMap.clear()
            selectedLatLng = latLng
            mMap.addMarker(MarkerOptions().position(latLng))

            getAddressFromLatLng(latLng)
        }

        //check for location permission
        if (hasLocationPermission()) {
            enableMyLocation()
        } else {
            requestLocationPermission()
        }

    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = false
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun getAddressFromLatLng(latLng: LatLng) {
        val geocoder = Geocoder(this, Locale.getDefault())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { address ->
                handleReverseGeocodingResult(address, latLng)
            }
        } else {
            try {
                val address = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                handleReverseGeocodingResult(address, latLng)
            } catch (e: IOException) {
                handleGeocoderError(latLng)
            }
        }
    }

    private fun handleFoundAddress(addressList: List<Address>?, locationName: String) {
        runOnUiThread {
            if (addressList != null && addressList.isNotEmpty()) {
                val address = addressList[0]
                val latLng = LatLng(address.latitude, address.longitude)

                mMap.clear()
                selectedLatLng = latLng
                mMap.addMarker(MarkerOptions().position(latLng).title(address.getAddressLine(0)))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                selectAddress = address.getAddressLine(0) ?: ""

                binding.tvSelectedAddress.text = selectAddress
            } else {
                Toast.makeText(this, getString(R.string.location_not_found), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun handleReverseGeocodingResult(addresses: List<Address>?, latLng: LatLng) {
        runOnUiThread {
            if (addresses != null && addresses.isNotEmpty()) {
                val address: Address = addresses[0]
                selectAddress = address.getAddressLine(0) ?: ""
                binding.tvSelectedAddress.text = selectAddress
            } else {
                handleGeocoderError(latLng)
            }
        }
    }

    private fun handleGeocoderError(latLng: LatLng) {
        binding.tvSelectedAddress.text = getString(R.string.address_not_found)
        selectAddress = "${latLng.latitude}, ${latLng.longitude}"
    }

}
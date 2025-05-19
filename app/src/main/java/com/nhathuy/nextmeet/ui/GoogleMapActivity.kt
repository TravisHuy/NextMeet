package com.nhathuy.nextmeet.ui

import android.content.Intent
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
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
import java.io.IOException
import java.util.Locale

class GoogleMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var searchBar: SearchBar
    private lateinit var searchView: SearchView
    private lateinit var geocoder: Geocoder
    private var currentMarker: Marker? = null
    private lateinit var saveButton: Button
    private var currentAddress: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_map)


        searchBar = findViewById(R.id.searchBar)
        searchView = findViewById(R.id.searchView)
        saveButton = findViewById(R.id.saveButton)
        geocoder = Geocoder(this, Locale.getDefault())



        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragmentContainer) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupSearchView()
        setupSaveButton()
    }

    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            currentAddress?.let { address ->
                val intent = Intent()
                intent.putExtra("address", address)
                setResult(RESULT_OK, intent)
                finish()
            } ?: run {
                Toast.makeText(this, "Please choose an address before saving", Toast.LENGTH_SHORT).show()
            }
        }
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

    private fun searchAddress(address: String) {
        try {
            val addresses = geocoder.getFromLocationName(address, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val location = addresses[0]
                val latLng = LatLng(location.latitude, location.longitude)
                updateMap(latLng, address)
            } else {
                Toast.makeText(this, "Không tìm thấy địa chỉ", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Lỗi khi tìm kiếm địa chỉ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateMap(latLng: LatLng, title: String) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

        currentMarker?.remove()  // Remove the old marker
        currentMarker = mMap.addMarker(MarkerOptions().position(latLng).title(title))

        searchBar.text = title

        currentAddress = title
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Vị trí mặc định: Hồ Chí Minh
        val defaultLatLng = LatLng(10.8231, 106.6297)
        updateMap(defaultLatLng, "Hồ Chí Minh")

        // Thiết lập listener cho sự kiện click trên bản đồ
        mMap.setOnMapClickListener { latLng ->
            try {
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val addressText = address.getAddressLine(0) ?: "Vị trí không xác định"
                    updateMap(latLng, addressText)
                }
            } catch (e: IOException) {
                Toast.makeText(this, "Lỗi khi lấy địa chỉ cho vị trí này", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
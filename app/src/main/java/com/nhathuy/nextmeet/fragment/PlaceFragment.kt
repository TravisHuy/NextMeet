package com.nhathuy.nextmeet.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nhathuy.nextmeet.databinding.FragmentPlaceBinding
import com.nhathuy.nextmeet.ui.GoogleMapActivity

class PlaceFragment : Fragment() {

    private var _binding: FragmentPlaceBinding? = null
    private val binding get() = _binding!!


    private val GOOGLE_MAP_REQUEST_CODE = 1001


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.geo.setOnClickListener {
            showMap()
        }


    }
    private fun showMap() {
        val intent = Intent(context, GoogleMapActivity::class.java)
        startActivityForResult(intent, GOOGLE_MAP_REQUEST_CODE)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_MAP_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val address = data?.getStringExtra("address")
            address?.let {
                binding.addressTextView.text = it
                binding.addressTextView.visibility = View.VISIBLE
            }
        }
    }

    //selectAddress
    fun getSelectAddress():String{
        return binding.addressTextView.text.toString()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

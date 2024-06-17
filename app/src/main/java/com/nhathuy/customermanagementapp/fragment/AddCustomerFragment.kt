package com.nhathuy.customermanagementapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nhathuy.customermanagementapp.R
import com.nhathuy.customermanagementapp.databinding.FragmentAddCustomerBinding

class AddCustomerFragment : Fragment() {
    private lateinit var binding:FragmentAddCustomerBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding= FragmentAddCustomerBinding.inflate(layoutInflater)









        return binding.root
    }


}
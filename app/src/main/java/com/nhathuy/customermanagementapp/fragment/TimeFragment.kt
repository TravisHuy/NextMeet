package com.nhathuy.customermanagementapp.fragment

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.nhathuy.customermanagementapp.R
import com.nhathuy.customermanagementapp.databinding.FragmentTimeBinding
import java.util.Calendar


class TimeFragment : Fragment() {

    private var _binding: FragmentTimeBinding ?= null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding=FragmentTimeBinding.inflate(inflater,container,false)
        val view=binding.root

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.day_options,
            android.R.layout.simple_spinner_item
        ).also {
            adapter->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerDay.adapter=adapter
        }



        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.hour_options,
            android.R.layout.simple_spinner_item
        ).also {
                adapter->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerHour.adapter=adapter
        }

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.repeat_options,
            android.R.layout.simple_spinner_item
        ).also {
                adapter->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerAgain.adapter=adapter
        }


        setupSpinnerListeners()


        return view
    }

    private fun setupSpinnerListeners() {
        binding.spinnerDay.onItemSelectedListener= object :AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                if(position==3){
                    val calendar=Calendar.getInstance()
                    DatePickerDialog(requireContext(),{
                            _,year,month,dayOfMonth->
                    },calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

        }
        binding.spinnerHour.onItemSelectedListener= object :AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                if(position==4){
                    val calendar=Calendar.getInstance()
                    TimePickerDialog(requireContext(),{
                            _,hourOfDay,minute->
                    },calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),true).show()
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

        }
    }

}
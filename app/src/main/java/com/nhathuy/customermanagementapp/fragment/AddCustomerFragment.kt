package com.nhathuy.customermanagementapp.fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.nhathuy.customermanagementapp.R
import com.nhathuy.customermanagementapp.databinding.FragmentAddCustomerBinding
import com.nhathuy.customermanagementapp.model.Customer
import com.nhathuy.customermanagementapp.viewmodel.CustomerViewModel

class AddCustomerFragment : Fragment() {
    private lateinit var binding:FragmentAddCustomerBinding
    private lateinit var customerViewModel: CustomerViewModel
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding= FragmentAddCustomerBinding.inflate(layoutInflater)

        customerViewModel=ViewModelProvider(this).get(CustomerViewModel::class.java)
        sharedPreferences=requireActivity().getSharedPreferences("user_id",Context.MODE_PRIVATE)

        binding.btnSubmit.setOnClickListener {
            onAddCustomer()
        }

        return binding.root
    }

    private fun onAddCustomer() {
        val name=binding.edAddCustomerName.text.toString()
        val phone=binding.edAddCustomerPhone.text.toString()
        val email=binding.edAddCustomerEmail.text.toString()
        val address=binding.edAddCustomerAddress.text.toString()
        val group=binding.edAddCustomerGroup.text.toString()
        val notes=binding.edAddCustomerNotes.text.toString()


        if(name.isEmpty()||phone.isEmpty()||email.isEmpty()||address.isEmpty()||group.isEmpty()||notes.isEmpty()){
            Toast.makeText(context,getString(R.string.all_fields_are_required),Toast.LENGTH_LONG).show()
            return
        }
        else if(phone.length!=10){
           binding.edAddCustomerPhone.error=getString(R.string.error_phone)
            return
        }
        else if(name.length>25){
            binding.edAddCustomerName.error=getString(R.string.error_name)
            return
        }

        val userId=sharedPreferences.getInt("user_id",-1);

        if(userId==-1){
            Toast.makeText(context,"User not logged in",Toast.LENGTH_LONG).show()
            return
        }

        val customer=Customer(userId=userId,name = name, phone = phone, email = email, address = address, group = group, notes = notes)
        if(customer!=null){
            customerViewModel.register(customer)
            Toast.makeText(context,"Customer add successfully",Toast.LENGTH_SHORT).show()
        }
        else{
            Toast.makeText(context,"Customer add failed",Toast.LENGTH_LONG).show()
        }

        binding.edAddCustomerName.text?.clear()
        binding.edAddCustomerPhone.text?.clear()
        binding.edAddCustomerEmail.text?.clear()
        binding.edAddCustomerAddress.text?.clear()
        binding.edAddCustomerGroup.text?.clear()
        binding.edAddCustomerNotes.text?.clear()
    }


}
package com.nhathuy.customermanagementapp.fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nhathuy.customermanagementapp.R
import com.nhathuy.customermanagementapp.databinding.FragmentAddCustomerBinding
import com.nhathuy.customermanagementapp.model.Customer
import com.nhathuy.customermanagementapp.resource.Resource
import com.nhathuy.customermanagementapp.viewmodel.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import javax.inject.Inject

@AndroidEntryPoint
class AddCustomerFragment : Fragment() {
    private lateinit var binding: FragmentAddCustomerBinding

    private lateinit var sharedPreferences: SharedPreferences

    private  val customerViewModel: CustomerViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddCustomerBinding.inflate(layoutInflater)

        setupObserViewModel()

        sharedPreferences = requireActivity().getSharedPreferences("user_id", Context.MODE_PRIVATE)

        binding.btnSubmit.setOnClickListener {
            onAddCustomer()
        }

        (activity as AppCompatActivity).supportActionBar?.hide()

        return binding.root
    }

    private fun setupObserViewModel(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                customerViewModel.addCustomerState.collect{
                        result ->
                    when(result) {
                        is Resource.Loading -> {

                        }
                        is Resource.Success -> {
                            if(result.data == true) {
                                Toast.makeText(
                                    requireContext(),
                                    "Error updated customer successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        is Resource.Error -> {
                            Toast.makeText(
                                requireContext(),
                                "Error updating customer: ${result.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }
    private fun onAddCustomer() {
        val name = binding.edAddCustomerName.text.toString()
        val phone = binding.edAddCustomerPhone.text.toString()
        val email = binding.edAddCustomerEmail.text.toString()
        val address = binding.edAddCustomerAddress.text.toString()
        val group = binding.edAddCustomerGroup.text.toString()
        val notes = binding.edAddCustomerNotes.text.toString()

        if (!validateInputs(name, phone, email, address, group, notes)) {
            return
        }
        val userId = sharedPreferences.getInt("user_id", -1);

        if (userId == -1) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_LONG).show()
            return
        }

        val customer = Customer(
            userId = userId,
            name = name,
            phone = phone,
            email = email,
            address = address,
            group = group,
            notes = notes
        )
        if (customer != null) {
            customerViewModel.register(customer)
            Toast.makeText(context, "Customer add successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Customer add failed", Toast.LENGTH_LONG).show()
        }

        clearInputs()
    }

    private fun validateInputs(
        name: String,
        phone: String,
        email: String,
        address: String,
        group: String,
        notes: String
    ): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            binding.addCustomerNameLayout.error = getString(R.string.enter_name)
            isValid = false
        } else if (name.length > 25) {
            binding.addCustomerNameLayout.error = getString(R.string.error_name)
            isValid = false
        } else {
            binding.addCustomerNameLayout.error = null
        }

        if (phone.isEmpty()) {
            binding.addCustomerPhoneLayout.error = getString(R.string.enter_phone)
            isValid = false
        } else if (phone.length != 10) {
            binding.addCustomerPhoneLayout.error = getString(R.string.error_phone)
            isValid = false
        } else {
            binding.addCustomerPhoneLayout.error = null
        }

        if (email.isEmpty()) {
            binding.addCustomerEmailLayout.error = getString(R.string.enter_email)
            isValid = false
        } else if (!isValidEmail(email)) {
            binding.addCustomerEmailLayout.error = getString(R.string.email_invalid)
            isValid = false
        } else {
            binding.addCustomerEmailLayout.error = null
        }

        if (address.isEmpty()) {
            binding.addCustomerAddressLayout.error = getString(R.string.enter_address)
            isValid = false
        } else {
            binding.addCustomerAddressLayout.error = null
        }

        if (group.isEmpty()) {
            binding.addCustomerGroupLayout.error = getString(R.string.enter_group)
            isValid = false
        } else {
            binding.addCustomerGroupLayout.error = null
        }

        if (notes.isEmpty()) {
            binding.addCustomerNotesLayout.error = getString(R.string.enter_notes)
            isValid = false
        } else {
            binding.addCustomerNotesLayout.error = null
        }

        return isValid
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun clearInputs() {
        binding.edAddCustomerName.text?.clear()
        binding.edAddCustomerPhone.text?.clear()
        binding.edAddCustomerEmail.text?.clear()
        binding.edAddCustomerAddress.text?.clear()
        binding.edAddCustomerGroup.text?.clear()
        binding.edAddCustomerNotes.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as AppCompatActivity).supportActionBar?.show()
    }
}
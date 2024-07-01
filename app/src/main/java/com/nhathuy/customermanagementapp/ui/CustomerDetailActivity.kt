package com.nhathuy.customermanagementapp.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.nhathuy.customermanagementapp.R
import com.nhathuy.customermanagementapp.databinding.ActivityCustomerDetailBinding
import com.nhathuy.customermanagementapp.model.Customer

class CustomerDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityCustomerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val customer: Customer? = intent.getParcelableExtra("Customer_extra")
        customer?.let {
            displayCustomerDetail(it)
        }

        binding.arrowLeft.setOnClickListener {
            onBackPressed()
        }
    }

    private fun displayCustomerDetail(customer: Customer) {
        binding.editName.text=customer.name
        binding.editPhone.text=customer.phone
        binding.editEmail.text=customer.email
        binding.editAddress.text=customer.address
        binding.editGroup.text=customer.group
        binding.editNotes.text=customer.notes
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}
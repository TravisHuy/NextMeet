package com.nhathuy.customermanagementapp.ui

import android.app.Dialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.nhathuy.customermanagementapp.R
import com.nhathuy.customermanagementapp.databinding.ActivityCustomerDetailBinding
import com.nhathuy.customermanagementapp.model.Customer
import com.nhathuy.customermanagementapp.viewmodel.CustomerViewModel
import com.nhathuy.customermanagementapp.viewmodel.UserViewModel

class CustomerDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerDetailBinding
    private lateinit var viewModel:CustomerViewModel
    private lateinit var userViewModel: UserViewModel
    private var currentUserId: Int =-1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityCustomerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userViewModel= ViewModelProvider(this).get(UserViewModel::class.java)
        viewModel=ViewModelProvider(this).get(CustomerViewModel::class.java)

        userViewModel.getCurrentUser().observe(this){
            user ->
            currentUserId= user?.id?:-1
        }

        val customer: Customer? = intent.getParcelableExtra("Customer_extra")
        customer?.let {
            displayCustomerDetail(it)
        }

        binding.arrowLeft.setOnClickListener {
            onBackPressed()
        }

        binding.edit.setOnClickListener {
            showEditDialog()
        }
    }

    private fun showEditDialog() {
        val dialog= Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.edit_customer)

        val name= dialog.findViewById<TextInputEditText>(R.id.ed_edit_customer_name)
        val phone= dialog.findViewById<TextInputEditText>(R.id.ed_edit_customer_phone)
        val email= dialog.findViewById<TextInputEditText>(R.id.ed_edit_customer_email)
        val address= dialog.findViewById<TextInputEditText>(R.id.ed_edit_customer_address)
        val group= dialog.findViewById<TextInputEditText>(R.id.ed_edit_customer_group)
        val notes= dialog.findViewById<TextInputEditText>(R.id.ed_edit_customer_notes)

        //
        val btn_edit=dialog.findViewById<Button>(R.id.btn_edit)

        val customer: Customer ? =intent.getParcelableExtra("Customer_extra")

        customer?.let {
            name.setText(it.name)
            phone.setText(it.phone)
            email.setText(it.email)
            address.setText(it.address)
            group.setText(it.group)
            notes.setText(it.notes)
        }

        btn_edit.setOnClickListener {
            val updatedCustomer = Customer(
                customer?.id!!,
                currentUserId,
                name=name.text.toString(),
                address= address.text.toString(),
                phone= phone.text.toString(),
                email= email.text.toString(),
                group= group.text.toString(),
                notes= notes.text.toString()
            )
            viewModel.editCustomer(updatedCustomer)
            dialog.dismiss()
            displayCustomerDetail(updatedCustomer)
        }
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.attributes?.windowAnimations=R.style.DialogAnimation;
        dialog.window?.setGravity(Gravity.BOTTOM)

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
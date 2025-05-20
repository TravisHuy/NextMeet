package com.nhathuy.nextmeet.ui

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ActivityLoginBinding
import com.nhathuy.nextmeet.model.User
import com.nhathuy.nextmeet.resource.Resource
import com.nhathuy.nextmeet.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val userViewModel: UserViewModel by viewModels()

    private var address: String? = null
    private var latitude: Double? = null
    private var longitude: Double? = null

    private var activeDialog: Dialog? = null

    private val mapPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { data ->
                    address = data.getStringExtra(GoogleMapActivity.EXTRA_SELECTED_ADDRESS)
                    latitude = data.getDoubleExtra(GoogleMapActivity.EXTRA_SELECTED_LAT, 0.0)
                    longitude = data.getDoubleExtra(GoogleMapActivity.EXTRA_SELECTED_LNG, 0.0)

                    activeDialog?.findViewById<TextView>(R.id.tv_register_address)
                        ?.let { addressTextView ->
                            if (address != null) {
                                addressTextView.text = address
                            }
                        }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObserverViewModel()
        //register
        binding.btnRegister.setOnClickListener {
            showDialog()
        }

        //login
        binding.btnLogin.setOnClickListener {
            login()
        }

        binding.forgottenPass.setOnClickListener {
            showForgotDialog()
        }

    }

    private fun setupObserverViewModel() {
        lifecycleScope.launch {
            userViewModel.loginState.collect { result ->
                when (result) {
                    is Resource.Loading -> {

                    }

                    is Resource.Success -> {
                        result.data?.let { user ->
                            user.isLoggedIn = 1
                            userViewModel.updateUser(user)

                            val sharedPreferences =
                                getSharedPreferences("user_id", Context.MODE_PRIVATE)
                            sharedPreferences.edit().putInt("user_id", user.id).apply()


                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            Toast.makeText(
                                this@LoginActivity,
                                getString(R.string.login_successfully),
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }
                    }

                    is Resource.Error -> {
                        Toast.makeText(
                            this@LoginActivity,
                            getString(R.string.login_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        lifecycleScope.launch {
            userViewModel.passwordUpdateState.collect { result ->
                when (result) {
                    is Resource.Loading -> {

                    }

                    is Resource.Success -> {
                        if (result.data == true) {
                            Toast.makeText(
                                this@LoginActivity,
                                "Password update successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    is Resource.Error -> {
                        Toast.makeText(
                            this@LoginActivity,
                            result.message ?: "Failed to update password",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    fun login() {
        val phone = binding.edLoginPhone.text.toString()
        val password = binding.edLoginPass.text.toString()

        if (!isValidateInput(phone, password)) {
            return
        }

        userViewModel.login(phone, password)
    }

    private fun isValidateInput(phone: String, password: String): Boolean {
        var isValidate = true

        if (phone.isEmpty()) {
            binding.loginPhoneLayout.error = getString(R.string.enter_phone)
            isValidate = false
        } else if (phone.length != 10) {
            binding.loginPhoneLayout.error = getString(R.string.error_phone)
            isValidate = false
        } else {
            binding.loginPhoneLayout.error = null
        }

        if (password.isEmpty()) {
            binding.loginPassLayout.error = getString(R.string.enter_password)
            isValidate = false
        } else if (password.length < 6) {
            binding.loginPassLayout.error = getString(R.string.error_phone)
            isValidate = false
        } else {
            binding.loginPassLayout.error = null
        }


        return isValidate
    }

    fun showDialog() {
        val dialog = Dialog(this)
        activeDialog = dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_register_form)

        val btnSubmit = dialog.findViewById<Button>(R.id.btn_submit)

        val ed_name = dialog.findViewById<TextInputEditText>(R.id.ed_register_name)
        val ed_phone = dialog.findViewById<TextInputEditText>(R.id.ed_register_phone)
        val ed_email = dialog.findViewById<TextInputEditText>(R.id.ed_register_email)
        val ed_password = dialog.findViewById<TextInputEditText>(R.id.ed_register_password)
        val tv_register_address = dialog.findViewById<TextView>(R.id.tv_register_address)
        val choose_location = dialog.findViewById<LinearLayout>(R.id.linear_layout_address)
        val btnAddAddress = dialog.findViewById<ImageButton>(R.id.btn_add_address)


        address = null
        latitude = null
        longitude = null

        btnAddAddress.setOnClickListener {
            val intent = Intent(this@LoginActivity, GoogleMapActivity::class.java)
            intent.putExtra("address",address)
            if(latitude!=null && longitude!= null){
                intent.putExtra("latitude",latitude)
                intent.putExtra("longitude",longitude)
            }
            mapPickerLauncher.launch(intent)
        }

        btnSubmit.setOnClickListener {
            val name = ed_name.text.toString()
            val phone = ed_phone.text.toString()
            val email = ed_email.text.toString()
            val password = ed_password.text.toString()
            val address = tv_register_address.text.toString()

            if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.all_fields_are_required), Toast.LENGTH_LONG)
                    .show()
            } else if (phone.length != 10) {
                ed_phone.error = getString(R.string.error_phone);
            } else if (name.length > 25) {
                ed_name.error = getString(R.string.error_name)
            } else if (password.length < 6) {
                ed_password.error = getString(R.string.error_password)
            } else {
                val user = User(
                    name = name,
                    phone = phone,
                    email = email,
                    password = password,
                    defaultLatitude = latitude,
                    defaultLongitude = longitude
                )
                userViewModel.register(user)

                lifecycleScope.launch {
                    userViewModel.registrationState.collect { result ->
                        when (result) {
                            is Resource.Loading -> {

                            }

                            is Resource.Success -> {
                                if (result.data == true) {
                                    dialog.dismiss()
                                    Toast.makeText(
                                        this@LoginActivity,
                                        getString(R.string.register_successfull),
                                        Toast.LENGTH_LONG
                                    )
                                        .show()
                                }
                                this.cancel()
                            }

                            is Resource.Error -> {
                                Toast.makeText(
                                    this@LoginActivity,
                                    result.message ?: "Registration failed",
                                    Toast.LENGTH_LONG
                                ).show()
                                // Cancel the collector after error
                                this.cancel()
                            }
                        }
                    }
                }
            }
        }

        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_background)
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation;
        dialog.window?.setGravity(Gravity.BOTTOM)
    }


    private fun showForgotDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.forgot_password)

        val btnSubmit = dialog.findViewById<Button>(R.id.btn_submit)

        val ed_new_password = dialog.findViewById<TextInputEditText>(R.id.ed_forgot_new_pass)
        val ed_re_password = dialog.findViewById<TextInputEditText>(R.id.ed_forgot_re_pass)


        btnSubmit.setOnClickListener {
            val newPassword = ed_new_password.text.toString()
            val rePassword = ed_re_password.text.toString()

            if (newPassword.isEmpty() || rePassword.isEmpty()) {
                Toast.makeText(this, getText(R.string.all_fields_are_required), Toast.LENGTH_LONG)
                    .show()
            } else if (newPassword != rePassword) {
                Toast.makeText(this, "Password do not match", Toast.LENGTH_SHORT).show()
            } else if (newPassword.length < 6) {
                Toast.makeText(this, "Password must be least 6 character", Toast.LENGTH_SHORT)
                    .show()
            } else {
                val phone = binding.edLoginPhone.text.toString()
                if (phone.isEmpty() || phone.length != 10) {
                    Toast.makeText(this, "Please must be least 6 character", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    userViewModel.updatePassword(phone, newPassword)
                    Toast.makeText(this, "Password update successfully", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
//        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation;
        dialog.window?.setGravity(Gravity.CENTER_HORIZONTAL)
    }
}

package com.nhathuy.nextmeet.ui

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ActivityLoginBinding
import com.nhathuy.nextmeet.model.LoginForm
import com.nhathuy.nextmeet.model.PasswordResetForm
import com.nhathuy.nextmeet.model.RegistrationForm
import com.nhathuy.nextmeet.model.ValidationResult
import com.nhathuy.nextmeet.resource.Resource
import com.nhathuy.nextmeet.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
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

                    val addressTextError =
                        activeDialog?.findViewById<TextView>(R.id.tv_register_address_error)

                    activeDialog?.findViewById<TextView>(R.id.tv_register_address)
                        ?.let { addressTextView ->
                            if (address != null) {
                                addressTextView.text = address
                                addressTextView.visibility = View.VISIBLE
                                addressTextError?.visibility = View.GONE
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
        setupClickListeners()
        loadRememberedCredentials()
    }

    /**
     * Tải thông tin đăng nhập đã lưu nếu Remember Me được bật
     */
    private fun loadRememberedCredentials() {
        if (userViewModel.isRememberMeEnabled()) {
            val phone = userViewModel.getUserPhone()

            if (phone.isNotEmpty()) {
                binding.edLoginPhone.setText(phone)
                binding.edLoginPass.requestFocus()
                binding.switchRememberMe.isChecked = true
            }
        }
    }

    private fun setupClickListeners() {
        //register
        binding.btnRegister.setOnClickListener {
            showRegisterDialog()
        }

        //login
        binding.btnLogin.setOnClickListener {
            handleLogin()
        }
        //forgot pass
        binding.forgottenPass.setOnClickListener {
            showForgotPasswordDialog()
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

                            val rememberMe = binding.switchRememberMe.isChecked
                            val phone = binding.edLoginPhone.text.toString()


                            userViewModel.createLoginSession(user.id, rememberMe, phone)

                            Toast.makeText(
                                this@LoginActivity,
                                getString(R.string.login_successfully),
                                Toast.LENGTH_SHORT
                            ).show()

                            val intent = Intent(this@LoginActivity, MainActivity2::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)

                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

                            finish()
                        }
                    }

                    is Resource.Error -> {

                        val errorMessage = when {
                            result.message?.contains("password", ignoreCase = true) == true ->
                                getString(R.string.invalid_password)

                            result.message?.contains("not found", ignoreCase = true) == true ||
                                    result.message?.contains(
                                        "invalid",
                                        ignoreCase = true
                                    ) == true ->
                                getString(R.string.user_not_found)

                            else ->
                                result.message ?: getString(R.string.login_failed)
                        }

                        Toast.makeText(
                            this@LoginActivity,
                            errorMessage,
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

        //form login xác thực với observer
        lifecycleScope.launch {
            userViewModel.loginFormState.collectLatest { validationResult ->
                if (validationResult.isNotEmpty()) {
                    binding.loginPhoneLayout.error = null
                    binding.loginPassLayout.error = null

                    validationResult["phone"]?.let { result ->
                        if (!result.isValid) {
                            binding.loginPhoneLayout.error = result.errorMessage
                        }
                    }
                    validationResult["password"]?.let { result ->
                        if (!result.isValid) {
                            binding.loginPassLayout.error = result.errorMessage
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            userViewModel.registrationState.collect { result ->
                when (result) {
                    is Resource.Loading -> {

                    }

                    is Resource.Success -> {
                        if (result.data == true) {
                            activeDialog?.dismiss()
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

        //form register xác thực với observer
        lifecycleScope.launch {
            userViewModel.registerFormState.collectLatest { validationResults ->
                activeDialog?.let { dialog ->
                    if (validationResults.isNotEmpty()) {
                        clearRegistrationDialogError(dialog)
                        setRegistrationDialogErrors(dialog, validationResults)
                    }
                }
            }
        }
        //form password reset xác thực với observer
        lifecycleScope.launch {
            userViewModel.passwordFormResetState.collectLatest { validationResults ->
                activeDialog?.let { dialog ->
                    if (validationResults.isNotEmpty()) {
                        clearPasswordResetDialogError(dialog)
                        setPasswordResetDialogErros(dialog, validationResults)
                    }
                }
            }
        }

        lifecycleScope.launch {
            userViewModel.rememberMeState.collect { isRememberMeEnabled ->
                binding.switchRememberMe.isChecked = isRememberMeEnabled
            }
        }

    }


    private fun handleLogin() {
        val phone = binding.edLoginPhone.text.toString()
        val password = binding.edLoginPass.text.toString()
        val rememberMe = binding.switchRememberMe.isChecked

        //xóa lỗi cũ nếu có
        binding.loginPhoneLayout.error = null
        binding.loginPassLayout.error = null

        userViewModel.setRememberMe(rememberMe)

        userViewModel.validateAndLogin(LoginForm(phone, password), rememberMe)
    }

    private fun showRegisterDialog() {
        val dialog = Dialog(this)
        activeDialog = dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_register_form)

        val btnSubmit = dialog.findViewById<Button>(R.id.btn_submit)

        val edName = dialog.findViewById<TextInputEditText>(R.id.ed_register_name)
        val edPhone = dialog.findViewById<TextInputEditText>(R.id.ed_register_phone)
        val edEmail = dialog.findViewById<TextInputEditText>(R.id.ed_register_email)
        val edPassword = dialog.findViewById<TextInputEditText>(R.id.ed_register_password)
        val tvRegisterAddress = dialog.findViewById<TextView>(R.id.tv_register_address)
        val btnAddAddress = dialog.findViewById<ImageButton>(R.id.btn_add_address)


        address = null
        latitude = null
        longitude = null

        btnAddAddress.setOnClickListener {
            val intent = Intent(this@LoginActivity, GoogleMapActivity::class.java)
            intent.putExtra("address", address)
            if (latitude != null && longitude != null) {
                intent.putExtra("latitude", latitude)
                intent.putExtra("longitude", longitude)
            }
            mapPickerLauncher.launch(intent)
        }

        btnSubmit.setOnClickListener {
            val name = edName.text.toString()
            val phone = edPhone.text.toString()
            val email = edEmail.text.toString()
            val password = edPassword.text.toString()
            val addressText = tvRegisterAddress.text.toString()


            val user = RegistrationForm(
                name = name,
                phone = phone,
                email = email,
                password = password,
                address = addressText,
                latitude = latitude,
                longitude = longitude
            )
            if (userViewModel.validateAndRegister(user)) {
                dialog.dismiss()
            }
        }

        configureDialogAppearance(dialog, Gravity.BOTTOM)
    }

    private fun configureDialogAppearance(dialog: Dialog, gravity: Int) {
        dialog.show()
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(R.drawable.rounded_background)
            attributes.windowAnimations = R.style.DialogAnimation
            setGravity(gravity)
        }
    }


    private fun showForgotPasswordDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.forgot_password)

        val btnSubmit = dialog.findViewById<Button>(R.id.btn_submit)

        val edPhone = dialog.findViewById<TextInputEditText>(R.id.ed_forgot_phone)
        val edNewPassword = dialog.findViewById<TextInputEditText>(R.id.ed_forgot_new_pass)
        val edRePassword = dialog.findViewById<TextInputEditText>(R.id.ed_forgot_re_pass)


        btnSubmit.setOnClickListener {
            val phone = edPhone.text.toString()
            val newPassword = edNewPassword.text.toString()
            val rePassword = edRePassword.text.toString()

            val passwordResetForm =
                PasswordResetForm(
                    phone = phone,
                    newPassword = newPassword,
                    confirmPassword = rePassword
                )
            if (userViewModel.validateAndUpdatePassword(passwordResetForm)) {
                dialog.dismiss()
            }
        }

        configureDialogAppearance(dialog, Gravity.BOTTOM)
    }

    private fun clearRegistrationDialogError(dialog: Dialog) {
        val nameLayout = dialog.findViewById<TextInputLayout>(R.id.register_name_Layout)
        val phoneLayout = dialog.findViewById<TextInputLayout>(R.id.register_phone_Layout)
        val emailLayout = dialog.findViewById<TextInputLayout>(R.id.register_email_Layout)
        val passwordLayout = dialog.findViewById<TextInputLayout>(R.id.register_password_Layout)

        nameLayout?.error = null
        phoneLayout?.error = null
        emailLayout?.error = null
        passwordLayout?.error = null
    }

    private fun setRegistrationDialogErrors(
        dialog: Dialog,
        validationResults: Map<String, ValidationResult>
    ) {
        val nameLayout = dialog.findViewById<TextInputLayout>(R.id.register_name_Layout)
        val phoneLayout = dialog.findViewById<TextInputLayout>(R.id.register_phone_Layout)
        val emailLayout = dialog.findViewById<TextInputLayout>(R.id.register_email_Layout)
        val passwordLayout = dialog.findViewById<TextInputLayout>(R.id.register_password_Layout)
        val addressText = dialog.findViewById<TextView>(R.id.tv_register_address)
        val addressTextError = dialog.findViewById<TextView>(R.id.tv_register_address_error)


        validationResults["name"]?.let { result ->
            if (!result.isValid) {
                nameLayout?.error = result.errorMessage
            }
        }

        validationResults["phone"]?.let { result ->
            if (!result.isValid) {
                phoneLayout?.error = result.errorMessage
            }
        }

        validationResults["email"]?.let { result ->
            if (!result.isValid) {
                emailLayout?.error = result.errorMessage
            }
        }
        validationResults["password"]?.let { result ->
            if (!result.isValid) {
                passwordLayout?.error = result.errorMessage
            }
        }

        validationResults["address"]?.let { result ->
            if (!result.isValid) {
                addressText.visibility = View.GONE
                addressTextError.visibility = View.VISIBLE
                addressTextError.text = result.errorMessage
            } else {
                addressText.visibility = View.VISIBLE
                addressTextError.visibility = View.GONE
            }
        }
    }

    private fun clearPasswordResetDialogError(dialog: Dialog) {
        val phoneLayout = dialog.findViewById<TextInputLayout>(R.id.forgot_phone_Layout)
        val newPassword = dialog.findViewById<TextInputLayout>(R.id.forgot_pass_Layout)
        val confirmPassLayout = dialog.findViewById<TextInputLayout>(R.id.forgot_re_pass_Layout)
        val addressText = dialog.findViewById<TextView>(R.id.tv_register_address)
        val addressTextError = dialog.findViewById<TextView>(R.id.tv_register_address_error)

        phoneLayout?.error = null
        newPassword?.error = null
        confirmPassLayout?.error = null
        addressText.text = null

        addressText.visibility = View.VISIBLE
        addressTextError.visibility = View.GONE
    }

    private fun setPasswordResetDialogErros(
        dialog: Dialog,
        validationResults: Map<String, ValidationResult>
    ) {
        val phoneLayout = dialog.findViewById<TextInputLayout>(R.id.forgot_phone_Layout)
        val newPassword = dialog.findViewById<TextInputLayout>(R.id.forgot_pass_Layout)
        val confirmPassLayout = dialog.findViewById<TextInputLayout>(R.id.forgot_re_pass_Layout)


        validationResults["phone"]?.let { result ->
            if (!result.isValid) {
                phoneLayout?.error = result.errorMessage
            }
        }

        validationResults["newPassword"]?.let { result ->
            if (!result.isValid) {
                newPassword?.error = result.errorMessage
            }
        }

        validationResults["confirmPassword"]?.let { result ->
            if (!result.isValid) {
                confirmPassLayout?.error = result.errorMessage
            }
        }
    }
}

package com.nhathuy.nextmeet.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.BottomRegisterFormBinding
import com.nhathuy.nextmeet.model.User
import com.nhathuy.nextmeet.resource.Resource
import com.nhathuy.nextmeet.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterModalBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomRegisterFormBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomRegisterFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSubmitButton()
        observeRegistrationState()
    }

    private fun setupSubmitButton() {
        binding.btnSubmit.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val name = binding.edRegisterName.text.toString()
        val phone = binding.edRegisterPhone.text.toString()
        val email = binding.edRegisterEmail.text.toString()
        val password = binding.edRegisterPassword.text.toString()

        if (!validateInput(name, phone, email, password)) {
            return
        }

        val user = User(name = name, phone = phone, email = email, password = password)
        userViewModel.register(user)
    }

    private fun validateInput(name: String, phone: String, email: String, password: String): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            binding.registerNameLayout.error = getString(R.string.enter_name)
            isValid = false
        } else if (name.length > 25) {
            binding.registerNameLayout.error = getString(R.string.error_name)
            isValid = false
        } else {
            binding.registerNameLayout.error = null
        }

        if (phone.isEmpty()) {
            binding.registerPhoneLayout.error = getString(R.string.enter_phone)
            isValid = false
        } else if (phone.length != 10) {
            binding.registerPhoneLayout.error = getString(R.string.error_phone)
            isValid = false
        } else {
            binding.registerPhoneLayout.error = null
        }

        if (email.isEmpty()) {
            binding.registerEmailLayout.error = getString(R.string.enter_email)
            isValid = false
        } else {
            binding.registerEmailLayout.error = null
        }

        if (password.isEmpty()) {
            binding.registerPasswordLayout.error = getString(R.string.enter_password)
            isValid = false
        } else if (password.length < 6) {
            binding.registerPasswordLayout.error = getString(R.string.error_password)
            isValid = false
        } else {
            binding.registerPasswordLayout.error = null
        }

        return isValid
    }

    private fun observeRegistrationState() {
        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.registrationState.collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        // You could show a loading indicator here
                    }
                    is Resource.Success -> {
                        if (result.data == true) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.register_successfull),
                                Toast.LENGTH_LONG
                            ).show()
                            dismiss()
                        }
                        this.cancel()
                    }
                    is Resource.Error -> {
                        Toast.makeText(
                            requireContext(),
                            result.message ?: "Registration failed",
                            Toast.LENGTH_LONG
                        ).show()
                        this.cancel()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "RegisterModalBottomSheet"
    }
}
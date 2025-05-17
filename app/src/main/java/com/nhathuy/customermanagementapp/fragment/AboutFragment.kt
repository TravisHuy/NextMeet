package com.nhathuy.customermanagementapp.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nhathuy.customermanagementapp.R
import com.nhathuy.customermanagementapp.databinding.FragmentAboutBinding
import com.nhathuy.customermanagementapp.resource.Resource
import com.nhathuy.customermanagementapp.ui.LoginActivity
import com.nhathuy.customermanagementapp.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AboutFragment : Fragment() {
    private lateinit var binding: FragmentAboutBinding
    private val userViewModel: UserViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding= FragmentAboutBinding.inflate(layoutInflater)

        binding.facebook.setOnClickListener{
            openUrl("https://www.facebook.com/honhathuy.travishuy")
        }
        binding.github.setOnClickListener{
            openUrl("https://github.com/TravisHuy")
        }
        binding.linkedin.setOnClickListener{
            openUrl("https://www.linkedin.com/in/honhathuy/")
        }


        binding.btnLogout?.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        (activity as AppCompatActivity).supportActionBar?.hide()
        return binding.root
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes"){
                _,_-> performLogout()
            }
            .setNegativeButton("No",null)
            .show()
    }

    private fun performLogout() {
        lifecycleScope.launch{
            userViewModel.logoutState.collect{
                resource ->
                when(resource) {
                    is Resource.Loading ->{

                    }
                    is Resource.Success -> {
                        if(resource.data == true){
                            Toast.makeText(requireContext(),"Đăng xuất thành công",Toast.LENGTH_SHORT).show()
                            navigationLogin()
                        }
                    }
                    is Resource.Error -> {
                        Toast.makeText(requireContext(),"Đăng xuất lỗi",Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun navigationLogin() {
        val intent= Intent(requireContext(),LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun openUrl(url: String) {
        val intent=Intent(Intent.ACTION_VIEW)
        intent.data=Uri.parse(url)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as AppCompatActivity).supportActionBar?.show()
    }
}
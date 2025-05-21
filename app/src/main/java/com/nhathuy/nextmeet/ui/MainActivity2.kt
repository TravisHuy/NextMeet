package com.nhathuy.nextmeet.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ActivityMain2Binding
import com.nhathuy.nextmeet.model.User
import com.nhathuy.nextmeet.resource.Resource
import com.nhathuy.nextmeet.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class MainActivity2 : AppCompatActivity() {
    private lateinit var binding:ActivityMain2Binding
    private val userViewModel: UserViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUserInfo()
        setupLogoutButton()
        observeLogoutState()
        displayLoginTime()

    }

    private fun setupUserInfo(){
        // Quan sát thông tin người dùng hiện tại từ ViewModel
        userViewModel.getCurrentUser().observe(this) { user ->
            user?.let {
                displayUserInfo(it)
            } ?: run {
                // Nếu không có thông tin người dùng, chuyển về màn hình đăng nhập
                redirectToLogin()
            }
        }
    }
    /**
     * Hiển thị thông tin người dùng lên giao diện
     */
    private fun displayUserInfo(user: User) {
        binding.apply {
            tvUserName.text = user.name
            tvUserPhone.text = user.phone
            tvUserEmail.text = user.email

            // Hiển thị trạng thái Remember Me
            val rememberMeStatus = if (userViewModel.isRememberMeEnabled()) {
                getString(R.string.enabled)
            } else {
                getString(R.string.disabled)
            }
            tvRememberMeStatus.text = getString(R.string.remember_me_status, rememberMeStatus)

            // Hiển thị thông tin về địa chỉ nếu có
            if (user.defaultLatitude != null && user.defaultLongitude != null) {
                tvUserLocation.visibility = View.VISIBLE
                tvUserLocation.text = getString(
                    R.string.location_format,
                    user.defaultLatitude.toString(),
                    user.defaultLongitude.toString()
                )
            } else {
                tvUserLocation.visibility = View.GONE
            }
        }
    }

   /**
     * Thiết lập sự kiện cho nút đăng xuất
     */
    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    /**
     * Hiển thị hộp thoại xác nhận đăng xuất
     */
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout_confirmation_title)
            .setMessage(R.string.logout_confirmation_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                userViewModel.logout()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    /**
     * Quan sát trạng thái đăng xuất
     */
    private fun observeLogoutState() {
        lifecycleScope.launch {
            userViewModel.logoutState.collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        // Có thể hiển thị loading nếu cần
                    }
                    is Resource.Success -> {
                        Toast.makeText(
                            this@MainActivity2,
                            R.string.logout_success,
                            Toast.LENGTH_SHORT
                        ).show()
                        redirectToLogin()
                    }
                    is Resource.Error -> {
                        Toast.makeText(
                            this@MainActivity2,
                            result.message ?: getString(R.string.logout_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    /**
     * Chuyển hướng về màn hình đăng nhập
     */
    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        // Xóa tất cả các activity trước đó trong stack
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        // Thêm hiệu ứng chuyển màn hình
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        finish()
    }
    private fun displayLoginTime(){
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        binding.tvLoginTime.text = getString(R.string.login_time_format, currentTime)
    }
}
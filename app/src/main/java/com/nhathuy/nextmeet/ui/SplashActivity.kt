package com.nhathuy.nextmeet.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ActivitySplashBinding
import com.nhathuy.nextmeet.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val userViewModel: UserViewModel by viewModels()

    companion object{
        private const val TAG = "SplashActivity"
        private const val DELAY_MILLIS = 500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            delay(DELAY_MILLIS)
            checkLoginStatusNavigate()
        }
    }

    private fun checkLoginStatusNavigate(){
        try{
            userViewModel.getCurrentUser().observe(this) { user ->
                user?.let {
                    navigateToMain()
                } ?: run {
                    navigateToLogin()
                }
            }
        }
        catch(e : Exception){
            Log.e(TAG, "Error checking login status: ${e.message}")
            navigateToLogin()
        }
    }

    private fun navigateToLogin(){
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        finish()
    }

    private fun navigateToMain(){
        val intent = Intent(this, SolutionActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        finish()
    }
}
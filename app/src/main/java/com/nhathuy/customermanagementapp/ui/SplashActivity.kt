package com.nhathuy.customermanagementapp.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.nhathuy.customermanagementapp.R
import com.nhathuy.customermanagementapp.databinding.ActivitySplashBinding
import com.nhathuy.customermanagementapp.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private val userViewModel: UserViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        binding= ActivitySplashBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        userViewModel.getCurrentUser().observe(this, Observer {
            user->
            if(user!=null && user.isLoggedIn==1){
                switchMain()
            }
            else{
                switchLogin()
            }
        })
    }
    fun switchMain(){
        Handler(Looper.getMainLooper()).postDelayed({
            val intent=Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
            finish()
        },1500)
    }
    fun switchLogin(){
        Handler(Looper.getMainLooper()).postDelayed({
            val intent=Intent(this,LoginActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
            finish()
        },1500)
    }
}
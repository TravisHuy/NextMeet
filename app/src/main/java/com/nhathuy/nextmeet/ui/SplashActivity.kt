package com.nhathuy.nextmeet.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ActivitySplashBinding
import com.nhathuy.nextmeet.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private val userViewModel: UserViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginStatus()
            overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
            finish()
        },1500)
    }
    private fun checkLoginStatus(){
        if(userViewModel.isLoggedIn()){
            val intent=Intent(this, TestActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
        }else{
            val intent=Intent(this, LoginActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
        }
        finish()
    }
//    fun switchMain(){
//        Handler(Looper.getMainLooper()).postDelayed({
//            val intent=Intent(this, MainActivity2::class.java)
//            startActivity(intent)
//            overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
//            finish()
//        },1500)
//    }
//    fun switchLogin(){
//        Handler(Looper.getMainLooper()).postDelayed({
//            val intent=Intent(this,LoginActivity::class.java)
//            startActivity(intent)
//            overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
//            finish()
//        },1500)
//    }
}
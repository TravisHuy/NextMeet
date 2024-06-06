package com.nhathuy.customermanagementapp.ui

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.nhathuy.customermanagementapp.MainActivity
import com.nhathuy.customermanagementapp.R
import com.nhathuy.customermanagementapp.databinding.ActivityLoginBinding
import com.nhathuy.customermanagementapp.model.User
import com.nhathuy.customermanagementapp.viewmodel.UserViewModel

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var userViewModel: UserViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userViewModel=ViewModelProvider(this).get(UserViewModel::class.java)


        //register
        binding.btnRegister.setOnClickListener {
            showDialog()
        }

        //login
        binding.btnLogin.setOnClickListener{
            login()
        }
    }
    fun login(){
        val phone=binding.edLoginPhone.text.toString()
        val password=binding.edLoginPass.text.toString()

        if(phone.isEmpty()||password.isEmpty()){
            binding.edLoginPhone.error=getString(R.string.error)
            binding.edLoginPass.error=getString(R.string.error)
        }
        if(password.length<6){
            binding.edLoginPass.error=getString(R.string.error_password)
        }
       if(phone.length!=10){
           binding.edLoginPhone.error=getString(R.string.error_phone);
        }
        userViewModel.login(phone, password).observe(this, Observer {
            user->
            if(user!=null){
                startActivity(Intent(this,MainActivity::class.java))
                Toast.makeText(this,getString(R.string.login_successfully),Toast.LENGTH_LONG).show()
            }
            else{
                Toast.makeText(this,getString(R.string.login_failed),Toast.LENGTH_LONG).show()
            }
        })
    }
    fun showDialog(){
        val dialog= Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_register_form)

        val btnSubmit=dialog.findViewById<Button>(R.id.btn_submit)

        val ed_name=dialog.findViewById<TextInputEditText>(R.id.ed_register_name)
        val ed_phone=dialog.findViewById<TextInputEditText>(R.id.ed_register_phone)
        val ed_email=dialog.findViewById<TextInputEditText>(R.id.ed_register_email)
        val ed_password=dialog.findViewById<TextInputEditText>(R.id.ed_register_password)

        btnSubmit.setOnClickListener{
            val name=ed_name.text.toString()
            val phone=ed_phone.text.toString()
            val email=ed_email.text.toString()
            val password=ed_password.text.toString()

            if(name.isEmpty()||phone.isEmpty()||email.isEmpty()||password.isEmpty()){
                Toast.makeText(this,getString(R.string.all_fields_are_required),Toast.LENGTH_LONG).show()
            }
            else if(phone.length!=10){
                ed_phone.error=getString(R.string.error_phone);
            }
            else if(name.length>25){
                ed_name.error=getString(R.string.error_name)
            }
            else if(password.length<6){
                ed_password.error=getString(R.string.error_password)
            }
            else{
                val user=User(name=name, phone = phone, email = email, password = password)
                userViewModel.register(user)
                dialog.dismiss()
                startActivity(Intent(this,MainActivity::class.java))
                Toast.makeText(this,getString(R.string.register_successfull),Toast.LENGTH_LONG).show()
            }
        }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
//        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations=R.style.DialogAnimation;
        dialog.window?.setGravity(Gravity.BOTTOM)
    }
}
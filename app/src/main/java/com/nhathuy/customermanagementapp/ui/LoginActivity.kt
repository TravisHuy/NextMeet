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
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
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

        binding.forgottenPass.setOnClickListener {
            showForgotDialog()
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
           binding.edLoginPhone.error=getString(R.string.error_phone)
        }


       if(!isValidateInput(phone,password)){
           return
       }



        userViewModel.login(phone, password).observe(this, Observer {
            user->
            if(user!=null){
                user.isLoggedIn=1
                userViewModel.updateUser(user)


                //save userId to SharedPreferences
                val sharedPreferences=getSharedPreferences("user_id",Context.MODE_PRIVATE)
                sharedPreferences.edit().putInt("user_id",user.id).apply()



                startActivity(Intent(this, MainActivity::class.java))
                Toast.makeText(this,getString(R.string.login_successfully),Toast.LENGTH_LONG).show()
            }
            else{
                Toast.makeText(this,getString(R.string.login_failed),Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun isValidateInput(phone: String, password: String): Boolean {
        var isValidate=true

        if(phone.isEmpty()){
            binding.loginPhoneLayout.error=getString(R.string.enter_phone)
            isValidate=false
        }else if(phone.length!=10){
            binding.loginPhoneLayout.error=getString(R.string.error_phone)
            isValidate=false
        }else {
            binding.loginPhoneLayout.error = null
        }

        if(password.isEmpty()){
            binding.loginPassLayout.error=getString(R.string.enter_password)
            isValidate=false
        }else if(password.length<6){
            binding.loginPassLayout.error=getString(R.string.error_phone)
            isValidate=false
        }else {
            binding.loginPassLayout.error = null
        }


        return isValidate
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
                Toast.makeText(this,getString(R.string.register_successfull),Toast.LENGTH_LONG).show()
            }
        }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
//        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations=R.style.DialogAnimation;
        dialog.window?.setGravity(Gravity.BOTTOM)
    }



    private fun showForgotDialog() {
        val dialog= Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.forgot_password)

        val btnSubmit=dialog.findViewById<Button>(R.id.btn_submit)

        val ed_new_password=dialog.findViewById<TextInputEditText>(R.id.ed_forgot_new_pass)
        val ed_re_password=dialog.findViewById<TextInputEditText>(R.id.ed_forgot_re_pass)


        btnSubmit.setOnClickListener {
            val newPassword= ed_new_password.text.toString()
            val rePassword=ed_re_password.text.toString()

            if(newPassword.isEmpty()||rePassword.isEmpty()){
                Toast.makeText(this,getText(R.string.all_fields_are_required),Toast.LENGTH_LONG).show()
            }
            else if(newPassword!=rePassword){
                Toast.makeText(this,"Password do not match",Toast.LENGTH_SHORT).show()
            }
            else if(newPassword.length<6){
                Toast.makeText(this,"Password must be least 6 character",Toast.LENGTH_SHORT).show()
            }

            else{
                val phone=binding.edLoginPhone.text.toString()
                if(phone.isEmpty() || phone.length!=10){
                    Toast.makeText(this,"Please must be least 6 character",Toast.LENGTH_SHORT).show()
                }
                else{
                    userViewModel.updatePassword(phone,newPassword)
                    Toast.makeText(this,"Password update successfully",Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
//        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations=R.style.DialogAnimation;
        dialog.window?.setGravity(Gravity.BOTTOM)
    }
}
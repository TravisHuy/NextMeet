package com.nhathuy.customermanagementapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nhathuy.customermanagementapp.database.AppDatabase
import com.nhathuy.customermanagementapp.model.User
import com.nhathuy.customermanagementapp.repository.UserRepository
import kotlinx.coroutines.launch

class UserViewModel(application: Application):AndroidViewModel(application) {

    private val repository:UserRepository

    init {
        val userDao=AppDatabase.getDatabase(application).userDao()
        repository=UserRepository(userDao)
    }

    fun register(user:User) =viewModelScope.launch {
        repository.register(user)
    }
    fun login(phone:String,password:String):LiveData<User?>{
        val result=MutableLiveData<User?>()
        viewModelScope.launch {
            result.postValue(repository.login(phone,password))
        }
        return result
    }
}
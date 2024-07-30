package com.nhathuy.customermanagementapp.repository

import androidx.lifecycle.LiveData
import com.nhathuy.customermanagementapp.dao.UserDao
import com.nhathuy.customermanagementapp.model.User

class UserRepository(private val userDao: UserDao) {

    suspend fun register(user:User){
        userDao.register(user);
    }
    suspend fun login(phone:String,password:String):User?{
        return userDao.login(phone,password)
    }

    suspend fun  updateUser(user: User){
        userDao.updateUser(user)
    }

    fun getCurrentUser() : LiveData<User?>{
        return userDao.getCurrentUser()
    }

    suspend fun logout(){
        userDao.logout()
    }

    suspend fun updatePassword(phone:String,newPassword:String){
        userDao.updatePassword(phone,newPassword)
    }
}
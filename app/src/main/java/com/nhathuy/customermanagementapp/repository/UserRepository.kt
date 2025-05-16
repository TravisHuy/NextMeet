package com.nhathuy.customermanagementapp.repository

import androidx.lifecycle.LiveData
import com.nhathuy.customermanagementapp.dao.UserDao
import com.nhathuy.customermanagementapp.model.User
import com.nhathuy.customermanagementapp.resource.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(private val userDao: UserDao) {

    suspend fun register(user:User) : Flow<Resource<Boolean>> = flow{
        emit(Resource.Loading())
        try {
            userDao.register(user)
            emit(Resource.Success(true))
        }
        catch (e:Exception){
            emit(Resource.Error("Registration failed: ${e.message}"))
        }
    }
    suspend fun login(phone:String,password:String): Flow<Resource<User>> = flow{
        emit(Resource.Loading())
        try {
            val user = userDao.login(phone, password)

            if(user != null){
                val loggedInUser = user.copy(isLoggedIn = 1)
                userDao.updateUser(loggedInUser)
                emit(Resource.Success(loggedInUser))
            }
        }
        catch (e:Exception) {
            emit(Resource.Error("Invalid phone or password"))
        }
    }

    suspend fun  updateUser(user: User) : Flow<Resource<Boolean>> = flow{
        emit(Resource.Loading())
        try{
            userDao.updateUser(user)
            emit(Resource.Success(true))
        }
        catch(e:Exception){
            emit(Resource.Error("Update failed: ${e.message}"))
        }
    }

    fun getCurrentUser() : LiveData<User?>{
        return userDao.getCurrentUser()
    }

    suspend fun logout() : Flow<Resource<Boolean>> = flow{
        emit(Resource.Loading())
        try {
            userDao.logout()
            Resource.Success(true)
        }
        catch (e:Exception){
            emit(Resource.Error("Logout failed: ${e.message}"))
        }
    }

    suspend fun updatePassword(phone:String,newPassword:String): Flow<Resource<Boolean>> = flow{
        emit(Resource.Loading())
        try {
            userDao.updatePassword(phone,newPassword)
            Resource.Success(true)
        }
        catch (e:Exception){
            Resource.Error("Updated password failed: ${e.message}")
        }
    }
}
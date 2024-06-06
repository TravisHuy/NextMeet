package com.nhathuy.customermanagementapp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nhathuy.customermanagementapp.model.User

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun register(user: User)

    @Query("SELECT * FROM users WHERE phone = :phone AND password = :password")
    suspend fun login(phone: String, password: String): User?


}
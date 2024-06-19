package com.nhathuy.customermanagementapp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nhathuy.customermanagementapp.model.Customer

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun register(customer: Customer)

    @Query("select * from customers")
    suspend fun  getAllCustomer():List<Customer>
}
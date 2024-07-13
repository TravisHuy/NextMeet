package com.nhathuy.customermanagementapp.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nhathuy.customermanagementapp.model.Transaction

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun register(transaction: Transaction)

    @Query("select * from transactions")
    fun  getAllTransactions(): LiveData<List<Transaction>>


    @Update
    suspend fun editTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("delete from transactions")
    suspend fun deleteAllTransactions()
}
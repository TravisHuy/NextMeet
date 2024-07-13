package com.nhathuy.customermanagementapp.repository

import androidx.lifecycle.LiveData
import com.nhathuy.customermanagementapp.dao.TransactionDao
import com.nhathuy.customermanagementapp.model.Customer
import com.nhathuy.customermanagementapp.model.Transaction

class TransactionRepository(private val transactionDao: TransactionDao) {

    suspend fun register(transaction: Transaction){
        transactionDao.register(transaction)
    }

    fun getAllTransactions(): LiveData<List<Transaction>> {
        return transactionDao.getAllTransactions()
    }


    suspend fun editTransaction(transaction: Transaction){
        transactionDao.editTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction){
        transactionDao.deleteTransaction(transaction)
    }


    suspend fun deleteAllTransactions(){
        transactionDao.deleteAllTransactions()
    }
}
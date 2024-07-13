package com.nhathuy.customermanagementapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.nhathuy.customermanagementapp.database.AppDatabase
import com.nhathuy.customermanagementapp.model.Customer
import com.nhathuy.customermanagementapp.model.Transaction
import com.nhathuy.customermanagementapp.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application):AndroidViewModel(application) {

    private val transactionRepository:TransactionRepository

    init {
        val transactionDao=AppDatabase.getDatabase(application).transactionDao()
        transactionRepository= TransactionRepository(transactionDao)
    }

    fun addTransaction(transaction: Transaction) = viewModelScope.launch {
        transactionRepository.register(transaction)
    }

    fun getAllTransactions(): LiveData<List<Transaction>> {
        return transactionRepository.getAllTransactions()
    }

    fun editTransaction(transaction: Transaction) = viewModelScope.launch {
        transactionRepository.editTransaction(transaction)
    }

    fun deleteTransaction(transaction: Transaction){
        viewModelScope.launch(Dispatchers.IO) {
            transactionRepository.deleteTransaction(transaction)
        }
    }

    fun  deleteAllTransactions() = viewModelScope.launch{
        transactionRepository.deleteAllTransactions()
    }
}
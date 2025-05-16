package com.nhathuy.customermanagementapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhathuy.customermanagementapp.database.AppDatabase
import com.nhathuy.customermanagementapp.model.Transaction
import com.nhathuy.customermanagementapp.repository.TransactionRepository
import com.nhathuy.customermanagementapp.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(private val transactionRepository: TransactionRepository) :
    ViewModel() {

    private val _allTransactionState =
        MutableStateFlow<Resource<List<Transaction>>>(Resource.Loading())
    val allTransactionState: StateFlow<Resource<List<Transaction>>> = _allTransactionState

    private val _addTransactionState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val addTransactionState: StateFlow<Resource<Boolean>> = _addTransactionState

    private val _editTransactionState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val editTransactionState: StateFlow<Resource<Boolean>> = _editTransactionState

    private val _deleteTransactionState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val deleteTransactionState: StateFlow<Resource<Boolean>> = _deleteTransactionState

    private val _deleteAllTransactionState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val deleteAllTransactionState: StateFlow<Resource<Boolean>> = _deleteAllTransactionState

    fun addTransaction(transaction: Transaction) = viewModelScope.launch {
        transactionRepository.register(transaction).collect { result ->
            _addTransactionState.value = result
        }
    }

    fun getAllTransactions() = viewModelScope.launch {
        transactionRepository.getAllTransactions().collect {
            result ->
            _allTransactionState.value = result
        }
    }

    fun editTransaction(transaction: Transaction) = viewModelScope.launch {
        transactionRepository.editTransaction(transaction).collect { result ->
            _editTransactionState.value = result
        }
    }

    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch{
        transactionRepository.deleteTransaction(transaction).collect {
            result ->
            _deleteTransactionState.value = result
        }
    }

    fun deleteAllTransactions() = viewModelScope.launch {
        transactionRepository.deleteAllTransactions().collect {
            result ->
            _deleteAllTransactionState.value = result
        }
    }
}
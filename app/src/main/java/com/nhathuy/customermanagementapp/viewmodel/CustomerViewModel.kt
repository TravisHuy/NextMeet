package com.nhathuy.customermanagementapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhathuy.customermanagementapp.database.AppDatabase
import com.nhathuy.customermanagementapp.model.Customer
import com.nhathuy.customermanagementapp.model.Transaction
import com.nhathuy.customermanagementapp.repository.CustomerRepository
import com.nhathuy.customermanagementapp.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerViewModel @Inject constructor(private val customerRepository: CustomerRepository) :
    ViewModel() {

    private val _allCustomerState =
        MutableStateFlow<Resource<List<Customer>>>(Resource.Loading())
    val allCustomerState: StateFlow<Resource<List<Customer>>> = _allCustomerState

    private val _addCustomerState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val addCustomerState: StateFlow<Resource<Boolean>> = _addCustomerState

    private val _editCustomerState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val editCustomerState: StateFlow<Resource<Boolean>> = _editCustomerState

    private val _getCustomerById = MutableStateFlow<Resource<Customer?>>(Resource.Loading())
    val getCustomerById: StateFlow<Resource<Customer?>> = _getCustomerById

    private val _deleteCustomerState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val deleteCustomerState: StateFlow<Resource<Boolean>> = _deleteCustomerState

    private val _deleteAllCustomerState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val deleteAllCustomerState: StateFlow<Resource<Boolean>> = _deleteAllCustomerState

    fun register(customer: Customer) = viewModelScope.launch {
        customerRepository.register(customer).collect { result ->
            _addCustomerState.value = result
        }
    }

    fun getAllCustomers() = viewModelScope.launch {
        customerRepository.getAllCustomers().collect { customers ->
            _allCustomerState.value = customers
        }
    }

    fun editCustomer(customer: Customer) = viewModelScope.launch {
        customerRepository.editCustomer(customer).collect { result ->
            _editCustomerState.value = result
        }
    }

    fun deleteCustomer(customer: Customer) = viewModelScope.launch {
        customerRepository.deleteCustomer(customer).collect { result ->
            _deleteCustomerState.value = result
        }
    }

    //getCustomerById
    fun getCustomerById(customerId: Int) = viewModelScope.launch {
        customerRepository.getCustomerById(customerId).collect {
            result ->
            _getCustomerById.value = result
        }
    }

    fun deleteAllCustomers() = viewModelScope.launch {
        customerRepository.deleteAllCustomers().collect { result ->
            _addCustomerState.value = result
        }
    }
}
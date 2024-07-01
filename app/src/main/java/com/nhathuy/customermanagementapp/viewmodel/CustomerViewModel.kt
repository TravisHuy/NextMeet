package com.nhathuy.customermanagementapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.nhathuy.customermanagementapp.database.AppDatabase
import com.nhathuy.customermanagementapp.model.Customer
import com.nhathuy.customermanagementapp.repository.CustomerRepository
import com.nhathuy.customermanagementapp.repository.UserRepository
import kotlinx.coroutines.launch

class CustomerViewModel(application: Application):AndroidViewModel(application) {

    private val customerRepository:CustomerRepository


    init {
        val customerDao=AppDatabase.getDatabase(application).customerDao()
        customerRepository= CustomerRepository(customerDao)
    }


    fun register(customer: Customer) = viewModelScope.launch {
        customerRepository.register(customer)
    }

    fun getAllCustomers(): LiveData<List<Customer>> {
        return customerRepository.getAllCustomers()
    }

    fun editCustomer(customer: Customer) = viewModelScope.launch {
        customerRepository.editCustomer(customer)
    }
}
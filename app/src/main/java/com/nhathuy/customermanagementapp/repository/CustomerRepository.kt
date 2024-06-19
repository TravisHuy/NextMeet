package com.nhathuy.customermanagementapp.repository

import androidx.lifecycle.LiveData
import com.nhathuy.customermanagementapp.dao.CustomerDao
import com.nhathuy.customermanagementapp.model.Customer

class CustomerRepository(private val customerDao: CustomerDao) {


    suspend fun register(customer: Customer){
        customerDao.register(customer)
    }


    fun getAllCustomers(): LiveData<List<Customer>> {
        return customerDao.getAllCustomer()
    }
}
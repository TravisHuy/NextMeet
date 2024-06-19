package com.nhathuy.customermanagementapp.repository

import com.nhathuy.customermanagementapp.dao.CustomerDao
import com.nhathuy.customermanagementapp.model.Customer

class CustomerRepository(private val customerDao: CustomerDao) {


    suspend fun register(customer: Customer){
        customerDao.register(customer)
    }
    suspend fun getAllCustomer():List<Customer>{
      return customerDao.getAllCustomer()
    }
}
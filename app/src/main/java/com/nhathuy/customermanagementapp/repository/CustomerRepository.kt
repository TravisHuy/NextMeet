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
    suspend fun editCustomer(customer: Customer){
        customerDao.editCustomer(customer)
    }

    suspend fun deleteCustomer(customer: Customer){
        customerDao.deleteCustomer(customer)
    }

    fun getCustomerById(customerId:Int) :LiveData<Customer?>{
        return customerDao.getCustomerById(customerId)
    }
    suspend fun deleteAllCustomers(){
        customerDao.deleteAllCustomer()
    }
}
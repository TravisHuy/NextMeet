package com.nhathuy.customermanagementapp.repository

import androidx.lifecycle.LiveData
import com.nhathuy.customermanagementapp.dao.CustomerDao
import com.nhathuy.customermanagementapp.model.Customer
import com.nhathuy.customermanagementapp.resource.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepository @Inject constructor(private val customerDao: CustomerDao) {

    suspend fun register(customer: Customer): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            customerDao.register(customer)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi thêm khác hàng"))
        }
    }


    fun getAllCustomers(): Flow<Resource<List<Customer>>> = flow {
        emit(Resource.Loading())
        try {
            customerDao.getAllCustomer().collect { customers ->
                emit(Resource.Success(customers))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi không lấy tất cả khách hàng"))
        }
    }

    suspend fun editCustomer(customer: Customer): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            customerDao.editCustomer(customer)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "chình sửa khác hàng loi"))
        }
    }

    suspend fun deleteCustomer(customer: Customer): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            customerDao.deleteCustomer(customer)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "xóa khác hàng lỗi"))
        }
    }

    fun getCustomerById(customerId: Int): Flow<Resource<Customer?>> = flow {
        emit(Resource.Loading())
        try {
            val customer = customerDao.getCustomerById(customerId)
            emit(Resource.Success(customer))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "xóa khác hàng lỗi"))
        }
    }

    suspend fun deleteAllCustomers(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            customerDao.deleteAllCustomer()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "xóa khác hàng lỗi"))
        }
    }
}
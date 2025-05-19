package com.nhathuy.nextmeet.repository

import com.nhathuy.nextmeet.dao.TransactionDao
import com.nhathuy.nextmeet.model.Transaction
import com.nhathuy.nextmeet.resource.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(private val transactionDao: TransactionDao) {

    suspend fun register(transaction: Transaction): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            transactionDao.register(transaction)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "đăng ký thất bại"))
        }
    }

    fun getAllTransactions(): Flow<Resource<List<Transaction>>> = flow {
        emit(Resource.Loading())
        try {
            transactionDao.getAllTransactions().collect { list ->
                emit(Resource.Success(list))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "lỗi không lấy được tất cả transaction"))
        }

    }

    suspend fun editTransaction(transaction: Transaction): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            transactionDao.editTransaction(transaction)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Kiểm tra transaction lỗi"))
        }
    }

    suspend fun deleteTransaction(transaction: Transaction): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            transactionDao.deleteTransaction(transaction)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Xóa transaction lỗi"))
        }
    }

    suspend fun deleteAllTransactions(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            transactionDao.deleteAllTransactions()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Xóa tất cả transactions lỗi"))
        }
    }
}
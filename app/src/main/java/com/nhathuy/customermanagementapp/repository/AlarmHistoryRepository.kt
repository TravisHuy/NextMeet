package com.nhathuy.customermanagementapp.repository

import androidx.lifecycle.LiveData
import com.nhathuy.customermanagementapp.dao.AlarmHistoryDao
import com.nhathuy.customermanagementapp.model.AlarmHistory
import com.nhathuy.customermanagementapp.resource.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmHistoryRepository @Inject constructor(private val alarmHistoryDao: AlarmHistoryDao) {

    suspend fun insertAlarmHistory(alarmHistory: AlarmHistory): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            alarmHistoryDao.insertAlarmHistory(alarmHistory)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi thêm lịch sử nhắc nhỡ"))
        }
    }

    fun getUnDisplayedAlarmHistory(): Flow<Resource<List<AlarmHistory>>> = flow {
        emit(Resource.Loading())
        try {
            alarmHistoryDao.getUnDisplayedAlarmHistory().collect { result ->
                emit(Resource.Success(result))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi không hiển  lịch sử nhắc nhỡ"))
        }
    }

    suspend fun markAlarmAsDisplayed(id: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            alarmHistoryDao.markAlarmAsDisplayed(id)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi không hiển  lịch sử nhắc nhỡ"))
        }
    }

    suspend fun markAllAlarmsAsDisplayed(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            alarmHistoryDao.markAllAlarmsAsDisplayed()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi không hiển  lịch sử nhắc nhỡ"))
        }
    }

    suspend fun deleteAlarmHistory(alarmHistory: AlarmHistory) : Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            alarmHistoryDao.deleteAlarmHistory(alarmHistory)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi xóa  lịch sử nhắc nhỡ"))
        }
    }
}
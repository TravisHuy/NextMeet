package com.nhathuy.customermanagementapp.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nhathuy.customermanagementapp.model.AlarmHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarmHistory(alarmHistory: AlarmHistory)

    @Query("select * from alarm_history order by date desc")
    fun getAllAlarmHistory() : Flow<List<AlarmHistory>>

    @Query("select * from alarm_history where was_displayed=0 order by date desc")
    fun getUnDisplayedAlarmHistory(): Flow<List<AlarmHistory>>

    @Query("update alarm_history set was_displayed = 1 where id=:id")
    suspend fun markAlarmAsDisplayed(id:Int)

    @Query("update alarm_history set was_displayed=1")
    suspend fun markAllAlarmsAsDisplayed()

    @Delete
    suspend fun deleteAlarmHistory(alarmHistory: AlarmHistory)

}
package com.nhathuy.customermanagementapp.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nhathuy.customermanagementapp.model.AlarmHistory

@Dao
interface AlarmHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarmHistory(alarmHistory: AlarmHistory)

    @Query("select * from alarm_history order by date desc")
    fun getAllAlarmHistory() : LiveData<List<AlarmHistory>>

    @Query("select * from alarm_history where wasDisplayed=0 order by date desc")
    fun getUnDisplayedAlarmHistory(): LiveData<List<AlarmHistory>>

    @Query("update alarm_history set wasDisplayed = 1 where id=:id")
    suspend fun markAlarmAsDisplayed(id:Int)

    @Query("update alarm_history set wasDisplayed=1")
    suspend fun markAllAlarmsAsDisplayed()

    @Delete
    suspend fun deleteAlarmHistory(alarmHistory: AlarmHistory)

}
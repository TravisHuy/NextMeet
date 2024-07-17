package com.nhathuy.customermanagementapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.dialog.MaterialDialogs
import com.nhathuy.customermanagementapp.adapter.AlarmHistoryAdapter
import com.nhathuy.customermanagementapp.databinding.ActivityAlarmHistoryBinding
import com.nhathuy.customermanagementapp.databinding.AlarmHistoryItemBinding
import com.nhathuy.customermanagementapp.model.AlarmHistory
import com.nhathuy.customermanagementapp.viewmodel.AlarmHistoryViewModel
import com.nhathuy.customermanagementapp.viewmodel.CustomerViewModel

class AlarmHistoryActivity : AppCompatActivity() {

    private lateinit var binding:ActivityAlarmHistoryBinding
    private lateinit var alarmHistoryViewModel: AlarmHistoryViewModel
    private lateinit var customerViewModel: CustomerViewModel
    private lateinit var adapter: AlarmHistoryAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityAlarmHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        alarmHistoryViewModel = ViewModelProvider(this).get(AlarmHistoryViewModel::class.java)
        customerViewModel = ViewModelProvider(this).get(CustomerViewModel::class.java)
        setupRecyclerView()
        observeAlarmHistory()

    }

    private fun setupRecyclerView() {



        adapter = AlarmHistoryAdapter(this, customerViewModel){
            alarmHistory ->  showDeleteComfirmDialog(alarmHistory)
        }
        binding.recHistoryAlarm.apply {
            layoutManager = LinearLayoutManager(this@AlarmHistoryActivity)
            adapter = this@AlarmHistoryActivity.adapter
        }
    }

    private fun showDeleteComfirmDialog(alarmHistory: AlarmHistory) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Alarm History")
            .setMessage("Are you sure you want to delete this item?")
            .setPositiveButton("Delete") { dialog, which ->
                alarmHistoryViewModel.deleteAlarmHistory(alarmHistory)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeAlarmHistory() {



        alarmHistoryViewModel.allAlarmHistory.observe(this) { alarmHistory ->
            adapter.submit(alarmHistory)
        }

        val customerId=intent.getIntExtra("customer_id",-1)
        val time = intent.getStringExtra("time") ?:""
        val date = intent.getStringExtra("date") ?:""
        val notes = intent.getStringExtra("notes")?:""



        if (customerId != -1) {
            val alarmHistory = AlarmHistory(
                customerId = customerId,
                time = time,
                date = date,
                notes = notes,
                wasDisplayed = true
            )
            alarmHistoryViewModel.insertAlarmHistory(alarmHistory)
        }
    }
}
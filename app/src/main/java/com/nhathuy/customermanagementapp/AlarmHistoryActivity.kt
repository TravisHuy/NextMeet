package com.nhathuy.customermanagementapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.dialog.MaterialDialogs
import com.nhathuy.customermanagementapp.adapter.AlarmHistoryAdapter
import com.nhathuy.customermanagementapp.databinding.ActivityAlarmHistoryBinding
import com.nhathuy.customermanagementapp.databinding.AlarmHistoryItemBinding
import com.nhathuy.customermanagementapp.model.AlarmHistory
import com.nhathuy.customermanagementapp.resource.Resource
import com.nhathuy.customermanagementapp.viewmodel.AlarmHistoryViewModel
import com.nhathuy.customermanagementapp.viewmodel.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlarmHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmHistoryBinding
    private val alarmHistoryViewModel: AlarmHistoryViewModel by viewModels()
    private lateinit var adapter: AlarmHistoryAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeAlarmHistory()

    }

    private fun setupRecyclerView() {

        adapter = AlarmHistoryAdapter(this) { alarmHistory ->
            showDeleteComfirmDialog(alarmHistory)
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

        lifecycleScope.launch {
            alarmHistoryViewModel.unDisplayedAlarms.collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {

                    }

                    is Resource.Success -> {
                        resource.data?.let { alarms ->
                            adapter.submit(alarms)
                        }
                    }

                    is Resource.Error -> {
                        Toast.makeText(
                            this@AlarmHistoryActivity,
                            resource.message ?: "Unknown error occurred",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }


        val customerId = intent.getIntExtra("customer_id", -1)
        val time = intent.getStringExtra("time") ?: ""
        val date = intent.getStringExtra("date") ?: ""
        val notes = intent.getStringExtra("notes") ?: ""
        val appointmentId = intent.getIntExtra("appointment_id", -1)


        if (customerId != -1) {
            val alarmHistory = AlarmHistory(
                appointmentId = appointmentId,
                time = time,
                date = date,
                notes = notes,
                wasDisplayed = true
            )
            alarmHistoryViewModel.insertAlarmHistory(alarmHistory)
        }
    }
}
package com.nhathuy.customermanagementapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.nhathuy.customermanagementapp.R
import com.nhathuy.customermanagementapp.databinding.AlarmHistoryItemBinding
import com.nhathuy.customermanagementapp.model.AlarmHistory
import com.nhathuy.customermanagementapp.viewmodel.CustomerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AlarmHistoryAdapter(private val context: Context, private val customerViewModel: CustomerViewModel,
                            private val onDelete: (AlarmHistory) -> Unit) : RecyclerView.Adapter<AlarmHistoryAdapter.AlarmHistoryViewHolder>() {

    private var alarmHistoryList: List<AlarmHistory> = emptyList()


    inner class AlarmHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)  {
        val customerName: TextView= itemView.findViewById<TextView>(R.id.tv_customer_name)
        val time : TextView= itemView.findViewById<TextView>(R.id.timeTextView)
        val date : TextView= itemView.findViewById<TextView>(R.id.dateTextView)
        val notes : TextView = itemView.findViewById<TextView>(R.id.notes)


        init {
            itemView.setOnClickListener{
                val alarmHistory= alarmHistoryList[adapterPosition]
                onDelete(alarmHistory)
                true
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmHistoryViewHolder {
       return AlarmHistoryViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.alarm_history_item,parent,false))
    }

    override fun onBindViewHolder(holder: AlarmHistoryViewHolder, position: Int) {
        val alarmHistory= alarmHistoryList[position]

        holder.time.text= alarmHistory.time
        holder.date.text= alarmHistory.date
        holder.notes.text= alarmHistory.notes

        // Fetch customer name
        customerViewModel.getCustomerById(alarmHistory.customerId).observe(context as LifecycleOwner) { customer ->
            holder.customerName.text = customer?.name ?: "Unknown"
        }

    }

    override fun getItemCount(): Int = alarmHistoryList.size


    fun submit(newAlarmHistory: List<AlarmHistory>) {
        alarmHistoryList=newAlarmHistory
        notifyDataSetChanged()
    }

}
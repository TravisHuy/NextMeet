package com.nhathuy.nextmeet.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.model.AlarmHistory


class AlarmHistoryAdapter(private val context: Context,
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
//        customerViewModel.getCustomerById(alarmHistory.customerId).observe(context as LifecycleOwner) { customer ->
//            holder.customerName.text = customer?.name ?: "Unknown"
//        }

    }

    override fun getItemCount(): Int = alarmHistoryList.size


    fun submit(newAlarmHistory: List<AlarmHistory>) {
        alarmHistoryList=newAlarmHistory
        notifyDataSetChanged()
    }

}
package com.nhathuy.customermanagementapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.nhathuy.customermanagementapp.R
import com.nhathuy.customermanagementapp.model.Appointment
import com.nhathuy.customermanagementapp.model.Customer
import com.nhathuy.customermanagementapp.viewmodel.CustomerViewModel

class AppointmentAdapter(private val context:Context,
                         private var listAppointment: List<Appointment>,
                        private val customerViewModel: CustomerViewModel):
    RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {


    class AppointmentViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        val customerName: TextView = itemView.findViewById(R.id.customer_name)
        val date: TextView = itemView.findViewById(R.id.date)
        val time: TextView = itemView.findViewById(R.id.time)
        val address: TextView = itemView.findViewById(R.id.address)
        val notes: TextView = itemView.findViewById(R.id.notes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val itemView =LayoutInflater.from(context).inflate(R.layout.appointment_item,parent,false)
        return AppointmentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val appointment=listAppointment[position]
        holder.date.text = appointment.date
        holder.time.text = appointment.time
        holder.address.text = appointment.address
        holder.notes.text = appointment.notes
        // Fetch customer name
        customerViewModel.getCustomerById(appointment.customerId).observe(context as LifecycleOwner) { customer ->
            holder.customerName.text = customer?.name ?: "Unknown"
        }
    }

    override fun getItemCount(): Int {
        return listAppointment.size
    }

    fun setData(appointments: List<Appointment>) {
        listAppointment = appointments
        notifyDataSetChanged()
    }
}
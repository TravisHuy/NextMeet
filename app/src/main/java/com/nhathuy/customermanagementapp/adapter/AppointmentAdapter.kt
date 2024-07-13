package com.nhathuy.customermanagementapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nhathuy.customermanagementapp.R
import com.nhathuy.customermanagementapp.model.Appointment
import com.nhathuy.customermanagementapp.model.Transaction
import com.nhathuy.customermanagementapp.viewmodel.AppointmentViewModel
import com.nhathuy.customermanagementapp.viewmodel.CustomerViewModel

class AppointmentAdapter(private val context:Context,
                         private var listAppointment: List<Appointment>,
                         private val customerViewModel: CustomerViewModel,
                         private val appointmentViewModel: AppointmentViewModel,
                         private val onSelectionChanged: (Boolean) -> Unit)
    :RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {


    private var selectItems= mutableSetOf<Appointment>()
    var isSelectionMode= false

    inner class AppointmentViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        val customerName: TextView = itemView.findViewById(R.id.customer_name)
        val date: TextView = itemView.findViewById(R.id.date)
        val time: TextView = itemView.findViewById(R.id.time)
        val address: TextView = itemView.findViewById(R.id.address)
        val notes: TextView = itemView.findViewById(R.id.notes)


        val checkbox: CheckBox = itemView.findViewById(R.id.checkBox_appointment)
        val threeDotMenu: ImageView = itemView.findViewById(R.id.three_dot_menu_appointment)

        init {
            itemView.setOnLongClickListener{
                toggleSelectionMode(adapterPosition)
                true
            }
            itemView.setOnClickListener {
                if(isSelectionMode){
                    toggleSelection(adapterPosition)
                }
            }
            threeDotMenu.setOnClickListener {
                showOptionsDialog(listAppointment[position])
            }
        }

        private fun toggleSelectionMode(adapterPosition: Int) {
            isSelectionMode =!isSelectionMode
            toggleSelection(position)
            onSelectionChanged(isSelectionMode)
            notifyDataSetChanged()
        }
        private fun toggleSelection(position: Int) {
            val appointment = listAppointment[position]
            if(selectItems.contains(appointment)){
                selectItems.remove(appointment)
            }
            else{
                selectItems.add(appointment)
            }

            notifyDataSetChanged()

            if(selectItems.isEmpty()){
                isSelectionMode=false
                onSelectionChanged(false)
                notifyDataSetChanged()
            }
            else{
                updateSelectmode()
            }
        }

        private fun showOptionsDialog(appointment: Appointment) {
            val options = if (selectItems.size > 1) {
                arrayOf("Delete", "Delete All")
            } else {
                arrayOf("Edit", "Delete", "Delete All")
            }

            MaterialAlertDialogBuilder(context)
                .setItems(options) { _, position ->
                    when {
                        options[position] == "Edit" -> onEditClick(appointment)
                        options[position] == "Delete" -> onDeleteClick(appointment)
                        options[position] == "Delete All" -> onDeleteAllClick()
                    }
                }
                .show()
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val itemView =LayoutInflater.from(context).inflate(R.layout.appointment_item,parent,false)
        return AppointmentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val appointment=listAppointment[position]
        val isSelected = selectItems.contains(appointment)

        holder.date.text = appointment.date
        holder.time.text = appointment.time
        holder.address.text = appointment.address
        holder.notes.text = appointment.notes
        // Fetch customer name
        customerViewModel.getCustomerById(appointment.customerId).observe(context as LifecycleOwner) { customer ->
            holder.customerName.text = customer?.name ?: "Unknown"
        }

        holder.checkbox.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        holder.checkbox.isChecked = isSelected
        holder.threeDotMenu.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int {
        return listAppointment.size
    }

    private fun onEditClick(appointment: Appointment) {

    }

   //delete transaction
    private fun onDeleteClick(appointment: Appointment) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Delete Appointment(s)")
            .setMessage("Are you sure you want to delete this appointment(s)")
            .setPositiveButton("Delete") {
                _,_ ->
                selectItems.forEach {
                    appointment ->
                    appointmentViewModel.deleteAppointment(appointment)
                }
                clearSelection()
            }
            .setNegativeButton("Cancel",null)
            .show()
    }


    private fun onDeleteAllClick() {
        MaterialAlertDialogBuilder(context)
            .setTitle("Delete All Appointments")
            .setMessage("Are you sure you want to delete all appointment(s)")
            .setPositiveButton("Delete") {
                    _,_ ->
                    appointmentViewModel.deleteAllAppointments()
                clearSelection()
            }
            .setNegativeButton("Cancel",null)
            .show()
    }

    private fun clearSelection() {
        selectItems.clear()
        isSelectionMode = false
        onSelectionChanged(false)
        notifyDataSetChanged()
    }

    private fun updateSelectmode() {
        isSelectionMode = selectItems.isNotEmpty()
        onSelectionChanged(isSelectionMode)
        notifyDataSetChanged()
    }




    fun setData(appointments: List<Appointment>) {
        listAppointment = appointments
        notifyDataSetChanged()
    }
}
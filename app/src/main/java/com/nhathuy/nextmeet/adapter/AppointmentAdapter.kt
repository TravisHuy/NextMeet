package com.nhathuy.nextmeet.adapter

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.fragment.PlaceFragment
import com.nhathuy.nextmeet.fragment.TimeFragment
import com.nhathuy.nextmeet.model.Appointment
import com.nhathuy.nextmeet.resource.Resource
import com.nhathuy.nextmeet.viewmodel.AppointmentViewModel
import com.nhathuy.nextmeet.viewmodel.CustomerViewModel
import kotlinx.coroutines.launch

class AppointmentAdapter(private val context:Context,
                         private var listAppointment: List<Appointment>,
                         private val customerViewModel: CustomerViewModel,
                         private val appointmentViewModel: AppointmentViewModel,
                         private val onSelectionChanged: (Boolean) -> Unit,
                         private val fragmentManager: FragmentManager,
                         private val lifecycleOwner: LifecycleOwner
                         ):RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {

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
            toggleSelection(adapterPosition)
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
        (holder.itemView.context as? LifecycleOwner)?.let { lifecycleOwner ->
            lifecycleOwner.lifecycleScope.launch {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    customerViewModel.getCustomerById(appointment.customerId)
                    customerViewModel.getCustomerById.collect { result ->
                        when (result) {
                            is Resource.Success -> {
                                holder.customerName.text = result.data?.name ?: "Unknown"
                            }
                            else -> {
                                // Keep the default "Unknown" text
                            }
                        }
                    }
                }
            }
        }

        holder.checkbox.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        holder.checkbox.isChecked = isSelected
        holder.threeDotMenu.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int {
        return listAppointment.size
    }

    private fun onEditClick(appointment: Appointment) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.add_alram)

        val window = dialog.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setGravity(Gravity.CENTER)

        val tabLayout = dialog.findViewById<TabLayout>(R.id.tablayout)
        val viewPager = dialog.findViewById<ViewPager2>(R.id.viewpager)
        val cancelButton = dialog.findViewById<Button>(R.id.cancel)
        val saveButton = dialog.findViewById<Button>(R.id.alram_save)

        val adapter = ViewPageAdapter(fragmentManager, lifecycleOwner.lifecycle)

        val timeFragment = TimeFragment().apply {
            arguments = Bundle().apply {
                putString("date", appointment.date)
                putString("time", appointment.time)
            }
        }
        val placeFragment = PlaceFragment().apply {
            arguments = Bundle().apply {
                putString("address", appointment.address)
            }
        }

        adapter.addFragment(timeFragment, context.getString(R.string.pick_date_amp_time))
        adapter.addFragment(placeFragment, context.getString(R.string.pick_place))

        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when(position) {
                0 -> context.getString(R.string.pick_date_amp_time)
                1 -> context.getString(R.string.pick_place)
                else -> null
            }
        }.attach()

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        saveButton.setOnClickListener {
            val (date, time) = timeFragment.getSelectDateTime()
            val address = placeFragment.getSelectAddress()
            val (repeatInterval, repeatUnit) = timeFragment.getRepeatInfo()

            val updatedAppointment = appointment.copy(
                date = date,
                time = time,
                address = address,
                notes = "Repeat: $repeatInterval $repeatUnit\t ${appointment.notes}"
            )

            appointmentViewModel.editAppointment(updatedAppointment)
            Toast.makeText(context, "Appointment updated", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
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
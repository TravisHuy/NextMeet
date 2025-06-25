package com.nhathuy.nextmeet.adapter


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nhathuy.nextmeet.databinding.ItemTodayAppointmentBinding
import com.nhathuy.nextmeet.model.AppointmentPlus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AppointmentTodayAdapter() :
    ListAdapter<AppointmentPlus, AppointmentTodayAdapter.AppointmentTodayViewHolder>(DiffCallback) {

    inner class AppointmentTodayViewHolder(val binding: ItemTodayAppointmentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(appointment: AppointmentPlus) {
            with(binding){
                tvTitle.text = appointment.title
                tvLocation.text = appointment.location
                tvTime.text = formatTime(appointment.startDateTime)
            }
        }
    }

    private fun formatTime(time: Long?): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedDate = formatter.format(Date(time!!))
        return formattedDate
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AppointmentTodayViewHolder {
        val binding =
            ItemTodayAppointmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppointmentTodayViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: AppointmentTodayViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<AppointmentPlus>() {

        override fun areItemsTheSame(
            oldItem: AppointmentPlus,
            newItem: AppointmentPlus
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: AppointmentPlus,
            newItem: AppointmentPlus
        ): Boolean {
            return oldItem == newItem
        }
    }
}
package com.nhathuy.nextmeet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ItemHistoryAppointmentBinding
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.AppointmentWithContact
import com.nhathuy.nextmeet.model.AppointmentStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAppointmentAdapter(
    private val onItemClick: (AppointmentWithContact) -> Unit,
    private val onRepeatClick: (AppointmentWithContact) -> Unit,
    private val onLongClick : (AppointmentWithContact) -> Unit
) : ListAdapter<AppointmentWithContact, HistoryAppointmentAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    private val appointmentWithContact = mutableListOf<AppointmentWithContact>()
    private val selectedAppointments = mutableSetOf<Int>()

    fun getSelectedAppointments() : List<AppointmentWithContact> {
        return appointmentWithContact.filter { selectedAppointments.contains(it.appointment.id) }
    }

    //xóa appointment
    fun removeAppointments(appointmentsToRemove: List<AppointmentWithContact>) {
        appointmentsToRemove.forEach { appointment ->
            val position = appointmentWithContact.indexOf(appointment)
            if (position != -1) {
                appointmentWithContact.removeAt(position)
                notifyItemRemoved(position)
            }
        }
        selectedAppointments.clear()
    }

    fun restoreAppointments(appointmentsToRestore : List<AppointmentWithContact>) {
        appointmentsToRestore.forEach { appointment ->
            val insertPosition = findInsertPosition(appointment)
            appointmentWithContact.add(insertPosition, appointment)
            notifyItemInserted(insertPosition)
        }
    }

    private fun  findInsertPosition(appointment: AppointmentWithContact) : Int{
        for(i in appointmentWithContact.indices){
            if(appointmentWithContact[i].appointment.startDateTime > appointment.appointment.startDateTime) {
                return i
            }
        }

        return appointmentWithContact.size
    }


    inner class HistoryViewHolder(private val binding: ItemHistoryAppointmentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(appointmentWithContact: AppointmentWithContact) {
            val appointment = appointmentWithContact.appointment

            with(binding) {
                tvTitle.text = appointment.title
                tvDateTime.text = formatDateTime(appointment.startDateTime)

                if (appointment.location.isNotEmpty()) {
                    layoutLocation.visibility = View.VISIBLE
                    tvLocation.text = appointment.location
                } else {
                    layoutLocation.visibility = View.GONE
                }

                if (appointment.description.isNotEmpty()) {
                    tvDescription.visibility = View.VISIBLE
                    tvDescription.text = appointment.description
                } else {
                    tvDescription.visibility = View.GONE
                }

                // Xử lý hiển thị contact name
                if (appointment.contactId != null && !appointmentWithContact.contactName.isNullOrEmpty()) {
                    layoutContact.visibility = View.VISIBLE
                    tvContactName.text = appointmentWithContact.contactName
                } else if (appointment.contactId != null) {
                    layoutContact.visibility = View.VISIBLE
                    tvContactName.text = root.context.getString(R.string.contact_deleted)
                } else {
                    layoutContact.visibility = View.GONE
                }

                val duration = calculateDuration(appointment.startDateTime, appointment.endDateTime)
                tvDuration.text = duration

                // set status chip
                setupStatusChip(appointment.status)

                // set color indicator
                setupColorIndicator(appointment.status)

                // set additional information
                setupAdditionalInfo(appointment)

                if (appointment.status == AppointmentStatus.COMPLETED) {
                    layoutActions.visibility = View.VISIBLE
                    btnRepeat.setOnClickListener { onRepeatClick(appointmentWithContact) }
                    btnViewDetails.setOnClickListener { onItemClick(appointmentWithContact) }
                } else {
                    layoutActions.visibility = View.GONE
                }

                root.setOnClickListener { onItemClick(appointmentWithContact) }

                root.setOnLongClickListener {
                    onLongClick(appointmentWithContact)
                    true
                }
            }
        }

        private fun setupStatusChip(status: AppointmentStatus) {
            with(binding.chipStatus) {
                text = status.displayName

                when (status) {
                    AppointmentStatus.COMPLETED -> {
                        chipBackgroundColor = ContextCompat.getColorStateList(
                            context, R.color.success_background
                        )
                        setTextColor(ContextCompat.getColor(context, R.color.success_color))
                    }

                    AppointmentStatus.CANCELLED -> {
                        chipBackgroundColor = ContextCompat.getColorStateList(
                            context, R.color.warning_background
                        )
                        setTextColor(ContextCompat.getColor(context, R.color.warning_color))
                    }

                    AppointmentStatus.MISSED -> {
                        chipBackgroundColor = ContextCompat.getColorStateList(
                            context, R.color.error_background
                        )
                        setTextColor(ContextCompat.getColor(context, R.color.error_color))
                    }

                    else -> {
                        chipBackgroundColor = ContextCompat.getColorStateList(
                            context, R.color.gray_light
                        )
                        setTextColor(ContextCompat.getColor(context, R.color.light_text_secondary))
                    }
                }
            }
        }

        private fun setupColorIndicator(status: AppointmentStatus) {
            val colorRes = when (status) {
                AppointmentStatus.COMPLETED -> R.color.success_color
                AppointmentStatus.CANCELLED -> R.color.warning_color
                AppointmentStatus.MISSED -> R.color.error_color
                else -> R.color.light_primary
            }

            binding.viewColorIndicator.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, colorRes)
            )
        }

        private fun setupAdditionalInfo(appointment: AppointmentPlus) {
            with(binding) {
                when (appointment.status) {
                    AppointmentStatus.COMPLETED -> {
                        layoutAdditionalInfo.visibility = View.VISIBLE
                        ivAdditionalIcon.setImageResource(R.drawable.ic_check_circle)
                        tvAdditionalInfo.text = root.context.getString(R.string.successful_comletion)

                        val iconColor = ContextCompat.getColor(root.context, R.color.success_color)
                        ivAdditionalIcon.setColorFilter(iconColor)
                        tvAdditionalInfo.setTextColor(iconColor)
                    }

                    AppointmentStatus.CANCELLED -> {
                        layoutAdditionalInfo.visibility = View.VISIBLE
                        ivAdditionalIcon.setImageResource(R.drawable.ic_cancel)
                        tvAdditionalInfo.text = root.context.getString(R.string.cancelled)

                        val iconColor = ContextCompat.getColor(root.context, R.color.warning_color)
                        ivAdditionalIcon.setColorFilter(iconColor)
                        tvAdditionalInfo.setTextColor(iconColor)
                    }

                    AppointmentStatus.MISSED -> {
                        layoutAdditionalInfo.visibility = View.VISIBLE
                        ivAdditionalIcon.setImageResource(R.drawable.ic_error)
                        tvAdditionalInfo.text = root.context.getString(R.string.missed)

                        val iconColor = ContextCompat.getColor(root.context, R.color.error_color)
                        ivAdditionalIcon.setColorFilter(iconColor)
                        tvAdditionalInfo.setTextColor(iconColor)
                    }

                    else -> {
                        layoutAdditionalInfo.visibility = View.GONE
                    }
                }
            }
        }

        private fun formatDateTime(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

        private fun calculateDuration(startTime: Long, endTime: Long): String {
            val durationMs = endTime - startTime
            val hours = durationMs / (1000 * 60 * 60)
            val minutes = (durationMs % (1000 * 60 * 60)) / (1000 * 60)

            return when {
                hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                hours > 0 -> "${hours}h"
                minutes > 0 -> "${minutes}m"
                else -> "< 1m"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryAppointmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<AppointmentWithContact>() {
        override fun areItemsTheSame(
            oldItem: AppointmentWithContact,
            newItem: AppointmentWithContact
        ): Boolean {
            return oldItem.appointment.id == newItem.appointment.id
        }

        override fun areContentsTheSame(
            oldItem: AppointmentWithContact,
            newItem: AppointmentWithContact
        ): Boolean {
            return oldItem == newItem
        }
    }
}
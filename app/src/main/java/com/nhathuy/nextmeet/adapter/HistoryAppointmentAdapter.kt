package com.nhathuy.nextmeet.adapter

import android.content.ClipData
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
import com.nhathuy.nextmeet.model.AppointmentStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAppointmentAdapter(
    private val onItemClick: (AppointmentPlus) -> Unit,
    private val onRepeatClick: (AppointmentPlus) -> Unit
) : ListAdapter<AppointmentPlus, HistoryAppointmentAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    inner class HistoryViewHolder(private val binding: ItemHistoryAppointmentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(appointment: AppointmentPlus) {
            with(binding) {
                tvTitle.text = appointment.title
                tvDateTime.text = formatDateTime(appointment.startDateTime)
                tvLocation.text = appointment.location

                tvDescription.text = appointment.description

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
                    btnRepeat.setOnClickListener { onRepeatClick(appointment) }
                    btnViewDetails.setOnClickListener { onItemClick(appointment) }
                } else {
                    layoutActions.visibility = View.GONE
                }

                root.setOnClickListener { onItemClick(appointment) }
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
                        tvAdditionalInfo.text = "Hoàn thành thành công"

                        val iconColor = ContextCompat.getColor(root.context, R.color.success_color)
                        ivAdditionalIcon.setColorFilter(iconColor)
                        tvAdditionalInfo.setTextColor(iconColor)
                    }

                    AppointmentStatus.CANCELLED -> {
                        layoutAdditionalInfo.visibility = View.VISIBLE
                        ivAdditionalIcon.setImageResource(R.drawable.ic_cancel)
                        tvAdditionalInfo.text = "Đã hủy"

                        val iconColor = ContextCompat.getColor(root.context, R.color.warning_color)
                        ivAdditionalIcon.setColorFilter(iconColor)
                        tvAdditionalInfo.setTextColor(iconColor)
                    }

                    AppointmentStatus.MISSED -> {
                        layoutAdditionalInfo.visibility = View.VISIBLE
                        ivAdditionalIcon.setImageResource(R.drawable.ic_error)
                        tvAdditionalInfo.text = "Đã bỏ lỡ"

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


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HistoryAppointmentAdapter.HistoryViewHolder {
        val binding = ItemHistoryAppointmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: HistoryAppointmentAdapter.HistoryViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<AppointmentPlus>() {
        override fun areItemsTheSame(oldItem: AppointmentPlus, newItem: AppointmentPlus): Boolean {
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
package com.nhathuy.nextmeet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ItemAppointmentLayoutBinding
import com.nhathuy.nextmeet.model.AppointmentPlus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppointmentPlusAdapter(
    private val appointments: MutableList<AppointmentPlus>,
    private val onClickListener: (AppointmentPlus) -> Unit,
    private val onLongClickListener: (AppointmentPlus) -> Unit,
    private val onPinClickListener: (AppointmentPlus) -> Unit,
    private val navigationMap: (AppointmentPlus) -> Unit,
    private val onSelectionChanged: (Int) -> Unit = { }
) :
    RecyclerView.Adapter<AppointmentPlusAdapter.AppointmentPlusViewHolder>() {

    inner class AppointmentPlusViewHolder(val binding: ItemAppointmentLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(appointment: AppointmentPlus) {
            with(binding) {
                appointmentTitle.text = appointment.title
                appointmentLocation.text = appointment.location
                appointmentDescription.text = appointment.description
                appointmentStartTime.text = formatTime(appointment.startDateTime)

                root.setOnClickListener {
                    onClickListener(appointment)
                }

                root.setOnLongClickListener {
                    onLongClickListener(appointment)
                    true
                }

                ivPin.visibility = if (appointment.isPinned) View.VISIBLE else View.GONE
                ivPin.setOnClickListener { onPinClickListener(appointment) }

                setupBackgroundColor(appointment.color)
            }
        }
        private fun formatTime(time: Long?): String {
            val formatter = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
            val formattedDate = formatter.format(Date(time!!))
            return formattedDate
        }

        private fun setupBackgroundColor(colorName: String) {
            val colorResId = getColorResourceId(colorName)
            val color = ContextCompat.getColor(binding.root.context, colorResId)
            binding.cardAppointmentLayout.setCardBackgroundColor(color)
        }

        private fun getColorResourceId(colorName: String): Int {
            return when (colorName) {
                "color_white" -> R.color.color_white
                "color_red" -> R.color.color_red
                "color_orange" -> R.color.color_orange
                "color_yellow" -> R.color.color_yellow
                "color_green" -> R.color.color_green
                "color_teal" -> R.color.color_teal
                "color_blue" -> R.color.color_blue
                "color_dark_blue" -> R.color.color_dark_blue
                "color_purple" -> R.color.color_purple
                "color_pink" -> R.color.color_pink
                "color_brown" -> R.color.color_brown
                "color_gray" -> R.color.color_gray
                else -> R.color.color_white
            }
        }
    }



    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AppointmentPlusAdapter.AppointmentPlusViewHolder {
        val binding = ItemAppointmentLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppointmentPlusViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: AppointmentPlusAdapter.AppointmentPlusViewHolder,
        position: Int
    ) {
        holder.bind(appointments[position])
    }

    override fun getItemCount(): Int = appointments.size

    fun updateAppointments(newAppointments: List<AppointmentPlus>) {
        val diffResult = DiffUtil.calculateDiff(
            AppointmentDiffCallback(appointments.toList(),newAppointments)
        )
        appointments.clear()
        appointments.addAll(newAppointments)
        diffResult.dispatchUpdatesTo(this)
    }

    private class AppointmentDiffCallback(
        private val oldList: List<AppointmentPlus>,
        private val newList: List<AppointmentPlus>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
package com.nhathuy.nextmeet.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ItemAppointmentLayoutBinding
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.AppointmentStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppointmentPlusAdapter(
    private val appointments: MutableList<AppointmentPlus>,
    private val onClickListener: (AppointmentPlus) -> Unit,
    private val onLongClickListener: (AppointmentPlus,Int) -> Unit,
    private val onPinClickListener: (AppointmentPlus) -> Unit,
    private val navigationMap: (AppointmentPlus) -> Unit,
    private val onSelectionChanged: (Int) -> Unit = {}
) : RecyclerView.Adapter<AppointmentPlusAdapter.AppointmentPlusViewHolder>() {

    private var multiSelectMode = false
    private val selectedAppointments = mutableSetOf<Int>()

    fun setMultiSelectionMode(enabled: Boolean) {
        multiSelectMode = enabled
        if (!enabled) {
            selectedAppointments.clear()
            onSelectionChanged(0)
        }
        notifyDataSetChanged()
    }

    fun isMultiSelectMode(): Boolean = multiSelectMode

    fun isSelected(appointmentId:Int) : Boolean = selectedAppointments.contains(appointmentId)

    fun toggleSelection(appointmentId:Int){
        if(selectedAppointments.contains(appointmentId)){
            selectedAppointments.remove(appointmentId)
        }
        else{
            selectedAppointments.add(appointmentId)
        }
        onSelectionChanged(selectedAppointments.size)

        val position = appointments.indexOfFirst {
            it.id == appointmentId
        }

        if(position !=-1 ){
            notifyItemChanged(position)
        }
    }

    fun getSelectedAppointments(): List<AppointmentPlus> {
        return appointments.filter { selectedAppointments.contains(it.id) }
    }

    fun clearSelection() {
        selectedAppointments.clear()
        onSelectionChanged(0)
        notifyDataSetChanged()
    }

    fun getSelectedCount() : Int = selectedAppointments.size

    fun selectAll(){
        selectedAppointments.clear()
        selectedAppointments.addAll(appointments.map { it.id })
        onSelectionChanged(selectedAppointments.size)
        notifyDataSetChanged()
    }
    inner class AppointmentPlusViewHolder(val binding: ItemAppointmentLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(appointment: AppointmentPlus) {
            with(binding) {
                appointmentTitle.text = appointment.title
                appointmentLocation.text = appointment.location
                appointmentDescription.text = appointment.description
                appointmentStartTime.text = formatTime(appointment.startDateTime)

                ivPin.visibility = if (appointment.isPinned) View.VISIBLE else View.GONE

                ivPin.setOnClickListener { onPinClickListener(appointment) }

                // Bind status
                bindStatus(appointment)


                setupBackgroundColor(appointment.color)

                //click
                setupClickListeners(appointment)


                if (multiSelectMode) {
                    if (selectedAppointments.contains(appointment.id)) {
                        cardAppointmentLayout.apply {
                            strokeWidth = 6
                            strokeColor = ContextCompat.getColor(itemView.context, R.color.selection_border_color)
                            alpha = 0.8f
                        }
                    } else {
                        cardAppointmentLayout.apply {
                            strokeWidth = 1
                            strokeColor = ContextCompat.getColor(itemView.context, R.color.gray_light) // hoặc màu mặc định
                            alpha = 1.0f
                        }
                    }
                } else {
                    cardAppointmentLayout.apply {
                        strokeWidth = 1
                        strokeColor = ContextCompat.getColor(itemView.context, R.color.gray_light) // hoặc màu mặc định
                        alpha = 1.0f
                    }
                }

            }
        }

        private fun bindStatus(appointment: AppointmentPlus) {
            binding.tvStatus.apply {
                text = appointment.status.displayName.uppercase()

                // Set background color based on status
                val backgroundColor = getStatusColor(appointment.status)
                background = ContextCompat.getDrawable(context, R.drawable.status_badge_background)
                backgroundTintList = ColorStateList.valueOf(backgroundColor)

                // Set text color (white for most, dark for light backgrounds)
                setTextColor(getStatusTextColor(appointment.status))
            }
        }
        private fun getStatusColor(status: AppointmentStatus): Int {
            return when (status) {
                AppointmentStatus.SCHEDULED -> ContextCompat.getColor(binding.root.context, R.color.blue)
                AppointmentStatus.PREPARING -> ContextCompat.getColor(binding.root.context, R.color.orange)
                AppointmentStatus.TRAVELLING -> ContextCompat.getColor(binding.root.context, R.color.light_primary)
                AppointmentStatus.IN_PROGRESS -> ContextCompat.getColor(binding.root.context, R.color.green)
                AppointmentStatus.DELAYED -> ContextCompat.getColor(binding.root.context, R.color.red)
                else -> 0
            }
        }

        private fun getStatusTextColor(status: AppointmentStatus): Int {
            // Hầu hết status dùng text trắng, chỉ một số status nhạt dùng text đen
            return when (status) {
                AppointmentStatus.COMPLETED -> ContextCompat.getColor(binding.root.context, R.color.black)
                else -> ContextCompat.getColor(binding.root.context, R.color.white)
            }
        }
        private fun setupClickListeners(appointment: AppointmentPlus){
            binding.apply {
                // Click listener for the appointment card
                ivNavigationMap.setOnClickListener {
                    navigationMap(appointment)
                }


                ivPin.setOnClickListener {
                    onPinClickListener(appointment)
                }

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
        val appointment = appointments[position]
        holder.bind(appointment)

        holder.binding.apply {
            root.setOnClickListener(null)
            root.setOnLongClickListener(null)

            root.setOnLongClickListener {
                if(!multiSelectMode){
                    setMultiSelectionMode(true)
                }
                toggleSelection(appointment.id)
                onLongClickListener(appointment,position)
                true
            }
            root.setOnClickListener {
                if(multiSelectMode){
                    toggleSelection(appointment.id)
                } else {
                    onClickListener(appointment)
                }
            }
        }
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

    // xóa appointment
    fun removeAppointments(appointmentsToRemove : List<AppointmentPlus>){
        appointmentsToRemove.forEach {
            appointment ->
            val position = appointments.indexOf(appointment)
            if (position != -1) {
                appointments.removeAt(position)
                notifyItemRemoved(position)
            }
        }
    }
    // hoàn appointment
    fun restoreAppointments(appointmentsToRestore: List<AppointmentPlus>){
        appointmentsToRestore.forEach { appointment ->
            // Tìm vị trí phù hợp để insert (có thể sort theo thời gian)
            val insertPosition = findInsertPosition(appointment)
            appointments.add(insertPosition, appointment)
            notifyItemInserted(insertPosition)
        }
    }
    // Logic để tìm vị trí phù hợp
    private fun findInsertPosition(appointment: AppointmentPlus): Int {
        for (i in appointments.indices) {
            if (appointments[i].startDateTime > appointment.startDateTime) {
                return i
            }
        }
        return appointments.size
    }

    fun getCurrentAppointments(): List<AppointmentPlus> {
        return appointments.toList()
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
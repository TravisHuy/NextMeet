package com.nhathuy.nextmeet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ItemLocationStatisticsBinding
import com.nhathuy.nextmeet.model.LocationStatistics


/**
 * Adapter cho RecyclerView hiển thị thống kê các địa điểm.
 *
 * @param onLocationClick Hàm callback khi người dùng nhấn vào một địa điểm.
 */
class LocationStatisticsAdapter(
    private val onLocationClick: (LocationStatistics) -> Unit
) : ListAdapter<LocationStatistics, LocationStatisticsAdapter.LocationViewHolder>(
    LocationDiffCallback()
) {

    /**
     * ViewHolder cho từng item thống kê địa điểm.
     */
    inner class LocationViewHolder(val binding: ItemLocationStatisticsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        /**
         * Gán dữ liệu từ LocationStatistics vào view.
         */
        fun bind(locationStatistics: LocationStatistics) {
            binding.apply {
                tvLocationName.text = locationStatistics.locationName
                tvAppointmentCount.text = root.context.getString(
                    R.string.appointment_count_format,
                    locationStatistics.appointmentCount
                )
                if (locationStatistics.upcomingCount > 0) {
                    tvUpcomingCount.text = root.context.getString(
                        R.string.upcoming_count_format,
                        locationStatistics.upcomingCount
                    )
                    tvUpcomingCount.visibility = android.view.View.VISIBLE
                } else {
                    tvUpcomingCount.visibility = android.view.View.GONE
                }
                root.setOnClickListener { onLocationClick(locationStatistics) }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LocationStatisticsAdapter.LocationViewHolder {
        val binding = ItemLocationStatisticsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LocationViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: LocationStatisticsAdapter.LocationViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    /**
     * DiffUtil để tối ưu cập nhật danh sách.
     */
    private class LocationDiffCallback : DiffUtil.ItemCallback<LocationStatistics>() {
        override fun areItemsTheSame(
            oldItem: LocationStatistics,
            newItem: LocationStatistics
        ): Boolean {
            return oldItem.locationName == newItem.locationName
        }

        override fun areContentsTheSame(
            oldItem: LocationStatistics,
            newItem: LocationStatistics
        ): Boolean {
            return oldItem == newItem
        }
    }

}
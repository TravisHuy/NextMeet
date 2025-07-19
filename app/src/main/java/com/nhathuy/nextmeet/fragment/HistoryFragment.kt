package com.nhathuy.nextmeet.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.adapter.HistoryAppointmentAdapter
import com.nhathuy.nextmeet.databinding.FragmentHistoryBinding
import com.nhathuy.nextmeet.viewmodel.HistoryViewModel
import com.nhathuy.nextmeet.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.nhathuy.nextmeet.model.AppointmentPlus
import dagger.hilt.android.AndroidEntryPoint

import kotlinx.coroutines.flow.combine

@AndroidEntryPoint
class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private var currentUserId: Int = 0

    private lateinit var historyAppointmentAdapter: HistoryAppointmentAdapter
    private lateinit var historyViewModel: HistoryViewModel
    private lateinit var userViewModel: UserViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViewModels()
        setupUI()
        observeData()
    }

    private fun initializeViewModels() {
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        historyViewModel = ViewModelProvider(this)[HistoryViewModel::class.java]
    }

    private fun setupUI() {
        setupUserInfo()
        setupRecyclerView()
        setupFilterTabs()
    }

    private fun setupUserInfo() {
        userViewModel.getCurrentUser().observe(viewLifecycleOwner) { user ->
            user?.let {
                currentUserId = user.id
                historyViewModel.loadHistoryData(currentUserId)
            }
        }
    }

    private fun setupRecyclerView() {
        historyAppointmentAdapter = HistoryAppointmentAdapter(onItemClick = { appointment ->
            handleItemClick(appointment)
        }, onRepeatClick = { appointment ->
            handleRepeatClick(appointment)
        })

        binding.recyclerHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAppointmentAdapter
        }
    }

    private fun setupFilterTabs() {
        // Set initial selected state
        updateFilterUI(HistoryViewModel.HistoryFilter.ALL)

        // Set up click listeners for filter tabs
        binding.cardAll.setOnClickListener {
            historyViewModel.setFilter(HistoryViewModel.HistoryFilter.ALL)
        }

        binding.cardCompleted.setOnClickListener {
            historyViewModel.setFilter(HistoryViewModel.HistoryFilter.COMPLETED)
        }

        binding.cardCancelled.setOnClickListener {
            historyViewModel.setFilter(HistoryViewModel.HistoryFilter.CANCELLED)
        }

        binding.cardMissed.setOnClickListener {
            historyViewModel.setFilter(HistoryViewModel.HistoryFilter.MISSED)
        }
    }

    private fun observeData() {
        // Check if fragment is still attached before collecting flows
        viewLifecycleOwner.lifecycleScope.launch {
            historyViewModel.isLoading.collect { isLoading ->
                if (isBindingValid()) {
                    updateLoadingState(isLoading)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            historyViewModel.errorMessage.collect { error ->
                if (isBindingValid() && error != null) {
                    showError(error)
                    historyViewModel.clearError()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            historyViewModel.currentFilter.collect { filter ->
                if (isBindingValid()) {
                    updateFilterUI(filter)
                    // Update empty state when filter changes
                    updateEmptyStateForCurrentFilter()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            historyViewModel.historyCounts.collect { counts ->
                if (isBindingValid()) {
                    updateStatistics(counts)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                historyViewModel.filteredAppointments,
                historyViewModel.isLoading,
                historyViewModel.errorMessage
            ) { appointments, isLoading, errorMessage ->
                Triple(appointments, isLoading, errorMessage)
            }.collect { (appointments, isLoading, errorMessage) ->
                if (isBindingValid()) {
                    handleAppointmentsDisplay(appointments, isLoading, errorMessage)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            historyViewModel.groupedAppointments.collect { groupedAppointments ->
                if (isBindingValid()) {
                    Log.d("HistoryFragment", "Grouped appointments: ${groupedAppointments.size} groups")
                }
            }
        }
    }

    private fun isBindingValid(): Boolean {
        return _binding != null && isAdded && !isRemoving && !isDetached
    }

    private fun handleAppointmentsDisplay(
        appointments: List<AppointmentPlus>,
        isLoading: Boolean,
        errorMessage: String?
    ) {
        when {
            // Loading state
            isLoading -> {
                showLoadingState()
            }
            // Error state
            errorMessage != null -> {
                showErrorState(errorMessage)
            }
            // Empty state
            appointments.isEmpty() -> {
                showEmptyState()
            }
            // Content state
            else -> {
                showContentState(appointments)
            }
        }
    }

    private fun showLoadingState() {
        if (!isBindingValid()) return

        binding.apply {
            layoutLoading.visibility = View.VISIBLE
            recyclerHistory.visibility = View.GONE
            layoutEmptyState.visibility = View.GONE
        }
    }

    private fun showErrorState(errorMessage: String) {
        if (!isBindingValid()) return

        binding.apply {
            layoutLoading.visibility = View.GONE
            recyclerHistory.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE

            tvEmptyTitle.text = "Có lỗi xảy ra"
            tvEmptyDescription.text = errorMessage
            ivEmptyIcon.setImageResource(R.drawable.ic_error)
        }
    }

    private fun showEmptyState() {
        if (!isBindingValid()) return

        binding.apply {
            layoutLoading.visibility = View.GONE
            recyclerHistory.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
        }
        updateEmptyStateForCurrentFilter()
    }

    private fun showContentState(appointments: List<AppointmentPlus>) {
        if (!isBindingValid()) return

        binding.apply {
            layoutLoading.visibility = View.GONE
            layoutEmptyState.visibility = View.GONE
            recyclerHistory.visibility = View.VISIBLE
        }

        historyAppointmentAdapter.submitList(appointments)
    }

    private fun updateEmptyStateForCurrentFilter() {
        if (!isBindingValid()) return

        val currentFilter = historyViewModel.currentFilter.value

        binding.apply {
            when (currentFilter) {
                HistoryViewModel.HistoryFilter.ALL -> {
                    tvEmptyTitle.text = "Chưa có lịch sử cuộc hẹn"
                    tvEmptyDescription.text = "Các cuộc hẹn đã hoàn thành, hủy hoặc bỏ lỡ sẽ xuất hiện ở đây"
                    ivEmptyIcon.setImageResource(R.drawable.ic_history) // Add appropriate icon
                }
                HistoryViewModel.HistoryFilter.COMPLETED -> {
                    tvEmptyTitle.text = "Chưa có cuộc hẹn hoàn thành"
                    tvEmptyDescription.text = "Các cuộc hẹn bạn đã tham gia thành công sẽ xuất hiện ở đây"
                    ivEmptyIcon.setImageResource(R.drawable.ic_check_circle) // Add appropriate icon
                }
                HistoryViewModel.HistoryFilter.CANCELLED -> {
                    tvEmptyTitle.text = "Chưa có cuộc hẹn bị hủy"
                    tvEmptyDescription.text = "Các cuộc hẹn bạn đã hủy sẽ xuất hiện ở đây"
                    ivEmptyIcon.setImageResource(R.drawable.ic_cancel) // Add appropriate icon
                }
                HistoryViewModel.HistoryFilter.MISSED -> {
                    tvEmptyTitle.text = "Chưa có cuộc hẹn bị bỏ lỡ"
                    tvEmptyDescription.text = "Các cuộc hẹn bạn đã bỏ lỡ sẽ xuất hiện ở đây"
                    ivEmptyIcon.setImageResource(R.drawable.ic_schedule) // Add appropriate icon
                }
            }
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        if (isLoading && isBindingValid()) {
            showLoadingState()
        }
    }

    private fun updateFilterUI(currentFilter: HistoryViewModel.HistoryFilter) {
        if (!isBindingValid()) return

        // Reset all cards to unselected state
        resetAllFilterCards()

        // Set selected card based on current filter
        when (currentFilter) {
            HistoryViewModel.HistoryFilter.ALL -> {
                setCardSelected(binding.cardAll, true)
            }
            HistoryViewModel.HistoryFilter.COMPLETED -> {
                setCardSelected(binding.cardCompleted, true)
            }
            HistoryViewModel.HistoryFilter.CANCELLED -> {
                setCardSelected(binding.cardCancelled, true)
            }
            HistoryViewModel.HistoryFilter.MISSED -> {
                setCardSelected(binding.cardMissed, true)
            }
        }
    }

    private fun resetAllFilterCards() {
        if (!isBindingValid()) return

        setCardSelected(binding.cardAll, false)
        setCardSelected(binding.cardCompleted, false)
        setCardSelected(binding.cardCancelled, false)
        setCardSelected(binding.cardMissed, false)
    }

    private fun setCardSelected(card: View, isSelected: Boolean) {
        if (!isBindingValid()) return

        val cardView = card as com.google.android.material.card.MaterialCardView
        val textView = cardView.getChildAt(0) as android.widget.TextView

        if (isSelected) {
            cardView.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.primary_color)
            )
            cardView.strokeColor = ContextCompat.getColor(requireContext(), R.color.primary_color)
            textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            textView.setTypeface(null, android.graphics.Typeface.BOLD)
        } else {
            cardView.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), android.R.color.white)
            )
            cardView.strokeColor = ContextCompat.getColor(requireContext(), R.color.gray_light)
            textView.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.light_text_secondary)
            )
            textView.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }

    private fun updateStatistics(counts: com.nhathuy.nextmeet.model.HistoryCounts) {
        if (!isBindingValid()) return

        binding.apply {
            tvCompletedCount.text = counts.completed.toString()
            tvCancelledCount.text = counts.cancelled.toString()
            tvMissedCount.text = counts.missed.toString()
        }
    }

    private fun showError(message: String) {
        if (!isBindingValid()) return

        try {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                .setAction("Thử lại") {
                    refreshData()
                }
                .show()
        } catch (e: Exception) {
            Log.e("HistoryFragment", "Error showing snackbar: ${e.message}")
        }

        Log.e("HistoryFragment", "Error: $message")
    }

    private fun handleItemClick(appointment: AppointmentPlus) {
        Log.d("HistoryFragment", "Appointment clicked: ${appointment.title}")
    }

    private fun handleRepeatClick(appointment: AppointmentPlus) {
        Log.d("HistoryFragment", "Repeat appointment clicked: ${appointment.title}")
    }

    fun refreshData() {
        if (currentUserId != 0) {
            historyViewModel.refresh(currentUserId)
        } else {
            userViewModel.getCurrentUser().observe(viewLifecycleOwner) { user ->
                user?.let {
                    currentUserId = user.id
                    historyViewModel.refresh(currentUserId)
                }
            }
        }
    }

    fun getAppointmentsInDateRange(startDate: java.util.Calendar, endDate: java.util.Calendar) {
        if (currentUserId != 0) {
            historyViewModel.getAppointmentsInDateRange(currentUserId, startDate, endDate)
        }
    }

    fun setFilter(filter: HistoryViewModel.HistoryFilter) {
        historyViewModel.setFilter(filter)
    }

    fun getCurrentFilter(): HistoryViewModel.HistoryFilter {
        return historyViewModel.currentFilter.value
    }

    fun hasDataForCurrentFilter(): Boolean {
        return historyViewModel.hasAppointmentsForCurrentFilter()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
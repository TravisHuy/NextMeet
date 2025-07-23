package com.nhathuy.nextmeet.fragment

import android.app.Dialog
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
import android.view.Gravity
import android.view.Window
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.nhathuy.nextmeet.databinding.BottomSheetHistoryBinding
import com.nhathuy.nextmeet.model.AppointmentWithContact
import com.nhathuy.nextmeet.viewmodel.AppointmentPlusViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import androidx.appcompat.app.AlertDialog
import android.content.DialogInterface
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@AndroidEntryPoint
class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private var currentUserId: Int = 0

    private lateinit var historyAppointmentAdapter: HistoryAppointmentAdapter

    private lateinit var historyViewModel: HistoryViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var appointmentViewModel: AppointmentPlusViewModel

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
        appointmentViewModel = ViewModelProvider(this)[AppointmentPlusViewModel::class.java]
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
        historyAppointmentAdapter = HistoryAppointmentAdapter(
            onItemClick = { appointment ->
                handleItemClick(appointment)
            }, onRepeatClick = { appointment ->
                handleRepeatClick(appointment)
            },
            onLongClick = { appointment ->
                showLongClickMenu(appointment)
            }
        )

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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    historyViewModel.isLoading.collect { isLoading ->
                        if (isBindingValid()) {
                            updateLoadingState(isLoading)
                        }
                    }
                }

                launch {
                    historyViewModel.errorMessage.collect { error ->
                        if (isBindingValid() && error != null) {
                            showError(error)
                            historyViewModel.clearError()
                        }
                    }
                }

                launch {
                    historyViewModel.currentFilter.collect { filter ->
                        if (isBindingValid()) {
                            updateFilterUI(filter)
                            // Update empty state when filter changes
                            updateEmptyStateForCurrentFilter()
                        }
                    }
                }
                launch {
                    historyViewModel.historyCounts.collect { counts ->
                        if (isBindingValid()) {
                            updateStatistics(counts)
                        }
                    }
                }
                launch {
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
                launch {
                    historyViewModel.groupedAppointments.collect { groupedAppointments ->
                        if (isBindingValid()) {
                            Log.d(
                                "HistoryFragment",
                                "Grouped appointments: ${groupedAppointments.size} groups"
                            )
                        }
                    }
                }
            }
        }
    }

    private fun isBindingValid(): Boolean {
        return _binding != null && isAdded && !isRemoving && !isDetached
    }

    private fun handleAppointmentsDisplay(
        appointments: List<AppointmentWithContact>,
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

            tvEmptyTitle.text = getString(R.string.error_occurred   )
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

    private fun showContentState(appointments: List<AppointmentWithContact>) {
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
                    tvEmptyTitle.text = getString(R.string.history_empty_all_title)
                    tvEmptyDescription.text =
                        getString(R.string.history_empty_all_desc)
                    ivEmptyIcon.setImageResource(R.drawable.ic_history) // Add appropriate icon
                }

                HistoryViewModel.HistoryFilter.COMPLETED -> {
                    tvEmptyTitle.text = getString(R.string.history_empty_completed_title)
                    tvEmptyDescription.text =
                        getString(R.string.history_empty_completed_desc)
                    ivEmptyIcon.setImageResource(R.drawable.ic_check_circle) // Add appropriate icon
                }

                HistoryViewModel.HistoryFilter.CANCELLED -> {
                    tvEmptyTitle.text = getString(R.string.history_empty_cancelled_title)
                    tvEmptyDescription.text = getString(R.string.history_empty_cancelled_desc)
                    ivEmptyIcon.setImageResource(R.drawable.ic_cancel) // Add appropriate icon
                }

                HistoryViewModel.HistoryFilter.MISSED -> {
                    tvEmptyTitle.text = getString(R.string.history_empty_missed_title)
                    tvEmptyDescription.text = getString(R.string.history_empty_missed_desc)
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
                .setAction(getString(R.string.retry)) {
                    refreshData()
                }
                .show()
        } catch (e: Exception) {
            Log.e("HistoryFragment", "Error showing snackbar: ${e.message}")
        }

        Log.e("HistoryFragment", "Error: $message")
    }

    private fun handleItemClick(appointment: AppointmentWithContact) {
        Log.d("HistoryFragment", "Appointment clicked: ${appointment.appointment.title}")
    }

    private fun handleRepeatClick(appointment: AppointmentWithContact) {
        Log.d("HistoryFragment", "Repeat appointment clicked: ${appointment.appointment.title}")
    }

    private fun showLongClickMenu(appointment: AppointmentWithContact) {
        val dialogBinding = BottomSheetHistoryBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.llDelete.setOnClickListener {
            dialog.dismiss()
            showDeleteConfirmationDialog(appointment)
        }

        dialogBinding.llDeleteAll.setOnClickListener {
            dialog.dismiss()
            showDeleteAllConfirmationDialog()
        }

        dialogBinding.root.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(R.drawable.rounded_background)
            attributes.windowAnimations = R.style.DialogAnimation
            setGravity(Gravity.BOTTOM)
        }
    }

    private fun showDeleteConfirmationDialog(appointment: AppointmentWithContact) {
        if (!isBindingValid()) return

        try {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.delete_confirm_title))
                .setMessage(getString(R.string.delete_confirm_message, appointment.appointment.title))
                .setIcon(R.drawable.ic_remove)
                .setPositiveButton(getString(R.string.btn_delete)) { dialog, _ ->
                    handleDeleteAppointment(appointment)
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(true)
                .show()
        } catch (e: Exception) {
            Log.e("HistoryFragment", "Error showing delete confirmation dialog: ${e.message}")
            showSnackbar(getString(R.string.error_occurred), Snackbar.LENGTH_SHORT)
        }
    }

    private fun showDeleteAllConfirmationDialog() {
        if (!isBindingValid()) return

        val currentAppointments = historyViewModel.filteredAppointments.value

        if (currentAppointments.isEmpty()) {
            showSnackbar(getString(R.string.snack_no_appointments_to_delete), Snackbar.LENGTH_SHORT)
            return
        }

        try {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.delete_all_confirm_title))
                .setMessage(getString(R.string.delete_all_confirm_message,currentAppointments.size))
                .setIcon(R.drawable.ic_remove)
                .setPositiveButton(getString(R.string.delete_all)) { dialog, _ ->
                    handleDeleteAllAppointments(currentAppointments)
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(true)
                .show()
        } catch (e: Exception) {
            Log.e("HistoryFragment", "Error showing delete all confirmation dialog: ${e.message}")
            showSnackbar(getString(R.string.error_occurred), Snackbar.LENGTH_SHORT)
        }
    }

    private fun handleDeleteAppointment(appointment: AppointmentWithContact) {
        try {
            // Show loading indication
            showSnackbar(getString(R.string.snack_deleting), Snackbar.LENGTH_SHORT)

            // Delete appointment via ViewModel
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    appointmentViewModel.deleteAppointment(appointment.appointment.id)

                    if (isBindingValid()) {
                        showSnackbar(
                            getString(R.string.snack_deleted,appointment.appointment.title),
                            Snackbar.LENGTH_SHORT
                        )
                    }

                    // Refresh data to update UI
                    refreshData()

                    Log.d(
                        "HistoryFragment",
                        "Successfully deleted appointment: ${appointment.appointment.title}"
                    )

                } catch (e: Exception) {
                    Log.e("HistoryFragment", "Error deleting appointment: ${e.message}")
                    if (isBindingValid()) {
                        showSnackbar(getString(R.string.snack_delete_error), Snackbar.LENGTH_LONG)
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("HistoryFragment", "Error in handleDeleteAppointment: ${e.message}")
            showSnackbar(getString(R.string.error_occurred), Snackbar.LENGTH_LONG)
        }
    }

    private fun handleDeleteAllAppointments(appointmentsToDelete: List<AppointmentWithContact>) {
        try {
            // Show loading indication
            showSnackbar(getString(R.string.snack_deleting_many,appointmentsToDelete.size), Snackbar.LENGTH_SHORT)

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    var deletedCount = 0
                    var errorCount = 0

                    appointmentsToDelete.forEach { appointment ->
                        try {
                            appointmentViewModel.deleteAppointment(appointment.appointment.id)
                            deletedCount++
                            // Add small delay to avoid overwhelming the database
                            delay(50)
                        } catch (e: Exception) {
                            errorCount++
                            Log.e(
                                "HistoryFragment",
                                "Error deleting appointment ${appointment.appointment.id}: ${e.message}"
                            )
                        }
                    }

                    if (isBindingValid()) {
                        when {
                            errorCount == 0 -> {
                                showSnackbar(
                                    getString(R.string.snack_deleted_all_success,deletedCount),
                                    Snackbar.LENGTH_LONG
                                )
                            }

                            deletedCount == 0 -> {
                                showSnackbar(
                                    getString(R.string.snack_deleted_all_none),
                                    Snackbar.LENGTH_LONG
                                )
                            }

                            else -> {
                                showSnackbar(
                                    getString(R.string.snack_deleted_all_partial, deletedCount, errorCount),
                                    Snackbar.LENGTH_LONG
                                )
                            }
                        }
                    }

                    // Refresh data to update UI
                    refreshData()

                    Log.d(
                        "HistoryFragment",
                        "Delete all completed - Success: $deletedCount, Errors: $errorCount"
                    )

                } catch (e: Exception) {
                    Log.e("HistoryFragment", "Error in delete all operation: ${e.message}")
                    if (isBindingValid()) {
                        showSnackbar(getString(R.string.snack_delete_error), Snackbar.LENGTH_LONG)
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("HistoryFragment", "Error in handleDeleteAllAppointments: ${e.message}")
            showSnackbar(getString(R.string.error_occurred), Snackbar.LENGTH_LONG)
        }
    }

    private fun showSnackbar(message: String, duration: Int = Snackbar.LENGTH_LONG) {
        if (!isBindingValid()) return

        try {
            Snackbar.make(binding.root, message, duration).show()
        } catch (e: Exception) {
            Log.e("HistoryFragment", "Error showing snackbar: ${e.message}")
        }
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
package com.nhathuy.nextmeet.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.FragmentDashBoardBinding
import com.nhathuy.nextmeet.ui.TestActivity

/**
 * A simple [Fragment] subclass.
 * Use the [DashBoardFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DashBoardFragment : BaseTabFragment() {

    private var _binding: FragmentDashBoardBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDashBoardBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        if (userVisibleHint) {
            loadDataOnFirstVisible()
        }
    }

    private fun setupViews() {
        // Setup các views
        setupRecyclerViews()
        setupClickListeners()
    }

    private fun setupRecyclerViews() {
        // Setup RecyclerView cho today appointments
        binding.rvTodayApppointments.apply {
            layoutManager = LinearLayoutManager(context)
            // adapter = todayAppointmentsAdapter
        }

        // Setup RecyclerView cho recent notes
        binding.rvNoteRecents.apply {
            layoutManager = LinearLayoutManager(context)
            // adapter = recentNotesAdapter
        }
    }

    private fun setupClickListeners() {
        // Click listeners cho các cards
        binding.cardToday.setOnClickListener {
            // Handle click
        }

        binding.cardNotes.setOnClickListener {
            // Switch to Notes tab
            testActivity?.switchToTab(TestActivity.TabType.NOTES)
        }

        binding.cardContact.setOnClickListener {
            // Navigate to contact
            testActivity?.navController?.navigate(R.id.nav_contact)
        }

        binding.cardAppointmentSoon.setOnClickListener {
            // Navigate to appointment
            testActivity?.navController?.navigate(R.id.nav_appointment)
        }
    }

    override fun loadDataOnFirstVisible() {
        // Load data lần đầu khi fragment visible
        loadDashboardData()
    }

    override fun refreshData() {
        // Refresh tất cả data
        loadDashboardData()
    }

    private fun loadDashboardData() {
        // Load data từ repository/database
        // loadTodayAppointments()
        // loadRecentNotes()
        // loadStatistics()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
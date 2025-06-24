package com.nhathuy.nextmeet.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.adapter.AppointmentTodayAdapter
import com.nhathuy.nextmeet.databinding.FragmentDashBoardBinding
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.Note
import com.nhathuy.nextmeet.resource.AppointmentUiState
import com.nhathuy.nextmeet.resource.ContactUiState
import com.nhathuy.nextmeet.resource.NoteUiState
import com.nhathuy.nextmeet.ui.TestActivity
import com.nhathuy.nextmeet.viewmodel.AppointmentPlusViewModel
import com.nhathuy.nextmeet.viewmodel.AppointmentViewModel
import com.nhathuy.nextmeet.viewmodel.ContactViewModel
import com.nhathuy.nextmeet.viewmodel.NoteViewModel
import com.nhathuy.nextmeet.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class DashBoardFragment : Fragment() {

    private var _binding: FragmentDashBoardBinding? = null
    private val binding get() = _binding!!

    //viewmodel
    private lateinit var userViewModel: UserViewModel
    private lateinit var appointmentViewModel: AppointmentPlusViewModel
    private lateinit var contactViewModel: ContactViewModel
    private lateinit var noteViewModel: NoteViewModel

    //adapter
    private lateinit var todayAppointmentAdapter: AppointmentTodayAdapter
    //
    private var currentUserId: Int = 0

    private var todayAppointmentCount = 0
    private var noteCount = 0
    private var contactCount = 0
    private var upcomingAppointmentCount = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDashBoardBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViewModels()
        setupUI()
        observeData()
    }

    // khởi tao viewmodel
    private fun initializeViewModels() {
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        appointmentViewModel = ViewModelProvider(this)[AppointmentPlusViewModel::class.java]
        contactViewModel = ViewModelProvider(this)[ContactViewModel::class.java]
        noteViewModel = ViewModelProvider(this)[NoteViewModel::class.java]
    }

    // khởi tạo ui
    private fun setupUI() {
        //setup click listeners cho các card
        setupCardClickListeners()

        //setup click listeners cho các button trong empty state
        setupEmptyStateButtons()

        //setup recycler view
        setupRecyclerView()
    }

    private fun setupCardClickListeners() {
        binding.cardToday.setOnClickListener {
            //navigateToAppointment("today")
        }
        binding.cardNotes.setOnClickListener {
            //navigateNotes()
        }
        binding.cardContact.setOnClickListener {
            //navigateToContact()
        }
        binding.cardAppointmentSoon.setOnClickListener {
            //navigateToAppointment("upcoming")
        }
    }

    // sử ly khi empty state button
    private fun setupEmptyStateButtons() {
        binding.btnAddAppointment.setOnClickListener {
            navigateToAddAppointment()
        }
        binding.btnAddNote.setOnClickListener {
            navigateToAddNote()
        }
    }

    private fun setupRecyclerView() {
        binding.rvTodayApppointments.layoutManager = LinearLayoutManager(requireContext())
        todayAppointmentAdapter = AppointmentTodayAdapter()
        binding.rvTodayApppointments.adapter = todayAppointmentAdapter
    }

    private fun observeData() {
        observeCurrentData()
        observeAppointmentData()
        observeContactData()
        observeNoteData()
    }

    // lắng nghe thông tin user current
    private fun observeCurrentData() {
        userViewModel.getCurrentUser().observe(viewLifecycleOwner) { user ->
            user?.let {
                currentUserId = user.id
                loadDashboardData()
            }
        }
    }

    // tải thông tin man hình dash board
    private fun loadDashboardData() {
        if (currentUserId != 0) {

            // tải cuộc hẹn
            appointmentViewModel.getAllAppointments(currentUserId)

            //tải ghi chú
            lifecycleScope.launch {
                noteViewModel.getAllNotes(currentUserId).collect { notes ->
                    noteCount = notes.size
                    updateContactCard()
                    updateNotesSection(notes.take(3))
                }
            }

            //tải liên hệ
            contactViewModel.getAllContacts(currentUserId)
        }
    }

    // lắng nghe thông tin cuộc hẹn
    private fun observeAppointmentData() {
        lifecycleScope.launch {
            appointmentViewModel.appointmentUiState.collect { state ->
                when (state) {
                    is AppointmentUiState.AppointmentsLoaded -> {
                        val appointments = state.appointments

                        // tính toán cuộc hẹn
                        calculateAppointmentCount(appointments)

                        // cập nhật ui
                        updateAppointmentCards()
                        updateTodayAppointmentsSection(appointments.filter { isToday(it.startDateTime) })

                        // cập nhật trạng thái empty
                        updateOverallEmptyState()
                    }

                    is AppointmentUiState.Error -> {
                        showMessage(state.message)
                    }

                    else -> {}
                }
            }
        }
    }

    // lắng nghe thông tin liên hệ
    private fun observeContactData() {
        lifecycleScope.launch {
            contactViewModel.contactUiState.collect { state ->
                when (state) {
                    is ContactUiState.ContactsLoaded -> {
                        contactCount = state.contacts.size
                        updateContactCard()
                        updateOverallEmptyState()
                    }

                    is ContactUiState.Error -> {
                        showMessage(state.message)
                    }

                    else -> {}
                }
            }
        }
    }

    //lắng nghe thông tin ghi chứ
    private fun observeNoteData() {
        lifecycleScope.launch {
            noteViewModel.uiState.collect { state ->
                when (state) {
                    is NoteUiState.NotesLoaded -> {
                        noteCount = state.notes.size
                        updateNoteCard()
                        updateNotesSection(state.notes.take(3))
                        updateOverallEmptyState()
                    }

                    is NoteUiState.Error -> {
                        showMessage(state.message)
                    }

                    else -> {}
                }
            }
        }
    }

    // tính toán lại ngày tháng
    private fun calculateAppointmentCount(appointments: List<AppointmentPlus>) {
        val now = System.currentTimeMillis()

        todayAppointmentCount = appointments.count { isToday(it.startDateTime) }
        upcomingAppointmentCount = appointments.count { it.startDateTime > now}
    }

    // kiểm tra thời gian là ngày hôm nay
    private fun isToday(timestamp: Long) : Boolean {
        val today = Calendar.getInstance()
        val date = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }

        return today.get(Calendar.YEAR) == date.get(Calendar.YEAR)
                && today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
    }
    //cập nhat lại cuộn hẹn card
    private fun updateAppointmentCards(){
        binding.tvDashBoardToday.text = todayAppointmentCount.toString()
        binding.tvDashBoardAppointmentSoon.text = upcomingAppointmentCount.toString()
    }

    //cập nhat ghi chu card
    private fun updateNoteCard() {
        binding.tvDashBoardNotes.text = noteCount.toString()
    }

    // cập nhật liên hệ card
    private fun updateContactCard() {
        binding.tvDashBoardContact.text = contactCount.toString()
    }

    // cập nhật lại hiển thị danh sach cuộn hẹn ngày hôm nay
    private fun updateTodayAppointmentsSection(todayAppointments : List<AppointmentPlus>){
        if(todayAppointments.isEmpty()) {
            binding.rvTodayApppointments.visibility = View.GONE
            binding.layoutEmptyTodayAppointments.visibility = View.VISIBLE
        }
        else{
            binding.rvTodayApppointments.visibility = View.VISIBLE
            binding.layoutEmptyTodayAppointments.visibility = View.GONE
            todayAppointmentAdapter.submitList(todayAppointments)
        }
    }
    // khoi tao cuoc hen ngay hom nay
    private fun setupTodayAppointmentRec(){

    }

    // câp nhật ghi chú
    private fun updateNotesSection(notes : List<Note>){
        if(notes.isEmpty()){
            binding.rvNoteRecents.visibility = View.GONE
            binding.layoutEmptyRecentNotes.visibility = View.VISIBLE
        }
        else{
            binding.rvNoteRecents.visibility = View.VISIBLE
            binding.layoutEmptyRecentNotes.visibility = View.GONE

            //setupAdapter
        }
    }

    //cập nhat lại empty state
    private fun updateOverallEmptyState() {
        val hasData =
            todayAppointmentCount > 0 || contactCount > 0 || noteCount > 0 || upcomingAppointmentCount > 0

        if (hasData) {
            binding.statCardGrid.visibility = View.VISIBLE
            binding.cardTodayAppointment.visibility = View.VISIBLE
            binding.cardNoteRecents.visibility = View.VISIBLE
            binding.cardMaps.visibility = View.VISIBLE
            binding.layoutWelcomeEmptyState.visibility = View.GONE
        } else {
            binding.statCardGrid.visibility = View.GONE
            binding.cardTodayAppointment.visibility = View.GONE
            binding.cardNoteRecents.visibility = View.GONE
            binding.cardMaps.visibility = View.GONE
            binding.layoutWelcomeEmptyState.visibility = View.VISIBLE
        }
    }

    //hiển thị thông báo
    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Navigation methods
     */
    private fun navigateToAppointments(filter: String = "") {
        // TODO: Implement navigation to appointments fragment with filter
    }

    private fun navigateToNotes() {
        // TODO: Implement navigation to notes fragment
    }

    private fun navigateToContacts() {
        // TODO: Implement navigation to contacts fragment
    }

    private fun navigateToAddAppointment() {
        // TODO: Implement navigation to add appointment
    }

    private fun navigateToAddNote() {
        // TODO: Implement navigation to add note
    }
}
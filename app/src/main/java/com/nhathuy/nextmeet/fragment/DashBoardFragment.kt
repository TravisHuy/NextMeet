package com.nhathuy.nextmeet.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.adapter.AppointmentTodayAdapter
import com.nhathuy.nextmeet.adapter.NoteRecentAdapter
import com.nhathuy.nextmeet.databinding.FragmentDashBoardBinding
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.Note
import com.nhathuy.nextmeet.model.NoteImage
import com.nhathuy.nextmeet.model.NoteType
import com.nhathuy.nextmeet.resource.AppointmentUiState
import com.nhathuy.nextmeet.resource.ContactUiState
import com.nhathuy.nextmeet.resource.NoteUiState
import com.nhathuy.nextmeet.ui.TestActivity
import com.nhathuy.nextmeet.utils.Constant
import com.nhathuy.nextmeet.utils.NavigationCallback
import com.nhathuy.nextmeet.viewmodel.AppointmentPlusViewModel
import com.nhathuy.nextmeet.viewmodel.AppointmentViewModel
import com.nhathuy.nextmeet.viewmodel.ContactViewModel
import com.nhathuy.nextmeet.viewmodel.NoteViewModel
import com.nhathuy.nextmeet.viewmodel.SearchViewModel
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
    private lateinit var noteRecentAdapter: NoteRecentAdapter

    //
    private var currentUserId: Int = 0

    private var todayAppointmentCount = 0
    private var noteCount = 0
    private var contactCount = 0
    private var upcomingAppointmentCount = 0


    // Map lưu trữ danh sách ảnh cho mỗi note
    private val noteImagesMap = mutableMapOf<Int, List<NoteImage>>()

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
            navigateToAppointment(Constant.FILTER_TODAY)
        }
        binding.cardNotes.setOnClickListener {
            navigateToNotes()
        }
        binding.cardContact.setOnClickListener {
            navigateToContacts()
        }
        binding.cardAppointmentSoon.setOnClickListener {
            navigateToAppointment(Constant.FILTER_UPCOMING)
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
        setupAppointmentTodayAdapter()
        setupNoteRecentAdapter()
    }

    private fun setupAppointmentTodayAdapter() {
        binding.rvTodayApppointments.layoutManager = LinearLayoutManager(requireContext())
        todayAppointmentAdapter = AppointmentTodayAdapter()
        binding.rvTodayApppointments.adapter = todayAppointmentAdapter
    }

    private fun setupNoteRecentAdapter() {
        binding.rvNoteRecents.layoutManager = LinearLayoutManager(requireContext())
        noteRecentAdapter = NoteRecentAdapter()
        binding.rvNoteRecents.adapter = noteRecentAdapter
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
                    updateNoteCard()
                    loadImagesForPhotoNotesAndUpdate(notes.take(3))
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
                    is NoteUiState.Error -> {
                        showMessage(state.message)
                    }

                    else -> {}
                }
            }
        }
    }

    // Hàm mới để tải ảnh cho tất cả note kiểu PHOTO
    private fun loadImagesForPhotoNotesAndUpdate(notes: List<Note>) {
        val photoNotes = notes.filter { it.noteType == NoteType.PHOTO }

        if (photoNotes.isEmpty()) {
            // Không có photo notes, cập nhật UI luôn
            Log.d("DashBoardFragment", "No photo notes, updating UI directly")
            updateNotesSection(notes)
            updateOverallEmptyState()
            return
        }

        Log.d("DashBoardFragment", "Loading images for ${photoNotes.size} photo notes")
        var loadedCount = 0
        val totalPhotoNotes = photoNotes.size

        photoNotes.forEach { note ->
            noteViewModel.getImagesForNote(note.id) { images ->
                Log.d("DashBoardFragment", "Loaded ${images.size} images for note ID: ${note.id}")
                noteImagesMap[note.id] = images
                loadedCount++

                // Chỉ cập nhật UI khi tất cả ảnh đã được load
                if (loadedCount == totalPhotoNotes) {
                    Log.d("DashBoardFragment", "All images loaded, updating UI")
                    updateNotesSection(notes)
                    updateOverallEmptyState()
                }
            }
        }
    }

    // tính toán lại ngày tháng
    private fun calculateAppointmentCount(appointments: List<AppointmentPlus>) {
        val now = System.currentTimeMillis()

        todayAppointmentCount = appointments.count { isToday(it.startDateTime) }
        upcomingAppointmentCount = appointments.count { it.startDateTime > now }
    }

    // kiểm tra thời gian là ngày hôm nay
    private fun isToday(timestamp: Long): Boolean {
        val today = Calendar.getInstance()
        val date = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }

        return today.get(Calendar.YEAR) == date.get(Calendar.YEAR)
                && today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
    }

    //cập nhat lại cuộn hẹn card
    private fun updateAppointmentCards() {
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
    private fun updateTodayAppointmentsSection(todayAppointments: List<AppointmentPlus>) {
        if (todayAppointments.isEmpty()) {
            binding.rvTodayApppointments.visibility = View.GONE
            binding.layoutEmptyTodayAppointments.visibility = View.VISIBLE
        } else {
            binding.rvTodayApppointments.visibility = View.VISIBLE
            binding.layoutEmptyTodayAppointments.visibility = View.GONE
            todayAppointmentAdapter.submitList(todayAppointments)
        }
    }

    // câp nhật ghi chú
    private fun updateNotesSection(notes: List<Note>) {
        val combinedNotes = mutableListOf<Note>()
        combinedNotes.addAll(notes)
        if (notes.isEmpty()) {
            binding.rvNoteRecents.visibility = View.GONE
            binding.layoutEmptyRecentNotes.visibility = View.VISIBLE
        } else {
            binding.rvNoteRecents.visibility = View.VISIBLE
            binding.layoutEmptyRecentNotes.visibility = View.GONE
            noteRecentAdapter.updateNotesWithImages(notes, noteImagesMap)
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
    private fun navigateToAppointment(filter: String = "") {
        try {
            // set filter vào searchViewModel truoc khi navigate
            if(filter.isNotEmpty()){
                val searchQuery = when(filter){
                    Constant.FILTER_TODAY -> "Today"
                    Constant.FILTER_UPCOMING -> "Upcoming"
                    else -> ""
                }
                if (searchQuery.isNotEmpty()) {
                    val searchViewModel = ViewModelProvider(requireActivity())[SearchViewModel::class.java]
                    searchViewModel.setNavigationFilter(searchQuery)
                }
            }
            val bottomNavigation =
                (activity as? AppCompatActivity)?.findViewById<BottomNavigationView>(R.id.nav_bottom_navigation)
            if (bottomNavigation != null) {
                val appointmentMenuItem = bottomNavigation.menu.findItem(R.id.nav_appointment)
                if (appointmentMenuItem != null) {
                    bottomNavigation.selectedItemId = appointmentMenuItem.itemId
                    Log.d("Navigation", "Navigated to Appointment with filter: $filter")
                    return
                }
            }
        } catch (e: Exception) {
            Log.e("Navigation", "Failed to navigate to AppointmentFragment: ${e.message}")
        }
    }


    private fun navigateToNotes() {
        try {
            val parentFragment = parentFragment
            if (parentFragment is HomeFragment) {
                val homeBinding = parentFragment.binding
                homeBinding.homeViewpager2.currentItem = 1
                Log.d("Navigation", "Navigated to Notes tab in ViewPager2")
                return
            }
        } catch (e: Exception) {
            Log.e("Navigation", "Failed to navigate to NotesFragment: ${e.message}")
        }
    }

    private fun navigateToContacts() {
        try {
            val bottomNavigation =
                (activity as? AppCompatActivity)?.findViewById<BottomNavigationView>(R.id.nav_bottom_navigation)
            if (bottomNavigation != null) {
                val contactMenuItem = bottomNavigation.menu.findItem(R.id.nav_contact)
                if (contactMenuItem != null) {
                    bottomNavigation.selectedItemId = contactMenuItem.itemId
                    Log.d("Navigation", "Navigated to Contact via BottomNavigation")
                    return
                }
            }
        } catch (e: Exception) {
            Log.e("Navigation", "Failed to navigate to ContactFragment: ${e.message}")
            Toast.makeText(
                requireContext(),
                "Không thể chuyển đến trang Liên hệ. Vui lòng thử lại.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun navigateToAddAppointment() {
        try{
            val bottomNavigation = (activity as? AppCompatActivity)?.findViewById<BottomNavigationView>(R.id.nav_bottom_navigation)

            if(bottomNavigation != null){
                val appointmentMenuItem = bottomNavigation.menu.findItem(R.id.nav_appointment)
                if (appointmentMenuItem != null) {
                    bottomNavigation.selectedItemId = appointmentMenuItem.itemId

                    // Đặt delay nhỏ để fragment load xong trước khi trigger FAB
                    view?.postDelayed({
                        triggerAppointmentFAB()
                    }, 300)

                    Log.d("Navigation", "Navigated to Appointment to add new")
                    return
                }
            }
        }
        catch (e: Exception) {
            Log.e("Navigation", "Failed to navigate to add appointment: ${e.message}")
        }
    }

    private fun navigateToAddNote() {
        try {
            val parentFragment = parentFragment
            if (parentFragment is HomeFragment) {
                val homeBinding = parentFragment.binding
                homeBinding.homeViewpager2.currentItem = 1

                // Đặt delay nhỏ để fragment load xong trước khi trigger FAB
                view?.postDelayed({
                    triggerNoteFAB()
                }, 300)

                Log.d("Navigation", "Navigated to Notes to add new")
                return
            }
        } catch (e: Exception) {
            Log.e("Navigation", "Failed to navigate to add note: ${e.message}")
        }
    }

    // Trigger FAB click cho AppointmentMapFragment
    private fun triggerAppointmentFAB() {
        try {
            val fragments = requireActivity().supportFragmentManager.fragments
            for (fragment in fragments) {
                if (fragment is AppointmentMapFragment) {
                    fragment.triggerAddAction()
                    return
                }

                // Kiểm tra child fragments
                fragment?.let {
                    val childFragments = it.childFragmentManager.fragments
                    for (childFragment in childFragments) {
                        if (childFragment is AppointmentMapFragment) {
                            childFragment.triggerAddAction()
                            return
                        }
                    }
                }
            }
            Log.w("Navigation", "AppointmentMapFragment not found for FAB trigger")
        } catch (e: Exception) {
            Log.e("Navigation", "Failed to trigger appointment FAB: ${e.message}")
        }
    }

    // Trigger FAB click cho NoteFragment
    private fun triggerNoteFAB() {
        try {
            val parentFragment = parentFragment
            if (parentFragment is HomeFragment) {
                // Tìm NoteFragment trong ViewPager2
                val fragments = parentFragment.childFragmentManager.fragments
                for (fragment in fragments) {
                    // Kiểm tra nếu là ViewPager fragment container
                    if (fragment != null) {
                        val childFragments = fragment.childFragmentManager.fragments
                        for (childFragment in childFragments) {
                            if (childFragment is NotesFragment) {
                                (childFragment as? NavigationCallback)?.triggerAddAction()
                                return
                            }
                        }
                    }
                }
            }
            Log.w("Navigation", "NoteFragment not found for FAB trigger")
        } catch (e: Exception) {
            Log.e("Navigation", "Failed to trigger note FAB: ${e.message}")
        }
    }
}
package com.nhathuy.nextmeet.fragment

import android.content.Intent
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.adapter.AppointmentTodayAdapter
import com.nhathuy.nextmeet.adapter.LocationStatisticsAdapter
import com.nhathuy.nextmeet.adapter.NoteRecentAdapter
import com.nhathuy.nextmeet.databinding.FragmentDashBoardBinding
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.LocationStatistics
import com.nhathuy.nextmeet.model.Note
import com.nhathuy.nextmeet.model.NoteImage
import com.nhathuy.nextmeet.model.NoteType
import com.nhathuy.nextmeet.resource.AppointmentUiState
import com.nhathuy.nextmeet.resource.ContactUiState
import com.nhathuy.nextmeet.resource.NoteUiState
import com.nhathuy.nextmeet.ui.NavigationMapActivity
import com.nhathuy.nextmeet.ui.SolutionActivity
import com.nhathuy.nextmeet.ui.TestActivity
import com.nhathuy.nextmeet.utils.AppointmentNavigationCallback
import com.nhathuy.nextmeet.utils.Constant
import com.nhathuy.nextmeet.utils.NavigationCallback
import com.nhathuy.nextmeet.viewmodel.AppointmentPlusViewModel
import com.nhathuy.nextmeet.viewmodel.ContactViewModel
import com.nhathuy.nextmeet.viewmodel.NoteViewModel
import com.nhathuy.nextmeet.viewmodel.SearchViewModel
import com.nhathuy.nextmeet.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class DashBoardFragment : Fragment(), OnMapReadyCallback {

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
    private lateinit var locationStatisticsAdapter: LocationStatisticsAdapter

    // Google Maps
    private var googleMap: GoogleMap? = null
    private var mapFragment: SupportMapFragment? = null

    //
    private var currentUserId: Int = 0

    private var todayAppointmentCount = 0
    private var noteCount = 0
    private var contactCount = 0
    private var upcomingAppointmentCount = 0

    // Map statistics
    private var locationCount = 0
    private var topLocations = listOf<LocationStatistics>()
    private var allAppointments = listOf<AppointmentPlus>()

    private var allNotes = listOf<Note>()

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
        setupGoogleMaps()
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

        // setup click view all
        setupClickViewAll()
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

        // Thêm click listener cho card map
        binding.cardMaps.setOnClickListener {
            navigateToAppointmentMap()
        }
    }

    private fun setupClickViewAll() {
        binding.viewAllNotes.setOnClickListener {
            navigateToNotes()
        }

        binding.viewAllAppointment.setOnClickListener {
            navigateToAppointmentMap()
        }

        binding.viewAllMap.setOnClickListener {
            navigateToAppointmentMap()
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
        setupLocationStatisticsAdapter()
    }

    private fun setupAppointmentTodayAdapter() {
        binding.rvTodayApppointments.layoutManager = LinearLayoutManager(requireContext())
        todayAppointmentAdapter = AppointmentTodayAdapter()
        binding.rvTodayApppointments.adapter = todayAppointmentAdapter
    }

    private fun setupNoteRecentAdapter() {
        binding.rvNoteRecents.layoutManager = LinearLayoutManager(requireContext())
        noteRecentAdapter = NoteRecentAdapter(notes = mutableListOf())
        binding.rvNoteRecents.adapter = noteRecentAdapter
    }

    private fun setupLocationStatisticsAdapter() {
        binding.rvLocationStatistics.layoutManager = LinearLayoutManager(requireContext())
        locationStatisticsAdapter = LocationStatisticsAdapter(
            onLocationClick = { location ->
                onLocationClick(location)
            },
            onNavigationMap = { location ->
                navigateToNavigationMap(location.representativeAppointment)
            })
        binding.rvLocationStatistics.adapter = locationStatisticsAdapter
    }

    private fun navigateToNavigationMap(appointment: AppointmentPlus) {
        try {
            val intent = Intent(requireContext(), NavigationMapActivity::class.java).apply {
                putExtra(Constant.EXTRA_APPOINTMENT_ID, appointment.id)
            }
            startActivity(intent)

            Log.d(
                "AppointmentMapFragment",
                "Navigated to NavigationMap for appointment: ${appointment.id}"
            )
        } catch (e: Exception) {
            Log.e("AppointmentMapFragment", "Failed to navigate to NavigationMap", e)
            Toast.makeText(requireContext(),
                getString(R.string.unable_to_route_map), Toast.LENGTH_SHORT)
                .show()
        }
    }


    private fun setupGoogleMaps() {
        mapFragment =
            childFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Cấu hình map
        googleMap?.apply {
            uiSettings.apply {
                isZoomControlsEnabled = false
                isCompassEnabled = false
                isMapToolbarEnabled = false
                isMyLocationButtonEnabled = false
            }

            // Disable map interactions để tránh conflict với scroll
            uiSettings.setAllGesturesEnabled(false)
        }

        // Cập nhật markers nếu đã có data
        updateMapMarkers()
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
                    allNotes = notes
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
                        allAppointments = appointments

                        // tính toán cuộc hẹn
                        calculateAppointmentCount(appointments)

                        // tính toán thống kê địa điểm
                        calculateLocationStatistics(appointments)

                        // cập nhật ui
                        updateAppointmentCards()
                        updateTodayAppointmentsSection(appointments.filter { isToday(it.startDateTime) })
                        updateMapSection()

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

    // tính toán thống kê địa điểm
    private fun calculateLocationStatistics(appointments: List<AppointmentPlus>) {
        val now = System.currentTimeMillis()

        // Lọc appointments có địa điểm
        val appointmentsWithLocation = appointments.filter {
            it.location.isNotBlank() && it.latitude != 0.0 && it.longitude != 0.0
        }

        if (appointmentsWithLocation.isEmpty()) {
            locationCount = 0
            topLocations = emptyList()
            return
        }

        // Group theo địa điểm
        val locationGroups = appointmentsWithLocation.groupBy { it.location }
        locationCount = locationGroups.size

        // Tính top locations
        topLocations = locationGroups.map { (location, appointments) ->
            val upcomingCount = appointments.count { it.startDateTime > now }

            val representativeAppointment = appointments.filter {
                it.startDateTime > now
            }
                .minByOrNull { it.startDateTime }
                ?: appointments.maxByOrNull { it.startDateTime }
                ?: appointments.first()

            LocationStatistics(
                locationName = location,
                appointmentCount = appointments.size,
                latitude = appointments.first().latitude,
                longitude = appointments.first().longitude,
                upcomingCount = upcomingCount,
                representativeAppointment = representativeAppointment
            )
        }.sortedByDescending { it.appointmentCount }
            .take(3) // Lấy top 3
    }

    // cập nhật map section
    private fun updateMapSection() {
        if (locationCount > 0) {
            binding.apply {
                // Hiển thị statistics
                layoutMapStatistics.visibility = View.VISIBLE
                layoutEmptyMap.visibility = View.GONE
                mapFragmentContainer.visibility = View.VISIBLE

                // Cập nhật số liệu
                tvLocationCount.text = getString(R.string.location_count, locationCount)

                // Cập nhật danh sách top locations
                locationStatisticsAdapter.submitList(topLocations)

                // Cập nhật markers trên map
                updateMapMarkers()
            }
        } else {
            binding.apply {
                layoutMapStatistics.visibility = View.GONE
                layoutEmptyMap.visibility = View.VISIBLE
                mapFragmentContainer.visibility = View.GONE
            }
        }
    }

    // cập nhật markers trên map
    private fun updateMapMarkers() {
        googleMap?.let { map ->
            map.clear()

            if (topLocations.isNotEmpty()) {
                val boundsBuilder = LatLngBounds.Builder()

                topLocations.forEach { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    map.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(location.locationName)
                            .snippet(
                                getString(
                                    R.string.appointment_count_format,
                                    location.appointmentCount
                                )
                            )
                    )
                    boundsBuilder.include(latLng)
                }

                // Fit map để hiển thị tất cả markers
                try {
                    val bounds = boundsBuilder.build()
                    val padding = 100 // padding in pixels
                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                } catch (e: Exception) {
                    // Fallback nếu có lỗi
                    val firstLocation = topLocations.first()
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(firstLocation.latitude, firstLocation.longitude),
                            12f
                        )
                    )
                }
            }
        }
    }

    // xử lý click vào location
    private fun onLocationClick(location: LocationStatistics) {
        // Focus map tại location được click
        googleMap?.let { map ->
            val latLng = LatLng(location.latitude, location.longitude)
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(latLng, 15f),
                1000,
                null
            )
        }

        // Có thể thêm hiệu ứng highlight marker
        showMessage("Hiển thị ${location.locationName}")
    }

    // navigation đến appointment map
    private fun navigateToAppointmentMap() {
        try {
            val bottomNavigation =
                (activity as? AppCompatActivity)?.findViewById<BottomNavigationView>(R.id.nav_bottom_navigation)
            if (bottomNavigation != null) {
                val appointmentMenuItem = bottomNavigation.menu.findItem(R.id.nav_appointment)
                if (appointmentMenuItem != null) {
                    bottomNavigation.selectedItemId = appointmentMenuItem.itemId
                    Log.d("Navigation", "Navigated to AppointmentMap")
                    return
                }
            }
        } catch (e: Exception) {
            Log.e("Navigation", "Failed to navigate to AppointmentMapFragment: ${e.message}")
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
            noteRecentAdapter.updateNotesWithImages(combinedNotes, noteImagesMap)
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
            if (filter.isNotEmpty()) {
                val searchQuery = when (filter) {
                    Constant.FILTER_TODAY -> getString(R.string.today)
                    Constant.FILTER_UPCOMING -> getString(R.string.upcoming)
                    else -> ""
                }
                if (searchQuery.isNotEmpty()) {
//                    val searchViewModel = ViewModelProvider(requireActivity())[SearchViewModel::class.java]
//                    searchViewModel.setNavigationFilter(searchQuery)
                    (activity as? SolutionActivity)?.let { solutionActivity ->
                        // Switch to appointment tab (index 1)
                        solutionActivity.binding.viewPager2.setCurrentItem(1, true)
                        solutionActivity.binding.navBottomNavigation.selectedItemId =
                            R.id.nav_appointment

                        // Post a delayed action to ensure fragment is ready
                        solutionActivity.binding.root.postDelayed({
                            triggerAppointmentWithContactNavigation(searchQuery)
                        }, 500)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Navigation", "Failed to navigate to AppointmentFragment: ${e.message}")
        }
    }

    private fun triggerAppointmentWithContactNavigation(searchFilter: String) {
        val fragments = requireActivity().supportFragmentManager.fragments
        for (fragment in fragments) {
            if (fragment is AppointmentNavigationCallback && fragment.isVisible) {
                fragment.onNavigateToAppointmentWithDashboard(searchFilter)
                break
            }
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
        try {
            val bottomNavigation =
                (activity as? AppCompatActivity)?.findViewById<BottomNavigationView>(R.id.nav_bottom_navigation)

            if (bottomNavigation != null) {
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
        } catch (e: Exception) {
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

    override fun onResume() {
        super.onResume()
        if (currentUserId != 0) {
            refreshNotes()
        }
    }

    private fun refreshNotes() {
        // Clear cache cũ
        noteImagesMap.clear()

        // Trigger lại observe để load fresh data
        viewLifecycleOwner.lifecycleScope.launch {
            noteViewModel.getAllNotes(currentUserId).collect { notes ->
                allNotes = notes

                loadImagesForPhotoNotesAndUpdate(notes)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
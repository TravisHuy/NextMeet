package com.nhathuy.nextmeet.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.search.SearchView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.adapter.AppointmentPlusAdapter
import com.nhathuy.nextmeet.adapter.ColorPickerAdapter
import com.nhathuy.nextmeet.adapter.ContactsAdapter
import com.nhathuy.nextmeet.adapter.SearchSuggestionsAdapter
import com.nhathuy.nextmeet.databinding.DialogAddAppointmentBinding
import com.nhathuy.nextmeet.databinding.FragmentAppointmentMapBinding
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.AppointmentStatus
import com.nhathuy.nextmeet.model.ContactNameId
import com.nhathuy.nextmeet.model.SearchSuggestion
import com.nhathuy.nextmeet.model.SearchSuggestionType
import com.nhathuy.nextmeet.model.SearchType
import com.nhathuy.nextmeet.resource.AppointmentUiState
import com.nhathuy.nextmeet.resource.ContactUiState
import com.nhathuy.nextmeet.resource.SearchUiState
import com.nhathuy.nextmeet.ui.AddNoteActivity
import com.nhathuy.nextmeet.ui.GoogleMapActivity
import com.nhathuy.nextmeet.ui.NavigationMapActivity
import com.nhathuy.nextmeet.utils.Constant
import com.nhathuy.nextmeet.viewmodel.AppointmentPlusViewModel
import com.nhathuy.nextmeet.viewmodel.AppointmentViewModel
import com.nhathuy.nextmeet.viewmodel.ContactViewModel
import com.nhathuy.nextmeet.viewmodel.SearchViewModel
import com.nhathuy.nextmeet.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AppointmentMapFragment : Fragment() {

    private var _binding: FragmentAppointmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel
    private lateinit var contactViewModel: ContactViewModel
    private lateinit var appointmentViewModel: AppointmentPlusViewModel
    private lateinit var searchViewModel: SearchViewModel

    private var addAppointmentDialog: Dialog? = null
    private var currentUserId: Int = 0
    private var currentContactId: Int = 0

    // Location and appointment data
    private var location: String? = null
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var reminderTime: Long? = null
    private val contactMap = mutableMapOf<String, ContactNameId>()
    private var selectedColorName: String = "color_white"

    // Search state - simplified
    private var currentSearchQuery: String? = null
    private var isSearchMode: Boolean = false
    private var isSearchViewExpanded: Boolean = false

    // Selection state
    private var isSelectionMode: Boolean = false

    // Dialog state - like ContactFragment
    private var currentEditingAppointment: AppointmentPlus? = null
    private var isEditMode: Boolean = false
    private var isDialogShowing: Boolean = false
    private var lastClickTime = 0L
    private var CLICK_DELAY = 500L
    private var shouldRestoreDialog = false

    // Adapters
    private lateinit var appointmentAdapter: AppointmentPlusAdapter
    private lateinit var searchSuggestionsAdapter: SearchSuggestionsAdapter
    private lateinit var colorAdapter: ColorPickerAdapter


    //variables cho geocoding
    private var geocodingJob: Job? = null
    private var geocodingCache = mutableMapOf<String, Pair<Double?,Double?>>()
    private var orignalMapLocation :String? = null
    private var isLocationMap: Boolean = false


    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleMapPickerResult(result)
    }

    companion object {
        val listColor = listOf(
            R.color.color_white, R.color.color_red, R.color.color_orange,
            R.color.color_yellow, R.color.color_green, R.color.color_teal,
            R.color.color_blue, R.color.color_dark_blue, R.color.color_purple,
            R.color.color_pink, R.color.color_brown, R.color.color_gray
        )

        val colorSourceNames = mapOf(
            R.color.color_white to "color_white",
            R.color.color_red to "color_red",
            R.color.color_orange to "color_orange",
            R.color.color_yellow to "color_yellow",
            R.color.color_green to "color_green",
            R.color.color_teal to "color_teal",
            R.color.color_blue to "color_blue",
            R.color.color_dark_blue to "color_dark_blue",
            R.color.color_purple to "color_purple",
            R.color.color_pink to "color_pink",
            R.color.color_brown to "color_brown",
            R.color.color_gray to "color_gray"
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentMapBinding.inflate(inflater, container, false)
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
        contactViewModel = ViewModelProvider(this)[ContactViewModel::class.java]
        appointmentViewModel = ViewModelProvider(this)[AppointmentPlusViewModel::class.java]
        searchViewModel = ViewModelProvider(this)[SearchViewModel::class.java]
    }

    private fun setupUI() {
        setupUserInfo()
        setupRecyclerView()
        setupSwipeRefresh()
        setupFabMenu()
        setupSelectionToolbar()
        setupSearchFeature()
    }

    private fun observeData() {
        observeUserData()
        observeContactData()
        observeAppointmentData()
        observeSearchData()
    }

    // MARK: - Setup Methods

    private fun setupUserInfo() {
        userViewModel.getCurrentUser().observe(viewLifecycleOwner) { user ->
            user?.let {
                currentUserId = user.id
                initializeSearchForUser()
                loadInitialData()
            }
        }
    }

    private fun setupRecyclerView() {
        appointmentAdapter = AppointmentPlusAdapter(
            appointments = mutableListOf(),
            onClickListener = ::handleAppointmentClick,
            onLongClickListener = ::handleAppointmentLongClick,
            onPinClickListener = ::togglePinned,
            navigationMap = ::handleNavigationMap,
            onSelectionChanged = ::updateSelectedCount
        )

        binding.recyclerViewAppointments.apply {
            adapter = appointmentAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshAppointments.setOnRefreshListener {
            refreshData()
        }
    }

    private fun setupFabMenu() {
        binding.fabAddAppointment.setOnClickListener {
            // Kiểm tra double click
            if (!canPerformClick()) {
                return@setOnClickListener
            }

            // Kiểm tra nếu dialog đang hiển thị
            if (isDialogShowing) {
                return@setOnClickListener
            }

            // Mở dialog để thêm appointment mới
            showAppointmentDialog()
        }
    }

    private fun setupSelectionToolbar() {
        binding.selectionToolbar.visibility = View.GONE

        binding.btnClose.setOnClickListener { exitSelectionMode() }
        binding.btnPin.setOnClickListener { handlePinAction() }
        binding.btnShare.setOnClickListener { handleShareAction() }
        binding.btnDelete.setOnClickListener { handleDeleteAction() }
        binding.btnMore.setOnClickListener { handleMoreAction(it) }
    }

    // MARK: - Search Setup (Optimized)

    private fun setupSearchFeature() {
        setupSearchBar()
        setupSearchView()
        setupSearchAdapter()
        setupSearchListeners()
    }

    private fun setupSearchBar() {
        binding.searchBar.apply {
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_clear_search -> {
                        clearSearch()
                        true
                    }

                    else -> false
                }
            }
            setNavigationOnClickListener {
                handleSearchBarNavigation()
            }
        }
        updateSearchBarMenu()
    }

    private fun setupSearchView() {
        binding.searchView.apply {
            setupWithSearchBar(binding.searchBar)

            editText.apply {
                doOnTextChanged { text, _, _, _ ->
                    val query = text?.toString()?.trim() ?: ""
                    searchViewModel.updateQuery(query)
                }

                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        val query = text.toString()
                        if (query.isNotBlank()) {
                            performSearch(query)
                            hideSearchView()
                        }
                        true
                    } else false
                }
            }

            addTransitionListener { _, _, newState ->
                handleSearchViewStateChange(newState)
            }
        }
    }

    private fun setupSearchAdapter() {
        searchSuggestionsAdapter = SearchSuggestionsAdapter(
            onSuggestionClick = ::handleSuggestionClick,
            onDeleteSuggestion = ::handleDeleteSuggestion
        )

        binding.rvSearchSuggestions.apply {
            adapter = searchSuggestionsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun setupSearchListeners() {
        // All search listeners are now set up in their respective setup methods
    }

    // MARK: - Search Logic (Simplified)

    private fun initializeSearchForUser() {
        searchViewModel.initializeSearch(currentUserId)
        searchViewModel.setSearchType(SearchType.APPOINTMENT)
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            clearSearch()
            return
        }

        currentSearchQuery = query
        isSearchMode = true

        searchViewModel.searchImmediate(query, SearchType.APPOINTMENT)
        updateSearchBarMenu()
        hideKeyboard()
        showLoading()

        Log.d("AppointmentSearch", "Performing search for: $query")
    }

    private fun performQuickFilter(filterText: String) {
        currentSearchQuery = filterText
        isSearchMode = true

        searchViewModel.applyQuickFilter(filterText, SearchType.APPOINTMENT)
        updateSearchBar(filterText)
        updateSearchBarMenu()
        hideKeyboard()
        showLoading()

        Log.d("AppointmentQuickFilter", "Applying quick filter: $filterText")
    }

    private fun clearSearch() {
        currentSearchQuery = null
        isSearchMode = false
        isSearchViewExpanded = false

        updateSearchBar("")
        searchViewModel.clearSearch()
        updateSearchBarMenu()
        loadAppointments()
        updateUIState()
    }

    private fun hideSearchView() {
        binding.searchView.hide()
        hideKeyboard()
    }

    // MARK: - Search Event Handlers (Optimized)

    private fun handleSearchViewStateChange(newState: SearchView.TransitionState) {
        when (newState) {
            SearchView.TransitionState.SHOWING -> {
                isSearchViewExpanded = true
                enterSearchMode()
            }

            SearchView.TransitionState.HIDDEN -> {
                isSearchViewExpanded = false
                exitSearchMode()
            }

            else -> {}
        }
    }

    private fun handleSearchBarNavigation() {
        when {
            binding.searchView.isShowing -> binding.searchView.hide()
            isSearchMode -> clearSearch()
            else -> requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun handleSuggestionClick(suggestion: SearchSuggestion) {
        when (suggestion.type) {
            SearchSuggestionType.QUICK_FILTER -> {
                performQuickFilter(suggestion.text)
                hideSearchView()
            }

            else -> {
                binding.searchView.editText.setText(suggestion.text)
                performSearch(suggestion.text)
                hideSearchView()
            }
        }
    }

    private fun handleDeleteSuggestion(suggestion: SearchSuggestion) {
        if (suggestion.type in listOf(SearchSuggestionType.HISTORY, SearchSuggestionType.RECENT)) {
            searchViewModel.deleteSearchHistory(suggestion)
        }
    }

    // MARK: - UI State Management (Simplified)

    private fun enterSearchMode() {
        binding.fabAddAppointment.hide()
        hideBottomNavigation()
        hideEmptyStates()
        searchViewModel.generateSuggestions("")
    }

    private fun exitSearchMode() {
        binding.fabAddAppointment.show()
        showBottomNavigation()
        updateUIState()
    }

    private fun updateUIState() {
        when {
            isSearchViewExpanded -> return // Don't update UI while search view is expanded

            isSearchMode && appointmentAdapter.itemCount == 0 -> {
                showSearchEmptyState()
            }

            !isSearchMode && appointmentAdapter.itemCount == 0 -> {
                showRegularEmptyState()
            }

            else -> {
                showAppointmentsList()
                updateSearchBar(currentSearchQuery ?: "")
            }
        }
    }

    private fun showSearchEmptyState() {
        binding.apply {
            searchEmptyState.visibility = View.VISIBLE
            recyclerViewAppointments.visibility = View.GONE
            emptyState.visibility = View.GONE
            appBarLayout.visibility = View.VISIBLE
        }

        val message = getSearchEmptyMessage()
        binding.searchEmptyState.findViewById<TextView>(R.id.tv_empty_message)?.text = message
        updateSearchBar(currentSearchQuery ?: "")
    }

    private fun showRegularEmptyState() {
        binding.apply {
            searchEmptyState.visibility = View.GONE
            recyclerViewAppointments.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            appBarLayout.visibility = View.VISIBLE
        }
    }

    private fun showAppointmentsList() {
        binding.apply {
            searchEmptyState.visibility = View.GONE
            recyclerViewAppointments.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
            appBarLayout.visibility = View.VISIBLE
        }
    }

    private fun hideEmptyStates() {
        binding.apply {
            emptyState.visibility = View.GONE
            searchEmptyState.visibility = View.GONE
        }
    }

    private fun updateSearchBar(text: String) {
        binding.searchBar.setText(text)
    }

    private fun updateSearchBarMenu() {
        binding.searchBar.menu.clear()
        binding.searchBar.inflateMenu(R.menu.search_menu)

        val clearMenuItem = binding.searchBar.menu.findItem(R.id.action_clear_search)
        clearMenuItem?.isVisible = !currentSearchQuery.isNullOrBlank()
    }

    private fun getSearchEmptyMessage(): String {
        return when (currentSearchQuery) {
            "Today" -> "Không có cuộc hẹn nào hôm nay"
            "Upcoming" -> "Không có cuộc hẹn sắp tới"
            "Pinned" -> "Không có cuộc hẹn đã ghim"
            "This Week" -> "Không có cuộc hẹn tuần này"
            else -> "Không tìm thấy cuộc hẹn nào cho \"$currentSearchQuery\""
        }
    }

    // MARK: - Data Observers (Optimized)

    private fun observeUserData() {
        // Already handled in setupUserInfo()
    }

    private fun observeContactData() {
        viewLifecycleOwner.lifecycleScope.launch {
            contactViewModel.contactNamesAndIds.collect { contacts ->
                contactMap.clear()
                contacts.forEach { contact ->
                    contactMap[contact.name] = contact
                }
            }
        }
    }

    private fun observeAppointmentData() {
        viewLifecycleOwner.lifecycleScope.launch {
            appointmentViewModel.appointmentUiState.collect { state ->
                handleAppointmentUiState(state)
            }
        }
    }

    private fun observeSearchData() {
        viewLifecycleOwner.lifecycleScope.launch {
            searchViewModel.uiState.collect { state ->
                handleSearchUiState(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            searchViewModel.suggestions.collect { suggestions ->
                searchSuggestionsAdapter.submitList(suggestions)
            }
        }
    }

    private fun handleAppointmentUiState(state: AppointmentUiState) {
        when (state) {
            is AppointmentUiState.Loading -> showLoading()

            is AppointmentUiState.AppointmentsLoaded -> {
                hideLoading()
                updateAppointmentsList(state.appointments)
            }

            is AppointmentUiState.AppointmentCreated -> {
                hideLoading()
                showMessage(state.message)
                dismissAppointmentDialog()
                refreshData()
                appointmentViewModel.resetUiState()
            }

            is AppointmentUiState.AppointmentUpdated -> {
                hideLoading()
                showMessage(state.message)
                dismissAppointmentDialog()
                refreshData()
                appointmentViewModel.resetUiState()
            }

            is AppointmentUiState.Error -> {
                hideLoading()
                showMessage(state.message)
                appointmentViewModel.resetUiState()
                addAppointmentDialog?.findViewById<MaterialButton>(R.id.btn_save)?.isEnabled = true
            }

            else -> {}
        }
    }

    private fun handleSearchUiState(state: SearchUiState) {
        when (state) {
            is SearchUiState.Loading -> showLoading()

            is SearchUiState.SearchResultsLoaded -> {
                hideLoading()
                updateAppointmentsList(state.results.appointments)
                updateSearchBar(state.query)
                updateUIState()
            }

            is SearchUiState.SuggestionsLoaded -> {
                // Already handled by flow collection
            }

            is SearchUiState.Error -> {
                hideLoading()
                showMessage(state.message)
            }

            is SearchUiState.SearchHistoryDeleted -> {
                showMessage(state.message)
            }

            else -> {}
        }
    }

    // MARK: - Helper Methods

    private fun loadInitialData() {
        contactViewModel.getContactNamesAndIds(currentUserId)
        loadAppointments()
    }

    private fun refreshData() {
        if (isSearchMode) {
            currentSearchQuery?.let { query ->
                performSearch(query)
            }
        } else {
            loadAppointments()
        }
    }

    private fun loadAppointments() {
        if (currentUserId != 0) {
            hideEmptyStates()
            appointmentViewModel.getAllAppointments(
                userId = currentUserId,
                searchQuery = currentSearchQuery ?: "",
                showPinnedOnly = false,
                status = AppointmentStatus.SCHEDULED
            )
        }
    }

    private fun updateAppointmentsList(appointments: List<AppointmentPlus>) {
        appointmentAdapter.updateAppointments(appointments)
        updateUIState()
    }

    private fun showLoading() {
        binding.swipeRefreshAppointments.isRefreshing = true
    }

    private fun hideLoading() {
        binding.swipeRefreshAppointments.isRefreshing = false
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchView.windowToken, 0)
    }

    private fun hideBottomNavigation() {
        (activity as? AppCompatActivity)?.findViewById<View>(R.id.nav_bottom_navigation)?.visibility =
            View.GONE
    }

    private fun showBottomNavigation() {
        (activity as? AppCompatActivity)?.findViewById<View>(R.id.nav_bottom_navigation)?.visibility =
            View.VISIBLE
    }

    // MARK: - Event Handlers

    private fun handleAppointmentClick(appointment: AppointmentPlus) {
        if (!appointmentAdapter.isMultiSelectMode()) {
            // Kiểm tra double click
            if (!canPerformClick()) {
                return
            }

            // Kiểm tra nếu dialog đang hiển thị
            if (isDialogShowing) {
                return
            }

            showAppointmentDialog(appointment)
        }
    }

    private fun handleAppointmentLongClick(appointment: AppointmentPlus, position: Int) {
        if (!appointmentAdapter.isMultiSelectMode()) {
            appointmentAdapter.setMultiSelectionMode(true)
        }
    }

    private fun canPerformClick(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < CLICK_DELAY) {
            return false
        }
        lastClickTime = currentTime
        return true
    }


    private fun updateSelectedCount(count: Int) {
        binding.tvSelectionCount.text = if (count > 1) {
            "$count selected appointments"
        } else {
            "$count selected appointment"
        }

        when {
            count == 0 && isSelectionMode -> exitSelectionMode()
            count > 0 && !isSelectionMode -> enterSelectionMode()
        }
    }

    private fun enterSelectionMode() {
        isSelectionMode = true
        binding.selectionToolbar.visibility = View.VISIBLE
        binding.appBarLayout.visibility = View.GONE
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        binding.selectionToolbar.visibility = View.GONE
        binding.appBarLayout.visibility = View.VISIBLE
        appointmentAdapter.setMultiSelectionMode(false)
        appointmentAdapter.clearSelection()
    }

    private fun togglePinned(appointment: AppointmentPlus) {
        appointmentViewModel.togglePin(appointment.id)
    }

    private fun handleNavigationMap(appointment: AppointmentPlus) {
        val intent = Intent(requireContext(), NavigationMapActivity::class.java)
        intent.putExtra(Constant.EXTRA_APPOINTMENT_ID, appointment.id)
        startActivity(intent)
    }

    private fun handleMapPickerResult(result: androidx.activity.result.ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                location = data.getStringExtra(GoogleMapActivity.EXTRA_SELECTED_ADDRESS)
                latitude = data.getDoubleExtra(GoogleMapActivity.EXTRA_SELECTED_LAT, 0.0)
                longitude = data.getDoubleExtra(GoogleMapActivity.EXTRA_SELECTED_LNG, 0.0)

                addAppointmentDialog?.findViewById<TextInputEditText>(R.id.et_appointment_location)
                    ?.let { addressEditText ->
                        if (!location.isNullOrEmpty()) {
                            addressEditText.setText(location)
                        } else {
                            addressEditText.setText(getString(R.string.no_location_selected))
                            location = ""
                        }
                    }
            }
        }
        if (shouldRestoreDialog) {
            shouldRestoreDialog = false
            binding.root.post {
                if (isEditMode && currentEditingAppointment != null) {
                    showAppointmentDialog(currentEditingAppointment)
                } else {
                    showAppointmentDialog()
                }
            }
        }
    }

    // MARK: - Selection Actions

    private fun handlePinAction() {
        val selectedAppointments = appointmentAdapter.getSelectedAppointments()
        selectedAppointments.forEach {
            appointmentViewModel.togglePin(it.id)
        }
        showMessage("Đã cập nhật trạng thái ghim cho ${selectedAppointments.size} cuộc hẹn")
        exitSelectionMode()
    }

    private fun handleShareAction() {
        val selectedAppointments = appointmentAdapter.getSelectedAppointments()
        if (selectedAppointments.isNotEmpty()) {
            val shareText = buildString {
                append("Thông tin cuộc hẹn:\n\n")
                selectedAppointments.forEach { appointment ->
                    append("Tiêu đề: ${appointment.title}\n")
                    append("Nội dung: ${appointment.description}\n")
                    append("Địa chỉ: ${appointment.location}\n\n")
                }
            }

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }

            startActivity(Intent.createChooser(shareIntent, "Chia sẻ cuộc hẹn"))
        }
        exitSelectionMode()
    }

    private fun handleDeleteAction() {
        val selectedAppointments = appointmentAdapter.getSelectedAppointments()
        if (selectedAppointments.isNotEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa cuộc hẹn")
                .setMessage("Bạn có chắc chắn muốn xóa ${selectedAppointments.size} cuộc hẹn đã chọn?")
                .setPositiveButton("Xóa") { dialog, _ ->
                    selectedAppointments.forEach { appointment ->
                        appointmentViewModel.deleteAppointment(appointment.id)
                    }
                    showMessage("Đã xóa ${selectedAppointments.size} cuộc hẹn")
                    exitSelectionMode()
                    dialog.dismiss()
                }
                .setNegativeButton("Hủy") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } else {
            exitSelectionMode()
        }
    }

    private fun handleMoreAction(view: View) {
        val popMenu = PopupMenu(requireContext(), view)
        popMenu.menuInflater.inflate(R.menu.menu_selection_more, popMenu.menu)

        val selectAllItem = popMenu.menu.findItem(R.id.action_select_all)
        val deselectAllItem = popMenu.menu.findItem(R.id.action_deselect_all)

        val selectedCount = appointmentAdapter.getSelectedCount()
        val totalCount = appointmentAdapter.itemCount

        selectAllItem.isVisible = selectedCount < totalCount
        deselectAllItem.isVisible = selectedCount > 0

        popMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_select_all -> {
                    appointmentAdapter.selectAll()
                    true
                }

                R.id.action_deselect_all -> {
                    appointmentAdapter.clearSelection()
                    true
                }

                else -> false
            }
        }

        popMenu.show()// Implement more actions
    }

    // MARK: - Add Appointment Dialog

    private fun showAppointmentDialog(appointmentToEdit: AppointmentPlus? = null) {
        // Double check - Kiểm tra nếu đã có dialog đang hiển thị
        if (isDialogShowing) {
            return
        }

        // Dismiss dialog cũ nếu có (safety check)
        addAppointmentDialog?.let { dialog ->
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }

        isEditMode = appointmentToEdit != null
        currentEditingAppointment = appointmentToEdit
        isDialogShowing = true

        val dialog = Dialog(requireContext())
        addAppointmentDialog = dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogBinding =
            DialogAddAppointmentBinding.inflate(LayoutInflater.from(requireContext()))
        dialog.setContentView(dialogBinding.root)

        // Cấu hình dialog dựa trên mode
        setupDialogForMode(dialogBinding, appointmentToEdit)

        setupContactDropdown(dialogBinding)
        setupColorPicker(dialogBinding)
        setupLocationInput(dialogBinding)

        // Setup location picker
        dialogBinding.tilAppointmentLocation.setEndIconOnClickListener {
            shouldRestoreDialog = true
            val intent = Intent(requireContext(), GoogleMapActivity::class.java).apply {
                // Nếu đang edit và có vị trí hiện tại
                if (isEditMode && appointmentToEdit?.latitude != null && appointmentToEdit.longitude != null) {
                    putExtra("current_lat", appointmentToEdit.latitude)
                    putExtra("current_lng", appointmentToEdit.longitude)
                    putExtra("current_address", appointmentToEdit.location)
                }
            }
            mapPickerLauncher.launch(intent)
        }

        dialogBinding.btnCancel.setOnClickListener {
            if (!canPerformClick()) {
                return@setOnClickListener
            }
            dismissAppointmentDialog()
        }

        dialogBinding.layoutReminder.setOnClickListener {
            showDateTimePicker(dialogBinding)
        }

        dialogBinding.btnSave.setOnClickListener {
            // Kiểm tra để tránh click nhiều lần
            if (!canPerformClick()) {
                return@setOnClickListener
            }

            dialogBinding.btnSave.isEnabled = false

            if (isEditMode) {
                updateAppointment(dialogBinding, appointmentToEdit!!)
            } else {
                saveAppointment(dialogBinding)
            }
        }

        dialog.setOnDismissListener {
            if (!shouldRestoreDialog) {
                resetDialogState()
            }
        }
        dialog.setOnCancelListener {
            if (!shouldRestoreDialog) {
                resetDialogState()
            }
        }

        location?.let { addr ->
            dialogBinding.etAppointmentLocation.setText(addr)
        }

        dialog.show()
        dialog.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawableResource(R.drawable.border_dialog_background)
            attributes.windowAnimations = R.style.DialogAnimation
            setGravity(Gravity.CENTER_HORIZONTAL)
        }
    }

    private fun setupDialogForMode(
        dialogBinding: DialogAddAppointmentBinding,
        appointmentToEdit: AppointmentPlus?
    ) {
        if (isEditMode && appointmentToEdit != null) {
            // Setup cho Edit mode
            setupEditMode(dialogBinding, appointmentToEdit)
        } else {
            // Setup cho Add mode
            setupAddMode(dialogBinding)
        }
    }

    private fun setupEditMode(
        dialogBinding: DialogAddAppointmentBinding,
        appointment: AppointmentPlus
    ) {
        with(dialogBinding) {
            // Điền dữ liệu hiện tại
            etAppointmentTitle.setText(appointment.title)
            etNotes.setText(appointment.description)
            etAppointmentLocation.setText(appointment.location)
            cbFavorite.isChecked = appointment.isPinned

            // Setup location
            if (appointment.location.isNotBlank()) {
                etAppointmentLocation.setText(appointment.location)
                location = appointment.location
                latitude = appointment.latitude
                longitude = appointment.longitude
            } else {
                etAppointmentLocation.setText("")
                location = null
                latitude = null
                longitude = null
            }

            // Setup reminder time
            reminderTime = appointment.startDateTime
            updateReminderDisplay(dialogBinding)

            // Setup color
            selectedColorName = appointment.color
            if (::colorAdapter.isInitialized) {
                colorAdapter.setSelectedColor(appointment.color)
            }

            // Setup contact
            currentContactId = appointment.contactId

            // Thay đổi text button
            btnSave.text = "Cập nhật"
        }
    }

    private fun setupAddMode(dialogBinding: DialogAddAppointmentBinding) {
        with(dialogBinding) {
            // Clear tất cả fields
            etAppointmentTitle.setText("")
            etNotes.setText("")
            etAppointmentLocation.setText("")
            cbFavorite.isChecked = false
            tvReminderTime.text = "Chọn thời gian"

            // Reset location data
            location = null
            latitude = null
            longitude = null

            // Reset reminder time
            reminderTime = null

            // Reset color
            selectedColorName = "color_white"
            if (::colorAdapter.isInitialized) {
                colorAdapter.setSelectedColor("color_white")
            }

            // Reset contact
            currentContactId = 0

            // Set text button
            btnSave.text = "Thêm"
        }
    }

    // Method để reset trạng thái dialog
    private fun resetDialogState() {
        isDialogShowing = false
        addAppointmentDialog = null
        currentEditingAppointment = null

        // Reset location data
        location = null
        latitude = null
        longitude = null
        reminderTime = null
        selectedColorName = "color_white"
        currentContactId = 0
    }

    // Tạo method riêng để dismiss dialog
    private fun dismissAppointmentDialog() {
        try {
            addAppointmentDialog?.dismiss()
        } catch (e: Exception) {
            // Ignore exception khi dismiss
        } finally {
            resetDialogState()
        }
    }

    // MARK: - Dialog Setup Methods (keeping existing implementation)

    private fun setupContactDropdown(dialogAddAppointmentBinding: DialogAddAppointmentBinding) {
        // Ngăn người dùng nhập text, chỉ cho phép chọn từ dropdown
        dialogAddAppointmentBinding.autoContactName.apply {
            inputType = InputType.TYPE_NULL
            isFocusable = false
            isCursorVisible = false
            keyListener = null
        }

        viewLifecycleOwner.lifecycleScope.launch {
            contactViewModel.contactNamesAndIds.collect { contacts ->
                if (contacts.isNotEmpty()) {
                    val contactNames = mutableListOf("-- Chọn liên hệ --").apply {
                        addAll(contacts.map { it.name })
                    }.toTypedArray()

                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        contactNames
                    )
                    dialogAddAppointmentBinding.autoContactName.setAdapter(adapter)

                    // Xử lý hiển thị contact trong edit mode
                    if (isEditMode && currentContactId != 0) {
                        val selectedContact = contacts.find { it.id == currentContactId }
                        if (selectedContact != null) {
                            // Tìm position của contact trong list (nhớ +1 vì có "-- Chọn liên hệ --" ở đầu)
                            val contactPosition =
                                contacts.indexOfFirst { it.id == currentContactId }
                            if (contactPosition != -1) {
                                val displayPosition = contactPosition + 1
                                dialogAddAppointmentBinding.autoContactName.setText(
                                    contactNames[displayPosition],
                                    false
                                )
                            } else {
                                // Nếu không tìm thấy, reset về default
                                dialogAddAppointmentBinding.autoContactName.setText(
                                    contactNames[0],
                                    false
                                )
                                currentContactId = 0
                            }
                        } else {
                            // Contact không tồn tại, reset về default
                            dialogAddAppointmentBinding.autoContactName.setText(
                                contactNames[0],
                                false
                            )
                            currentContactId = 0
                        }
                    } else {
                        // Add mode hoặc không có contact ID
                        dialogAddAppointmentBinding.autoContactName.setText(contactNames[0], false)
                        if (!isEditMode) {
                            currentContactId = 0
                        }
                    }

                    // Xử lý khi người dùng chọn contact từ dropdown
                    dialogAddAppointmentBinding.autoContactName.setOnItemClickListener { _, _, position, _ ->
                        if (position == 0) {
                            // Chọn "-- Chọn liên hệ --"
                            currentContactId = 0
                            dialogAddAppointmentBinding.autoContactName.setText(
                                contactNames[0],
                                false
                            )
                        } else {
                            // Chọn contact cụ thể
                            val selectedContact =
                                contacts[position - 1] // -1 vì bỏ qua "-- Chọn liên hệ --"
                            currentContactId = selectedContact.id
                            dialogAddAppointmentBinding.autoContactName.setText(
                                selectedContact.name,
                                false
                            )

                            Log.d(
                                "ContactDropdown",
                                "Selected contact: ${selectedContact.name} with ID: ${selectedContact.id}"
                            )
                        }
                    }
                } else {
                    // Không có contact nào
                    handleNoContactsAvailable()
                }
            }
        }
    }

    private fun handleNoContactsAvailable() {
        // Dismiss dialog hiện tại
        dismissAppointmentDialog()

        // Hiển thị dialog hướng dẫn người dùng tạo contact
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Không có liên hệ")
            .setMessage("Bạn cần tạo ít nhất một liên hệ trước khi có thể tạo cuộc hẹn. Bạn có muốn chuyển đến trang Liên hệ để tạo liên hệ mới không?")
            .setPositiveButton("Chuyển đến Liên hệ") { dialog, _ ->
                dialog.dismiss()
                navigateToContactFragment()
            }
            .setNegativeButton("Hủy") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false) // Không cho phép cancel bằng cách nhấn outside
            .show()
    }

    private fun navigateToContactFragment() {
        try {
            val bottomNavigation = (activity as? AppCompatActivity)?.findViewById<BottomNavigationView>(R.id.nav_bottom_navigation)
            if(bottomNavigation != null){
                val contactMenuItem = bottomNavigation.menu.findItem(R.id.nav_contact)
                if (contactMenuItem != null) {
                    bottomNavigation.selectedItemId = contactMenuItem.itemId
                    Log.d("Navigation", "Navigated to Contact via BottomNavigation")
                    return
                }
            }
        } catch (e: Exception) {
            Log.e("Navigation", "Failed to navigate to ContactFragment: ${e.message}")
            Toast.makeText(requireContext(), "Không thể chuyển đến trang Liên hệ. Vui lòng thử lại.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupColorPicker(dialogBinding: DialogAddAppointmentBinding) {
        colorAdapter = ColorPickerAdapter(listColor, colorSourceNames) { colorResId, colorName ->
            val color = ContextCompat.getColor(requireContext(), colorResId)
            dialogBinding.layoutAddAppointment.setBackgroundColor(color)
            selectedColorName = colorName
        }

        dialogBinding.rvColorPicker.adapter = colorAdapter
        dialogBinding.rvColorPicker.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    private fun setupLocationInput(dialogBinding: DialogAddAppointmentBinding) {
        var isLocationFromMap = false

        dialogBinding.tilAppointmentLocation.apply {
            helperText = "💡 Nhập địa chỉ để tự động tìm tọa độ, hoặc nhấn 📍 để chọn chính xác"
            setEndIconDrawable(R.drawable.ic_geo)
            setEndIconContentDescription("Chọn vị trí trên bản đồ")
        }

        dialogBinding.etAppointmentLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val manualLocation = s?.toString()?.trim()

                if (!isLocationFromMap) {
                    geocodingJob?.cancel()

                    if (!manualLocation.isNullOrEmpty()&& manualLocation.length >= 3) {
                        location = manualLocation

                        geocodingJob = lifecycleScope.launch {
                            delay(800)
                            dialogBinding.tilAppointmentLocation.helperText = "🔄 Đang tìm tọa độ..."

                            geocodeAddress(manualLocation) { lat, lng ->
                                if (lat != null && lng != null) {
                                    latitude = lat
                                    longitude = lng
                                    dialogBinding.tilAppointmentLocation.helperText =
                                        "📍 Tọa độ: ${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}"
                                    Log.d("Geocoding", "Found coordinates: $lat, $lng for address: $manualLocation")
                                } else {
                                    latitude = null
                                    longitude = null
                                    dialogBinding.tilAppointmentLocation.helperText =
                                        "⚠️ Không tìm thấy tọa độ - có thể chọn trên bản đồ để chính xác hơn"
                                    Log.d("Geocoding", "No coordinates found for address: $manualLocation")
                                }
                            }
                        }
                    }
                    else if (manualLocation.isNullOrEmpty()) {
                        location = null
                        latitude = null
                        longitude = null
                        dialogBinding.tilAppointmentLocation.helperText =
                            "💡 Nhập địa chỉ để tự động tìm tọa độ, hoặc nhấn 📍 để chọn chính xác"
                        geocodingJob?.cancel()
                    }
                    else {
                        location = manualLocation
                        latitude = null
                        longitude = null
                        dialogBinding.tilAppointmentLocation.helperText = "📝 Nhập thêm để tìm tọa độ..."
                    }
                }

                if (isLocationFromMap) {
                    isLocationFromMap = false
                }
            }
        })

        dialogBinding.etAppointmentLocation.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !isLocationFromMap) {
                val currentText = dialogBinding.etAppointmentLocation.text?.toString()?.trim()
                if (currentText != location) {
                    latitude = null
                    longitude = null
                }
            }
        }
        // Clear helper text when focus lost
        dialogBinding.etAppointmentLocation.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !isLocationFromMap) {
                // Don't clear if we have coordinates info
                val currentHelper = dialogBinding.tilAppointmentLocation.helperText?.toString()
                if (currentHelper != null && !currentHelper.contains("📍")) {
                    dialogBinding.tilAppointmentLocation.helperText = null
                }
            }
        }
    }
    private fun geocodeAddress(address: String, callback: (Double?, Double?) -> Unit) {
        if (address.isBlank() || address.length < 3) {
            callback(null, null)
            return
        }

        // Check cache first
        val cachedResult = geocodingCache[address]
        if (cachedResult != null) {
            callback(cachedResult.first, cachedResult.second)
            return
        }

        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // API 33+ - Async method
                geocoder.getFromLocationName(address, 1) { addresses ->
                    handleGeocodingResult(address, addresses, callback)
                }
            } else {
                // API < 33 - Sync method (need background thread)
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocationName(address, 1)
                        launch(Dispatchers.Main) {
                            handleGeocodingResult(address, addresses, callback)
                        }
                    } catch (e: Exception) {
                        Log.e("Geocoding", "Error geocoding address: ${e.message}")
                        launch(Dispatchers.Main) {
                            callback(null, null)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Geocoding", "Geocoder error: ${e.message}")
            callback(null, null)
        }
    }

    private fun handleGeocodingResult(
        address: String,
        addresses: List<android.location.Address>?,
        callback: (Double?, Double?) -> Unit
    ) {
        if (!addresses.isNullOrEmpty()) {
            val location = addresses[0]
            val result = Pair(location.latitude, location.longitude)
            geocodingCache[address] = result
            callback(location.latitude, location.longitude)
        } else {
            geocodingCache[address] = Pair(null, null)
            callback(null, null)
        }
    }
    private fun showDateTimePicker(binding: DialogAddAppointmentBinding) {
        val constraintBuilder =
            CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now())

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(constraintBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedDate
            showTimePicker(calendar, binding)
        }

        datePicker.show(childFragmentManager, "date_picker")
    }

    private fun showTimePicker(calendar: Calendar, binding: DialogAddAppointmentBinding) {
        val now = Calendar.getInstance()

        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(calendar.get(Calendar.MINUTE))
            .setTitleText("Select time")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val selectedHour = timePicker.hour
            val selectedMinute = timePicker.minute

            if (isToday(calendar)) {
                if (selectedHour < now.get(Calendar.HOUR_OF_DAY) ||
                    (selectedHour == now.get(Calendar.HOUR_OF_DAY)) && selectedMinute <= now.get(
                        Calendar.MINUTE
                    )
                ) {
                    Toast.makeText(
                        requireContext(),
                        "Please select a future time",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnPositiveButtonClickListener
                }
            }

            calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            calendar.set(Calendar.MINUTE, timePicker.minute)
            calendar.set(Calendar.SECOND, 0)

            reminderTime = calendar.timeInMillis
            updateReminderDisplay(binding)
        }

        timePicker.show(childFragmentManager, "time_picker")
    }

    private fun saveAppointment(dialogBinding: DialogAddAppointmentBinding) {
        val title = dialogBinding.etAppointmentTitle.text?.toString()?.trim() ?: ""
        val notes = dialogBinding.etNotes.text?.toString()?.trim() ?: ""
        val appointmentLocation = location ?: ""
        val isPinned = dialogBinding.cbFavorite.isChecked

        // Validation
        if (!validateAppointmentInput(dialogBinding, title)) {
            dialogBinding.btnSave.isEnabled = true
            return
        }

        val endTime = reminderTime!! + (60 * 60 * 1000)


        val appointment = AppointmentPlus(
            userId = currentUserId,
            contactId = currentContactId,
            title = title,
            description = notes,
            startDateTime = reminderTime!!,
            endDateTime = endTime,
            location = appointmentLocation,
            latitude = latitude ?: 0.0 ,
            longitude = longitude ?: 0.0,
            status = AppointmentStatus.SCHEDULED,
            color = selectedColorName,
            travelTimeMinutes = 0,
            isPinned = isPinned
        )

        appointmentViewModel.createAppointment(appointment)
    }

    private fun updateAppointment(
        dialogBinding: DialogAddAppointmentBinding,
        originalAppointment: AppointmentPlus
    ) {
        val title = dialogBinding.etAppointmentTitle.text?.toString()?.trim() ?: ""
        val notes = dialogBinding.etNotes.text?.toString()?.trim() ?: ""
        val appointmentLocation = location ?: ""
        val isPinned = dialogBinding.cbFavorite.isChecked

        // Validation
        if (!validateAppointmentInput(dialogBinding, title)) {
            dialogBinding.btnSave.isEnabled = true
            return
        }

        val endTime = reminderTime!! + (60 * 60 * 1000)

        val updatedAppointment = originalAppointment.copy(
            contactId = currentContactId,
            title = title,
            description = notes,
            startDateTime = reminderTime!!,
            endDateTime = endTime,
            location = appointmentLocation,
            latitude = latitude ?: 0.0,
            longitude = longitude ?: 0.0,
            color = selectedColorName,
            isPinned = isPinned
        )

        appointmentViewModel.updateAppointment(updatedAppointment)
    }

    private fun validateAppointmentInput(
        dialogBinding: DialogAddAppointmentBinding,
        title: String
    ): Boolean {
        // Clear previous errors
        dialogBinding.tilAppointmentTitle.error = null

        // Validation
        if (title.isEmpty()) {
            dialogBinding.tilAppointmentTitle.error = "Vui lòng nhập tiêu đề cuộc hẹn"
            return false
        }

        if (currentContactId == 0) {
            Toast.makeText(requireContext(), "Vui lòng chọn liên hệ", Toast.LENGTH_SHORT).show()
            return false
        }

        if (reminderTime == null) {
            Toast.makeText(requireContext(), "Vui lòng chọn thời gian nhắc nhở", Toast.LENGTH_SHORT)
                .show()
            return false
        }

        return true
    }

    private fun isToday(calendar: Calendar): Boolean {
        val today = Calendar.getInstance()
        return today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
    }

    private fun updateReminderDisplay(binding: DialogAddAppointmentBinding) {
        val formatter = SimpleDateFormat("dd MM yyyy, HH:mm", Locale.getDefault())
        val formattedDate = formatter.format(Date(reminderTime!!))
        binding.tvReminderTime.text = formattedDate
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dismissAppointmentDialog()
        _binding = null
    }
}
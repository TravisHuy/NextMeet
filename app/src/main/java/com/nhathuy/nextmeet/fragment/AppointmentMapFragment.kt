package com.nhathuy.nextmeet.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.search.SearchView
import com.google.android.material.snackbar.Snackbar
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.adapter.AppointmentPlusAdapter
import com.nhathuy.nextmeet.adapter.SearchSuggestionsAdapter
import com.nhathuy.nextmeet.databinding.FragmentAppointmentMapBinding
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.AppointmentStatus
import com.nhathuy.nextmeet.model.Contact
import com.nhathuy.nextmeet.model.ContactNameId
import com.nhathuy.nextmeet.model.SearchSuggestion
import com.nhathuy.nextmeet.model.SearchSuggestionType
import com.nhathuy.nextmeet.model.SearchType
import com.nhathuy.nextmeet.resource.AppointmentUiState
import com.nhathuy.nextmeet.resource.SearchUiState
import com.nhathuy.nextmeet.ui.AddAppointmentActivity
import com.nhathuy.nextmeet.ui.NavigationMapActivity
import com.nhathuy.nextmeet.utils.AppointmentNavigationCallback
import com.nhathuy.nextmeet.utils.Constant
import com.nhathuy.nextmeet.utils.NavigationCallback
import com.nhathuy.nextmeet.viewmodel.AppointmentPlusViewModel
import com.nhathuy.nextmeet.viewmodel.ContactViewModel
import com.nhathuy.nextmeet.viewmodel.SearchViewModel
import com.nhathuy.nextmeet.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AppointmentMapFragment : Fragment(), NavigationCallback, AppointmentNavigationCallback {

    private var _binding: FragmentAppointmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel
    private lateinit var contactViewModel: ContactViewModel
    private lateinit var appointmentViewModel: AppointmentPlusViewModel
    private lateinit var searchViewModel: SearchViewModel

    private var currentUserId: Int = 0
    private val contactMap = mutableMapOf<String, ContactNameId>()

    // Search state
    private var currentSearchQuery: String? = null
    private var isSearchMode: Boolean = false
    private var isSearchViewExpanded: Boolean = false

    // Selection state
    private var isSelectionMode: Boolean = false

    // Click protection
    private var lastClickTime = 0L
    private var CLICK_DELAY = 500L

    // Adapters
    private lateinit var appointmentAdapter: AppointmentPlusAdapter
    private lateinit var searchSuggestionsAdapter: SearchSuggestionsAdapter

    //
    private var selectedContactForFilter: Contact? = null
    private var isContactFilterMode: Boolean = false

    // Activity Result Launcher cho AddAppointmentActivity
    private val addAppointmentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val message = result.data?.getStringExtra("message") ?: "Thao tác thành công"
            showMessage(message)
            refreshData()
        }
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
            if (!canPerformClick()) {
                return@setOnClickListener
            }
            openAddAppointmentActivity()
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

    // MARK: - Search Setup (keeping existing implementation)
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
                    if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
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
        // Search listeners implementation
    }

    // MARK: - Navigation Methods
    private fun openAddAppointmentActivity(appointmentToEdit: AppointmentPlus? = null) {
        val intent = Intent(requireContext(), AddAppointmentActivity::class.java).apply {
            putExtra("current_user_id", currentUserId)

            // Nếu là edit mode, truyền thông tin appointment
            appointmentToEdit?.let { appointment ->
                putExtra("is_edit_mode", true)
                putExtra("appointment_id", appointment.id)
                putExtra("appointment_title", appointment.title)
                putExtra("appointment_description", appointment.description)
                putExtra("appointment_location", appointment.location)
                putExtra("appointment_latitude", appointment.latitude)
                putExtra("appointment_longitude", appointment.longitude)
                putExtra("appointment_start_time", appointment.startDateTime)
                putExtra("appointment_contact_id", appointment.contactId)
                putExtra("appointment_color", appointment.color)
                putExtra("appointment_is_pinned", appointment.isPinned)
            }
        }

        addAppointmentLauncher.launch(intent)
    }

    // MARK: - Event Handlers

    private fun handleAppointmentClick(appointment: AppointmentPlus) {
        if (!appointmentAdapter.isMultiSelectMode()) {
            if (!canPerformClick()) {
                return
            }
            // Mở activity để edit appointment
            openAddAppointmentActivity(appointment)
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
        binding.fabAddAppointment.hide()
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        binding.selectionToolbar.visibility = View.GONE
        binding.appBarLayout.visibility = View.VISIBLE
        appointmentAdapter.setMultiSelectionMode(false)
        appointmentAdapter.clearSelection()
        binding.fabAddAppointment.show()
    }

    private fun togglePinned(appointment: AppointmentPlus) {
        appointmentViewModel.togglePin(appointment.id)
    }

    private fun handleNavigationMap(appointment: AppointmentPlus) {
        val intent = Intent(requireContext(), NavigationMapActivity::class.java)
        intent.putExtra(Constant.EXTRA_APPOINTMENT_ID, appointment.id)
        startActivity(intent)
    }

    // MARK: - Search Methods (keeping existing implementation)
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

    private fun clearSearch() {
        currentSearchQuery = null
        isSearchMode = false
        isSearchViewExpanded = false
        isContactFilterMode = false
        selectedContactForFilter = null

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
            isSearchMode || isContactFilterMode -> clearSearch()
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

    // MARK: - UI State Management
    private fun updateUIState() {
        when {
            isSearchViewExpanded -> return
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
        return when {
            isContactFilterMode && selectedContactForFilter != null ->
                "Không có cuộc hẹn nào với ${selectedContactForFilter!!.name}"
            currentSearchQuery == "Today" -> "Không có cuộc hẹn nào hôm nay"
            currentSearchQuery == "Upcoming" -> "Không có cuộc hẹn sắp tới"
            currentSearchQuery == "Pinned" -> "Không có cuộc hẹn đã ghim"
            currentSearchQuery == "This Week" -> "Không có cuộc hẹn tuần này"
            else -> "Không tìm thấy cuộc hẹn nào cho \"$currentSearchQuery\""
        }
    }

    // MARK: - Data Observers
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

        viewLifecycleOwner.lifecycleScope.launch {
            searchViewModel.navigationFilter.collect { filter ->
                if (filter.isNotEmpty() && currentUserId != 0) {
                    handleNavigationFilter(filter)
                }
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
            is AppointmentUiState.Error -> {
                hideLoading()
                showMessage(state.message)
                appointmentViewModel.resetUiState()
            }
            is AppointmentUiState.StatusUpdated -> {
                // Hiển thị thông báo thành công
                showSuccessMessage(state.message)

                // Refresh data nếu cần
                refreshAppointmentsList()
            }
            else -> {}
        }
    }

    private fun refreshAppointmentsList() {
        appointmentViewModel.getAllAppointments(currentUserId)
    }
    private fun showSuccessMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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


    private fun handleNavigationFilter(filter: String) {
        Log.d("AppointmentNavFilter", "Handling navigation filter: $filter")

        // Set search mode
        currentSearchQuery = filter
        isSearchMode = true

        // Update search bar
        updateSearchBar(filter)
        updateSearchBarMenu()

        // Apply quick filter
        performQuickFilter(filter)

        // Clear navigation filter after handling
        searchViewModel.clearNavigationFilter()
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
        if (selectedAppointments.isEmpty()) {
            exitSelectionMode()
            return
        }


        // Backup appointments trước khi xóa khỏi UI
        val backupAppointments = selectedAppointments.toList()

        // Xóa khỏi UI trước (soft delete)
        appointmentAdapter.removeAppointments(selectedAppointments)

        // thoát selection mode trước khi hiển thị snackbar
        isSelectionMode = false
        binding.selectionToolbar.visibility = View.GONE
        binding.appBarLayout.visibility = View.VISIBLE
        appointmentAdapter.setMultiSelectionMode(false)
        appointmentAdapter.clearSelection()

        val snackbar = Snackbar.make(
            binding.root,
            "Đã xóa ${selectedAppointments.size} cuộc hẹn",
            Snackbar.LENGTH_LONG
        )

        var isUndoClicked = false

        snackbar.setAction("Hoàn tác") {
            isUndoClicked = true
            // Restore lại vào UI
            appointmentAdapter.restoreAppointments(backupAppointments)
            showMessage("Đã hoàn tác xóa cuộc hẹn")
        }

        snackbar.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                // Hiển thị lại FAB khi Snackbar bị dismiss
                binding.fabAddAppointment.show()

                // Chỉ xóa khỏi database khi snackbar bị dismiss và user không hoàn tác
                if (!isUndoClicked && event != DISMISS_EVENT_ACTION) {
                    // Thực hiện xóa thật khỏi database
                    backupAppointments.forEach { appointment ->
                        appointmentViewModel.deleteAppointment(appointment.id)
                    }
                }
            }
        })

        snackbar.show()
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

        popMenu.show()
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
//        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchView.windowToken, 0)
    }

    private fun hideBottomNavigation() {
        (activity as? AppCompatActivity)?.findViewById<View>(R.id.nav_bottom_navigation)?.visibility = View.GONE
    }

    private fun showBottomNavigation() {
        (activity as? AppCompatActivity)?.findViewById<View>(R.id.nav_bottom_navigation)?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun triggerAddAction() {
        binding.fabAddAppointment.performClick()
    }

    override fun onNavigateToAppointmentWithContact(contact: Contact) {
        selectedContactForFilter = contact
        isContactFilterMode = true

        // Update search bar to show contact filter
        val filterText = "Contact: ${contact.name}"
        updateSearchBar(filterText)
        currentSearchQuery = filterText
        isSearchMode = true

        // Apply contact filterd
        applyContactFilter(contact)

        // Update UI state
        updateSearchBarMenu()
        updateUIState()
    }

    override fun onNavigateToAppointmentWithDashboard(filter: String) {
        performQuickFilter(filter)
    }

    private fun applyContactFilter(contact: Contact) {
        showLoading()

        // Use search view model to filter by contact
        searchViewModel.applyContactFilter(contact.id, contact.name)

        // Update search bar display
        binding.searchBar.setText("Contact: ${contact.name}")

        Log.d("AppointmentContactFilter", "Filtering appointments for contact: ${contact.name} (ID: ${contact.id})")
    }

}
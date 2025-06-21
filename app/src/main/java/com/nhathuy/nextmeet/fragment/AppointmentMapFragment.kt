package com.nhathuy.nextmeet.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
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
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import kotlinx.coroutines.launch
import org.w3c.dom.Text
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
    private lateinit var searchViewModel:SearchViewModel

    private var addAppointmentDialog: Dialog? = null
    private var currentUserId: Int = 0
    private var currentContactId: Int = 0

    private var location: String? = null
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var reminderTime: Long? = null

    private val contactMap = mutableMapOf<String, ContactNameId>()

    private lateinit var appointmentAdapter: AppointmentPlusAdapter
    private lateinit var searchSuggestionsAdapter: SearchSuggestionsAdapter

    private lateinit var colorAdapter: ColorPickerAdapter
    private var selectedColorName: String = "color_white"

    private var currentSearchQuery: String? = null
    private var showPinedOnly: Boolean = false

    private var isLocationFromMap: Boolean = false
    private var isSelectionMode: Boolean = false
    private var isSearchMode: Boolean = false
    private var isSearchViewExpanded:Boolean = true

    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                location = data.getStringExtra(GoogleMapActivity.EXTRA_SELECTED_ADDRESS)
                latitude = data.getDoubleExtra(GoogleMapActivity.EXTRA_SELECTED_LAT, 0.0)
                longitude = data.getDoubleExtra(GoogleMapActivity.EXTRA_SELECTED_LNG, 0.0)
                isLocationFromMap = true

                addAppointmentDialog?.findViewById<TextInputEditText>(
                    R.id.et_appointment_location
                )
                    ?.let { addressEditText ->
                        if (!location.isNullOrEmpty()) {
                            addressEditText.setText(location)

                        } else {
                            addressEditText.setText(getString(R.string.no_location_selected))
                            location = ""
                            isLocationFromMap = false
                        }
                    }
            }
        }
    }

    companion object {
        val listColor = listOf(
            R.color.color_white,
            R.color.color_red,
            R.color.color_orange,
            R.color.color_yellow,
            R.color.color_green,
            R.color.color_teal,
            R.color.color_blue,
            R.color.color_dark_blue,
            R.color.color_purple,
            R.color.color_pink,
            R.color.color_brown,
            R.color.color_gray
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

        private const val FILTER_TODAY = "Today"
        private const val FILTER_PINNED = "Pinned"
        private const val FILTER_WEEK = "This Week"
        private const val FILTER_UPCOMING = "Upcoming"

        fun getFilterKeyFormText(text:String ,context: Context):String? {
            return when(text){
                context.getString(R.string.today)  -> FILTER_TODAY
                context.getString(R.string.pinned) -> FILTER_PINNED
                context.getString(R.string.weekend) -> FILTER_WEEK
                context.getString(R.string.upcoming) -> FILTER_UPCOMING
                else -> null
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAppointmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        contactViewModel = ViewModelProvider(this)[ContactViewModel::class.java]
        appointmentViewModel = ViewModelProvider(this)[AppointmentPlusViewModel::class.java]
        searchViewModel = ViewModelProvider(this)[SearchViewModel::class.java]


        setupUserInfo()
        setupRecyclerView()
        setupSwipeRefresh()
        setupObserver()
        setupFabMenu()
        setupSelectionToolbar()
        setupSearchFeature()
        setupAppointmentObserver()
        setupSearchObserver()
    }

    private fun setupUserInfo() {
        userViewModel.getCurrentUser().observe(viewLifecycleOwner) { user ->
            user?.let {
                currentUserId = user.id
                searchViewModel.initializeSearch(currentUserId)
                loadAppointments()
                contactViewModel.getContactNamesAndIds(currentUserId)
            }
        }
    }

    private fun setupRecyclerView() {
        appointmentAdapter = AppointmentPlusAdapter(
            appointments = mutableListOf(),
            onClickListener = { appointment ->
                handleAppointmentClick(appointment)
            },
            onLongClickListener = { appointment, position ->
                handleAppointmentLongClick(appointment, position)
            },
            onPinClickListener = { appointment ->
                togglePinned(appointment)
            },
            navigationMap = { appointment ->
                handleNavigationMap(appointment)
            },
            onSelectionChanged = { count ->
                updateSelectedCount(count)
            }
        )
        binding.recyclerViewAppointments.apply {
            adapter = appointmentAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshAppointments.setOnRefreshListener {
            loadAppointments()
        }
    }

    private fun setupObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            contactViewModel.contactNamesAndIds.collect { contacts ->
                contactMap.clear()
                contacts.forEach { contact ->
                    contactMap[contact.name] = contact
                }
            }
        }
    }

    private fun setupFabMenu() {
        binding.fabAddAppointment.setOnClickListener {
            showAddAppointment()
        }
    }

    private fun setupSelectionToolbar() {
        binding.selectionToolbar.visibility = View.GONE

        binding.btnClose.setOnClickListener {
            closeSelectionMode()
        }
        binding.btnPin.setOnClickListener {
            handlePinAction()
        }
        binding.btnShare.setOnClickListener {
            handleShareAction()
        }

        binding.btnDelete.setOnClickListener {
            handleDeleteAction()
        }

        binding.btnMore.setOnClickListener {
            handleMoreAction()
        }
    }


    private fun setupAppointmentObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            appointmentViewModel.appointmentUiState.collect { state ->
                when (state) {
                    is AppointmentUiState.Loading -> {
                        showLoading()
                    }

                    is AppointmentUiState.AppointmentsLoaded -> {
                        hideLoading()
                        appointmentAdapter.updateAppointments(state.appointments)
                        updateEmptyState(state.appointments.isEmpty())
                    }

                    is AppointmentUiState.AppointmentCreated -> {
                        hideLoading()
                        Snackbar.make(binding.root, state.message, Toast.LENGTH_SHORT).show()
                        addAppointmentDialog?.dismiss()
                        resetDialogData()
                        loadAppointments()
                        appointmentViewModel.resetUiState()
                    }

                    is AppointmentUiState.Error -> {
                        hideLoading()
                        Snackbar.make(binding.root, state.message, Toast.LENGTH_LONG).show()
                        appointmentViewModel.resetUiState()
                    }

                    is AppointmentUiState.PinToggled -> {
//                        showMessage(state.message)
//                        loadAppointments()
                    }

                    else -> {}
                }
            }
        }
    }

    // setup tìm kiếm
    private fun setupSearchFeature(){

        binding.emptyState.visibility = View.GONE
        binding.searchEmptyState.visibility = View.GONE

        binding.searchBar.setOnMenuItemClickListener {
            menuItem ->
            when (menuItem.itemId) {
                else -> false
            }
        }

        //nút xóa tìm kiếm
        setupClearSearchButton()

        //setup search view
        binding.searchView.setupWithSearchBar(binding.searchBar)

        // setup searchAdapter
        setupSearchAdapter()
        //xử lý tìm kiếm khi người dùng nhập
        handleSearchInput()
        //xử lý khi người dùng nhấn nút tìm kiếm
        handleClickSearch()
        // xử lý search view khi trạng thái thay đổi
        handleSearchChangeState()
        //xử lý back button in search
        handleSearchBack()
    }

    //
    private fun setupClearSearchButton(){

        binding.searchBar.menu.clear()
        binding.searchBar.inflateMenu(R.menu.search_menu)

        binding.searchBar.setOnMenuItemClickListener {
            menuItem ->
            when (menuItem.itemId) {
                R.id.action_clear_search -> {
                    clearSearchAndReset()
                    true
                }
                else -> false
            }
        }
        updateClearButtonVisibility()
    }

    // Method để clear search và reset
    private fun clearSearchAndReset() {
        resetSearchResults()
        updateClearButtonVisibility()
    }

    // Method để update visibility của nút clear
    private fun updateClearButtonVisibility() {
        val clearMenuItem = binding.searchBar.menu.findItem(R.id.action_clear_search)
        clearMenuItem?.isVisible = !currentSearchQuery.isNullOrBlank()
    }

    private fun setupSearchAdapter(){
        searchSuggestionsAdapter = SearchSuggestionsAdapter(
            onSuggestionClick = { suggestionText ->
                handleSuggestionClick(suggestionText)
            },
            onDeleteSuggestion = { suggestionText ->
                handleDeleteSuggestion(suggestionText)
            }
        )
        binding.rvSearchSuggestions.apply {
            adapter = searchSuggestionsAdapter
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            setHasFixedSize(true)
        }
    }
    private fun handleSearchInput(){
        binding.searchView.editText.doOnTextChanged{text,_,_,_ ->
            val query = text?.toString()?.trim()?:""
            searchViewModel.updateQuery(query)
        }

        //
        binding.searchView.addTransitionListener {
            _, previousState,newState ->
            if(newState == SearchView.TransitionState.SHOWING && !binding.searchView.text.isNullOrBlank()){
                val query = binding.searchView.text.toString().trim()
                if(query.isNotBlank()){
                    performSearch(query)
                }
            }
        }
    }

    private fun handleClickSearch(){
        binding.searchView.editText.setOnEditorActionListener { _, actionId, _ ->
            if(actionId == EditorInfo.IME_ACTION_SEARCH){
                val query = binding.searchView.text.toString()
                if(query.isNotBlank()){
                    performSearch(query)
                    hideKeyboard()
                    binding.searchView.hide()
                    binding.searchBar.setText("\"$query\"")
                    return@setOnEditorActionListener true
                }
                true
            }
            else{
                false
            }
        }
    }

    private fun handleSearchChangeState(){
        binding.searchView.addTransitionListener {_,_,newState ->
            when(newState){
                SearchView.TransitionState.SHOWING -> {
                    isSearchViewExpanded = true
                    isSearchMode = true

                    binding.fabAddAppointment.hide()
                    hideBottomNavigation()
                    binding.emptyState.visibility = View.GONE
                    binding.searchEmptyState.visibility = View.GONE
                    searchViewModel.initializeSearch(currentUserId)
                    searchViewModel.generateSuggestions("")
                }
                SearchView.TransitionState.HIDDEN -> {
                    isSearchViewExpanded = false
                    isSearchMode =  !currentSearchQuery.isNullOrBlank()

                    binding.fabAddAppointment.show()

                    showBottomNavigation()
                    if (!currentSearchQuery.isNullOrBlank() && appointmentAdapter.itemCount == 0) {
                        binding.searchEmptyState.visibility = View.VISIBLE
                        binding.recyclerViewAppointments.visibility = View.GONE
                        binding.emptyState.visibility = View.GONE
                    } else if (currentSearchQuery.isNullOrBlank()) {
                        // No search, show normal empty state if needed
                        resetSearchResults()
                    } else {
                        // Has results, show recycler view
                        binding.searchEmptyState.visibility = View.GONE
                        binding.recyclerViewAppointments.visibility = View.VISIBLE
                        binding.emptyState.visibility = View.GONE
                    }

                }
                else -> {}
            }
        }
    }

    private fun hideBottomNavigation(){
        val bottomNav = (activity as? AppCompatActivity)?.findViewById<View>(R.id.nav_bottom_navigation)
        bottomNav?.visibility = View.GONE
    }
    private fun showBottomNavigation(){
        val bottomNav = (activity as? AppCompatActivity)?.findViewById<View>(R.id.nav_bottom_navigation)
        bottomNav?.visibility = View.VISIBLE
    }
    private fun handleSearchBack(){
        binding.searchBar.setNavigationOnClickListener {
            when {
                binding.searchView.isShowing -> {
                    binding.searchView.hide()
                }
                !currentSearchQuery.isNullOrBlank() -> {
                    resetSearchResults()
                }
                else -> {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }
    // Thực hiện tìm kiếm
    private fun performSearch(query: String) {
        if (query.isBlank()) {
            resetSearchResults()
            return
        }
        currentSearchQuery = query
        searchViewModel.searchImmediate(query, SearchType.APPOINTMENT)
        Log.d("AppointmentSearch", "Performing search for: $query")
        hideKeyboard()
        binding.swipeRefreshAppointments.isRefreshing = true
        updateClearButtonVisibility()
    }
    // set lại kết quả tìm kiếm
    private fun resetSearchResults(){
        currentSearchQuery = null
        isSearchMode = false
        binding.searchBar.setText("")
        binding.searchView.editText.setText("")
        binding.searchEmptyState.visibility = View.GONE
        loadAppointments()
        searchViewModel.clearSearch()
        updateClearButtonVisibility()
    }

    // ẩn bàn phím
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchView.windowToken, 0)
    }
    private fun setupSearchObserver(){
        viewLifecycleOwner.lifecycleScope.launch {
            searchViewModel.uiState.collect {
                state ->
                when(state){
                    is SearchUiState.Idle -> {

                    }
                    is SearchUiState.LoadingSuggestions -> {}
                    is SearchUiState.SuggestionsLoaded -> {
                        updateSearchSuggestions(state.suggestions)
                    }
                    is SearchUiState.Loading -> {
                        showSearchLoading()
                    }
                    is SearchUiState.SearchResultsLoaded -> {
                        hideSearchLoading()
                        updateAppointmentsFromSearch(state.results.appointments)
//                        updateSearchResultsCount(state.results.appointments.size, state.query)
                        binding.searchBar.setText("${state.query}")
                    }
                    is SearchUiState.Error -> {
                        hideSearchLoading()
                        showMessage(state.message)
                    }
                    is SearchUiState.SearchHistoryDeleted -> {
                        showMessage(state.message)
                    }

                    is SearchUiState.SearchHistoryCleared -> {
                        showMessage(state.message)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            searchViewModel.searchResults.collect {
                results ->
                if(results.isNotEmpty()){
                    updateAppointmentsFromSearch(results.appointments)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            searchViewModel.suggestions.collect {
                suggestions ->
                updateSearchSuggestions(suggestions)
            }
        }
    }
    // Thêm các method hỗ trợ search
    private fun handleSuggestionClick(suggestionText: String) {
        // Set text to search bar
        binding.searchView.editText.setText(suggestionText)
        // Perform search
        performSearch(suggestionText)
        // Hide search view after selection
        binding.searchView.hide()
    }
    private fun handleDeleteSuggestion(suggestionText:String){
        searchViewModel.suggestions.value.find {
            it.text == suggestionText
        }?.let {
            suggestion ->
            searchViewModel.deleteSearchHistory(suggestion)
        }
    }

    private fun updateSearchSuggestions(suggestions: List<SearchSuggestion>) {
        searchSuggestionsAdapter.submitList(suggestions)
    }


    private fun updateSearchResultsCount(count: Int, query: String) {
        // Có thể update title hoặc subtitle để hiển thị số kết quả
        val message = if (count > 0) {
            "Found $count appointments for '$query'"
        } else {
            "No appointments found for '$query'"
        }
        // showMessage(message) // Uncomment nếu muốn hiển thị message
    }

    private fun showSearchLoading() {
        // Có thể hiển thị loading indicator cho search
        binding.swipeRefreshAppointments.isRefreshing = true
    }

    private fun hideSearchLoading() {
        binding.swipeRefreshAppointments.isRefreshing = false
    }

    private fun handleAppointmentClick(appointment: AppointmentPlus) {

    }

    private fun handleAppointmentLongClick(appointment: AppointmentPlus, position: Int) {
        if (!appointmentAdapter.isMultiSelectMode()) {
            appointmentAdapter.setMultiSelectionMode(true)
        }
    }

    private fun updateSelectedCount(count: Int) {
        binding.tvSelectionCount.text = if (count > 1) {
            "$count selected contants"
        } else {
            "$count selected contant"
        }
        if (count == 0 && isSelectionMode) {
            closeSelectionMode()
        } else if (count > 0 && !isSelectionMode) {
            enterSelectionMode()
        }
    }

    // đóng chế độ chọn
    private fun closeSelectionMode() {
        isSelectionMode = false
        binding.selectionToolbar.visibility = View.GONE
        binding.appBarLayout.visibility = View.VISIBLE
        appointmentAdapter.setMultiSelectionMode(false)
        appointmentAdapter.clearSelection()
    }

    // vào chế độ chọn
    private fun enterSelectionMode() {
        isLocationFromMap = true
        binding.selectionToolbar.visibility = View.VISIBLE
        binding.appBarLayout.visibility = View.GONE
    }

    //toggle pinned
    private fun togglePinned(appointment: AppointmentPlus) {
        appointmentViewModel.togglePin(appointment.id)
    }

    //chuyen sang tran hien thi ban do
    private fun handleNavigationMap(appointment: AppointmentPlus) {
        val intent = Intent(requireContext(), NavigationMapActivity::class.java)
        intent.putExtra(Constant.EXTRA_APPOINTMENT_ID, appointment.id)
        startActivity(intent)
    }

    //hien thi loading
    private fun showLoading() {
        binding.swipeRefreshAppointments.isRefreshing = true
    }

    //an loading
    private fun hideLoading() {
        binding.swipeRefreshAppointments.isRefreshing = false
    }


    // cập nhật trạng thái rỗng
    private fun updateEmptyState(isEmpty: Boolean) {
        val emptyStateView = view?.findViewById<View>(R.id.empty_state)

        if (isEmpty && !isSearchMode && currentSearchQuery.isNullOrEmpty()) {
            emptyStateView?.visibility = View.VISIBLE
            binding.recyclerViewAppointments.visibility = View.GONE
        } else {
            emptyStateView?.visibility = View.GONE
            binding.recyclerViewAppointments.visibility = View.VISIBLE
        }

        updateSearchEmptyState(isEmpty && isSearchMode && !currentSearchQuery.isNullOrEmpty())
    }

    private fun updateSearchEmptyState(showSearchEmptyState: Boolean) {
        val searchEmptyStateView = view?.findViewById<View>(R.id.search_empty_state)

        if (showSearchEmptyState) {
            searchEmptyStateView?.visibility = View.VISIBLE
        } else {
            searchEmptyStateView?.visibility = View.GONE
        }
    }



    // Hiển thị message
    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    // Hiển thị error
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    //load cuộn hẹn
    private fun loadAppointments() {
        if (currentUserId != 0) {
            binding.emptyState.visibility = View.GONE
            binding.searchEmptyState.visibility = View.GONE
            appointmentViewModel.getAllAppointments(
                userId = currentUserId,
                searchQuery = currentSearchQuery ?: "",
                showPinnedOnly = showPinedOnly,
                status = AppointmentStatus.SCHEDULED
            )
        }
    }

    //hiển thị dialog thêm cuộc hẹn
    private fun showAddAppointment() {
        val dialog = Dialog(requireContext())
        addAppointmentDialog = dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogBinding =
            DialogAddAppointmentBinding.inflate(LayoutInflater.from(requireContext()))
        dialog.setContentView(dialogBinding.root)

        // reset dialog
        resetDialogData()

        dialogBinding.tilAppointmentLocation.setEndIconOnClickListener {
            val intent = Intent(requireContext(), GoogleMapActivity::class.java)
            mapPickerLauncher.launch(intent)
        }

        setupContactDropdown(dialogBinding)
        setupColorPicker(dialogBinding)
        setupLocationInput(dialogBinding)


        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialogBinding.layoutReminder.setOnClickListener {
            showDateTimePicker(dialogBinding)
        }
        dialogBinding.btnSave.setOnClickListener {
            saveAppointment(dialogBinding)
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

    //set up contact drop down với placeholder option
    private fun setupContactDropdown(dialogAddAppointmentBinding: DialogAddAppointmentBinding) {
        // Ngăn người dùng nhập trực tiếp vào dropdown
        dialogAddAppointmentBinding.autoContactName.inputType = InputType.TYPE_NULL

        viewLifecycleOwner.lifecycleScope.launch {
            contactViewModel.contactNamesAndIds.collect { contacts ->
                if (contacts.isNotEmpty()) {
                    // Thêm option placeholder ở đầu danh sách
                    val contactNames = mutableListOf("-- Chọn liên hệ --").apply {
                        addAll(contacts.map { it.name })
                    }.toTypedArray()

                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        contactNames
                    )
                    dialogAddAppointmentBinding.autoContactName.setAdapter(adapter)

                    // Đặt text placeholder làm mặc định
                    dialogAddAppointmentBinding.autoContactName.setText(contactNames[0], false)
                    currentContactId = 0

                    dialogAddAppointmentBinding.autoContactName.setOnItemClickListener { _, _, position, _ ->
                        if (position == 0) {
                            // Nếu chọn placeholder, reset currentContactId
                            currentContactId = 0
                            dialogAddAppointmentBinding.autoContactName.setText(
                                contactNames[0],
                                false
                            )
                        } else {
                            // Nếu chọn contact thực, lấy thông tin contact (position - 1 vì có placeholder)
                            val selectedContactName = contacts[position - 1].name
                            currentContactId = contacts[position - 1].id
                            dialogAddAppointmentBinding.autoContactName.setText(
                                selectedContactName,
                                false
                            )
                        }
                    }
                } else {
                    // Nếu không có contact nào
                    val emptyArray = arrayOf("Chưa có liên hệ nào")
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        emptyArray
                    )
                    dialogAddAppointmentBinding.autoContactName.setAdapter(adapter)
                    dialogAddAppointmentBinding.autoContactName.setText(emptyArray[0], false)
                    currentContactId = 0
                }
            }
        }
    }

    private fun setupColorPicker(dialogBinding: DialogAddAppointmentBinding) {
        colorAdapter = ColorPickerAdapter(
            listColor,
            colorSourceNames
        ) { colorResId, colorName ->
            val color = ContextCompat.getColor(requireContext(), colorResId)

            dialogBinding.layoutAddAppointment.setBackgroundColor(color)
            selectedColorName = colorName
        }

        dialogBinding.rvColorPicker.adapter = colorAdapter
        dialogBinding.rvColorPicker.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    // setup location input
    private fun setupLocationInput(dialogBinding: DialogAddAppointmentBinding) {
        dialogBinding.etAppointmentLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val manualLocation = s?.toString()?.trim()

                // Chỉ cập nhật location nếu người dùng đang nhập thủ công (không phải từ map picker)
                if (!isLocationFromMap) {
                    if (!manualLocation.isNullOrEmpty()) {
                        location = manualLocation
                        // Reset coordinates vì đây là input thủ công
                        latitude = null
                        longitude = null
                    } else {
                        location = null
                        latitude = null
                        longitude = null
                    }
                }

                // Reset flag sau khi xử lý
                if (isLocationFromMap) {
                    isLocationFromMap = false
                }
            }
        })
//        // Thêm hint cho EditText
//        dialogBinding.etAppointmentLocation.hint = ""

        // Clear coordinates when user starts typing manually
        dialogBinding.etAppointmentLocation.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !isLocationFromMap) {
                // User started typing manually, prepare to clear coordinates
                val currentText = dialogBinding.etAppointmentLocation.text?.toString()?.trim()
                if (currentText != location) {
                    latitude = null
                    longitude = null
                }
            }
        }
    }

    // hiển thị ra ngày ,tháng năm
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

    // hiển thị thời gian chọn
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
        if (title.isEmpty()) {
            dialogBinding.tilAppointmentTitle.error = "Vui lòng nhập tiêu đề cuộc hẹn"
            return
        } else {
            dialogBinding.tilAppointmentTitle.error = null
        }

        if (currentContactId == 0) {
            Toast.makeText(requireContext(), "Vui lòng chọn liên hệ", Toast.LENGTH_SHORT).show()
            return
        }

        if (reminderTime == null) {
            Toast.makeText(requireContext(), "Vui lòng chọn thời gian nhắc nhở", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val endTime = reminderTime!! + (60 * 60 * 1000) // Add 1 hour

        val appointment = AppointmentPlus(
            userId = currentUserId,
            contactId = currentContactId,
            title = title,
            description = notes,
            startDateTime = reminderTime!!,
            endDateTime = endTime,
            location = appointmentLocation,
            latitude = latitude ?: 0.0,
            longitude = longitude ?: 0.0,
            status = AppointmentStatus.SCHEDULED,
            color = selectedColorName,
            travelTimeMinutes = 0,
            isPinned = isPinned
        )

        // tạo cuoc hẹn
        appointmentViewModel.createAppointment(appointment)
    }

    private fun resetDialogData() {
        currentContactId = 0
        location = null
        latitude = null
        longitude = null
        reminderTime = null
        selectedColorName = "color_white"
        isLocationFromMap = false
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

    // xu ly action pin
    private fun handlePinAction() {
        val selectedAppointments = appointmentAdapter.getSelectedAppointments()
        selectedAppointments.forEach {
            appointmentViewModel.togglePin(it.id)
        }
        showMessage("Đã cập nhật trạng thái ghim cho ${selectedAppointments.size} cuộc hẹn")
        closeSelectionMode()
    }

    // xu ly action share
    private fun handleShareAction() {
        val selectedContacts = appointmentAdapter.getSelectedAppointments()
        if (selectedContacts.isNotEmpty()) {
            val shareText = buildString {
                append("Thông tin cuộc hẹn:\n\n")
                selectedContacts.forEach { appointment ->
                    append("Tiêu đề: ${appointment.title}\n")
                    append("Nội dung: ${appointment.description}\n")
                    append("Địa chỉ: ${appointment.location}\n")
                    append("\n")
                }
            }

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }

            startActivity(Intent.createChooser(shareIntent, "Chia sẻ cuộn hẹn"))
        }
        closeSelectionMode()
    }

    // xử lý delete
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
                    closeSelectionMode()
                    dialog.dismiss()
                }
                .setNegativeButton("Hủy") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } else {
            closeSelectionMode()
        }
    }

    // xử lý action more
    private fun handleMoreAction() {

    }


    // cập nhật danh sách cuộc hẹn từ kết quả tìm kiếm
    private fun updateAppointmentsFromSearch(appointments:List<AppointmentPlus>){
        appointmentAdapter.updateAppointments(appointments)

        val isEmpty = appointments.isEmpty()

        // Update empty state with search context
        if (isSearchViewExpanded) {
            binding.searchEmptyState.visibility = View.GONE
            return
        }

        // Only show empty state in fragment when search view is closed
        if (isEmpty && isSearchMode && !currentSearchQuery.isNullOrEmpty()) {
            binding.searchEmptyState.visibility = View.VISIBLE
            binding.recyclerViewAppointments.visibility = View.GONE
            binding.emptyState.visibility = View.GONE
        } else {
            binding.searchEmptyState.visibility = View.GONE
            binding.recyclerViewAppointments.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        addAppointmentDialog?.dismiss()
        _binding = null
    }
}

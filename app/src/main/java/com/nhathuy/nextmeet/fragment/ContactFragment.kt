package com.nhathuy.nextmeet.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.PopupMenu
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.search.SearchView
import com.google.android.material.snackbar.Snackbar
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.adapter.ContactsAdapter
import com.nhathuy.nextmeet.adapter.SearchSuggestionsAdapter
import com.nhathuy.nextmeet.databinding.DialogAddContactBinding
import com.nhathuy.nextmeet.databinding.FragmentContactBinding
import com.nhathuy.nextmeet.model.Contact
import com.nhathuy.nextmeet.model.SearchSuggestion
import com.nhathuy.nextmeet.model.SearchSuggestionType
import com.nhathuy.nextmeet.model.SearchType
import com.nhathuy.nextmeet.resource.ContactUiState
import com.nhathuy.nextmeet.resource.SearchUiState
import com.nhathuy.nextmeet.ui.GoogleMapActivity
import com.nhathuy.nextmeet.ui.SolutionActivity
import com.nhathuy.nextmeet.utils.ValidationUtils
import com.nhathuy.nextmeet.viewmodel.ContactViewModel
import com.nhathuy.nextmeet.viewmodel.SearchViewModel
import com.nhathuy.nextmeet.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ContactFragment : Fragment(), SolutionActivity.NavigationCallback {
    private var _binding: FragmentContactBinding? = null
    private val binding get() = _binding!!

    private lateinit var contactViewModel: ContactViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var searchViewModel: SearchViewModel

    private var addContactDialog: Dialog? = null
    private var currentUserId: Int = 0

    private var address: String? = null
    private var latitude: Double? = null
    private var longitude: Double? = null

    // search state
    private var currentSearchQuery: String? = null
    private var showFavoriteOnly: Boolean = false
    private var isSearchViewExpanded: Boolean = false

    // selection state
    private var isSelectionMode: Boolean = false
    private var isSearchMode: Boolean = false

    // adapter
    private lateinit var searchSuggestionsAdapter: SearchSuggestionsAdapter
    private lateinit var contactsAdapter: ContactsAdapter

    // update contact
    private var currentEditingContact: Contact? = null
    private var isEditMode: Boolean = false
    private var isDialogShowing: Boolean = false
    private var lastClickTime = 0L
    private var CLICK_DELAY = 500L
    private var shouldRestoreDialog = false


    //delete contact
    private var deleteContact: Contact? = null

    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                address = data.getStringExtra(GoogleMapActivity.EXTRA_SELECTED_ADDRESS)
                latitude = data.getDoubleExtra(GoogleMapActivity.EXTRA_SELECTED_LAT, 0.0)
                longitude = data.getDoubleExtra(GoogleMapActivity.EXTRA_SELECTED_LNG, 0.0)

                addContactDialog?.findViewById<TextView>(R.id.tv_location)?.let { addressTextView ->
                    if (address != null) {
                        addressTextView.text = address
                    } else {
                        addressTextView.text = getString(R.string.no_location_selected)
                    }
                }

            }
        }

        if (shouldRestoreDialog) {
            shouldRestoreDialog = false
            binding.root.post {
                if (isEditMode && currentEditingContact != null) {
                    showContactDialog(currentEditingContact)
                } else {
                    showContactDialog()
                }
            }
        }
    }

    // lấy chuyển cảnh từng thêm cuộn hẹn đến
    private val addAppointmentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val navigateToContact =
                result.data?.getBooleanExtra("navigate_to_contact", false) ?: false
            if (navigateToContact) {
                showMessage("Vui lòng thêm liên hệ để tạo cuộc hẹn")
                showContactDialog()
            }
            val message = result.data?.getStringExtra("message")
            if (!message.isNullOrEmpty()) {
                showMessage(message)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentContactBinding.inflate(inflater, container, false)
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
        observerContactData()
        observeSearchData()
    }

    // khởi tạo thông tin người dùng
    private fun setupUserInfo() {
        userViewModel.getCurrentUser().observe(viewLifecycleOwner) { user ->
            user?.let {
                currentUserId = user.id
                initializeSearchForUser()
                loadContacts()
            }
        }
    }

    // thiết lập các observer
    private fun observerContactData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                contactViewModel.contactUiState.collect { state ->
                    handleContactUiState(state)
                }
            }
        }
    }

    //
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

    // xu ly contact ui state
    private fun handleContactUiState(state: ContactUiState) {
        when (state) {
            is ContactUiState.Idle -> {
                hideLoading()
            }

            is ContactUiState.Loading -> {
                showLoading()
            }

            is ContactUiState.ContactsLoaded -> {
                hideLoading()
                updateContactsList(state.contacts)
            }

            is ContactUiState.ContactCreated -> {
                hideLoading()
                showMessage(state.message)
                refreshData()
                contactViewModel.resetUiState()
                dismissDialog()
            }

            is ContactUiState.FavoriteToggled -> {
                showMessage(state.message)
                refreshData()
            }

            is ContactUiState.ContactUpdated -> {
                hideLoading()
                showMessage(state.message)
                refreshData()
                contactViewModel.resetUiState()
                dismissDialog()
            }

            is ContactUiState.Error -> {
                hideLoading()
                showMessage(state.message)
                contactViewModel.resetUiState()
                enableSaveButton()
            }

            else -> {}
        }
    }

    private fun handleSearchUiState(state: SearchUiState) {
        when (state) {
            is SearchUiState.Loading -> showLoading()

            is SearchUiState.SearchResultsLoaded -> {
                hideLoading()
                updateContactsList(state.results.contacts)
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

    // thiết lập recycler view
    private fun setupRecyclerView() {
        contactsAdapter = ContactsAdapter(
            contacts = mutableListOf(),
            onContactClick = { contact ->
                handleContactClick(contact)
            },
            onContactLongClick = { contact, position ->
                handleContactLongClick(contact, position)
            },
            onContactFavorite = { contact ->
                contactViewModel.toggleFavorite(contact.id)
            },
            onContactPhone = { contact ->
                makePhone(contact.phone)
            },
            onContactAppointment = { contact ->
                createAppointmentWithContact(contact)
            },
            onSelectionChanged = { count ->
                updateSelectedCount(count)
            }
        )
        binding.recyclerViewContacts.apply {
            adapter = contactsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    // Thiết lập swipeRefreshLayout
    private fun setupSwipeRefresh() {
        binding.swipeRefreshContacts.setOnRefreshListener {
            loadContacts()
        }
    }

    //thiết lập fab menu
    private fun setupFabMenu() {
        binding.fabAddContact.setOnClickListener {

            // kiểm tra double click
            if (!canPerformClick()) {
                return@setOnClickListener
            }

            // Kiểm tra nếu dialog đang hiển thị
            if (isDialogShowing) {
                return@setOnClickListener
            }

            // Mở dialog để thêm liên hệ mới
            showContactDialog()
        }
    }

    // hiển thị custom selection toolbar
    private fun setupSelectionToolbar() {

        binding.selectionToolbar.visibility = View.GONE

        binding.btnClose.setOnClickListener {
            closeSelectionMode()
        }

        binding.btnFavorite.setOnClickListener {
            handleFavoriteAction()
        }

        binding.btnShare.setOnClickListener {
            handleShareAction()
        }
        binding.btnDelete.setOnClickListener {
            handleDeleteAction()
        }
        binding.btnMore.setOnClickListener {
            showSelectionPopMenu(it)
        }
    }

    // search setup
    private fun setupSearchFeature() {
        setupSearchBar()
        setupSearchView()
        setupSearchAdapter()
        setupSearchListeners()
    }

    // setup search bar
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

    //setup search view
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
                addTransitionListener { _, _, newState ->
                    handleSearchViewStateChange(newState)
                }

            }
        }
    }

    //setup search adapter
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

    // handle search suggestion
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

    private fun enterSearchMode() {
        binding.fabAddContact.hide()
        hideBottomNavigation()
        hideEmptyStates()
        searchViewModel.generateSuggestions("")
    }

    private fun exitSearchMode() {
        binding.fabAddContact.show()
        showBottomNavigation()
        updateUIState()
    }

    private fun updateUIState() {
        when {
            isSearchViewExpanded -> return // Don't update UI while search view is expanded

            isSearchMode && contactsAdapter.itemCount == 0 -> {
                showSearchEmptyState()
            }

            !isSearchMode && contactsAdapter.itemCount == 0 -> {
                showRegularEmptyState()
            }

            else -> {
                showContactsList()
                updateSearchBar(currentSearchQuery ?: "")
            }
        }
    }

    private fun showSearchEmptyState() {
        binding.apply {
            searchEmptyState.visibility = View.VISIBLE
            recyclerViewContacts.visibility = View.GONE
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
            recyclerViewContacts.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            appBarLayout.visibility = View.VISIBLE
        }
    }

    // hien thi contact list
    private fun showContactsList() {
        binding.apply {
            searchEmptyState.visibility = View.GONE
            recyclerViewContacts.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
            appBarLayout.visibility = View.VISIBLE
        }
    }

    // an empty state
    private fun hideEmptyStates() {
        binding.apply {
            emptyState.visibility = View.GONE
            searchEmptyState.visibility = View.GONE
        }
    }

    // cap nhat search bar
    private fun updateSearchBar(text: String) {
        binding.searchBar.setText(text)
    }

    //setup search listeners
    private fun setupSearchListeners() {

    }

    //search logic
    private fun initializeSearchForUser() {
        searchViewModel.initializeSearch(currentUserId)
        searchViewModel.setSearchType(SearchType.CONTACT)
    }

    // perform search
    private fun performSearch(query: String) {
        if (query.isBlank()) {
            clearSearch()
            return
        }

        currentSearchQuery = query
        isSearchMode = true

        searchViewModel.searchImmediate(query, SearchType.CONTACT)

        updateSearchBarMenu()
        hideKeyboard()
        showLoading()

    }

    private fun performQuickFilter(filterText: String) {
        currentSearchQuery = filterText
        isSearchMode = true

        searchViewModel.applyQuickFilter(filterText, SearchType.CONTACT)
        updateSearchBar(filterText)
        updateSearchBarMenu()
        hideKeyboard()
        showLoading()
        Log.d("AppointmentQuickFilter", "Applying quick filter: $filterText")
    }

    // clear search
    private fun clearSearch() {
        currentSearchQuery = null
        isSearchMode = false
        isSearchViewExpanded = false

        updateSearchBar("")
        searchViewModel.clearSearch()
        updateSearchBarMenu()
        loadContacts()
        updateUIState()
    }

    // an search view
    private fun hideSearchView() {
        binding.searchView.hide()
        hideKeyboard()
    }

    // search event handlers
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

    private fun updateSearchBarMenu() {
        binding.searchBar.menu.clear()
        binding.searchBar.inflateMenu(R.menu.search_menu)

        val clearMenuItem = binding.searchBar.menu.findItem(R.id.action_clear_search)
        clearMenuItem?.isVisible = !currentSearchQuery.isNullOrBlank()
    }

    private fun getSearchEmptyMessage(): String {
        return when (currentSearchQuery) {
            context?.getString(R.string.favorite) -> "Không có liên hệ nào yêu thích"
            context?.getString(R.string.have_phone_number) -> "Không có liên hệ nào có số điện thoại "
            context?.getString(R.string.have_email) -> "Không có liên hệ nào có email"
            context?.getString(R.string.have_address) -> "Không có liên hệ nào có địa chỉ"
            else -> "Không tìm thấy liên hệ nào cho \"$currentSearchQuery\""
        }
    }

    //load danh sách contact
    private fun loadContacts() {
        if (currentUserId != 0) {
            contactViewModel.getAllContacts(
                userId = currentUserId,
                searchQuery = currentSearchQuery ?: "",
                showFavoriteOnly = showFavoriteOnly
            )
        }
    }

    //xử lý khi click vào contact
    private fun handleContactClick(contact: Contact) {
        if (!contactsAdapter.isMultiSelectMode()) {

            // Kiểm tra double click
            if (!canPerformClick()) {
                return
            }

            // Kiểm tra nếu dialog đang hiển thị
            if (isDialogShowing) {
                return
            }

            showContactDialog(contact)
        }
    }

    // xử lý để kiểm tra có click hay không
    private fun canPerformClick(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < CLICK_DELAY) {
            return false
        }
        lastClickTime = currentTime
        return true
    }

    //xử lý khi long click vào contact
    private fun handleContactLongClick(contact: Contact, position: Int) {
        if (!contactsAdapter.isMultiSelectMode()) {
            contactsAdapter.setMultiSelectMode(true)
        }
    }

    //vào chế độ selection
    private fun enterSelectionMode() {
        isSelectionMode = true
        binding.selectionToolbar.visibility = View.VISIBLE
        binding.appBarLayout.visibility = View.GONE
        binding.fabAddContact.visibility = View.GONE
    }

    //thoát chế độ selection
    private fun closeSelectionMode() {
        isSelectionMode = false
        binding.selectionToolbar.visibility = View.GONE
        binding.appBarLayout.visibility = View.VISIBLE
        binding.fabAddContact.visibility = View.VISIBLE
        contactsAdapter.setMultiSelectMode(false)
        contactsAdapter.clearSelection()
    }

    //cập nhật lại số lượng đã chọn
    private fun updateSelectedCount(count: Int) {
        binding.tvSelectionCount.text = if (count > 1) {
            "$count selected contacts"
        } else {
            "$count selected contact"
        }
        if (count == 0 && isSelectionMode) {
            closeSelectionMode()
        } else if (count > 0 && !isSelectionMode) {
            enterSelectionMode()
        }
    }

    //gọi điện thoại
    private fun makePhone(phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phone")
            }
            startActivity(intent)
        } catch (e: Exception) {
            showError("Không thể gọi điện")
        }
    }

    //tạo cuộc hẹn với contact
    private fun createAppointmentWithContact(contact: Contact) {

    }

    // cập nhật trạng thái rỗng
    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            // Có thể hiển thị empty view
            showMessage("Không có liên hệ nào")
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

    private fun showContactDialog(contactToEdit: Contact? = null) {
        // Double check - Kiểm tra nếu đã có dialog đang hiển thị
        if (isDialogShowing) {
            return
        }

        // Dismiss dialog cũ nếu có (safety check)
        addContactDialog?.let { dialog ->
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }


        isEditMode = contactToEdit != null
        currentEditingContact = contactToEdit
        isDialogShowing = true

        val dialog = Dialog(requireContext())
        addContactDialog = dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogBinding = DialogAddContactBinding.inflate(LayoutInflater.from(requireContext()))
        dialog.setContentView(dialogBinding.root)

        // Cấu hình dialog dựa trên mode
        setupDialogForMode(dialogBinding, contactToEdit)

        // Setup location picker
        dialogBinding.btnPickLocation.setOnClickListener {
            shouldRestoreDialog = true
            val intent = Intent(requireContext(), GoogleMapActivity::class.java).apply {
                // Nếu đang edit và có vị trí hiện tại
                if (isEditMode && contactToEdit?.latitude != null && contactToEdit.longitude != null) {
                    putExtra("current_lat", contactToEdit.latitude)
                    putExtra("current_lng", contactToEdit.longitude)
                    putExtra("current_address", contactToEdit.address)
                }
            }
            mapPickerLauncher.launch(intent)
        }

        // Setup save button
        dialogBinding.btnSave.setOnClickListener {

            // Kiểm tra để tránh click nhiều lần
            if (!canPerformClick()) {
                return@setOnClickListener
            }

            dialogBinding.btnSave.isEnabled = false

            if (isEditMode) {
                updateContact(dialogBinding, contactToEdit!!)
            } else {
                createContact(dialogBinding)
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            if (!canPerformClick()) {
                return@setOnClickListener
            }
            dismissDialog()
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

        address?.let { addr ->
            dialogBinding.tvLocation.text = addr
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

    // Method để reset trạng thái dialog
    private fun resetDialogState() {
        isDialogShowing = false
        addContactDialog = null
        currentEditingContact = null

        // Reset location data
        address = null
        latitude = null
        longitude = null
    }

    // Tạo method riêng để dismiss dialog
    private fun dismissDialog() {
        try {
            addContactDialog?.dismiss()
        } catch (e: Exception) {
            // Ignore exception khi dismiss
        } finally {
            resetDialogState()
        }
    }

    private fun setupDialogForMode(
        dialogBinding: DialogAddContactBinding,
        contactToEdit: Contact?
    ) {
        if (isEditMode && contactToEdit != null) {
            // Setup cho Edit mode
            setupEditMode(dialogBinding, contactToEdit)
        } else {
            // Setup cho Add mode
            setupAddMode(dialogBinding)
        }
        // xử lý khi nhận sai validation
        setupRealTimeValidation(dialogBinding)
    }

    private fun setupRealTimeValidation(dialogBinding: DialogAddContactBinding) {
        with(dialogBinding) {
            etFullName.doOnTextChanged { _, _, _, _ ->
                tilName.error = null
                if (!btnSave.isEnabled) {
                    btnSave.isEnabled = true
                }

                etPhone.doOnTextChanged { _, _, _, _ ->
                    tilPhone.error = null
                    if (!btnSave.isEnabled) {
                        btnSave.isEnabled = true
                    }
                }

                etEmail.doOnTextChanged { _, _, _, _ ->
                    tilEmail.error = null
                    if (!btnSave.isEnabled) {
                        btnSave.isEnabled = true
                    }
                }
            }
        }
    }

    private fun enableSaveButton() {
        addContactDialog?.findViewById<View>(R.id.btn_save)?.isEnabled = true
    }

    private fun setupEditMode(dialogBinding: DialogAddContactBinding, contact: Contact) {
        with(dialogBinding) {

            // Điền dữ liệu hiện tại
            etFullName.setText(contact.name)
            etPhone.setText(contact.phone)
            etEmail.setText(contact.email)
            etRole.setText(contact.role)
            etNotes.setText(contact.notes)
            cbFavorite.isChecked = contact.isFavorite

            // Setup location
            if (contact.address.isNotBlank()) {
                tvLocation.text = contact.address
                address = contact.address
                latitude = contact.latitude
                longitude = contact.longitude
            } else {
                tvLocation.text = getString(R.string.no_location_selected)
                address = null
                latitude = null
                longitude = null
            }

            // Thay đổi text button
            btnSave.text = "Cập nhật"
        }
    }

    private fun setupAddMode(dialogBinding: DialogAddContactBinding) {
        with(dialogBinding) {
            // Clear tất cả fields
            etFullName.setText("")
            etPhone.setText("")
            etEmail.setText("")
            etRole.setText("")
            etNotes.setText("")
            cbFavorite.isChecked = false
            tvLocation.text = getString(R.string.no_location_selected)

            // Reset location data
            address = null
            latitude = null
            longitude = null

            // Set text button
            btnSave.text = "Thêm"
        }
    }

    private fun createContact(dialogBinding: DialogAddContactBinding) {
        val name = dialogBinding.etFullName.text.toString()
        val phone = dialogBinding.etPhone.text.toString()
        val email = dialogBinding.etEmail.text.toString()
        val role = dialogBinding.etRole.text.toString()
        val notes = dialogBinding.etNotes.text.toString()
        val isFavorite = dialogBinding.cbFavorite.isChecked

        // Validate
        if (!validateContactInput(dialogBinding, name, phone, email)) {
            return
        }

        val locationText = dialogBinding.tvLocation.text.toString()
        val finalAddress = if (locationText != getString(R.string.no_location_selected)) {
            locationText
        } else ""

        val contact = Contact(
            userId = currentUserId,
            name = name,
            address = finalAddress,
            phone = phone,
            email = email,
            role = role,
            notes = notes,
            latitude = latitude,
            longitude = longitude,
            isFavorite = isFavorite
        )

        contactViewModel.createContact(contact)
//        addContactDialog?.dismiss()
    }

    private fun updateContact(dialogBinding: DialogAddContactBinding, originalContact: Contact) {
        val name = dialogBinding.etFullName.text.toString()
        val phone = dialogBinding.etPhone.text.toString()
        val email = dialogBinding.etEmail.text.toString()
        val role = dialogBinding.etRole.text.toString()
        val notes = dialogBinding.etNotes.text.toString()
        val isFavorite = dialogBinding.cbFavorite.isChecked

        // Validate
        if (!validateContactInput(dialogBinding, name, phone, email)) {
            dialogBinding.btnSave.isEnabled = true
            return
        }

        val locationText = dialogBinding.tvLocation.text.toString()
        val finalAddress = if (locationText != getString(R.string.no_location_selected)) {
            locationText
        } else ""

        val updatedContact = originalContact.copy(
            name = name,
            phone = phone,
            email = email,
            role = role,
            notes = notes,
            address = finalAddress,
            latitude = latitude,
            longitude = longitude,
            isFavorite = isFavorite
        )

        contactViewModel.updateContact(updatedContact)
//        addContactDialog?.dismiss()
    }

    private fun validateContactInput(
        dialogBinding: DialogAddContactBinding,
        name: String,
        phone: String,
        email: String
    ): Boolean {
        // Clear previous errors
        with(dialogBinding) {
            tilName.error = null
            tilPhone.error = null
            tilEmail.error = null
            tvLocation.error = null
        }

        // Validate fields
        val nameResult = ValidationUtils.validateName(name)
        val phoneResult = ValidationUtils.validatePhone(phone)
        val emailResult = ValidationUtils.validateEmail(email)

        val locationText = dialogBinding.tvLocation.text.toString()
        val addressResult = if (locationText.isNotBlank() &&
            locationText != getString(R.string.no_location_selected)
        ) {
            ValidationUtils.validateAddress(locationText)
        } else {
            ValidationUtils.validateAddress("")
        }

        var hasError = false
        if (!nameResult.isValid) {
            dialogBinding.tilName.error = nameResult.errorMessage
            hasError = true
        }
        if (!phoneResult.isValid) {
            dialogBinding.tilPhone.error = phoneResult.errorMessage
            hasError = true
        }
        if (!emailResult.isValid) {
            dialogBinding.tilEmail.error = emailResult.errorMessage
            hasError = true
        }
        if (!addressResult.isValid) {
            dialogBinding.tvLocation.error = addressResult.errorMessage
            hasError = true
        }

        return !hasError
    }


    private fun refreshData() {
        if (isSearchMode) {
            currentSearchQuery?.let { query ->
                performSearch(query)
            }
        } else {
            loadContacts()
        }
    }

    //update contact
    private fun updateContactsList(contacts: List<Contact>) {
        contactsAdapter.updateContacts(contacts)
        updateUIState()
    }

    //hien thi loading
    private fun showLoading() {
        binding.swipeRefreshContacts.isRefreshing = true
    }

    //an loading
    private fun hideLoading() {
        binding.swipeRefreshContacts.isRefreshing = false
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

    // xử lý action favorite
    private fun handleFavoriteAction() {
        val selectedContacts = contactsAdapter.getSelectedContacts()
        selectedContacts.forEach { contact ->
            contactViewModel.toggleFavorite(contact.id)
        }
        showMessage("Đã cập nhật trạng thái yêu thích cho ${selectedContacts.size} liên hệ")
        closeSelectionMode()
    }

    // xử lý action share
    private fun handleShareAction() {
        val selectedContacts = contactsAdapter.getSelectedContacts()
        if (selectedContacts.isNotEmpty()) {
            val shareText = buildString {
                append("Thông tin liên hệ:\n\n")
                selectedContacts.forEach { contact ->
                    append("Tên: ${contact.name}\n")
                    append("Điện thoại: ${contact.phone}\n")
                    if (contact.email.isNotBlank()) {
                        append("Email: ${contact.email}\n")
                    }
                    if (contact.role.isNotBlank()) {
                        append("Vai trò: ${contact.role}\n")
                    }
                    append("\n")
                }
            }

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }

            startActivity(Intent.createChooser(shareIntent, "Chia sẻ liên hệ"))
        }
        closeSelectionMode()
    }

    // xử lý action delete
    private fun handleDeleteAction() {
        val selectedContacts = contactsAdapter.getSelectedContacts()

        if (selectedContacts.isEmpty()) {
            closeSelectionMode()
            return
        }

        val backupContacts = selectedContacts.toList()
        contactsAdapter.removeContacts(selectedContacts)

        val snackbar = Snackbar.make(
            binding.root,
            "Đã xóa ${selectedContacts.size} liên hệ",
            Snackbar.LENGTH_LONG
        )
        var isUndoClicked = false

        snackbar.setAction("Hoàn tác") {
            isUndoClicked = true
            contactsAdapter.restoreContacts(backupContacts)
            showMessage("Đã hoàn tác xóa liên hệ")
        }

        snackbar.addCallback(
            object : Snackbar.Callback(){
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    if(!isUndoClicked && event != DISMISS_EVENT_ACTION){
                        backupContacts.forEach {
                            contact ->
                            contactViewModel.deleteContact(contact.id)
                        }
                    }
                }
            }
        )

        snackbar.show()
        closeSelectionMode()
    }


    private fun showSelectionPopMenu(view: View) {
        val popMenu = PopupMenu(requireContext(), view)
        popMenu.menuInflater.inflate(R.menu.menu_selection_more, popMenu.menu)

        val selectAllItem = popMenu.menu.findItem(R.id.action_select_all)
        val deselectAllItem = popMenu.menu.findItem(R.id.action_deselect_all)

        val selectedCount = contactsAdapter.getSelectedCount()
        val totalCount = contactsAdapter.itemCount

        selectAllItem.isVisible = selectedCount < totalCount
        deselectAllItem.isVisible = selectedCount > 0

        popMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_select_all -> {
                    contactsAdapter.selectAll()
                    true
                }

                R.id.action_deselect_all -> {
                    contactsAdapter.clearSelection()
                    true
                }

                else -> false
            }
        }

        popMenu.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissDialog()
        _binding = null
    }

    override fun onNavigateToContact() {
        showContactDialog()
        showMessage("Vui lòng thêm liên hệ để tạo cuộc hẹn")
    }
}


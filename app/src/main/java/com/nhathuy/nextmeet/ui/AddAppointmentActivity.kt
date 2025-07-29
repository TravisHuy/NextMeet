package com.nhathuy.nextmeet.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.adapter.ColorPickerAdapter
import com.nhathuy.nextmeet.databinding.ActivityAddAppointmentBinding
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.AppointmentStatus
import com.nhathuy.nextmeet.model.ContactNameId
import com.nhathuy.nextmeet.resource.AppointmentUiState
import com.nhathuy.nextmeet.resource.ContactUiState
import com.nhathuy.nextmeet.resource.NotificationUiState
import com.nhathuy.nextmeet.viewmodel.AppointmentPlusViewModel
import com.nhathuy.nextmeet.viewmodel.ContactViewModel
import com.nhathuy.nextmeet.viewmodel.NotificationViewModel
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
class AddAppointmentActivity : BaseActivity() {

    private lateinit var binding: ActivityAddAppointmentBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var contactViewModel: ContactViewModel
    private lateinit var appointmentViewModel: AppointmentPlusViewModel
    private lateinit var notificationViewModel : NotificationViewModel

    // Data variables
    private var currentUserId: Int = 0
    private var currentContactId: Int = 0
    private var location: String? = null
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var reminderTime: Long? = null
    private var selectedColorName: String = "color_white"

    // Edit mode variables
    private var isEditMode: Boolean = false
    private var appointmentId: Int = 0

    // Contact data
    private val contactMap = mutableMapOf<String, ContactNameId>()

    // Color picker
    private lateinit var colorAdapter: ColorPickerAdapter

    // Geocoding
    private var geocodingJob: Job? = null
    private var geocodingCache = mutableMapOf<String, Pair<Double?, Double?>>()

    // has show contact
    private var hasShownNoContactDialog = false

    // Notification tracking
    private var isNotificationScheduled = false
    private var notificationPermissionChecked = false

    //location fromMap
    private var isLocationFromMap = false

    // Activity Result Launcher
    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                val selectedAddress = data.getStringExtra(GoogleMapActivity.EXTRA_SELECTED_ADDRESS)
                val selectedLat = data.getDoubleExtra(GoogleMapActivity.EXTRA_SELECTED_LAT, 0.0)
                val selectedLng = data.getDoubleExtra(GoogleMapActivity.EXTRA_SELECTED_LNG, 0.0)

                isLocationFromMap = true

                location = selectedAddress
                latitude = selectedLat
                longitude = selectedLng

                if (!selectedAddress.isNullOrEmpty()) {
                    binding.etAppointmentLocation.setText(location)
                    binding.tilAppointmentLocation.helperText = getString(R.string.location_found_from_map)
                    Log.d(
                        "AddAppointmentActivity",
                        "Selected from map: $selectedAddress at ($selectedLat, $selectedLng)"
                    )
                } else {
                    binding.etAppointmentLocation.setText(getString(R.string.no_location_selected))
                    binding.tilAppointmentLocation.helperText = getString(R.string.no_locations_selected)
                    location = ""
                    latitude = null
                    longitude = null
                }
            }
        }
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

        val colorNamesToRes = colorSourceNames.entries.associate { (res, name) -> name to res }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddAppointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            enableEdgeToEdge()
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        initializeData()
        initializeViewModels()
        loadInitialData()
        setupUI()
        observeData()
        checkNotificationPermissions()
    }

    private fun initializeData() {
        currentUserId = intent.getIntExtra("current_user_id", 0)
        isEditMode = intent.getBooleanExtra("is_edit_mode", false)

        if (isEditMode) {
            appointmentId = intent.getIntExtra("appointment_id", 0)
            setupEditMode()
        }
    }

    private fun setupEditMode() {
        Log.d("AddAppointment", "Setting up edit mode")
        Log.d("AddAppointment", "Contact ID: $currentContactId")

        // Load data từ intent
        binding.etAppointmentTitle.setText(intent.getStringExtra("appointment_title") ?: "")
        binding.etNotes.setText(intent.getStringExtra("appointment_description") ?: "")

        location = intent.getStringExtra("appointment_location")
        binding.etAppointmentLocation.setText(location ?: "")

        latitude = intent.getDoubleExtra("appointment_latitude", 0.0).takeIf { it != 0.0 }
        longitude = intent.getDoubleExtra("appointment_longitude", 0.0).takeIf { it != 0.0 }

        updateLocationHelperText()

        reminderTime = intent.getLongExtra("appointment_start_time", 0L).takeIf { it != 0L }
        currentContactId = intent.getIntExtra("appointment_contact_id", 0)
        selectedColorName = intent.getStringExtra("appointment_color") ?: "color_white"
        binding.cbFavorite.isChecked = intent.getBooleanExtra("appointment_is_pinned", false)

        Log.d("AddAppointment", "Edit mode contact ID: $currentContactId")

        // Update UI for edit mode
        supportActionBar?.title = getString(R.string.edit_appointment)

        // Update reminder display if available
        reminderTime?.let { updateReminderDisplay() }
    }

    private fun updateLocationHelperText() {
        when {
            // Có cả location và coordinates
            !location.isNullOrEmpty() && latitude != null && longitude != null -> {
                binding.tilAppointmentLocation.helperText = getString(R.string.location_found)
            }
            // Có location nhưng không có coordinates
            !location.isNullOrEmpty() && (latitude == null || longitude == null) -> {
                binding.tilAppointmentLocation.helperText = getString(R.string.location_not_exactly)
            }
            // Không có location
            else -> {
                binding.tilAppointmentLocation.helperText = getString(R.string.location_hint_default)
            }
        }
    }


    private fun initializeViewModels() {
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        contactViewModel = ViewModelProvider(this)[ContactViewModel::class.java]
        appointmentViewModel = ViewModelProvider(this)[AppointmentPlusViewModel::class.java]
        notificationViewModel = ViewModelProvider(this)[NotificationViewModel::class.java]
    }

    private fun checkNotificationPermissions() {
        if (!notificationPermissionChecked) {
            notificationPermissionChecked = true
//            val hasPermissions = appointmentViewModel.checkNotificationPermissions()

//            if (!hasPermissions) {
//                showNotificationPermissionDialog()
//            }
        }
    }

    private fun showNotificationPermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Quyền thông báo")
            .setMessage("Ứng dụng cần quyền thông báo để nhắc nhở bạn về cuộc hẹn sắp tới. Bạn có muốn bật quyền này không?")
            .setPositiveButton("Bật quyền") { dialog, _ ->
                dialog.dismiss()
                // Direct user to settings
                showMessage("Vui lòng bật quyền thông báo trong Cài đặt > Ứng dụng > NextMeet > Thông báo")
            }
            .setNegativeButton("Bỏ qua") { dialog, _ ->
                dialog.dismiss()
                showMessage("Lưu ý: Sẽ không có thông báo nhắc nhở cho cuộc hẹn")
            }
            .show()
    }

    private fun setupUI() {
        setupToolbar()
        setupContactDropdown()
        setupColorPicker()
        setupLocationInput()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.titleAddAppointment.text = if (isEditMode) getString(R.string.edit_appointment) else getString(R.string.add_appointment)
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupContactDropdown() {
        binding.autoContactName.apply {
            inputType = InputType.TYPE_NULL
            isFocusable = false
            isCursorVisible = false
            keyListener = null
        }

        lifecycleScope.launch {
            contactViewModel.contactNamesAndIds.collect { contacts ->
                handleContactsData(contacts)
            }
        }
    }

    private fun handleContactsData(contacts: List<ContactNameId>) {
        contactMap.clear()
        contacts.forEach { contact ->
            contactMap[contact.name] = contact
        }

        when {
            contacts.isNotEmpty() -> {
                // Có contacts - setup bình thường
                hasShownNoContactDialog = false
                setupContactAdapter(contacts)
                binding.tilContactName.helperText = null
            }

            isEditMode -> {
                // Edit mode nhưng không có contacts - báo lỗi nhưng không hiển thị dialog
                binding.tilContactName.helperText = getString(R.string.no_contacts_found_edit)
                hasShownNoContactDialog = true
            }

            else -> {
                // Add mode và không có contacts - hiển thị dialog
                binding.tilContactName.helperText = getString(R.string.no_contacts_found_add)
                if (!hasShownNoContactDialog) {
                    // Thêm delay để đảm bảo đã load xong
                    lifecycleScope.launch {
                        delay(1000) // Wait 1 second

                        // Kiểm tra lại một lần nữa
                        if (contactMap.isEmpty() && !isEditMode && !hasShownNoContactDialog) {
                            handleNoContactsAvailable()
                        }
                    }
                }
            }
        }
    }

    private fun setupContactAdapter(contacts: List<ContactNameId>) {
        val contactNames = mutableListOf(getString(R.string.select_contact_hint)).apply {
            addAll(contacts.map { it.name })
        }.toTypedArray()

        val adapter = ArrayAdapter(
            this@AddAppointmentActivity,
            android.R.layout.simple_dropdown_item_1line,
            contactNames
        )
        binding.autoContactName.setAdapter(adapter)

        // Set selected contact for edit mode
        if (isEditMode && currentContactId != 0) {
            val selectedContact = contacts.find { it.id == currentContactId }
            selectedContact?.let {
                val contactPosition = contacts.indexOfFirst { contact -> contact.id == currentContactId }
                if (contactPosition != -1) {
                    val displayPosition = contactPosition + 1
                    binding.autoContactName.setText(contactNames[displayPosition], false)
                }
            }
        } else {
            binding.autoContactName.setText(contactNames[0], false)
            if (!isEditMode) {
                currentContactId = 0
            }
        }

        binding.autoContactName.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                currentContactId = 0
                binding.autoContactName.setText(contactNames[0], false)
            } else {
                val selectedContact = contacts[position - 1]
                currentContactId = selectedContact.id
                binding.autoContactName.setText(selectedContact.name, false)
                Log.d("ContactDropdown", "Selected contact: ${selectedContact.name} with ID: ${selectedContact.id}")
            }
        }
    }

    private fun handleNoContactsAvailable() {
        hasShownNoContactDialog = true

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.no_contact_title))
            .setMessage(getString(R.string.no_contact_message))
            .setPositiveButton(getString(R.string.quick_add_contact)) { dialog, _ ->
                dialog.dismiss()
                showQuickAddContactDialog()
            }
            .setNeutralButton(getString(R.string.go_to_contacts)) { dialog, _ ->
                dialog.dismiss()
                navigateToContactFragment()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showQuickAddContactDialog() {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_quick_add_contact, null)

        val tilName = dialogView.findViewById<TextInputLayout>(R.id.til_name)
        val tilPhone = dialogView.findViewById<TextInputLayout>(R.id.til_phone)
        val tilRole = dialogView.findViewById<TextInputLayout>(R.id.til_role)

        val etContactName = dialogView.findViewById<TextInputEditText>(R.id.et_contact_name)
        val etContactPhone = dialogView.findViewById<TextInputEditText>(R.id.et_contact_phone)
        val etContactRole = dialogView.findViewById<TextInputEditText>(R.id.et_contact_role)

        val alertDialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.quick_add_contact))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.create), null) // Set null để override sau
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.setOnShowListener { dialog ->
            val button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val name = etContactName.text?.toString()?.trim() ?: ""
                val phone = etContactPhone.text?.toString()?.trim() ?: ""
                val role = etContactRole.text?.toString()?.trim() ?: ""

                // Clear previous errors
                tilName.error = null
                tilPhone.error = null
                tilRole.error = null

                if (validateQuickContactInput(name, phone, role, tilName, tilPhone, tilRole)) {
                    button.isEnabled = false
                    createQuickContact(name, phone, role) { success ->
                        if (success) {
                            dialog.dismiss()
                        } else {
                            button.isEnabled = true
                        }
                    }
                }
            }
        }

        alertDialog.show()

        // Auto focus và hiển thị keyboard
        etContactName.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etContactName, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun validateQuickContactInput(
        name: String,
        phone: String,
        role: String,
        tilName: TextInputLayout,
        tilPhone: TextInputLayout,
        tilRole: TextInputLayout
    ): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            tilName.error = getString(R.string.error_name_empty)
            isValid = false
        } else if (name.length > 25) {
            tilName.error = getString(R.string.error_name_too_long)
            isValid = false
        }

        if (phone.isEmpty()) {
            tilPhone.error = getString(R.string.error_phone_empty)
            isValid = false
        } else if (phone.length != 10) {
            tilPhone.error = getString(R.string.error_phone_length)
            isValid = false
        } else if (!phone.matches("^[0-9]+$".toRegex())) {
            tilPhone.error = getString(R.string.error_phone_invalid)
            isValid = false
        }

        if (role.isEmpty()) {
            tilRole.error = getString(R.string.error_role_empty)
            isValid = false
        }

        return isValid
    }

    private fun createQuickContact(
        name: String,
        phone: String,
        role: String,
        onComplete: (Boolean) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val contactId = contactViewModel.quickAddContact(currentUserId, name, phone, role)

                if (contactId != null) {
                    showMessage(getString(R.string.contact_created,name))

                    // Wait for contact list to refresh
                    delay(500)

                    // Auto select the new contact
                    autoSelectNewContact(name, contactId.toInt())

                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                showMessage(getString(R.string.contact_creation_error, e.message ?: ""))
                onComplete(false)
            }
        }
    }

    private fun autoSelectNewContact(contactName: String, contactId: Int) {
        // Update current contact ID
        currentContactId = contactId

        // Update UI
        lifecycleScope.launch {
            // Wait for adapter to update
            delay(200)

            val contact = contactMap[contactName]
            if (contact != null) {
                binding.autoContactName.setText(contactName, false)
                showMessage(getString(R.string.contact_selected,contactName))

                // Clear helper text warning
                binding.tilContactName.helperText = null
            }
        }
    }

    private fun navigateToContactFragment() {
        val intent = Intent(this, SolutionActivity::class.java).apply {
            putExtra("navigate_to", "contact")
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    // cài đặt màu ban đầu
    private fun setupColorPicker() {
        colorAdapter = ColorPickerAdapter(listColor, colorSourceNames) { colorResId, colorName ->
            val color = ContextCompat.getColor(this, colorResId)
            binding.root.setBackgroundColor(color)
            selectedColorName = colorName
        }

        binding.rvColorPicker.apply {
            adapter = colorAdapter
            layoutManager = LinearLayoutManager(this@AddAppointmentActivity, LinearLayoutManager.HORIZONTAL, false)
        }

        // Đặt màu ban đầu cho chế độ chỉnh sửa
        if (isEditMode) {
            binding.rvColorPicker.post {
                colorAdapter.setSelectedColors(selectedColorName)

                // Đặt lại màu nền cho layout
                val colorResId = colorNamesToRes[selectedColorName] ?: R.color.color_white
                val color = ContextCompat.getColor(this@AddAppointmentActivity, colorResId)
                binding.root.setBackgroundColor(color)
            }
        }
    }

    private fun setupLocationInput() {
        updateLocationHelperText()
        binding.tilAppointmentLocation.apply {
            setEndIconDrawable(R.drawable.ic_geo)
            setEndIconContentDescription(getString(R.string.map_picker_description))

            setEndIconOnClickListener {
                val intent = Intent(this@AddAppointmentActivity, GoogleMapActivity::class.java).apply {
                    if (latitude != null && longitude != null) {
                        putExtra(GoogleMapActivity.EXTRA_SELECTED_LAT, latitude)
                        putExtra(GoogleMapActivity.EXTRA_SELECTED_LNG, longitude)
                        putExtra(GoogleMapActivity.EXTRA_SELECTED_ADDRESS, location)
                    }
                }
                mapPickerLauncher.launch(intent)
            }
        }

        binding.etAppointmentLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val manualLocation = s?.toString()?.trim()


                if (isLocationFromMap) {
                    isLocationFromMap = false
                    return
                }

                geocodingJob?.cancel()
                    if (!manualLocation.isNullOrEmpty() && manualLocation.length >= 3) {
                        location = manualLocation

                        geocodingJob = lifecycleScope.launch {
                            delay(800)
                            binding.tilAppointmentLocation.helperText = getString(R.string.location_searching)

                            geocodeAddress(manualLocation) { lat, lng ->
                                if (lat != null && lng != null) {
                                    latitude = lat
                                    longitude = lng
                                    binding.tilAppointmentLocation.helperText = getString(R.string.location_found)
//                                        "📍 Tọa độ: ${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}"
                                    Log.d("Geocoding", "Found coordinates: $lat, $lng for address: $manualLocation")
                                } else {
                                    latitude = null
                                    longitude = null
                                    binding.tilAppointmentLocation.helperText =
                                        getString(R.string.locations_not_found)
                                    Log.d("Geocoding", "No coordinates found for address: $manualLocation")
                                }
                            }
                        }
                    } else if (manualLocation.isNullOrEmpty()) {
                        location = null
                        latitude = null
                        longitude = null
                        binding.tilAppointmentLocation.helperText =
                            getString(R.string.location_hint_default)
                        geocodingJob?.cancel()
                    } else {
                        location = manualLocation
                        latitude = null
                        longitude = null
                        binding.tilAppointmentLocation.helperText = getString(R.string.location_enter_more)
                    }
                }
        })
    }

    private fun setupClickListeners() {
        binding.layoutReminder.setOnClickListener {
            showDateTimePicker()
        }


        binding.btnSave.setOnClickListener {
            binding.btnSave.isEnabled = false
            if (isEditMode) {
                updateAppointment()
            } else {
                saveAppointment()
            }
        }
    }

    private fun observeData() {
        // Existing appointment observation
        lifecycleScope.launch {
            appointmentViewModel.appointmentUiState.collect { state ->
                handleAppointmentUiState(state)
            }
        }

        // Add contact observation for quick add
        lifecycleScope.launch {
            contactViewModel.contactUiState.collect { state ->
                handleContactUiState(state)
            }
        }

        // Add notification observation
        lifecycleScope.launch {
            notificationViewModel.notificationUiState.collect { state ->
                handleNotificationUiState(state)
            }
        }
    }

    private fun handleAppointmentUiState(state: AppointmentUiState) {
        when (state) {
            is AppointmentUiState.Loading -> showLoading()

            is AppointmentUiState.AppointmentCreated -> {
                hideLoading()
                setResultAndFinish(state.message)
            }

            is AppointmentUiState.AppointmentUpdated -> {
                hideLoading()
                setResultAndFinish(state.message)
            }

            is AppointmentUiState.Error -> {
                hideLoading()
                showMessage(state.message)
                binding.btnSave.isEnabled = true
                appointmentViewModel.resetUiState()
            }

            else -> {}
        }
    }

    private fun handleContactUiState(state: ContactUiState) {
        when (state) {
            is ContactUiState.ContactCreated -> {
                // Contact created successfully - handled in createQuickContact
            }
            is ContactUiState.Error -> {
                showMessage(state.message)
            }
            else -> {}
        }
    }
    private fun handleNotificationUiState(state: NotificationUiState) {
        when (state) {
            is NotificationUiState.NotificationScheduled -> {
                isNotificationScheduled = true
                Log.d("AddAppointment", "Notification scheduled: ${state.message}")
            }
            is NotificationUiState.Error -> {
                isNotificationScheduled = false
                Log.e("AddAppointment", "Notification error: ${state.message}")
                // Don't show error to user as notification is secondary feature
            }
            else -> {}
        }
    }

    private fun loadInitialData() {
        if (currentUserId != 0) {
            // Đặt flag trước khi load data để tránh hiển thị dialog sai
            if (isEditMode) {
                hasShownNoContactDialog = true // Ngăn dialog hiển thị trong edit mode
            }

            contactViewModel.getContactNamesAndIds(currentUserId)
            notificationViewModel.setCurrentUserId(currentUserId)
        }
    }

    private fun saveAppointment() {
        val title = binding.etAppointmentTitle.text?.toString()?.trim() ?: ""
        val notes = binding.etNotes.text?.toString()?.trim() ?: ""
        val appointmentLocation = location ?: ""
        val isPinned = binding.cbFavorite.isChecked

        if (!validateAppointmentInput(title)) {
            binding.btnSave.isEnabled = true
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
            latitude = latitude ?: 0.0,
            longitude = longitude ?: 0.0,
            status = AppointmentStatus.SCHEDULED,
            color = selectedColorName,
            travelTimeMinutes = 0,
            isPinned = isPinned
        )

        if(reminderTime != null) {
            appointmentViewModel.createAppointment(appointment,true)
        }
        else{
            appointmentViewModel.createAppointment(appointment,false)
        }
    }

    private fun updateAppointment() {
        val title = binding.etAppointmentTitle.text?.toString()?.trim() ?: ""
        val notes = binding.etNotes.text?.toString()?.trim() ?: ""
        val appointmentLocation = location ?: ""
        val isPinned = binding.cbFavorite.isChecked

        if (!validateAppointmentInput(title)) {
            binding.btnSave.isEnabled = true
            return
        }

        lifecycleScope.launch {
            try {
                val originalAppointment = getOriginalAppointment()
                if (originalAppointment == null) {
                    showMessage(getString(R.string.error_find_appointment))
                    binding.btnSave.isEnabled = true
                    return@launch
                }

                val endTime = reminderTime!! + (60 * 60 * 1000)

                // Giữ nguyên status gốc, chỉ update các field cần thiết
                val updatedAppointment = originalAppointment.copy(
                    title = title,
                    description = notes,
                    startDateTime = reminderTime!!,
                    endDateTime = endTime,
                    location = appointmentLocation,
                    latitude = latitude ?: originalAppointment.latitude,
                    longitude = longitude ?: originalAppointment.longitude,
                    color = selectedColorName,
                    isPinned = isPinned,
                    updateAt = System.currentTimeMillis()
                )

                appointmentViewModel.updateAppointment(updatedAppointment, reminderTime != null)

            } catch (e: Exception) {
                showMessage(getString(R.string.msg_error_updated, e.message))
                binding.btnSave.isEnabled = true
            }
        }
    }
    private suspend fun getOriginalAppointment(): AppointmentPlus? {
        return try {
            val result = appointmentViewModel.getAppointmentByIdSync(appointmentId)
            if (result.isSuccess) {
                result.getOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun validateAppointmentInput(title: String): Boolean {
        binding.tilAppointmentTitle.error = null

        if (title.isEmpty()) {
            binding.tilAppointmentTitle.error = getString(R.string.error_title_empty)
            return false
        }

        if (currentContactId == 0) {
            Toast.makeText(this, getString(R.string.error_contact_required), Toast.LENGTH_SHORT).show()
            return false
        }

        if (reminderTime == null) {
            Toast.makeText(this, getString(R.string.error_reminder_required), Toast.LENGTH_SHORT).show()
            return false
        }

        val currentTime = System.currentTimeMillis()
        if (reminderTime!! < currentTime) {
            Toast.makeText(this, getString(R.string.error_reminder_in_past), Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }


    private fun showDateTimePicker() {
        val constraintBuilder = CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now())

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.select_date))
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(constraintBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedDate
            showTimePicker(calendar)
        }

        datePicker.show(supportFragmentManager, "date_picker")
    }

    private fun showTimePicker(calendar: Calendar) {
        val now = Calendar.getInstance()

        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(calendar.get(Calendar.MINUTE))
            .setTitleText(getString(R.string.select_time))
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val selectedHour = timePicker.hour
            val selectedMinute = timePicker.minute

            if (isToday(calendar)) {
                if (selectedHour < now.get(Calendar.HOUR_OF_DAY) ||
                    (selectedHour == now.get(Calendar.HOUR_OF_DAY)) && selectedMinute <= now.get(Calendar.MINUTE)
                ) {
                    Toast.makeText(this,getString(R.string.msg_select_future_time), Toast.LENGTH_SHORT).show()
                    return@addOnPositiveButtonClickListener
                }
            }

            calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            calendar.set(Calendar.MINUTE, timePicker.minute)
            calendar.set(Calendar.SECOND, 0)

            reminderTime = calendar.timeInMillis
            updateReminderDisplay()
        }

        timePicker.show(supportFragmentManager, "time_picker")
    }

    private fun isToday(calendar: Calendar): Boolean {
        val today = Calendar.getInstance()
        return today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
    }

    private fun updateReminderDisplay() {
        val formatter = SimpleDateFormat("dd MM yyyy, HH:mm", Locale.getDefault())
        val formattedDate = formatter.format(Date(reminderTime!!))
        binding.tvReminderTime.text = formattedDate
    }

    private fun geocodeAddress(address: String, callback: (Double?, Double?) -> Unit) {
        if (address.isBlank() || address.length < 3) {
            callback(null, null)
            return
        }

        val cachedResult = geocodingCache[address]
        if (cachedResult != null) {
            callback(cachedResult.first, cachedResult.second)
            return
        }

        try {
            val geocoder = Geocoder(this, Locale.getDefault())

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocationName(address, 1) { addresses ->
                    handleGeocodingResult(address, addresses, callback)
                }
            } else {
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

    private fun setResultAndFinish(message: String) {
        val resultIntent = Intent().apply {
            putExtra("message", message)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun showLoading() {
        binding.btnSave.isEnabled = false
        binding.loadingOverlay.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.btnSave.isEnabled = true
        binding.loadingOverlay.visibility = View.GONE
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }
}
package com.nhathuy.nextmeet.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Geocoder
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
class AddAppointmentActivity : AppCompatActivity() {

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

    // Activity Result Launcher
    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                location = data.getStringExtra(GoogleMapActivity.EXTRA_SELECTED_ADDRESS)
                latitude = data.getDoubleExtra(GoogleMapActivity.EXTRA_SELECTED_LAT, 0.0)
                longitude = data.getDoubleExtra(GoogleMapActivity.EXTRA_SELECTED_LNG, 0.0)

                if (!location.isNullOrEmpty()) {
                    binding.etAppointmentLocation.setText(location)
                } else {
                    binding.etAppointmentLocation.setText(getString(R.string.no_location_selected))
                    location = ""
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddAppointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        reminderTime = intent.getLongExtra("appointment_start_time", 0L).takeIf { it != 0L }
        currentContactId = intent.getIntExtra("appointment_contact_id", 0)
        selectedColorName = intent.getStringExtra("appointment_color") ?: "color_white"
        binding.cbFavorite.isChecked = intent.getBooleanExtra("appointment_is_pinned", false)

        Log.d("AddAppointment", "Edit mode contact ID: $currentContactId")

        // Update UI for edit mode
        binding.btnSave.text = "Cập nhật"
        supportActionBar?.title = "Chỉnh sửa cuộc hẹn"

        // Update reminder display if available
        reminderTime?.let { updateReminderDisplay() }
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
                binding.tilContactName.helperText = "⚠️ Không tìm thấy liên hệ - dữ liệu có thể đã bị xóa"
                hasShownNoContactDialog = true
            }

            else -> {
                // Add mode và không có contacts - hiển thị dialog
                binding.tilContactName.helperText = "⚠️ Không tìm thấy liên hệ - vui lòng thêm ít nhất 1 liên hệ để thêm cuộc hẹn."
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
        val contactNames = mutableListOf("-- Chọn liên hệ --").apply {
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
            .setTitle("Không có liên hệ")
            .setMessage("Bạn cần tạo ít nhất một liên hệ trước khi có thể tạo cuộc hẹn.")
            .setPositiveButton("Tạo liên hệ nhanh") { dialog, _ ->
                dialog.dismiss()
                showQuickAddContactDialog()
            }
            .setNeutralButton("Đến trang Liên hệ") { dialog, _ ->
                dialog.dismiss()
                navigateToContactFragment()
            }
            .setNegativeButton("Hủy") { dialog, _ ->
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
            .setTitle("Tạo liên hệ nhanh")
            .setView(dialogView)
            .setPositiveButton("Tạo", null) // Set null để override sau
            .setNegativeButton("Hủy") { dialog, _ ->
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
            tilName.error = "Tên không được để trống"
            isValid = false
        } else if (name.length > 25) {
            tilName.error = "Tên không được quá 25 ký tự"
            isValid = false
        }

        if (phone.isEmpty()) {
            tilPhone.error = "Số điện thoại không được để trống"
            isValid = false
        } else if (phone.length != 10) {
            tilPhone.error = "Số điện thoại phải có 10 chữ số"
            isValid = false
        } else if (!phone.matches("^[0-9]+$".toRegex())) {
            tilPhone.error = "Số điện thoại chỉ được chứa chữ số"
            isValid = false
        }

        if (role.isEmpty()) {
            tilRole.error = "Vai trò không được để trống"
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
                    showMessage("Đã tạo liên hệ: $name")

                    // Wait for contact list to refresh
                    delay(500)

                    // Auto select the new contact
                    autoSelectNewContact(name, contactId.toInt())

                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                showMessage("Lỗi tạo liên hệ: ${e.message}")
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
                showMessage("Đã chọn liên hệ: $contactName")

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
            binding.layoutAddAppointment.setBackgroundColor(color)
            selectedColorName = colorName
        }

        binding.rvColorPicker.apply {
            adapter = colorAdapter
            layoutManager = LinearLayoutManager(this@AddAppointmentActivity, LinearLayoutManager.HORIZONTAL, false)
        }

        // Đặt màu ban đầu cho chế độ chỉnh sửa
        if (isEditMode) {
            colorAdapter.setSelectedColor(selectedColorName)
        }
    }

    private fun setupLocationInput() {
        binding.tilAppointmentLocation.apply {
            helperText = "💡 Nhập địa chỉ để tự động tìm tọa độ, hoặc nhấn 📍 để chọn chính xác"
            setEndIconDrawable(R.drawable.ic_geo)
            setEndIconContentDescription("Chọn vị trí trên bản đồ")

            setEndIconOnClickListener {
                val intent = Intent(this@AddAppointmentActivity, GoogleMapActivity::class.java).apply {
                    if (latitude != null && longitude != null) {
                        putExtra("current_lat", latitude)
                        putExtra("current_lng", longitude)
                        putExtra("current_address", location)
                    }
                }
                mapPickerLauncher.launch(intent)
            }
        }

        var isLocationFromMap = false

        binding.etAppointmentLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val manualLocation = s?.toString()?.trim()

                if (!isLocationFromMap) {
                    geocodingJob?.cancel()

                    if (!manualLocation.isNullOrEmpty() && manualLocation.length >= 3) {
                        location = manualLocation

                        geocodingJob = lifecycleScope.launch {
                            delay(800)
                            binding.tilAppointmentLocation.helperText = "🔄 Đang tìm tọa độ..."

                            geocodeAddress(manualLocation) { lat, lng ->
                                if (lat != null && lng != null) {
                                    latitude = lat
                                    longitude = lng
                                    binding.tilAppointmentLocation.helperText =
                                        "📍 Tọa độ: ${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}"
                                    Log.d("Geocoding", "Found coordinates: $lat, $lng for address: $manualLocation")
                                } else {
                                    latitude = null
                                    longitude = null
                                    binding.tilAppointmentLocation.helperText =
                                        "⚠️ Không tìm thấy tọa độ - có thể chọn trên bản đồ để chính xác hơn"
                                    Log.d("Geocoding", "No coordinates found for address: $manualLocation")
                                }
                            }
                        }
                    } else if (manualLocation.isNullOrEmpty()) {
                        location = null
                        latitude = null
                        longitude = null
                        binding.tilAppointmentLocation.helperText =
                            "💡 Nhập địa chỉ để tự động tìm tọa độ, hoặc nhấn 📍 để chọn chính xác"
                        geocodingJob?.cancel()
                    } else {
                        location = manualLocation
                        latitude = null
                        longitude = null
                        binding.tilAppointmentLocation.helperText = "📝 Nhập thêm để tìm tọa độ..."
                    }
                }

                if (isLocationFromMap) {
                    isLocationFromMap = false
                }
            }
        })
    }

    private fun setupClickListeners() {
        binding.layoutReminder.setOnClickListener {
            showDateTimePicker()
        }

        binding.btnCancel.setOnClickListener {
            finish()
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

        val endTime = reminderTime!! + (60 * 60 * 1000)

        val updatedAppointment = AppointmentPlus(
            id = appointmentId,
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

        if(reminderTime != null){
            appointmentViewModel.updateAppointment(updatedAppointment,true)
        }
        else{
            appointmentViewModel.updateAppointment(updatedAppointment,false)
        }
    }

    private fun validateAppointmentInput(title: String): Boolean {
        binding.tilAppointmentTitle.error = null

        if (title.isEmpty()) {
            binding.tilAppointmentTitle.error = "Vui lòng nhập tiêu đề cuộc hẹn"
            return false
        }

        if (currentContactId == 0) {
            Toast.makeText(this, "Vui lòng chọn liên hệ", Toast.LENGTH_SHORT).show()
            return false
        }

        if (reminderTime == null) {
            Toast.makeText(this, "Vui lòng chọn thời gian nhắc nhở", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun showDateTimePicker() {
        val constraintBuilder = CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now())

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
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
            .setTitleText("Select time")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val selectedHour = timePicker.hour
            val selectedMinute = timePicker.minute

            if (isToday(calendar)) {
                if (selectedHour < now.get(Calendar.HOUR_OF_DAY) ||
                    (selectedHour == now.get(Calendar.HOUR_OF_DAY)) && selectedMinute <= now.get(Calendar.MINUTE)
                ) {
                    Toast.makeText(this, "Please select a future time", Toast.LENGTH_SHORT).show()
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
package com.nhathuy.nextmeet.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.adapter.AppointmentPlusAdapter
import com.nhathuy.nextmeet.adapter.ColorPickerAdapter
import com.nhathuy.nextmeet.adapter.ContactsAdapter
import com.nhathuy.nextmeet.databinding.DialogAddAppointmentBinding
import com.nhathuy.nextmeet.databinding.FragmentAppointmentMapBinding
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.AppointmentStatus
import com.nhathuy.nextmeet.model.ContactNameId
import com.nhathuy.nextmeet.resource.AppointmentUiState
import com.nhathuy.nextmeet.resource.ContactUiState
import com.nhathuy.nextmeet.ui.AddNoteActivity
import com.nhathuy.nextmeet.ui.GoogleMapActivity
import com.nhathuy.nextmeet.ui.NavigationMapActivity
import com.nhathuy.nextmeet.utils.Constant
import com.nhathuy.nextmeet.viewmodel.AppointmentPlusViewModel
import com.nhathuy.nextmeet.viewmodel.AppointmentViewModel
import com.nhathuy.nextmeet.viewmodel.ContactViewModel
import com.nhathuy.nextmeet.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
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

    private var addAppointmentDialog: Dialog? = null
    private var currentUserId: Int = 0
    private var currentContactId: Int = 0

    private var location: String? = null
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var reminderTime: Long? = null

    private val contactMap = mutableMapOf<String, ContactNameId>()

    private lateinit var appointmentAdapter: AppointmentPlusAdapter
    private lateinit var colorAdapter: ColorPickerAdapter
    private var selectedColorName: String = "color_white"

    private var currentSearchQuery: String? = null
    private var showFavoriteOnly: Boolean = false

    private var isLocationFromMap: Boolean = false

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

        setupUserInfo()
        setupRecyclerView()
        setupSwipeRefresh()
        setupObserver()
        setupFabMenu()
        setupSelectionToolbar()
        setupAppointmentObserver()
    }

    private fun setupUserInfo() {
        userViewModel.getCurrentUser().observe(viewLifecycleOwner) { user ->
            user?.let {
                currentUserId = user.id
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
            onLongClickListener = { appointment ->
                handleAppointmentLongClick(appointment)
            },
            onPinClickListener = { appointment ->
                togglePinned(appointment)
            },
            navigationMap = { appointment ->
                handleNavigationMap(appointment)
            },
            onSelectionChanged = { count ->

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

    }

    private fun setupAppointmentObserver(){
        viewLifecycleOwner.lifecycleScope.launch {
            appointmentViewModel.appointmentUiState.collect {
                state ->
                when(state){
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

    private fun handleAppointmentClick(appointment: AppointmentPlus){

    }
    private fun handleAppointmentLongClick(appointment: AppointmentPlus){

    }

    //toggle pinned
    private fun togglePinned(appointment: AppointmentPlus){
        appointmentViewModel.togglePin(appointment.id)
    }

    //chuyen sang tran hien thi ban do
    private fun handleNavigationMap(appointment: AppointmentPlus){
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

    //load cuộn hẹn
    private fun loadAppointments() {
        if (currentUserId != 0) {
            appointmentViewModel.getAllAppointments(
                userId = currentUserId,
                searchQuery = currentSearchQuery ?: "",
                showPinnedOnly = showFavoriteOnly,
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
        // Thêm hint cho EditText
        dialogBinding.etAppointmentLocation.hint = "Nhập địa chỉ hoặc chọn từ bản đồ"

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

            showTimePicker(calendar,binding)
        }

        datePicker.show(childFragmentManager, "date_picker")
    }

    // hiển thị thời gian chọn
    private fun showTimePicker(calendar: Calendar,binding: DialogAddAppointmentBinding) {
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
                    Toast.makeText(requireContext(), "Please select a future time", Toast.LENGTH_SHORT).show()
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

    private fun saveAppointment(dialogBinding: DialogAddAppointmentBinding){
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
            Toast.makeText(requireContext(), "Vui lòng chọn thời gian nhắc nhở", Toast.LENGTH_SHORT).show()
            return
        }

        val endTime = reminderTime!! + (60 * 60 * 1000) // Add 1 hour

        val appointment = AppointmentPlus(userId = currentUserId,
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
            isPinned = isPinned)

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

    override fun onDestroyView() {
        super.onDestroyView()
        addAppointmentDialog?.dismiss()
        _binding = null
    }
}

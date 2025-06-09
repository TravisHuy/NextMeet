package com.nhathuy.nextmeet.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.adapter.ContactsAdapter
import com.nhathuy.nextmeet.databinding.DialogAddContactBinding
import com.nhathuy.nextmeet.databinding.FragmentContactBinding
import com.nhathuy.nextmeet.model.Contact
import com.nhathuy.nextmeet.resource.ContactUiState
import com.nhathuy.nextmeet.ui.GoogleMapActivity
import com.nhathuy.nextmeet.utils.ValidationUtils
import com.nhathuy.nextmeet.viewmodel.ContactViewModel
import com.nhathuy.nextmeet.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ContactFragment : Fragment(){
    private var _binding: FragmentContactBinding? = null
    private val binding get() = _binding!!

    private lateinit var contactViewModel: ContactViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var contactsAdapter: ContactsAdapter

    private var addContactDialog: Dialog? = null
    private var currentUserId: Int = 0

    private var address: String? = null
    private var latitude: Double? = null
    private var longitude: Double? = null

    private var currentSearchQuery: String? = null
    private var showFavoriteOnly: Boolean = false

    private var isSelectionMode:Boolean = false

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

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        contactViewModel = ViewModelProvider(this)[ContactViewModel::class.java]

        setupUserInfo()
        setupRecyclerView()
        setupSwipeRefresh()
        setObserver()
        setupFabMenu()
        setupSelectionToolbar()
    }

    // khởi tạo thông tin người dùng
    private fun setupUserInfo() {
        userViewModel.getCurrentUser().observe(viewLifecycleOwner) { user ->
            user?.let {
                currentUserId = user.id
                loadContacts()
            }
        }
    }

    // thiết lập các observer
    private fun setObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                contactViewModel.contactUiState.collect { state ->
                    when (state) {
                        is ContactUiState.Idle -> {
                            hideLoading()
                        }

                        is ContactUiState.Loading -> {
                            showLoading()
                        }

                        is ContactUiState.ContactsLoaded -> {
                            hideLoading()
                            contactsAdapter.updateContacts(state.contacts)
                            updateEmptyState(state.contacts.isEmpty())
                        }

                        is ContactUiState.ContactCreated -> {
                            hideLoading()
                            showMessage(state.message)
                            loadContacts()
                        }

                        is ContactUiState.FavoriteToggled -> {
                            showMessage(state.message)
                            loadContacts()
                        }

                        else -> {}
                    }
                }
            }
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
            // Mở dialog để thêm liên hệ mới
            showAddContactDialog()
        }
    }

    // hiển thị custom selection toolbar
    private fun setupSelectionToolbar(){

        binding.selectionToolbar.visibility  =View.GONE

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
    }

    //thoát chế độ selection
    private fun closeSelectionMode(){
        isSelectionMode = false
        binding.selectionToolbar.visibility = View.GONE
        binding.appBarLayout.visibility = View.VISIBLE
        contactsAdapter.setMultiSelectMode(false)
        contactsAdapter.clearSelection()
    }

    //cập nhật lại số lượng đã chọn
    private fun updateSelectedCount(count: Int) {
        binding.tvSelectionCount.text = if(count>1){
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

    // hiển thị dialog để thêm liên hệ mới
    private fun showAddContactDialog() {
        val dialog = Dialog(requireContext())
        addContactDialog = dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogBinding = DialogAddContactBinding.inflate(LayoutInflater.from(requireContext()))
        dialog.setContentView(dialogBinding.root)

        dialogBinding.btnPickLocation.setOnClickListener {
            val intent = Intent(requireContext(), GoogleMapActivity::class.java)
            mapPickerLauncher.launch(intent)
        }
        dialogBinding.btnSave.setOnClickListener {
            // Lấy dữ liệu từ các trường nhập
            val name = dialogBinding.etFullName.text.toString()
            val phone = dialogBinding.etPhone.text.toString()
            val email = dialogBinding.etEmail.text.toString()
            val role = dialogBinding.etRole.text.toString()
            val notes = dialogBinding.etNotes.text.toString()
            val isFavorite = dialogBinding.cbFavorite.isChecked

            // Xóa lỗi cũ
            dialogBinding.tilName.error = null
            dialogBinding.tilPhone.error = null
            dialogBinding.tilEmail.error = null
            dialogBinding.tvLocation.error = null

            // Validate từng trường
            val nameResult = ValidationUtils.validateName(name)
            val phoneResult = ValidationUtils.validatePhone(phone)
            val emailResult = ValidationUtils.validateEmail(email)

            val locationText = dialogBinding.tvLocation.text.toString()
            val addressResult =
                if (locationText.isNotBlank() && locationText != getString(R.string.no_location_selected)) {
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

            if (hasError) return@setOnClickListener

            val contact = Contact(
                userId = currentUserId,
                name = name,
                address = locationText,
                phone = phone,
                email = email,
                role = role,
                notes = notes,
                latitude = latitude,
                longitude = longitude,
                isFavorite = isFavorite
            )
            contactViewModel.createContact(contact)
            dialog.dismiss()
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
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

    //hien thi loading
    private fun showLoading() {
        binding.swipeRefreshContacts.isRefreshing = true
    }

    //an loading
    private fun hideLoading() {
        binding.swipeRefreshContacts.isRefreshing = false
    }


    // xử lý action favorite
    private fun handleFavoriteAction(){
        val selectedContacts = contactsAdapter.getSelectedContacts()
        selectedContacts.forEach {
            contact ->
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
        if (selectedContacts.isNotEmpty()){
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa liên hệ")
                .setMessage("Bạn có chắc chắn muốn xóa ${selectedContacts.size} liên hệ đã chọn?")
                .setPositiveButton("Xóa") { dialog, _ ->
                    selectedContacts.forEach { contact ->
                        contactViewModel.deleteContact(contact.id)
                    }
                    showMessage("Đã xóa ${selectedContacts.size} liên hệ")
                    closeSelectionMode()
                }
                .setNegativeButton("Hủy") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
        else{
            closeSelectionMode()
        }
    }

    fun onBackPressed(): Boolean {
        return if (isSelectionMode) {
            closeSelectionMode()
            true
        } else {
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}


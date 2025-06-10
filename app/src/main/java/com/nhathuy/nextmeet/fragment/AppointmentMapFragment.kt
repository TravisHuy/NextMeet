package com.nhathuy.nextmeet.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.DialogAddAppointmentBinding
import com.nhathuy.nextmeet.databinding.FragmentAppointmentMapBinding
import com.nhathuy.nextmeet.model.ContactNameId
import com.nhathuy.nextmeet.ui.GoogleMapActivity
import com.nhathuy.nextmeet.viewmodel.AppointmentViewModel
import com.nhathuy.nextmeet.viewmodel.ContactViewModel
import com.nhathuy.nextmeet.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AppointmentMapFragment : Fragment() {

    private var _binding: FragmentAppointmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel
    private lateinit var contactViewModel: ContactViewModel
    private lateinit var appointmentViewModel: AppointmentViewModel

    private var addAppointmentDialog: Dialog? = null
    private var currentUserId: Int = 0
    private var currentContactId: Int = 0

    private var location: String? = null
    private var latitude: Double? = null
    private var longitude: Double? = null

    private val contactMap = mutableMapOf<String, ContactNameId>()

    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                location = data.getStringExtra(GoogleMapActivity.EXTRA_SELECTED_ADDRESS)
                latitude = data.getDoubleExtra(GoogleMapActivity.EXTRA_SELECTED_LAT, 0.0)
                longitude = data.getDoubleExtra(GoogleMapActivity.EXTRA_SELECTED_LNG, 0.0)

                addAppointmentDialog?.findViewById<TextView>(R.id.tv_location)
                    ?.let { addressTextView ->
                        if (location != null) {
                            addressTextView.text = location
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
        _binding = FragmentAppointmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        contactViewModel = ViewModelProvider(this)[ContactViewModel::class.java]
        appointmentViewModel = ViewModelProvider(this)[AppointmentViewModel::class.java]

        setupUserInfo()
        setupRecyclerView()
        setupSwipeRefresh()
        setupObserver()
        setupFabMenu()
        setupSelectionToolbar()
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

    }

    private fun setupSwipeRefresh() {

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
    //load cuộn hẹn
    private fun loadAppointments(){

    }
    //hiển thị dialog thêm cuộc hẹn
    private fun showAddAppointment(){
        val dialog = Dialog(requireContext())
        addAppointmentDialog = dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogBinding = DialogAddAppointmentBinding.inflate(LayoutInflater.from(requireContext()))
        dialog.setContentView(dialogBinding.root)

        dialogBinding.tilAppointmentLocation.setEndIconOnClickListener {
            val intent = Intent(requireContext(), GoogleMapActivity::class.java)
            mapPickerLauncher.launch(intent)
        }

        setupContactDropdown(dialogBinding)

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
//            saveAppointment(dialogBinding)
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
    private fun setupContactDropdown(dialogAddAppointmentBinding: DialogAddAppointmentBinding){
        // Ngăn người dùng nhập trực tiếp vào dropdown
        dialogAddAppointmentBinding.autoContactName.inputType = InputType.TYPE_NULL

        viewLifecycleOwner.lifecycleScope.launch {
            contactViewModel.contactNamesAndIds.collect { contacts ->
                if(contacts.isNotEmpty()){
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
                            dialogAddAppointmentBinding.autoContactName.setText(contactNames[0], false)
                        } else {
                            // Nếu chọn contact thực, lấy thông tin contact (position - 1 vì có placeholder)
                            val selectedContactName = contacts[position - 1].name
                            currentContactId = contacts[position - 1].id
                            dialogAddAppointmentBinding.autoContactName.setText(selectedContactName, false)
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

}

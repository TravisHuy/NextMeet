package com.nhathuy.customermanagementapp.ui

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.nhathuy.customermanagementapp.R
import com.nhathuy.customermanagementapp.adapter.ViewPageAdapter
import com.nhathuy.customermanagementapp.AlarmReceiver
import com.nhathuy.customermanagementapp.databinding.ActivityCustomerDetailBinding
import com.nhathuy.customermanagementapp.fragment.PlaceFragment
import com.nhathuy.customermanagementapp.fragment.TimeFragment
import com.nhathuy.customermanagementapp.model.Appointment
import com.nhathuy.customermanagementapp.model.Customer
import com.nhathuy.customermanagementapp.model.Transaction
import com.nhathuy.customermanagementapp.viewmodel.AppointmentViewModel
import com.nhathuy.customermanagementapp.viewmodel.CustomerViewModel
import com.nhathuy.customermanagementapp.viewmodel.TransactionViewModel
import com.nhathuy.customermanagementapp.viewmodel.UserViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CustomerDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerDetailBinding
    private lateinit var viewModel:CustomerViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var appointmentViewModel :  AppointmentViewModel
    private lateinit var transactionViewModel: TransactionViewModel
    private var currentUserId: Int =-1
    private var currentCustomer: Customer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityCustomerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userViewModel= ViewModelProvider(this).get(UserViewModel::class.java)
        viewModel=ViewModelProvider(this).get(CustomerViewModel::class.java)
        appointmentViewModel= ViewModelProvider(this).get(AppointmentViewModel::class.java)
        transactionViewModel= ViewModelProvider(this).get(TransactionViewModel::class.java)


        userViewModel.getCurrentUser().observe(this){
            user ->
            currentUserId= user?.id?:-1
        }

        currentCustomer = intent.getParcelableExtra("Customer_extra")
        currentCustomer?.let {
            displayCustomerDetail(it)
        }

        binding.arrowLeft.setOnClickListener {
            onBackPressed()
        }

        binding.edit.setOnClickListener {
            showEditDialog()
        }
        binding.remove.setOnClickListener {
            showDeleteDialog()
        }

        binding.icTransaction.setOnClickListener {
            showTransactionDialog()
        }
        binding.icAlaram.setOnClickListener {
            showAlarmDialog()
        }
    }



    //show Alarm Dialog
    private fun showAlarmDialog() {

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.add_alram)


        val window = dialog.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setGravity(Gravity.CENTER)


        // Thiết lập các view và xử lý sự kiện trong dialog
        val tabLayout = dialog.findViewById<TabLayout>(R.id.tablayout)
        val viewPager = dialog.findViewById<ViewPager2>(R.id.viewpager)
        val cancelButton = dialog.findViewById<Button>(R.id.cancel)
        val saveButton = dialog.findViewById<Button>(R.id.alram_save)




        // Thiết lập TabLayout và ViewPager2
        val adapter = ViewPageAdapter(supportFragmentManager, lifecycle)

        val timeFragment = TimeFragment()
        val placeFragment = PlaceFragment()

        adapter.addFragment(timeFragment, getString(R.string.pick_date_amp_time))
        adapter.addFragment(placeFragment, getString(R.string.pick_place))

        viewPager.adapter = adapter



        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when(position){
                0 -> getString(R.string.pick_date_amp_time)
                1 -> getString(R.string.pick_place)
                else -> null
            }
        }.attach()

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        saveButton.setOnClickListener {
           val (date,time) = timeFragment.getSelectDateTime()
           val address = placeFragment.getSelectAddress()
           val (repeatInterval, repeatUnit) = timeFragment.getRepeatInfo()

            // parse date and time
            val dateTimeString = "$date $time"
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val selectedDate = sdf.parse(dateTimeString)
            val currentDate = Calendar.getInstance().time

            if (selectedDate != null && selectedDate.before(currentDate)) {
                Toast.makeText(this, "Cannot select a past date and time", Toast.LENGTH_SHORT).show()
            }
            else if (address==null){
                Toast.makeText(this, "Select address", Toast.LENGTH_SHORT).show()
            }
            else {
                currentCustomer?.let { customer ->
                    val appointment = Appointment(
                        customerId = customer.id,
                        date = date,
                        time = time,
                        address = address,
                        notes = "Repeat: $repeatInterval $repeatUnit\t ${customer.notes}"
                    )

                    appointmentViewModel.register(appointment)
                    //schedule the alarm
                    scheduleAlarm(appointment)
                    Toast.makeText(this, "Appointment saved", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } ?: run {
                    Toast.makeText(this, "Error: Customer not found", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()

    }
    //schedule the alarm
    private fun scheduleAlarm(appointment: Appointment) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent= Intent(this, AlarmReceiver::class.java).apply {
            putExtra("customer_id",appointment.customerId)
            putExtra("date",appointment.date)
            putExtra("time",appointment.time)
            putExtra("address",appointment.address)
            putExtra("notes",appointment.notes)
        }

        val pendingIntent =  PendingIntent.getBroadcast(this,appointment.id,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        //parse date and time
        val dateTimeString= "${appointment.date} ${appointment.time}"
        val sdf= SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val calendar=Calendar.getInstance()
        calendar.time=sdf.parse(dateTimeString) ?: return


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent)

        }
        else{
            alarmManager.setExact(AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent)
        }


        // Log for debugging
        Log.d("AlarmScheduling", "Alarm scheduled for ${sdf.format(calendar.time)}")
    }



    private fun showEditDialog() {
        val dialog= Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.edit_customer)

        val name= dialog.findViewById<TextInputEditText>(R.id.ed_edit_customer_name)
        val phone= dialog.findViewById<TextInputEditText>(R.id.ed_edit_customer_phone)
        val email= dialog.findViewById<TextInputEditText>(R.id.ed_edit_customer_email)
        val address= dialog.findViewById<TextInputEditText>(R.id.ed_edit_customer_address)
        val group= dialog.findViewById<TextInputEditText>(R.id.ed_edit_customer_group)
        val notes= dialog.findViewById<TextInputEditText>(R.id.ed_edit_customer_notes)

        //
        val btn_edit=dialog.findViewById<Button>(R.id.btn_edit)

        val customer: Customer ? =intent.getParcelableExtra("Customer_extra")

        customer?.let {
            name.setText(it.name)
            phone.setText(it.phone)
            email.setText(it.email)
            address.setText(it.address)
            group.setText(it.group)
            notes.setText(it.notes)
        }

        btn_edit.setOnClickListener {
            val updatedCustomer = Customer(
                customer?.id!!,
                currentUserId,
                name=name.text.toString(),
                address= address.text.toString(),
                phone= phone.text.toString(),
                email= email.text.toString(),
                group= group.text.toString(),
                notes= notes.text.toString()
            )
            viewModel.editCustomer(updatedCustomer)
            dialog.dismiss()
            displayCustomerDetail(updatedCustomer)
        }
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.attributes?.windowAnimations=R.style.DialogAnimation;
        dialog.window?.setGravity(Gravity.BOTTOM)

    }
    //Show transaction dialog
    private fun showTransactionDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.add_transaction)

        val productService =dialog.findViewById<TextInputEditText>(R.id.ed_transaction_product_name)
        val quantity =dialog.findViewById<TextInputEditText>(R.id.ed_transaction_quantity)
        val price =dialog.findViewById<TextInputEditText>(R.id.ed_transaction_price)
        val dateEditText =dialog.findViewById<TextInputEditText>(R.id.ed_transaction_date)
        val dateLayout = dialog.findViewById<TextInputLayout>(R.id.add_transaction_date_layout)
        val submit = dialog.findViewById<Button>(R.id.btn_transaction)

        val calendar =Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(this,{
            _,year,month,dayOfMonth ->
            calendar.set(Calendar.YEAR,year)
            calendar.set(Calendar.MONTH,month)
            calendar.set(Calendar.DAY_OF_MONTH,dayOfMonth)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            dateEditText.setText(dateFormat.format(calendar.time))
        },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )


        dateLayout.setEndIconOnClickListener {
            datePickerDialog.show()
        }

        dateEditText.setOnClickListener {
            datePickerDialog.show()
        }

        submit.setOnClickListener {

            currentCustomer?.let {
                customer ->
                val transaction = Transaction(
                    userId =  currentUserId,
                    customerId = customer.id,
                    productOrService = productService.text.toString(),
                    quantity = quantity.text.toString().toInt(),
                    price = price.text.toString().toDouble(),
                    date = dateEditText.text.toString()
                )

                transactionViewModel.addTransaction(transaction)
                Toast.makeText(this, "Transaction added successfully", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } ?: run {
                Toast.makeText(this, "Transaction added failed", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.attributes?.windowAnimations=R.style.DialogAnimation;
        dialog.window?.setGravity(Gravity.BOTTOM)
    }
    private fun displayCustomerDetail(customer: Customer) {
        binding.editName.text=customer.name
        binding.editPhone.text=customer.phone
        binding.editEmail.text=customer.email
        binding.editAddress.text=customer.address
        binding.editGroup.text=customer.group
        binding.editNotes.text=customer.notes
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Customer")
            .setMessage("Are you sure you want to delete this customer?")
            .setPositiveButton("Yes"){
                _,_ -> deleteCustomer()
            }
            .setNegativeButton("No",null)
            .show()
    }

    private fun deleteCustomer() {
        currentCustomer?.let {
            customer ->  viewModel.deleteCustomer(customer)
            Toast.makeText(this,"Customer deleted",Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
    }
}
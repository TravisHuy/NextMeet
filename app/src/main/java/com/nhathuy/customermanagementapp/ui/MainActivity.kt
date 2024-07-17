package com.nhathuy.customermanagementapp.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat

import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.nhathuy.customermanagementapp.AlarmHistoryActivity
import com.nhathuy.customermanagementapp.R
import com.nhathuy.customermanagementapp.databinding.ActivityMainBinding
import com.nhathuy.customermanagementapp.fragment.AppointmentFragment
import com.nhathuy.customermanagementapp.fragment.CustomerFragment
import com.nhathuy.customermanagementapp.fragment.TransactionFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolBar)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController=navHostFragment.navController

       appBarConfiguration = AppBarConfiguration(setOf(
            R.id.customerFragment,
            R.id.appointmentFragment,
            R.id.transactionFragment,
            R.id.aboutFragment
        ))
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNavItem.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, _, _ ->
            invalidateOptionsMenu()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_fragment).navigateUp() || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.topbar_menu,menu)

        val searchItem =menu.findItem(R.id.search)
        val searchView =searchItem.actionView as SearchView

        // Apply custom style to SearchView
        val searchPlate = searchView.findViewById(androidx.appcompat.R.id.search_plate) as View
        searchPlate.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))

        val searchText = searchView.findViewById(androidx.appcompat.R.id.search_src_text) as TextView
        searchText.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        searchText.setHintTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))

        val searchClose = searchView.findViewById(androidx.appcompat.R.id.search_close_btn) as ImageView
        searchClose.setColorFilter(ContextCompat.getColor(this, android.R.color.black))

        // Set the hint text based on the current fragment
        setSearchHint(searchView)

        searchView.setOnQueryTextListener(object :SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                performSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                performSearch(newText)
                return true
            }

        })

        // Set up notification icon click listener
        val notificationItem = menu.findItem(R.id.notification)
        notificationItem.setOnMenuItemClickListener {
            openAlarmHistoryActivity()
            true
        }
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            else -> return super.onOptionsItemSelected(item)
        }
    }
    private fun performSearch(query: String?) {
        // Get the current Fragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val currentFragment = navHostFragment.childFragmentManager.fragments[0]

        // If the current fragment is CustomerFragment, perform the search
        when (currentFragment) {
            is CustomerFragment -> currentFragment.searchCustomers(query)
            is TransactionFragment -> currentFragment.searchTransactions(query)
            is AppointmentFragment -> currentFragment.searchAppointment(query)
        }
    }
    private fun setSearchHint(searchView: SearchView) {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val currentFragment = navHostFragment.childFragmentManager.fragments[0]

        val hint = when (currentFragment) {
            is CustomerFragment -> "Enter customer name"
            is TransactionFragment -> "Enter product or service"
            is AppointmentFragment -> "Enter appointment date"
            else -> "Search"
        }

        searchView.queryHint = hint
    }


    private fun openAlarmHistoryActivity() {
        startActivity(Intent(this,AlarmHistoryActivity::class.java))
    }
}

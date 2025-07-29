package com.nhathuy.nextmeet.ui

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.viewpager2.widget.ViewPager2
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.adapter.ViewPagerAdapter
import com.nhathuy.nextmeet.databinding.ActivitySolutionBinding
import com.nhathuy.nextmeet.model.Contact
import com.nhathuy.nextmeet.utils.NavigationCallback
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SolutionActivity : BaseActivity() {
    public final lateinit var binding: ActivitySolutionBinding
    private lateinit var viewPagerAdapter: ViewPagerAdapter

    interface NavigationCallback {
        fun onNavigateToContact()
        fun onNavigateToEditContact(contact: Contact)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySolutionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            enableEdgeToEdge()
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
                insets
            }
        }

        setupViewPager()
        setupBottomNavigation()

        handleNavigationIntent()
    }

    private fun setupViewPager() {
        viewPagerAdapter = ViewPagerAdapter(this)
        binding.viewPager2.adapter = viewPagerAdapter

        // tắt scroll viewpager2 bằng tay
        binding.viewPager2.isUserInputEnabled  = false

        //đồng bộ viewpager2 với bottomNavigationItem
        binding.viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.navBottomNavigation.menu.get(position).isChecked = true
            }
        })
    }

    private fun setupBottomNavigation() {
        binding.navBottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> binding.viewPager2.currentItem = 0
                R.id.nav_appointment -> binding.viewPager2.currentItem = 1
                R.id.nav_contact -> binding.viewPager2.currentItem = 2
                R.id.nav_settings -> binding.viewPager2.currentItem = 3
            }
            true
        }
    }
    private fun handleNavigationIntent(){
        val navigateTo = intent.getStringExtra("navigate_to")
        when(navigateTo){
            "contact" -> {
                binding.viewPager2.setCurrentItem(2,false)
                binding.navBottomNavigation.selectedItemId = R.id.nav_contact

                binding.root.postDelayed({
                    triggerContactNavigation()
                }, 100)

                // xóa intent
                intent.removeExtra("navigate_to")
            }
        }
    }
    private fun triggerContactNavigation(){
        val fragments = supportFragmentManager.fragments
        val contactData = intent.getParcelableExtra<Contact>("contact_data")
        val action = intent.getStringExtra("action")

        for(fragment in fragments){
            if(fragment is NavigationCallback && fragment.isVisible){
                when(action) {
                    "edit_contact" -> {
                        if(contactData != null) {
                            fragment.onNavigateToEditContact(contactData)
                        }
                    }
                    else -> {
                        fragment.onNavigateToContact()
                    }
                }
                break
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent()
    }
}
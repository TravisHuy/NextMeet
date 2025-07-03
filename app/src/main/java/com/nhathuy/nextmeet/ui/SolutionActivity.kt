package com.nhathuy.nextmeet.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.get
import androidx.viewpager2.widget.ViewPager2
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.adapter.ViewPagerAdapter
import com.nhathuy.nextmeet.databinding.ActivitySolutionBinding
import com.nhathuy.nextmeet.utils.NavigationCallback
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SolutionActivity : AppCompatActivity() {
    public final lateinit var binding: ActivitySolutionBinding
    private lateinit var viewPagerAdapter: ViewPagerAdapter

    interface NavigationCallback {
        fun onNavigateToContact()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySolutionBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        for(fragment in fragments){
            if(fragment is NavigationCallback && fragment.isVisible){
                fragment.onNavigateToContact()
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
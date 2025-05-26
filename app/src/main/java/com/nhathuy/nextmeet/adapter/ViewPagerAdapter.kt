package com.nhathuy.nextmeet.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.nhathuy.nextmeet.fragment.AppointmentFragment
import com.nhathuy.nextmeet.fragment.AppointmentMapFragment
import com.nhathuy.nextmeet.fragment.ContactFragment
import com.nhathuy.nextmeet.fragment.DashBoardFragment
import com.nhathuy.nextmeet.fragment.HistoryFragment
import com.nhathuy.nextmeet.fragment.HomeFragment
import com.nhathuy.nextmeet.fragment.NotesFragment
import com.nhathuy.nextmeet.fragment.SettingsFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> AppointmentMapFragment()
            2 -> ContactFragment()
            3 -> SettingsFragment()
            else -> HomeFragment()
        }
    }


}
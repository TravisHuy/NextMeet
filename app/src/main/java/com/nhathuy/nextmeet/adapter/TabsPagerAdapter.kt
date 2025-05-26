package com.nhathuy.nextmeet.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.nhathuy.nextmeet.fragment.DashBoardFragment
import com.nhathuy.nextmeet.fragment.HistoryFragment
import com.nhathuy.nextmeet.fragment.NotesFragment

class TabsPagerAdapter(fragment:Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DashBoardFragment()
            1 -> NotesFragment()
            2 -> HistoryFragment()
            else -> NotesFragment()
        }
    }

}
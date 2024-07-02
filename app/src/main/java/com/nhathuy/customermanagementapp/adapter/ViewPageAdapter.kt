package com.nhathuy.customermanagementapp.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPageAdapter(fragmentManager: FragmentManager,lifecycle: Lifecycle):FragmentStateAdapter(fragmentManager, lifecycle) {

    private val fragmentList= ArrayList<Fragment>()
    private val fragmentTitleList = ArrayList<String>()

    fun addFragment(fragment: Fragment,title:String){
        fragmentList.add(fragment)
        fragmentTitleList.add(title)
    }
    override fun getItemCount(): Int=fragmentList.size
    override fun createFragment(position: Int): Fragment =fragmentList[position]

    fun getPageTitle(position: Int): CharSequence? =fragmentTitleList[position]

}
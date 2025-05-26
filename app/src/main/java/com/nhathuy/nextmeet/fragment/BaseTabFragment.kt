package com.nhathuy.nextmeet.fragment

import android.content.Context
import androidx.fragment.app.Fragment
import com.nhathuy.nextmeet.ui.TestActivity

abstract class BaseTabFragment : Fragment() {
    protected var testActivity: TestActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is TestActivity){
            testActivity = context
        }
    }
    override fun onDetach() {
        super.onDetach()
        testActivity = null
    }
    // abstract method để refresh data
    abstract fun refreshData()

    // helper method để check xem fragment có đang visible không
    protected fun isFragmentVisible():Boolean {
        return isAdded && !isHidden && userVisibleHint
    }

    //method để lazy load data khi fragment visible lần đầu
    protected open fun loadDataOnFirstVisible(){

    }

    private var hasLoadedData = false

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if(isVisibleToUser && !hasLoadedData && isAdded){
            loadDataOnFirstVisible()
            hasLoadedData = true
        }
    }
}
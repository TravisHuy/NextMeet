package com.nhathuy.customermanagementapp.di

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.nhathuy.customermanagementapp.adapter.AppointmentAdapter
import com.nhathuy.customermanagementapp.adapter.TransactionAdapter
import com.nhathuy.customermanagementapp.fragment.AppointmentFragment
import com.nhathuy.customermanagementapp.fragment.TransactionFragment
import com.nhathuy.customermanagementapp.viewmodel.AppointmentViewModel
import com.nhathuy.customermanagementapp.viewmodel.CustomerViewModel
import com.nhathuy.customermanagementapp.viewmodel.TransactionViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(FragmentComponent::class)
object AdapterModule {

    @Provides
    fun provideTransactionAdapter(
        @ApplicationContext context: Context,
        customerViewModel: CustomerViewModel,
        transactionViewModel: TransactionViewModel,
        fragment: TransactionFragment  // To get the lifecycleOwner
    ): TransactionAdapter {
        return TransactionAdapter(
            context = context,
            listTransaction = emptyList(),  // Initial empty list
            customerViewModel = customerViewModel,
            lifecycleOwner = fragment,
            transactionViewModel = transactionViewModel,
            onSelectionChanged = { }
        )
    }

    @Provides
    fun provideAppointmentAdapter(
        @ApplicationContext context: Context,
        customerViewModel: CustomerViewModel,
        appointmentViewModel: AppointmentViewModel,
        fragment: AppointmentFragment,
        fragmentManager: FragmentManager
    ): AppointmentAdapter {
        return AppointmentAdapter(
            context = context,
            listAppointment = emptyList(),
            customerViewModel = customerViewModel,
            appointmentViewModel = appointmentViewModel,
            onSelectionChanged = {  },
            fragmentManager = fragment.childFragmentManager,
            lifecycleOwner = fragment
        )
    }

}
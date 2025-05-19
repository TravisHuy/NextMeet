package com.nhathuy.nextmeet.di

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.nhathuy.nextmeet.adapter.AppointmentAdapter
import com.nhathuy.nextmeet.adapter.TransactionAdapter
import com.nhathuy.nextmeet.fragment.AppointmentFragment
import com.nhathuy.nextmeet.fragment.TransactionFragment
import com.nhathuy.nextmeet.viewmodel.AppointmentViewModel
import com.nhathuy.nextmeet.viewmodel.CustomerViewModel
import com.nhathuy.nextmeet.viewmodel.TransactionViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
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
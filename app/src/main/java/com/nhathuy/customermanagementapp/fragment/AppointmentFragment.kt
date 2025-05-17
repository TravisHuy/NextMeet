package com.nhathuy.customermanagementapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nhathuy.customermanagementapp.adapter.AppointmentAdapter
import com.nhathuy.customermanagementapp.databinding.FragmentAppointmentBinding
import com.nhathuy.customermanagementapp.model.Appointment
import com.nhathuy.customermanagementapp.resource.Resource
import com.nhathuy.customermanagementapp.viewmodel.AppointmentViewModel
import com.nhathuy.customermanagementapp.viewmodel.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class AppointmentFragment : Fragment() {

    private var _binding: FragmentAppointmentBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var appointmentViewModel: AppointmentViewModel

    @Inject
    lateinit var customerViewModel: CustomerViewModel

    private lateinit var appointmentAdapter: AppointmentAdapter

    private var listAppointments: List<Appointment> = emptyList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAppointmentBinding.inflate(inflater, container, false)


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecylerView()
        observerViewModel()
    }

    private fun observerViewModel() {
        lifecycleScope.launch {
            appointmentViewModel.allAppointmentsState.collect { result ->
                when (result) {
                    is Resource.Loading -> {

                    }

                    is Resource.Success -> {
                        result.data?.let {
                            listAppointments = it
                            appointmentAdapter.setData(it)
                        }
                    }

                    is Resource.Error -> {
                        Toast.makeText(
                            requireContext(),
                            "Loi ko lay duoc danh sach customer ${result.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun setupRecylerView() {
        appointmentAdapter = AppointmentAdapter(
            requireContext(),
            emptyList(),
            customerViewModel,
            appointmentViewModel,
            onSelectionChanged = { isInSelectionMode ->

            },
            childFragmentManager,
            viewLifecycleOwner
        )

        binding.recAppointment.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = appointmentAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        appointmentViewModel.getAllAppointments()
    }

    fun searchAppointment(query: String?) {
        if (query.isNullOrBlank()) {
            appointmentAdapter.setData(listAppointments)
        } else {
            val filteredList = listAppointments.filter { appointment ->
                appointment.date.contains(query, ignoreCase = false)
                appointment.date.split(" ").any {
                    it.contains(query, ignoreCase = true)
                }
            }
            appointmentAdapter.setData(filteredList)
        }
    }
}
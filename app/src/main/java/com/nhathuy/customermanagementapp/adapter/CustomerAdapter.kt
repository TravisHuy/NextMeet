package com.nhathuy.customermanagementapp.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nhathuy.customermanagementapp.model.Customer

class CustomerAdapter(private val listCustomer:List<Customer>):
    RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>() {


    class CustomerViewHolder(itemView:View):RecyclerView.ViewHolder(itemView){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

}
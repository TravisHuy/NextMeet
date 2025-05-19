package com.nhathuy.nextmeet.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.model.Customer
import com.nhathuy.nextmeet.ui.CustomerDetailActivity


class CustomerAdapter(private val context:Context, private var listCustomer:List<Customer>,private val onDelete : (Customer) -> Unit):
    RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>() {
    class CustomerViewHolder(itemView:View):RecyclerView.ViewHolder(itemView){
        private val name:TextView=itemView.findViewById(R.id.name)
        private val email:TextView=itemView.findViewById(R.id.date)
        private val phone:TextView=itemView.findViewById(R.id.phone)

        fun bind(customer: Customer,context:Context,onDelete: (Customer) -> Unit) {
            name.text=customer.name
            email.text=customer.email
            phone.text=customer.phone
//            val userId=customer.userId

            itemView.setOnClickListener {
                val intent = Intent(context,CustomerDetailActivity::class.java).apply {
                    putExtra("Customer_extra",customer)

                }
                context.startActivity(intent)
            }

            itemView.setOnLongClickListener {
                onDelete(customer)
                true
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val itemView=LayoutInflater.from(context).inflate(R.layout.customer_item,parent,false)
        return CustomerViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return listCustomer.size
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
       val customer=listCustomer[position]
        holder.bind(customer,context,onDelete)
    }
    fun setData(customers: List<Customer>) {
        listCustomer = customers
        notifyDataSetChanged()
    }
}
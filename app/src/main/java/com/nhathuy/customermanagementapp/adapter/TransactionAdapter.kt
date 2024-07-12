package com.nhathuy.customermanagementapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.nhathuy.customermanagementapp.R
import com.nhathuy.customermanagementapp.model.Appointment
import com.nhathuy.customermanagementapp.model.Transaction
import com.nhathuy.customermanagementapp.viewmodel.CustomerViewModel

class TransactionAdapter(private val context: Context,
                        private var listTransaction:List<Transaction>,
                        private val customerViewModel: CustomerViewModel):
    RecyclerView.Adapter<TransactionAdapter.TransactionViewModel>() {


    class TransactionViewModel(itemView: View):RecyclerView.ViewHolder(itemView) {

       val customerName : TextView = itemView.findViewById(R.id.name)
       val productOrService: TextView = itemView.findViewById(R.id.productName)
       val quantity : TextView = itemView.findViewById(R.id.quantity)
       val price : TextView = itemView.findViewById(R.id.price)
       val date : TextView = itemView.findViewById(R.id.date)


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewModel {
        val itemView = LayoutInflater.from(context).inflate(R.layout.transaction_item,parent,false)
        return TransactionViewModel(itemView)
    }

    override fun onBindViewHolder(holder: TransactionViewModel, position: Int) {
        val transaction = listTransaction[position]

        holder.productOrService.text=transaction.productOrService
        holder.quantity.text= transaction.quantity.toString()
        holder.price.text=transaction.price.toString()
        holder.date.text=transaction.date

        customerViewModel.getCustomerById(transaction.customerId).observe(context as LifecycleOwner) { customer ->
            holder.customerName.text = customer?.name ?: "Unknown"
        }

    }
    override fun getItemCount(): Int {
       return listTransaction.size
    }
    fun setData(transactions: List<Transaction>) {
        listTransaction = transactions
        notifyDataSetChanged()
    }

}
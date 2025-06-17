package com.nhathuy.nextmeet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nhathuy.nextmeet.databinding.ItemRouteStepBinding
import com.nhathuy.nextmeet.model.RouteStep

class RouteStepAdapter : RecyclerView.Adapter<RouteStepAdapter.RouteStepViewHolder>() {

    private var routeSteps = listOf<RouteStep>()

    inner class RouteStepViewHolder(val binding: ItemRouteStepBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(routeStep: RouteStep){
            with(binding){
                ivStepIcon.setImageResource(routeStep.iconResId)
                tvStepInstruction.text = routeStep.instruction
                tvStepDistance.text = routeStep.distance
                tvStepDuration.text = routeStep.duration
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RouteStepAdapter.RouteStepViewHolder {
        val binding = ItemRouteStepBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RouteStepViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: RouteStepAdapter.RouteStepViewHolder,
        position: Int
    ) {
        holder.bind(routeSteps[position])
    }

    override fun getItemCount(): Int = routeSteps.size

    fun updateSteps(newSteps: List<RouteStep>){
        routeSteps = newSteps
        notifyDataSetChanged()
    }
}
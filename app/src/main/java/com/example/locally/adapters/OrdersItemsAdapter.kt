package com.example.locally.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.locally.R
import com.example.locally.database.OrderEntity
import com.example.locally.databinding.OrderItemBinding

class OrdersItemsAdapter(val list : List<OrderEntity>, val context: Context): RecyclerView.Adapter<OrdersItemsAdapter.MyViewHolder>() {

    private var onClickListener: OnClickListener? = null

    inner class MyViewHolder(binding: OrderItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val image = binding.orderItemPhoto
        val title = binding.orderItemTitle
        val category = binding.orderItemCategory
        val price = binding.orderItemPrice
        val location = binding.orderItemLocation
    }

    fun setOnClickListener(listener: OnClickListener){
        onClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = OrderItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val model = list[position]

        if (holder is MyViewHolder) {

            val image = when(model.imagePosition){
                1 -> R.drawable.ic_baseline_directions_car_24
                2 -> R.drawable.ic_baseline_home_24
                3 -> R.drawable.ic_baseline_shopping_basket_24
                4 -> R.drawable.ic_baseline_people_24
                5 -> R.drawable.ic_baseline_all_inclusive_24
                else -> R.drawable.ic_user_place_holder

            }

            Glide
                .with(context)
                .load(image)
                .centerCrop()
                .placeholder(image)
                .into(holder.image)

            holder.title.text = model.title
            holder.category.text = model.category
            holder.price.text = "${model.price} zł"
            holder.location.text = model.city

            holder.itemView.setOnClickListener {
                if(onClickListener != null) {
                    onClickListener!!.onClick(position, model.id)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface OnClickListener{
        fun onClick(position: Int, orderId: Int)
    }
}
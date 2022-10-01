package com.example.locally.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.locally.R
import com.example.locally.database.OpinionEntity
import com.example.locally.databinding.OpinionItemBinding

class OpinionsItemsAdapter(val opinions: List<OpinionEntity>, val context: Context): RecyclerView.Adapter<OpinionsItemsAdapter.MyOpinionsViewHolder>() {

    inner class MyOpinionsViewHolder(binding: OpinionItemBinding): RecyclerView.ViewHolder(binding.root){
        val user = binding.tvUser
        val description = binding.tvDescription
        val rating = binding.rating
        val image = binding.ivProfilePhoto
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyOpinionsViewHolder {
        val binding = OpinionItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyOpinionsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyOpinionsViewHolder, position: Int) {
        val model = opinions[position]

        Glide
            .with(context)
            .load(model.imageOfUser)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(holder.image)

        holder.user.text = model.userRatingName
        holder.description.text = model.description
        holder.rating.rating = model.rate.toFloat()
    }

    override fun getItemCount(): Int {
        return opinions.size
    }
}
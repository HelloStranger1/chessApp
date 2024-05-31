package com.hellostranger.chess_app.rv.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.databinding.FriendItemBinding
import com.hellostranger.chess_app.models.rvEntities.Friend

class FriendsAdapter(
    private val friendOnClickListener: FriendOnClickListener,
    private val maxItems : Int = -1
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {


    private var friendList = mutableListOf<Friend>()

    @SuppressLint("NotifyDataSetChanged")
    fun updateFriendList(updatedFriendList : List<Friend>) {
        this.friendList = updatedFriendList.toMutableList()
        notifyDataSetChanged()
    }

    inner class FriendViewHolder(private val binding : FriendItemBinding)
        : RecyclerView.ViewHolder(binding.root) {
            fun bind(friend : Friend) {
                Glide
                    .with(binding.ivFriendImage)
                    .load(friend.image)
                    .centerCrop()
                    .placeholder(R.drawable.ic_user_place_holder)
                    .into(binding.ivFriendImage)
                binding.tvFriendName.text = friend.name
            }
        }
    class FriendOnClickListener(
        val clickListener : (friend : Friend) -> Unit
    ) {
        fun onClick(friend: Friend) = clickListener(friend)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        return FriendViewHolder(
            FriendItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return if (friendList.size > maxItems && maxItems != -1) {
            maxItems
        } else {
            friendList.size
        }
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friendList[position]
        holder.itemView.setOnClickListener {
            friendOnClickListener.clickListener(friend)
        }
        holder.bind(friend)
    }
}
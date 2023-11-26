package com.hellostranger.chess_app

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hellostranger.chess_app.databinding.NotificationItemBinding
import com.hellostranger.chess_app.models.entites.GameHistory
import com.hellostranger.chess_app.models.entites.Notification

class NotificationAdapter(
    private val notificationOnClickListener: NotificationOnClickListener
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private var notificationList = mutableListOf<Notification>()

    fun updateNotificationList(updatedNotificationList : List<Notification>){
        this.notificationList = updatedNotificationList.toMutableList()
        notifyDataSetChanged()
    }

    inner class NotificationViewHolder(private val binding : NotificationItemBinding) : RecyclerView.ViewHolder(binding.root){
        val agreeImageView : ImageView = binding.ivAgree
        val declineImageView : ImageView = binding.ivReject
        fun bind(notification: Notification?){
            notification ?: return
            Glide
                .with(binding.ivImage)
                .load(notification.image)
                .centerCrop()
                .placeholder(R.drawable.ic_user_place_holder)
                .into(binding.ivImage)
            binding.tvName.text = notification.name
            binding.ivAgree.setImageResource(R.drawable.ic_agree)
            binding.ivReject.setImageResource(R.drawable.ic_reject)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        return NotificationViewHolder(
            NotificationItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return notificationList.size
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notificationList[position]
        holder.agreeImageView.setOnClickListener {
            notificationOnClickListener.agreeClickListener(notification)
            notifyItemChanged(holder.layoutPosition)
        }
        holder.declineImageView.setOnClickListener {
            notificationOnClickListener.declineClickListener(notification)
            notifyItemChanged(holder.layoutPosition)
        }
        holder.itemView.setOnClickListener {
            notificationOnClickListener.itemClickListener(notification)
        }
        holder.bind(notification)
    }
    class NotificationOnClickListener(
        val agreeClickListener: (notification : Notification) -> Unit,
        val declineClickListener: (notification : Notification) -> Unit,
        val itemClickListener : (notification : Notification) -> Unit)
    {
        fun onAgreeClick(notification: Notification) = agreeClickListener(notification)
        fun onDeclineClick(notification: Notification) = declineClickListener(notification)

        fun onItemClick(notification: Notification) = itemClickListener(notification)
    }

}
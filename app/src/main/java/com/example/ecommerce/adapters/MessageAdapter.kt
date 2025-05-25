package com.example.ecommerce.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ecommerce.R
import com.example.ecommerce.databinding.ItemMessageImageReceivedBinding
import com.example.ecommerce.databinding.ItemMessageImageSentBinding
import com.example.ecommerce.databinding.ItemMessageReceivedBinding
import com.example.ecommerce.databinding.ItemMessageSentBinding
import com.example.ecommerce.models.Message
import com.example.ecommerce.utils.TimeUtils
import com.google.firebase.auth.FirebaseAuth

class MessageAdapter(
    private val onImageClick: (String) -> Unit
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val VIEW_TYPE_IMAGE_SENT = 3
        private const val VIEW_TYPE_IMAGE_RECEIVED = 4
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return when {
            message.senderId == currentUserId && message.type == Message.TYPE_IMAGE -> VIEW_TYPE_IMAGE_SENT
            message.senderId == currentUserId -> VIEW_TYPE_SENT
            message.type == Message.TYPE_IMAGE -> VIEW_TYPE_IMAGE_RECEIVED
            else -> VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SentMessageViewHolder(binding)
            }
            VIEW_TYPE_RECEIVED -> {
                val binding = ItemMessageReceivedBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ReceivedMessageViewHolder(binding)
            }
            VIEW_TYPE_IMAGE_SENT -> {
                val binding = ItemMessageImageSentBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SentImageMessageViewHolder(binding)
            }
            VIEW_TYPE_IMAGE_RECEIVED -> {
                val binding = ItemMessageImageReceivedBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ReceivedImageMessageViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
            is SentImageMessageViewHolder -> holder.bind(message)
            is ReceivedImageMessageViewHolder -> holder.bind(message)
        }
    }

    inner class SentMessageViewHolder(
        private val binding: ItemMessageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.apply {
                messageText.text = message.text
                message.timestamp?.toDate()?.let { date ->
                    timestamp.text = TimeUtils.getRelativeTimeSpan(date)
                }
            }
        }
    }

    inner class ReceivedMessageViewHolder(
        private val binding: ItemMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.apply {
                messageText.text = message.text
                message.timestamp?.toDate()?.let { date ->
                    timestamp.text = TimeUtils.getRelativeTimeSpan(date)
                }
                
                // Load user avatar
                Glide.with(itemView)
                    .load(message.senderPhotoUrl)
                    .placeholder(R.drawable.default_avatar)
                    .into(binding.avatar)
            }
        }
    }

    inner class SentImageMessageViewHolder(
        private val binding: ItemMessageImageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.messageImage.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position).imageUrl?.let { url ->
                        onImageClick(url)
                    }
                }
            }
        }

        fun bind(message: Message) {
            binding.apply {
                // Show/hide caption
                if (message.text.isNotEmpty()) {
                    messageText.text = message.text
                    messageText.visibility = View.VISIBLE
                } else {
                    messageText.visibility = View.GONE
                }

                // Show/hide upload progress
                uploadProgress.visibility = if (message.isUploading) View.VISIBLE else View.GONE

                // Load image
                message.imageUrl?.let { url ->
                    Glide.with(itemView)
                        .load(url)
                        .placeholder(R.drawable.image_placeholder)
                        .into(messageImage)
                }

                // Set timestamp
                message.timestamp?.toDate()?.let { date ->
                    timestamp.text = TimeUtils.getRelativeTimeSpan(date)
                }
            }
        }
    }

    inner class ReceivedImageMessageViewHolder(
        private val binding: ItemMessageImageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.messageImage.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position).imageUrl?.let { url ->
                        onImageClick(url)
                    }
                }
            }
        }

        fun bind(message: Message) {
            binding.apply {
                // Show/hide caption
                if (message.text.isNotEmpty()) {
                    messageText.text = message.text
                    messageText.visibility = View.VISIBLE
                } else {
                    messageText.visibility = View.GONE
                }

                // Load image
                message.imageUrl?.let { url ->
                    Glide.with(itemView)
                        .load(url)
                        .placeholder(R.drawable.image_placeholder)
                        .into(messageImage)
                }

                // Load user avatar
                Glide.with(itemView)
                    .load(message.senderPhotoUrl)
                    .placeholder(R.drawable.default_avatar)
                    .into(binding.avatar)

                // Set timestamp
                message.timestamp?.toDate()?.let { date ->
                    timestamp.text = TimeUtils.getRelativeTimeSpan(date)
                }
            }
        }
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
} 
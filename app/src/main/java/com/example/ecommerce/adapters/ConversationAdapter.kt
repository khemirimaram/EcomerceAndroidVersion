package com.example.ecommerce.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ecommerce.R
import com.example.ecommerce.databinding.ItemConversationBinding
import com.example.ecommerce.models.Conversation
import com.example.ecommerce.utils.TimeUtils
import com.google.firebase.auth.FirebaseAuth

class ConversationAdapter(
    private val onConversationClick: (Conversation) -> Unit
) : ListAdapter<Conversation, ConversationAdapter.ConversationViewHolder>(ConversationDiffCallback()) {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding = ItemConversationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ConversationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ConversationViewHolder(
        private val binding: ItemConversationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(conversation: Conversation) {
            val currentUserId = auth.currentUser?.uid ?: return
            val otherUserId = conversation.getOtherParticipantId(currentUserId) ?: return

            binding.apply {
                // Set conversation title (product name or other user's name)
                val title = conversation.productInfo?.name ?: "Conversation"
                conversationTitle.text = title

                // Set last message
                val lastMessageText = conversation.lastMessage?.content ?: "Pas de message"
                lastMessagePreview.text = lastMessageText

                // Set timestamp
                val timestamp = conversation.lastMessage?.timestamp ?: 0L
                timeText.text = TimeUtils.getRelativeTimeSpan(timestamp)

                // Set unread count
                val unreadCount = conversation.getUnreadCount(currentUserId)
                if (unreadCount > 0) {
                    unreadCountBadge.text = unreadCount.toString()
                    unreadCountBadge.visibility = android.view.View.VISIBLE
                } else {
                    unreadCountBadge.visibility = android.view.View.GONE
                }

                // Load other user's avatar
                Glide.with(root.context)
                    .load(conversation.productInfo?.imageUrl)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .circleCrop()
                    .into(avatarImage)

                // Set click listener
                root.setOnClickListener {
                    onConversationClick(conversation)
                }
            }
        }
    }

    private class ConversationDiffCallback : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem == newItem
        }
    }
} 
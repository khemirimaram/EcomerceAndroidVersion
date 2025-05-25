package com.example.ecommerce

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecommerce.adapters.MessageAdapter
import com.example.ecommerce.databinding.ActivityChatBinding
import com.example.ecommerce.models.Message
import com.example.ecommerce.utils.NotificationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var messageAdapter: MessageAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var conversationId: String? = null
    private var otherUserId: String? = null
    private var otherUserName: String? = null
    private var productId: String? = null
    private var isTyping = false

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get data from intent
        conversationId = intent.getStringExtra("CONVERSATION_ID")
        otherUserId = intent.getStringExtra("OTHER_USER_ID")
        productId = intent.getStringExtra("PRODUCT_ID")

        if (conversationId == null || otherUserId == null) {
            Toast.makeText(this, getString(R.string.error_loading_chat), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupMessageInput()
        setupAttachButton()
        loadMessages()
        loadOtherUserInfo()
        setupTypingIndicator()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.loading_messages)
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter { imageUrl ->
            // Handle image click (e.g., show full screen)
            showFullScreenImage(imageUrl)
        }
        binding.messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun setupMessageInput() {
        binding.sendButton.setOnClickListener {
            val messageText = binding.messageEditText.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                binding.messageEditText.text?.clear()
            }
        }

        binding.messageEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !isTyping) {
                isTyping = true
                updateTypingStatus(true)
            }
        }
    }

    private fun setupAttachButton() {
        binding.attachButton.setOnClickListener {
            getContent.launch("image/*")
        }
    }

    private fun uploadImage(uri: Uri) {
        val imageId = UUID.randomUUID().toString()
        val imageRef = storage.reference.child("chat_images/$imageId")
        
        // Create temporary message with upload status
        val tempMessage = Message(
            senderId = auth.currentUser?.uid ?: return,
            type = Message.TYPE_IMAGE,
            isUploading = true
        )
        
        lifecycleScope.launch {
            try {
                // Add temporary message
                val messageRef = firestore.collection("conversations")
                    .document(conversationId!!)
                    .collection("messages")
                    .add(tempMessage)
                    .await()

                // Upload image
                val uploadTask = imageRef.putFile(uri).await()
                val imageUrl = imageRef.downloadUrl.await().toString()

                // Update message with image URL
                messageRef.update(
                    mapOf(
                        "imageUrl" to imageUrl,
                        "isUploading" to false,
                        "timestamp" to com.google.firebase.Timestamp.now()
                    )
                ).await()

                // Update conversation metadata
                updateConversationMetadata(getString(R.string.image_sent))
            } catch (e: Exception) {
                Toast.makeText(
                    this@ChatActivity,
                    getString(R.string.error_uploading_image),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadMessages() {
        showLoading()
        
        firestore.collection("conversations")
            .document(conversationId!!)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    showError(getString(R.string.error_loading_messages))
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    val messages = querySnapshot.toObjects(Message::class.java)
                    messageAdapter.submitList(messages)
                    binding.messagesRecyclerView.scrollToPosition(messages.size - 1)
                    
                    // Mark messages as read
                    markMessagesAsRead(querySnapshot.documents)
                }
                
                hideLoading()
            }
    }

    private fun loadOtherUserInfo() {
        lifecycleScope.launch {
            try {
                val userDoc = firestore.collection("users")
                    .document(otherUserId!!)
                    .get()
                    .await()

                otherUserName = userDoc.getString("username")
                supportActionBar?.title = otherUserName ?: getString(R.string.chat)

                // Setup typing indicator listener
                setupTypingListener(userDoc.getString("fcmToken"))
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    private fun sendMessage(text: String) {
        val currentUser = auth.currentUser ?: return
        
        val message = Message(
            senderId = currentUser.uid,
            text = text,
            type = Message.TYPE_TEXT,
            productId = productId
        )

        lifecycleScope.launch {
            try {
                // Add message to conversation
                firestore.collection("conversations")
                    .document(conversationId!!)
                    .collection("messages")
                    .add(message)
                    .await()

                // Update conversation metadata
                updateConversationMetadata(text)

                // Reset typing indicator
                isTyping = false
                updateTypingStatus(false)

            } catch (e: Exception) {
                showError(getString(R.string.error_sending_message))
            }
        }
    }

    private suspend fun updateConversationMetadata(lastMessage: String) {
        firestore.collection("conversations")
            .document(conversationId!!)
            .set(
                hashMapOf(
                    "lastMessage" to lastMessage,
                    "lastMessageTimestamp" to com.google.firebase.Timestamp.now(),
                    "unreadCount" to mapOf(
                        otherUserId!! to (messageAdapter.currentList.size + 1)
                    )
                ),
                SetOptions.merge()
            )
            .await()
    }

    private fun markMessagesAsRead(messages: List<com.google.firebase.firestore.DocumentSnapshot>) {
        val currentUser = auth.currentUser ?: return
        val batch = firestore.batch()
        
        messages.forEach { doc ->
            val message = doc.toObject(Message::class.java)
            if (message?.senderId != currentUser.uid && message?.read == false) {
                batch.update(doc.reference, "read", true)
            }
        }

        lifecycleScope.launch {
            try {
                batch.commit().await()
                
                // Update unread count in conversation
                firestore.collection("conversations")
                    .document(conversationId!!)
                    .update("unreadCount.${currentUser.uid}", 0)
                    .await()
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    private fun setupTypingIndicator() {
        // Update typing status in Firestore
        firestore.collection("conversations")
            .document(conversationId!!)
            .collection("typing")
            .document(auth.currentUser?.uid ?: return)
            .set(mapOf("isTyping" to false))
    }

    private fun setupTypingListener(otherUserFcmToken: String?) {
        firestore.collection("conversations")
            .document(conversationId!!)
            .collection("typing")
            .document(otherUserId!!)
            .addSnapshotListener { snapshot, _ ->
                val isOtherUserTyping = snapshot?.getBoolean("isTyping") ?: false
                binding.typingIndicator.visibility = if (isOtherUserTyping) View.VISIBLE else View.GONE
                if (isOtherUserTyping) {
                    binding.typingIndicator.text = getString(R.string.user_is_typing, otherUserName)
                }
            }
    }

    private fun updateTypingStatus(isTyping: Boolean) {
        firestore.collection("conversations")
            .document(conversationId!!)
            .collection("typing")
            .document(auth.currentUser?.uid ?: return)
            .set(mapOf("isTyping" to isTyping))
    }

    private fun showFullScreenImage(imageUrl: String) {
        // TODO: Implement full screen image viewer
        Toast.makeText(this, "Full screen image viewer coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        if (isTyping) {
            isTyping = false
            updateTypingStatus(false)
        }
    }
} 
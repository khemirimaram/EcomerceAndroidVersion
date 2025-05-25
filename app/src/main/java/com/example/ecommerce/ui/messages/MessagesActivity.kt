package com.example.ecommerce.ui.messages

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.util.Log
import com.example.ecommerce.BaseActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecommerce.adapters.ConversationAdapter
import com.example.ecommerce.databinding.ActivityMessagesBinding
import com.example.ecommerce.models.Conversation
import com.example.ecommerce.LoginActivity
import com.example.ecommerce.R
import com.example.ecommerce.ECommerceApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FirebaseFirestoreException

class MessagesActivity : BaseActivity() {
    private lateinit var binding: ActivityMessagesBinding
    private lateinit var conversationAdapter: ConversationAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid
    private var conversationListener: ListenerRegistration? = null
    
    companion object {
        private const val TAG = "MessagesActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Check if user is logged in first
            if (auth.currentUser == null) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return
            }

            binding = ActivityMessagesBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupToolbar()
            setupRecyclerView()
            setupSwipeRefresh()
            setupSearch()
            super.setupBottomNavigation() // Call parent's implementation
            binding.bottomNavigation?.selectedItemId = R.id.navigation_messages
        
            loadConversations()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MessagesActivity", e)
            showError("Une erreur est survenue lors de l'initialisation")
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        title = getString(R.string.messages)
    }

    private fun setupRecyclerView() {
        conversationAdapter = ConversationAdapter { conversation ->
            startActivity(Intent(this, com.example.ecommerce.ChatActivity::class.java).apply {
                putExtra("CONVERSATION_ID", conversation.id)
                putExtra("OTHER_USER_ID", conversation.getOtherParticipantId(currentUserId ?: return@ConversationAdapter))
                conversation.productId?.let { putExtra("PRODUCT_ID", it) }
            })
        }

        binding.conversationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MessagesActivity)
            adapter = conversationAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadConversations()
        }
    }

    private fun setupSearch() {
        // TODO: Implement search functionality
    }

    private fun loadConversations() {
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        showLoading()
        
        try {
            conversationListener?.remove()
            
            val userId = currentUserId ?: return
            
            // Try to load from both collections to handle potential data location
            val conversationsQuery = firestore.collection("conversations")
                .whereArrayContains("participants", userId)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            
            conversationListener = conversationsQuery.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error loading conversations", error)
                    handleFirestoreError(error)
                    return@addSnapshotListener
                }

                lifecycleScope.launch {
                    try {
                        val conversations = snapshot?.documents?.mapNotNull { doc ->
                            try {
                                doc.toObject(Conversation::class.java)?.copy(id = doc.id)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error converting document", e)
                                null
                            }
                        }?.sortedByDescending { it.lastMessageTimestamp } ?: emptyList()

                        if (conversations.isEmpty()) {
                            showEmptyState()
                        } else {
                            showConversations(conversations)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing conversations", e)
                        showError("Erreur lors du chargement des conversations")
                    } finally {
                        hideLoading()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up listener", e)
            showError("Impossible de charger les conversations")
            hideLoading()
        }
    }

    private fun handleFirestoreError(error: FirebaseFirestoreException) {
        Log.e(TAG, "Firestore error", error)
        when (error.code) {
            FirebaseFirestoreException.Code.FAILED_PRECONDITION -> {
                showError("Erreur de configuration de la base de données. Veuillez réessayer plus tard.")
            }
            FirebaseFirestoreException.Code.UNAVAILABLE -> {
                showError("Service temporairement indisponible")
            }
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                showError("Accès non autorisé")
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            else -> showError("Une erreur est survenue")
        }
        hideLoading()
    }

    private fun showError(message: String) {
        binding.apply {
            errorTextView.text = message
            errorTextView.visibility = View.VISIBLE
            conversationsRecyclerView.visibility = View.GONE
            emptyView.root.visibility = View.GONE
        }
    }

    private fun showEmptyState() {
        binding.apply {
            conversationsRecyclerView.visibility = View.GONE
            emptyView.root.visibility = View.VISIBLE
            errorTextView.visibility = View.GONE
        }
    }

    private fun showConversations(conversations: List<Conversation>) {
        binding.apply {
            conversationsRecyclerView.visibility = View.VISIBLE
            emptyView.root.visibility = View.GONE
            errorTextView.visibility = View.GONE
            conversationAdapter.submitList(conversations)
        }
    }

    private fun showLoading() {
        binding.apply {
            progressBar.visibility = View.VISIBLE
            swipeRefreshLayout.isRefreshing = true
            errorTextView.visibility = View.GONE
        }
    }

    private fun hideLoading() {
        binding.apply {
            progressBar.visibility = View.GONE
            swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        conversationListener?.remove()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
} 
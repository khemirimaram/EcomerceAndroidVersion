package com.example.ecommerce.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecommerce.ChatActivity
import com.example.ecommerce.LoginActivity
import com.example.ecommerce.ProfileActivity
import com.example.ecommerce.R
import com.example.ecommerce.adapters.ReviewAdapter
import com.example.ecommerce.databinding.ActivityProductDetailsBinding
import com.example.ecommerce.databinding.DialogLeaveReviewBinding
import com.example.ecommerce.models.Conversation
import com.example.ecommerce.models.Product
import com.example.ecommerce.models.Review
import com.example.ecommerce.repositories.ReviewRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProductDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductDetailsBinding
    private lateinit var reviewRepository: ReviewRepository
    private lateinit var reviewAdapter: ReviewAdapter
    private var product: Product? = null
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "ProductDetailsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.product_details)
        }

        // Initialize repositories and adapters
        reviewRepository = ReviewRepository()
        reviewAdapter = ReviewAdapter()

        // Setup RecyclerView
        binding.reviewsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ProductDetailsActivity)
            adapter = reviewAdapter
        }

        // Get product ID from intent
        val productId = intent.getStringExtra("PRODUCT_ID")
        if (productId == null) {
            Log.e(TAG, "Product ID is null")
            Toast.makeText(this, "Error: Product not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load product details
        loadProduct(productId)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_product_details, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadProduct(productId: String) {
        lifecycleScope.launch {
            try {
                showLoadingState()
                Log.d(TAG, "Starting to load product with ID: $productId")
                
                // Try both collections (products and produits)
                var productDoc = firestore.collection("produits").document(productId).get().await()
                if (!productDoc.exists()) {
                    Log.d(TAG, "Product not found in 'produits' collection, trying 'products'")
                    productDoc = firestore.collection("products").document(productId).get().await()
                }

                if (!productDoc.exists()) {
                    Log.e(TAG, "Product not found in either collection")
                    Toast.makeText(this@ProductDetailsActivity, "Product not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                Log.d(TAG, "Product document found, converting to Product object")
                // Convert document to Product object
                product = Product(
                    id = productDoc.id,
                    name = productDoc.getString("name") ?: "",
                    description = productDoc.getString("description") ?: "",
                    price = productDoc.getDouble("price") ?: 0.0,
                    category = productDoc.getString("category") ?: "",
                    condition = productDoc.getString("condition") ?: "",
                    location = productDoc.getString("location") ?: "",
                    sellerId = productDoc.getString("sellerId") ?: "",
                    sellerName = productDoc.getString("sellerName") ?: "",
                    images = when (val imagesData = productDoc.get("images")) {
                        is List<*> -> imagesData.filterIsInstance<String>()
                        is String -> listOf(imagesData)
                        else -> listOf()
                    }
                )

                Log.d(TAG, "Product loaded successfully. Seller ID: ${product?.sellerId}")
                
                // Update UI
                updateUI()
                // Load reviews
                loadReviews()
                // Setup rating card after product is loaded
                setupRatingCard()
                
                hideLoadingState()

            } catch (e: Exception) {
                Log.e(TAG, "Error loading product", e)
                Toast.makeText(this@ProductDetailsActivity, "Error loading product", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun updateUI() {
        product?.let { product ->
            binding.apply {
                // Set basic product info
                tvProductName.text = product.name
                tvPrice.text = String.format(getString(R.string.price_format), product.price)
                tvDescription.text = product.description
                tvLocation.text = product.location
                tvCondition.text = product.condition
                tvCategory.text = product.category

                // Set seller info
                tvSellerName.text = product.sellerName

                // Load seller rating
                loadSellerRating(product.sellerId)

                // Show/hide action buttons based on whether the current user is the seller
                val isCurrentUserSeller = auth.currentUser?.uid == product.sellerId
                btnContact.visibility = if (isCurrentUserSeller) View.GONE else View.VISIBLE
                btnEdit.visibility = if (isCurrentUserSeller) View.VISIBLE else View.GONE
                btnReview.visibility = if (isCurrentUserSeller) View.GONE else View.VISIBLE

                // Setup action buttons
                setupActionButtons(product)
            }
        }
    }

    private fun loadSellerRating(sellerId: String) {
        lifecycleScope.launch {
            reviewRepository.getSellerRating(sellerId).onSuccess { ratingInfo ->
                binding.ratingBar.rating = ratingInfo.averageRating
                binding.tvSellerRating.text = getString(
                    R.string.seller_rating_with_count,
                    ratingInfo.averageRating,
                    ratingInfo.numberOfRatings
                )
                binding.tvSellerRating.visibility = View.VISIBLE
            }.onFailure {
                binding.tvSellerRating.visibility = View.GONE
            }
        }
    }

    private fun loadReviews() {
        val sellerId = product?.sellerId ?: return
        
        lifecycleScope.launch {
            try {
                val reviews = firestore.collection("reviews")
                    .whereEqualTo("sellerId", sellerId)
                    .get()
                    .await()
                    .toObjects(Review::class.java)

                if (reviews.isEmpty()) {
                    binding.tvNoReviews.visibility = View.VISIBLE
                    binding.reviewsRecyclerView.visibility = View.GONE
                } else {
                    binding.tvNoReviews.visibility = View.GONE
                    binding.reviewsRecyclerView.visibility = View.VISIBLE
                    reviewAdapter.submitList(reviews)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading reviews", e)
                binding.tvNoReviews.visibility = View.VISIBLE
                binding.reviewsRecyclerView.visibility = View.GONE
            }
        }
    }

    private fun showReviewDialog() {
        val dialogBinding = DialogLeaveReviewBinding.inflate(layoutInflater)
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.leave_review))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.submit)) { dialog, _ ->
                val rating = dialogBinding.ratingBar.rating
                val comment = dialogBinding.etComment.text.toString()
                
                if (rating == 0f) {
                    Toast.makeText(this, getString(R.string.please_select_rating), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                product?.let { product ->
                    lifecycleScope.launch {
                        reviewRepository.addOrUpdateReview(
                            sellerId = product.sellerId,
                            productId = product.id,
                            rating = rating,
                            comment = comment.takeIf { it.isNotBlank() }
                        ).onSuccess {
                            Toast.makeText(this@ProductDetailsActivity, getString(R.string.review_submitted), Toast.LENGTH_SHORT).show()
                            loadReviews()
                            loadSellerRating(product.sellerId)
                        }.onFailure {
                            Toast.makeText(this@ProductDetailsActivity, getString(R.string.error_submitting_review), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun setupActionButtons(product: Product) {
        // Contact seller button
        binding.btnContact.setOnClickListener {
            if (auth.currentUser == null) {
                showLoginDialog()
                return@setOnClickListener
            }

            if (auth.currentUser?.uid == product.sellerId) {
                Toast.makeText(this, getString(R.string.cannot_message_yourself), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    // Check if conversation exists
                    val existingConversation = findExistingConversation(product.sellerId)
                    
                    val conversationId = existingConversation?.id ?: createNewConversation(product)
                    
                    // Open chat activity
                    startActivity(Intent(this@ProductDetailsActivity, ChatActivity::class.java).apply {
                        putExtra("CONVERSATION_ID", conversationId)
                        putExtra("OTHER_USER_ID", product.sellerId)
                        putExtra("PRODUCT_ID", product.id)
                    })
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting conversation", e)
                    Toast.makeText(this@ProductDetailsActivity, getString(R.string.error_starting_conversation), Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Edit product button (for seller)
        binding.btnEdit.setOnClickListener {
            Toast.makeText(this, getString(R.string.edit_feature_coming_soon), Toast.LENGTH_SHORT).show()
        }

        // View Profile button
        binding.btnViewProfile.setOnClickListener {
            Log.d(TAG, "View profile button clicked")
            Log.d(TAG, "Seller ID: ${product.sellerId}")
            
            if (product.sellerId.isBlank()) {
                Log.e(TAG, "Seller ID is blank")
                Toast.makeText(this, "Error: Seller information not available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            try {
                Log.d(TAG, "Starting ProfileActivity for seller: ${product.sellerId}")
                startActivity(Intent(this, ProfileActivity::class.java).apply {
                    putExtra("USER_ID", product.sellerId)
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error opening profile", e)
                Toast.makeText(this, "Error opening profile: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun findExistingConversation(sellerId: String): Conversation? {
        val currentUserId = auth.currentUser?.uid ?: return null
        
        return try {
            firestore.collection("conversations")
                .whereArrayContains("participants", currentUserId)
                .get()
                .await()
                .toObjects(Conversation::class.java)
                .find { conversation ->
                    conversation.participants.containsAll(listOf(currentUserId, sellerId))
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding existing conversation", e)
            null
        }
    }

    private suspend fun createNewConversation(product: Product): String {
        val currentUser = auth.currentUser ?: throw IllegalStateException("User not logged in")
        
        val conversation = hashMapOf(
            "participants" to listOf(currentUser.uid, product.sellerId),
            "participantNames" to mapOf(
                currentUser.uid to (currentUser.displayName ?: "Anonymous"),
                product.sellerId to product.sellerName
            ),
            "lastMessage" to "",
            "lastMessageTimestamp" to null,
            "unreadCount" to mapOf(
                currentUser.uid to 0,
                product.sellerId to 0
            ),
            "productId" to product.id,
            "productName" to product.name
        )
        
        return firestore.collection("conversations")
            .add(conversation)
            .await()
            .id
    }

    private fun showLoginDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.login_required))
            .setMessage(getString(R.string.login_to_message))
            .setPositiveButton(getString(R.string.sign_in)) { _, _ ->
                startActivity(Intent(this, LoginActivity::class.java))
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun setupRatingCard() {
        Log.d(TAG, "Setting up rating card. Product: ${product?.id}, Seller: ${product?.sellerId}")
        
        // Check if product is loaded
        if (product == null) {
            Log.e(TAG, "Product is null when setting up rating card")
            return
        }
        
        binding.apply {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val isCurrentUserSeller = currentUser?.uid == product?.sellerId
            
            Log.d(TAG, "Rating card setup - Current user: ${currentUser?.uid}")
            Log.d(TAG, "Rating card setup - Product seller: ${product?.sellerId}")
            Log.d(TAG, "Rating card setup - Is current user seller: $isCurrentUserSeller")
            
            // Additional auth state check
            if (currentUser == null) {
                Log.d(TAG, "Rating card hidden - User not logged in")
                ratingCard.visibility = View.GONE
                return@apply
            }
            
            if (isCurrentUserSeller) {
                Log.d(TAG, "Rating card hidden - Current user is the seller")
                ratingCard.visibility = View.GONE
            } else {
                Log.d(TAG, "Rating card shown - User can review this product")
                ratingCard.visibility = View.VISIBLE
            }

            submitRatingButton.setOnClickListener {
                val rating = ratingBar.rating
                if (rating == 0f) {
                    showError(getString(R.string.please_select_rating))
                    return@setOnClickListener
                }

                val comment = commentEditText.text.toString().trim()
                submitRating(rating, comment)
            }
        }
    }

    private fun submitRating(rating: Float, comment: String) {
        showLoadingState()
        
        lifecycleScope.launch {
            try {
                val result = reviewRepository.addOrUpdateReview(
                    sellerId = product?.sellerId ?: return@launch,
                    productId = product?.id ?: return@launch,
                    rating = rating,
                    comment = comment
                )
                
                result.onSuccess {
                    // Réinitialiser le formulaire
                    binding.apply {
                        ratingBar.rating = 0f
                        commentEditText.text?.clear()
                    }
                    
                    // Recharger les avis
                    loadReviews()
                    loadSellerRating(product?.sellerId ?: return@launch)
                    showMessage(getString(R.string.review_submitted))
                }.onFailure { exception ->
                    Log.e(TAG, "Error submitting review", exception)
                    showError(getString(R.string.error_submitting_review))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting review", e)
                showError(getString(R.string.error_submitting_review))
            } finally {
                hideLoadingState()
            }
        }
    }

    private fun showLoadingState() {
        binding.apply {
            // Disable all interactive elements
            submitRatingButton?.isEnabled = false
            ratingBar?.isEnabled = false
            commentEditText?.isEnabled = false
            btnContact.isEnabled = false
            btnEdit.isEnabled = false
            btnReview.isEnabled = false
            
            // Show loading indicator if you have one
            // progressBar.visibility = View.VISIBLE
        }
    }

    private fun hideLoadingState() {
        binding.apply {
            // Enable all interactive elements
            submitRatingButton?.isEnabled = true
            ratingBar?.isEnabled = true
            commentEditText?.isEnabled = true
            btnContact.isEnabled = true
            btnEdit.isEnabled = true
            btnReview.isEnabled = true
            
            // Hide loading indicator if you have one
            // progressBar.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
} 